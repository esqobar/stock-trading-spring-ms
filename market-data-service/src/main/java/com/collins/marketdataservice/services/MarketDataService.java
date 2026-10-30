package com.collins.marketdataservice.services;

import com.collins.marketdataservice.entities.Stock;
import com.collins.marketdataservice.payloads.StockPriceResponse;

import java.util.List;

public interface MarketDataService {
    List<StockPriceResponse> getAllPrices();

    StockPriceResponse getPrice(String symbol);

    List<Stock> getAllStocks();
}
