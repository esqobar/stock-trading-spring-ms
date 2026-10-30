package com.collins.marketdataservice.services.impl;

import com.collins.marketdataservice.entities.Stock;
import com.collins.marketdataservice.payloads.StockPriceResponse;
import com.collins.marketdataservice.repositories.MarketDataRepository;
import com.collins.marketdataservice.services.MarketDataService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
@Order(2)
public class MarketDataServiceImpl implements MarketDataService, CommandLineRunner {

    private final MarketDataRepository marketDataRepository;
    private final RedisTemplate<String, StockPriceResponse> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Random random = new Random();
    private static final String PRICE_KEY_PREFIX = "stock.price";
    private static final String PRICE_UPDATED_TOPIC = "stock.price.updated";

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * ConcurrentHashMap - for thread safety
     */
    private final Map<String, StockPriceResponse> stockPrices = new ConcurrentHashMap<>();

    /**
     * Initialize Stock Prices
     *
     * @PostConstruct -> runs when context is ready
     */
    @Override
    public void run(String...args) {
        log.info("Loading stock data....");

        List<Stock> stocks = marketDataRepository.findByActiveTrue();
        if (stocks.isEmpty()) {
            log.info("No active stocks found");
            return;
        }

        stocks.forEach(stock -> {
            StockPriceResponse stockPriceResponse = StockPriceResponse.builder()
                    .symbol(stock.getSymbol())
                    .companyName(stock.getCompanyName())
                    .price(stock.getInitialPrice())
                    .open(stock.getInitialPrice())
                    .high(stock.getInitialPrice())
                    .low(stock.getInitialPrice())
                    .change(BigDecimal.ZERO)
                    .changePercent(BigDecimal.ZERO)
                    .volume(0L)
                    .timestamp(LocalDateTime.now())
                    .build();

            stockPrices.put(stock.getSymbol(), stockPriceResponse);

            log.info("Loaded Stock: {} at {}", stock.getSymbol(), stock.getInitialPrice());
        });

        log.info("Initialized {} stocks", stockPrices.size());
    }


    /**
     * Scheduled Price Update - every 2 seconds
     */
    @Scheduled(fixedDelayString = "${market.price-update-interval}")
    public void updatePrices() {
        if (stockPrices.isEmpty()) {
            log.info("No stocks loaded");
            return;
        }

        stockPrices.forEach((symbol, currentPrice) -> {
            BigDecimal oldPrice = currentPrice.getPrice();

            // +- 0.5 price movement
            double prizeMovement  = (random.nextDouble() - 0.5) * 0.5;

            BigDecimal priceChange = oldPrice.multiply(BigDecimal.valueOf(prizeMovement/100))
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal newPrice = oldPrice.add(priceChange)
                    .setScale(2, RoundingMode.HALF_UP);

            // Preventing negative price
            if (newPrice.compareTo(BigDecimal.valueOf(1)) < 0) {
                newPrice = BigDecimal.valueOf(1);
            }

            BigDecimal changeAmount = oldPrice.subtract(currentPrice.getOpen())
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal changePercent = changeAmount.divide(currentPrice.getOpen(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);

            // Updating High/Low
            BigDecimal newHigh = newPrice.compareTo(currentPrice.getHigh()) > 0 ? newPrice : currentPrice.getHigh();
            BigDecimal newLow = newPrice.compareTo(currentPrice.getHigh()) < 0 ? newPrice : currentPrice.getLow();

            currentPrice.setPrice(newPrice);
            currentPrice.setChange(changeAmount);
            currentPrice.setChangePercent(changePercent);
            currentPrice.setHigh(newHigh);
            currentPrice.setLow(newLow);
            currentPrice.setTimestamp(LocalDateTime.now());

            // 1. Cache in Redis
            redisTemplate.opsForValue().set(PRICE_KEY_PREFIX + symbol, currentPrice);

            // 2. Broadcast via Websocket -> All Clients
            messagingTemplate.convertAndSend("/topic/prices" + symbol, currentPrice);

            // Publishing to Kafka
            Map<String, Object> priceUpdatedEvent = new HashMap<>();
            priceUpdatedEvent.put("symbol", symbol);
            priceUpdatedEvent.put("price", newPrice);
            priceUpdatedEvent.put("timestamp",LocalDateTime.now().toString());

            kafkaTemplate.send(PRICE_UPDATED_TOPIC,symbol, priceUpdatedEvent);
        });

        log.debug("Prices updated for {} stocks", stockPrices.size());
    }

    /**
     * Get all active stock prices. */
    @Override
    public List<StockPriceResponse> getAllPrices() {
        return stockPrices.values().stream().toList();
    }

    /**
     * Get current price for specific stock. */
    @Override
    public StockPriceResponse getPrice(String symbol) {

        String cacheKey = PRICE_KEY_PREFIX + symbol.toUpperCase();

        // 1. Check Redis cache
        StockPriceResponse cached = redisTemplate.opsForValue().get(cacheKey);

        if (cached != null) {
            log.info("Returning cached stock price for {}", symbol);
            return cached;
        }

        // 2. Fallback to in-memory stock prices
        StockPriceResponse price = stockPrices.get(symbol.toUpperCase());

        if (price == null) {
            throw new RuntimeException("No stock found: " + symbol);
        }

        // 3. Cache the result
        redisTemplate.opsForValue().set(cacheKey, price);

        return price;
    }

    /**
     * Get all stocks. */
    @Override
    public List<Stock> getAllStocks() {
        return marketDataRepository.findAll();
    }
}
