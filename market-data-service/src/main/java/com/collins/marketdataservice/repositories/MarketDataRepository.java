package com.collins.marketdataservice.repositories;

import com.collins.marketdataservice.entities.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MarketDataRepository extends JpaRepository<Stock, String> {
    List<Stock> findByActiveTrue();
}
