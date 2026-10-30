package com.collins.orderservice.resources;

import com.collins.orderservice.entities.Order;
import com.collins.orderservice.payloads.requests.OrderRequest;
import com.collins.orderservice.services.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<Order> placeOrder(@RequestHeader("X-User-Id") String userId,
                                            @Valid @RequestBody OrderRequest request) {
        request.setUserId(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.placeOrder(request));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrder(@RequestHeader("X-User-Id") String userId,
                                            @PathVariable String orderId) {
        Order order = orderService.getOrder(orderId);

        // Security Check
        if (!order.getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.status(HttpStatus.OK).body(order);
    }

    public ResponseEntity<List<Order>> getMyOrders(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.status(HttpStatus.OK).body(orderService.getMyOrders(userId));
    }
}
