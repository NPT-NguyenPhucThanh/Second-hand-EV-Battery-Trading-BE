package com.project.tradingev_batter.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    // Danh sách các endpoint public không cần JWT
    private static final List<String> PUBLIC_ENDPOINTS = Arrays.asList(
            "/api/auth/login",
            "/api/auth/register",
            "/api/guest",
            "/swagger-ui",
            "/v3/api-docs",
            "/ws",
            "/api/payment/vnpay-callback",
            "/api/docuseal/webhook"
    );

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    //hàm callback của framework, mỗi request sẽ gọi hàm này 1 lần
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String requestPath = request.getRequestURI();

        // Log ALL requests to /api/staff and /api/buyer
        if (requestPath.startsWith("/api/staff") || requestPath.startsWith("/api/buyer")) {
            System.out.println("=== FILTER RECEIVED REQUEST ===");
            System.out.println("URI: " + requestPath);
            System.out.println("Method: " + request.getMethod());
            System.out.println("Has Auth Header: " + (request.getHeader("Authorization") != null));
            System.out.println("================================");
        }

        // Bypass JWT validation cho public endpoints
        if (isPublicEndpoint(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");

        // Nếu không có Authorization header, tiếp tục filter chain
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("=== NO AUTH HEADER ===");
            System.out.println("URI: " + requestPath);
            System.out.println("Header value: " + authHeader);
            System.out.println("=======================");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authHeader.substring(7); //"Bearer ".length() = 7
            final String username = jwtService.extractUsername(jwt);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                //Log authorities
                System.out.println("=== JWT Auth Debug ===");
                System.out.println("Username: " + username);
                System.out.println("Authorities: " + userDetails.getAuthorities());
                System.out.println("Request URI: " + request.getRequestURI());
                System.out.println("======================");

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (ExpiredJwtException e) {
            // Token hết hạn - log và tiếp tục (sẽ bị 401 từ Spring Security)
            System.out.println("JWT Token đã hết hạn: " + e.getMessage());
        } catch (Exception e) {
            // Các lỗi JWT khác
            System.out.println("Lỗi xử lý JWT: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Kiểm tra xem endpoint có phải là public endpoint không
     */
    private boolean isPublicEndpoint(String requestPath) {
        return PUBLIC_ENDPOINTS.stream().anyMatch(requestPath::startsWith);
    }
}
