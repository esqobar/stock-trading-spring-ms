package com.collins.marketdataservice.configs;

import com.collins.marketdataservice.entities.Stock;
import com.collins.marketdataservice.repositories.MarketDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
@Order(1)
public class DataInitializer implements CommandLineRunner {
    private final MarketDataRepository marketDataRepository;

    @Override
    public void run(String... args) throws Exception {

        if(marketDataRepository.count() > 0) {
            log.info("Stocks already initialized - skipping");
            return;
        }

        log.info("Initializing stock data....");

        List<Stock> stocks = List.of(
                new Stock("AAPL", "Apple Inc", BigDecimal.valueOf(189.50), "NASDAQ", "USD", true),
                new Stock("GOOGL", "Alphabet Inc", BigDecimal.valueOf(141.80), "NASDAQ", "USD", true),
                new Stock("MSFT", "Microsoft Corp", BigDecimal.valueOf(378.90), "NASDAQ", "USD", true),
                new Stock("TCS", "Tata Consultancy Service", BigDecimal.valueOf(2850.00), "NSE", "INR", true),
                new Stock("INFY", "Infosys", BigDecimal.valueOf(1650.00), "NSE", "INR", true),
                new Stock("RELIANCE", "Reliance Industries", BigDecimal.valueOf(3920.00), "NSE", "INR", true)
        );

        marketDataRepository.saveAll(stocks);
        log.info("Stocks {} initialized successfully", stocks.size());
    }
}
