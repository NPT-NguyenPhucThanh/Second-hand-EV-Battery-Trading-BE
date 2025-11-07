package com.project.tradingev_batter.config;

import com.project.tradingev_batter.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, UserDetailsService userDetailsService) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(withDefaults())
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // === PUBLIC ENDPOINTS (GUEST) ===
                        .requestMatchers("/api/auth/**").permitAll()

                        // === PUBLIC API - Guest có thể xem products, sellers (không cần /api/guest) ===
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/sellers/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()

                        // === SWAGGER/OPENAPI ENDPOINTS ===
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()

                        // === WEBSOCKET ENDPOINTS ===
                        .requestMatchers("/ws-chat/**").permitAll() // WebSocket endpoint

                        // === DOCUSEAL WEBHOOK (MUST BE PUBLIC) ===
                        .requestMatchers("/api/docuseal/webhook").permitAll()
                        .requestMatchers("/api/docuseal/**").permitAll()

                        // === VNPAY CALLBACK (MUST BE PUBLIC) ===
                        .requestMatchers("/api/payment/vnpay-return").permitAll()
                        .requestMatchers("/api/payment/vnpay-ipn").permitAll()
                        .requestMatchers("/api/payment/mock-payment").permitAll()

                        // === MANAGER ENDPOINTS (Admin only - highest privilege) ===
                        .requestMatchers("/api/manager/**").hasRole("MANAGER")

                        // === STAFF ENDPOINTS (Operations - duyệt xe, kiểm định, kho, tranh chấp) ===
                        .requestMatchers("/api/staff/**").hasAnyRole("STAFF", "MANAGER")

                        // === SELLER ENDPOINTS ===
                        .requestMatchers("/api/seller/**").hasAnyRole("SELLER", "MANAGER")

                        // === BUYER ENDPOINTS (CLIENT đã được thay bằng BUYER) ===
                        .requestMatchers("/api/buyer/**").hasAnyRole("BUYER", "SELLER", "MANAGER")

                        // === CLIENT CONTROLLER - Dùng cho tất cả authenticated users ===
                        .requestMatchers("/api/client/**").authenticated()

                        // === CHAT ENDPOINTS - Tất cả authenticated users ===
                        .requestMatchers("/api/chat/**").authenticated()

                        // === NOTIFICATION ENDPOINTS ===
                        .requestMatchers("/api/notifications/**").authenticated()

                        // Tất cả các request khác yêu cầu authenticated
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173")); // FE URLs
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}