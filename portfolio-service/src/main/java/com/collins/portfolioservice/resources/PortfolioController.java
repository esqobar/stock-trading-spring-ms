package com.collins.portfolioservice.resources;

import com.collins.portfolioservice.services.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/portfolios")
@RequiredArgsConstructor
public class PortfolioController {
    private final PortfolioService portfolioService;

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMyPortfolio(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(portfolioService.getMyPortfolio(userId));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getPortfolio(@PathVariable String userId) {
        return ResponseEntity.ok(portfolioService.getPortfolio(userId));
    }
}
