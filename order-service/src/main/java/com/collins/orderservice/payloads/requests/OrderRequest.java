package com.collins.orderservice.payloads.requests;

import com.collins.orderservice.entities.enums.OrderType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderRequest {

    private String userId;

    @NotBlank(message = "Stock symbol is required")
    private String symbol;

    @NotNull(message = "Order type is required - BUY or SELL")
    private OrderType type;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;
}
