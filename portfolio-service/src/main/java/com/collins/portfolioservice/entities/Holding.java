package com.collins.portfolioservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "holdings", uniqueConstraints = @UniqueConstraint(
        columnNames = {"user_id", "symbol"}
))
public class Holding {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "symbol", nullable = false)
    private String symbol;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "quantity")
    private int quantity;

    @Column(name = "average_buy_price", precision = 15, scale = 2)
    private BigDecimal averageBuyPrice;

    @Column(name = "total_invested", precision = 15, scale = 2)
    private BigDecimal totalInvested;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
