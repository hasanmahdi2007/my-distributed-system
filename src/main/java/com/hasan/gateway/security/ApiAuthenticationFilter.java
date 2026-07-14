package com.hasan.gateway.security;

import com.hasan.gateway.repos.ApiKeyRepo; 
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Component
public class ApiAuthenticationFilter extends OncePerRequestFilter {

    private final ApiKeyRepo apiKeyRepo;

    public ApiAuthenticationFilter(ApiKeyRepo apiKeyRepo) {
        this.apiKeyRepo = apiKeyRepo;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Let registration requests pass through without a key
        if (request.getRequestURI().startsWith("/api/v1/clients/register")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Extract the API Key from the HTTP Headers
        String rawApiKey = request.getHeader("X-API-KEY");
        if (rawApiKey == null || rawApiKey.isEmpty()) {
            rejectRequest(response, "Missing X-API-KEY header");
            return;
        }

        // 3. Hash the incoming key and check the database
        String hashedIncomingKey = SecurityUtil.hashKey(rawApiKey);
        boolean keyExists = apiKeyRepo.findByKeyHash(hashedIncomingKey).isPresent();

        if (!keyExists) {
            rejectRequest(response, "Invalid API Key");
            return;
        }

        // 4. Key is valid! Let them pass to the Controller.
        filterChain.doFilter(request, response);
    }

    // Standard JSON rejection response
    private void rejectRequest(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }

}