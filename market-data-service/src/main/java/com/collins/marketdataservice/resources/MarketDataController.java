package com.collins.marketdataservice.resources;

import com.collins.marketdataservice.entities.Stock;
import com.collins.marketdataservice.payloads.StockPriceResponse;
import com.collins.marketdataservice.services.MarketDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/markets")
@Slf4j
@RequiredArgsConstructor
public class MarketDataController {
    private final MarketDataService marketDataService;

    @GetMapping("/stocks")
    public ResponseEntity<List<StockPriceResponse>> getAllStocks() {
        return ResponseEntity.status(HttpStatus.OK).body(marketDataService.getAllPrices());
    }

    @GetMapping("/stocks/{symbol}")
    public ResponseEntity<StockPriceResponse> getStockPrice(@PathVariable("symbol") String symbol) {
        return ResponseEntity.status(HttpStatus.OK).body(marketDataService.getPrice(symbol.toUpperCase()));
    }

    @GetMapping("/stocks/list")
    public ResponseEntity<List<Stock>> getStockList() {
        return ResponseEntity.status(HttpStatus.OK).body(marketDataService.getAllStocks());
    }

}
