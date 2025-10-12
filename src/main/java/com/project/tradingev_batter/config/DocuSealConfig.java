package com.project.tradingev_batter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

//Configuration cho DocuSeal Integration
@Configuration
@ConfigurationProperties(prefix = "docuseal")
@Data
public class DocuSealConfig {
    
    /**
     * API Key từ DocuSeal
     * Lấy từ DocuSeal Dashboard -> Settings -> API
     */
    private Api api;
    
    /**
     * Template IDs cho các loại hợp đồng
     */
    private Template template;
    
    /**
     * Webhook configuration
     */
    private Webhook webhook;
    
    @Data
    public static class Api {
        /**
         * DocuSeal API Key
         */
        private String key;
        
        /**
         * Base URL của DocuSeal API
         * VD: https://docuseal.com hoặc https://your-self-hosted-domain.com
         */
        private String baseUrl;
    }
    
    @Data
    public static class Template {
        /**
         * Template ID cho hợp đồng đăng bán (Seller ký với hệ thống)
         */
        private String productListing;
        
        /**
         * Template ID cho hợp đồng mua bán (Buyer ký với Seller)
         */
        private String saleTransaction;
    }
    
    @Data
    public static class Webhook {
        /**
         * Public URL để nhận webhook callback từ DocuSeal
         * VD: https://yourdomain.com/api/docuseal/webhook
         */
        private String url;
    }
    
    /**
     * Bean RestTemplate để gọi DocuSeal API
     * Cấu hình timeout 30 giây
     */
    @Bean(name = "docuSealRestTemplate")
    public RestTemplate docuSealRestTemplate() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(30000); // 30 seconds
        factory.setConnectionRequestTimeout(30000);
        return new RestTemplate(factory);
    }
}
