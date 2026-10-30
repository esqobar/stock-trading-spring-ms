package com.collins.marketdataservice.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "stocks")
public class Stock {
    @Id
    @Column(nullable = false, unique = true)
    private String symbol;

    @Column(nullable = false)
    private String companyName;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal initialPrice;

    @Column(nullable = false)
    private String exchange; // NSE, NASDAQ

    @Column(nullable = false)
    private String currency; //USD,GBP, EUR

    private boolean active = true;
}
