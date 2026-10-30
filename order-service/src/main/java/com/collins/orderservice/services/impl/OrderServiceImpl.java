package com.collins.orderservice.services.impl;

import com.collins.orderservice.client.MarketDataClient;
import com.collins.orderservice.client.UserServiceClient;
import com.collins.orderservice.entities.Order;
import com.collins.orderservice.entities.enums.OrderStatus;
import com.collins.orderservice.entities.enums.OrderType;
import com.collins.orderservice.payloads.requests.OrderRequest;
import com.collins.orderservice.repositories.OrderRepository;
import com.collins.orderservice.services.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final MarketDataClient marketDataClient;
    private final UserServiceClient userServiceClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String ORDER_EXECUTED_TOPIC = "order.executed";
    private static final String ORDER_FAILED_TOPIC = "order.failed";

    /**Place a BUY or SELL order
     *
     * FLOW:
     * 1. Get the current price from the market
     * 2. Calculate total amount
     * 3. Execute
     * 4. Publish order event to kafka
     * */
    @Override
    public Order placeOrder(OrderRequest request) {
        log.info("Placing {} order : {} X {} for user: {}", request.getType(),
                request.getQuantity(), request.getSymbol(), request.getUserId());

        // Step 1
        Map<String,Object> priceData = marketDataClient.getStockPrice(request.getSymbol());

        BigDecimal currentPrice = new BigDecimal(priceData.get("price").toString());

        BigDecimal totalAmount = currentPrice.multiply(BigDecimal.valueOf(request.getQuantity()));

        // Creating order
        Order order = Order.builder()
                .userId(request.getUserId())
                .symbol(request.getSymbol())
                .type(request.getType())
                .quantity(request.getQuantity())
                .price(currentPrice)
                .totalAmount(totalAmount)
                .status(OrderStatus.PENDING)
                .build();

        Order savedOrder = orderRepository.save(order);
        log.info("Order created : {}", savedOrder.getId());

        try {
            if (request.getType() == OrderType.BUY) {
                userServiceClient.deductFunds(request.getUserId(), totalAmount);
            } else {
                userServiceClient.creditFunds(request.getUserId(), totalAmount);
            }

            savedOrder.setStatus(OrderStatus.EXECUTED);
            savedOrder.setExecutedAt(LocalDateTime.now());
            orderRepository.save(savedOrder);

            log.info("Order EXECUTED: {} {} x {} at {}", request.getType(), request.getQuantity(),
                    request.getSymbol(), currentPrice);

            publishedOrderEvent(ORDER_EXECUTED_TOPIC, savedOrder, null);

        } catch (Exception e) {
            savedOrder.setStatus(OrderStatus.FAILED);
            savedOrder.setFailureReason(e.getMessage());
            orderRepository.save(savedOrder);

            log.error("Order FAILED: {} reason: {}",
                    savedOrder.getId(), e.getMessage());

            publishedOrderEvent(ORDER_FAILED_TOPIC, savedOrder, e.getMessage());
        }
        return savedOrder;
    }

    private void publishedOrderEvent(String topic, Order order, String reason) {
        Map<String, Object> orderEvent = new HashMap<>();
        orderEvent.put("orderId", order.getId());
        orderEvent.put("userId", order.getUserId());
        orderEvent.put("symbol", order.getSymbol());
        orderEvent.put("type", order.getType().name());
        orderEvent.put("quantity", order.getQuantity());
        orderEvent.put("price", order.getPrice());
        orderEvent.put("totalAmount", order.getTotalAmount());
        orderEvent.put("status", order.getStatus().name());
        if (reason != null) orderEvent.put("failureReason", reason);

        kafkaTemplate.send(topic, order.getId(), orderEvent);

        log.info("Event published: {} for order: {}", topic, order.getId());

    }

    @Override
    public Order getOrder(String orderId) {
        return orderRepository.findById(orderId).orElseThrow(()
        -> new RuntimeException("Order not found for orderId: " + orderId));
    }

    @Override
    public List<Order> getMyOrders(String userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
