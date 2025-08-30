package com.company.jpos.security;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        try {
            String authHeader = request.getHeader("Authorization");
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                
                // Simple token validation (implement JWT validation in production)
                if (isValidToken(token)) {
                    logger.debug("Valid token for request: {}", request.getRequestURI());
                } else {
                    logger.warn("Invalid token for request: {}", request.getRequestURI());
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
            }
            
        } catch (Exception e) {
            logger.error("JWT filter error", e);
        }
        
        filterChain.doFilter(request, response);
    }
    
    private boolean isValidToken(String token) {
        // Simple validation for demo (implement proper JWT validation in production)
        return token != null && token.length() > 10;
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Skip JWT filter for health checks and metrics
        return path.startsWith("/health") || 
               path.startsWith("/ready") || 
               path.startsWith("/metrics") ||
               path.startsWith("/actuator");
    }
}