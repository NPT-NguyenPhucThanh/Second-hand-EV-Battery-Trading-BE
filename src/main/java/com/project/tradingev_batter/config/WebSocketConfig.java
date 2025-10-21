package com.project.tradingev_batter.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

//WebSocket Configuration cho Chat real-time
//Sử dụng STOMP protocol over WebSocket
//Timeout 5 phút (300 giây)

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
        // Create TaskScheduler for heartbeat
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(1);
        taskScheduler.setThreadNamePrefix("ws-heartbeat-");
        taskScheduler.initialize();

        // Enable simple broker cho /topic và /queue
        config.enableSimpleBroker("/topic", "/queue")
                // Set heartbeat: server gửi heartbeat mỗi 25 giây, client expected mỗi 25 giây
                .setHeartbeatValue(new long[]{25000, 25000})
                .setTaskScheduler(taskScheduler); // Set TaskScheduler cho heartbeat

        // Prefix cho messages gửi từ client đến server
        config.setApplicationDestinationPrefixes("/app");
        
        // Prefix cho user-specific destinations
        config.setUserDestinationPrefix("/user");
    }

    //Register STOMP endpoints
    //Client sẽ connect đến endpoint này
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint: ws://localhost:8080/ws-chat
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*") // Cho phép tất cả origins (config lại cho production)
                .withSockJS(); // Fallback option nếu WebSocket không support
    }

    // Configure WebSocket transport settingsSPRINT
    // Set timeout 5 phút (300 giây)
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration
                .setMessageSizeLimit(512 * 1024) // 512 KB (cho file attachments)
                .setSendBufferSizeLimit(1024 * 1024) // 1 MB
                .setSendTimeLimit(300 * 1000); // 5 phút timeout (300 giây)
    }
}
