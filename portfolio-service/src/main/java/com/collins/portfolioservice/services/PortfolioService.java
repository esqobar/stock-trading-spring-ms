package com.collins.portfolioservice.services;

import java.util.Map;

public interface PortfolioService {
    Map<String, Object> getPortfolio(String userId);

    Map<String, Object> getMyPortfolio(String userId);
}
