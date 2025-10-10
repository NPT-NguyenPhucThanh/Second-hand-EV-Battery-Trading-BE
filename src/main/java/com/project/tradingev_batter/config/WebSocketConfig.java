package com.project.tradingev_batter.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

//WebSocket Configuration cho Chat real-time
//Sử dụng STOMP protocol over WebSocket

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configure message broker
     * - /topic: broadcast messages (public channels)
     * - /queue: point-to-point messages (private chat)
     * - /app: application destination prefix
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple broker cho /topic và /queue
        config.enableSimpleBroker("/topic", "/queue");
        
        // Prefix cho messages gửi từ client đến server
        config.setApplicationDestinationPrefixes("/app");
        
        // Prefix cho user-specific destinations
        config.setUserDestinationPrefix("/user");
    }

    /**
     * Register STOMP endpoints
     * Client sẽ connect đến endpoint này
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint: ws://localhost:8080/ws-chat
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*") // Cho phép tất cả origins (config lại cho production)
                .withSockJS(); // Fallback option nếu WebSocket không support
    }
}
