package com.collins.portfolioservice.services;

import com.collins.portfolioservice.entities.Holding;
import com.collins.portfolioservice.repositories.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class PortfolioEventConsumer {

    private final PortfolioRepository portfolioRepository;

    @KafkaListener(topics = "order.executed")
    public void consumeOrderExecuted(@Payload Map<String, Object> payload) {

        try {
            String userId = (String) payload.get("userId");
            String symbol = (String) payload.get("symbol");
            String type = (String) payload.get("type");

            int quantity = Integer.parseInt(payload.get("quantity").toString());

            BigDecimal price = new BigDecimal(payload.get("price").toString());

            log.info("Updating portfolio - user: {} {} X {} at {}", userId, symbol, type, price);

            if ("BUY".equals(type)) {
                addHolding(userId, symbol, quantity, price);
            } else if ("SELL".equals(type)) {
                removeHolding(userId, symbol, quantity);
            }
        } catch (Exception e) {
            log.error("Error updating portfolio: {}", e.getMessage());
        }
    }


    /**
     * Add or Update after BUY order
     * Calculate new average buy price
     * @param userId
     * @param symbol
     * @param quantity
     * @param price
     */
    private void addHolding(String userId, String symbol, int quantity, BigDecimal price) {

        Holding holding = portfolioRepository.findByUserIdAndSymbol(userId, symbol)
                .orElse(null);

        if (holding == null) {
            // First time buying this stock
            holding = Holding.builder()
                    .userId(userId)
                    .symbol(symbol)
                    .companyName(symbol)
                    .quantity(quantity)
                    .averageBuyPrice(price)
                    .totalInvested(price.multiply(BigDecimal.valueOf(quantity)))
                    .build();
        } else {
            // Already holding this stock = update average price
            int newQuantity = quantity + holding.getQuantity();
            BigDecimal newInvested = holding.getTotalInvested()
                    .add(price.multiply(BigDecimal.valueOf(quantity)));

            BigDecimal newAvgPrice = newInvested.divide(
                    BigDecimal.valueOf(newQuantity),
                    2,
                    BigDecimal.ROUND_HALF_UP
            );

            holding.setQuantity(newQuantity);
            holding.setTotalInvested(newInvested);
            holding.setAverageBuyPrice(newAvgPrice);
        }

        portfolioRepository.save(holding);
        log.info("Holding updated: {} {} shares of {}", userId, holding.getQuantity(), symbol);
    }


    private void removeHolding(String userId, String symbol, int quantity) {

        Holding holding = portfolioRepository.findByUserIdAndSymbol(userId, symbol)
                .orElseThrow(() -> new RuntimeException("Holding not found: " + symbol));

        if (holding.getQuantity() < quantity) {
            throw new RuntimeException("Insufficient shares to SELL");
        }

        int newQuantity = holding.getQuantity() - quantity;
        if (newQuantity == 0) {
            portfolioRepository.delete(holding);
            log.info("Holding removed: {} sold all {} shares", userId, symbol);
        } else {
            BigDecimal newInvested = holding.getAverageBuyPrice()
                    .multiply(BigDecimal.valueOf(newQuantity));

            holding.setQuantity(newQuantity);
            holding.setTotalInvested(newInvested);

            portfolioRepository.save(holding);
            log.info("Holding reduced: {} now has {} shared of {}", userId, newQuantity, symbol);
        }
    }
}
