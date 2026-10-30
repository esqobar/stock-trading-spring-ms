package com.collins.orderservice.services;

import com.collins.orderservice.entities.Order;
import com.collins.orderservice.payloads.requests.OrderRequest;
import jakarta.validation.Valid;

import java.util.List;

public interface OrderService {
    Order placeOrder(@Valid OrderRequest request);

    Order getOrder(String orderId);

    List<Order> getMyOrders(String userId);
}
