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

                        // === Guest API endpoints - không cần authentication ===
                        .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()

                        // === Guest có thể xem danh sách, tìm kiếm, chi tiết sản phẩm ===
                        .requestMatchers(HttpMethod.GET, "/api/guest/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/guest/sellers/**").permitAll()
                        
                        // === SWAGGER/OPENAPI ENDPOINTS ===
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()

                        // === WEBSOCKET ENDPOINTS ===
                        .requestMatchers("/ws-chat/**").permitAll() // WebSocket endpoint
                        
                        // === DOCUSEAL WEBHOOK (MUST BE PUBLIC) ===
                        .requestMatchers("/api/docuseal/webhook").permitAll() // DocuSeal callback
                        .requestMatchers("/api/docuseal/**").permitAll()  // Public cho toàn bộ webhook
                        .requestMatchers("/api/docuseal/webhook/test").permitAll()  //cụ thể cho /test
                        
                        // === VNPAY CALLBACK (MUST BE PUBLIC) ===
                        .requestMatchers("/api/payment/vnpay-return").permitAll()
                        .requestMatchers("/api/payment/vnpay-ipn").permitAll()
                        .requestMatchers("/api/payment/mock-payment").permitAll()

                        // === MANAGER ENDPOINTS ===
                        .requestMatchers("/api/manager/**").hasRole("MANAGER")
                        
                        // === SELLER ENDPOINTS ===
                        .requestMatchers("/api/seller/**").hasAnyRole("SELLER", "MANAGER")
                        
                        // === CLIENT/BUYER ENDPOINTS ===
                        .requestMatchers("/api/client/**").hasAnyRole("CLIENT", "SELLER", "MANAGER")
                        .requestMatchers("/api/buyer/**").hasAnyRole("CLIENT", "SELLER", "MANAGER")
                        
                        // === CHAT ENDPOINTS ===
                        .requestMatchers("/api/chat/**").authenticated()
                        
                        // Tất cả các request khác cần authenticated
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
