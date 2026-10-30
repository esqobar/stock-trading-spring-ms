package com.collins.marketdataservice.configs;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * How it works:
 * 1. Client connects to /ws endpoint
 * 2. Client subscribes to /topic/prices/{symbol}
 * 3. Server publishes price updates to that topic
 * 4. Client receives updated in real time - no polling needed
 *
 * This is exactly how Zerodha/ Robinhood / Groww stream realtime prices
 * */

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Topics prefixed with /topic -> broadcast to all subscribers
        registry.enableSimpleBroker("/topic");

        // Application destination prefix
        // Client message to /app/** are routed to @MessageMapping methods
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Websocket endpoint - clients connect here
        // SockJs fallback for browsers that don't support websocket

        registry.addEndpoint("/ws").setAllowedOrigins("*").withSockJS();
    }
}
