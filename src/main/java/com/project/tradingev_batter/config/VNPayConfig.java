package com.project.tradingev_batter.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class VNPayConfig {
    
    @Value("${vnpay.tmn-code}")
    private String tmnCode;
    
    @Value("${vnpay.hash-secret}")
    private String hashSecret;
    
    @Value("${vnpay.api-url}")
    private String apiUrl;
    
    @Value("${vnpay.return-url}")
    private String returnUrl;
    
    @Value("${vnpay.ipn-url}")
    private String ipnUrl;
    
    @Value("${vnpay.version}")
    private String version;
    
    @Value("${vnpay.command}")
    private String command;
    
    @Value("${vnpay.order-type}")
    private String orderType;
    
    @Value("${vnpay.currency-code}")
    private String currencyCode;
    
    @Value("${vnpay.locale}")
    private String locale;
    
    @Value("${vnpay.timeout-minutes}")
    private int timeoutMinutes;
    
    @Value("${vnpay.mock-mode}")
    private boolean mockMode;
}
