package com.demo.auth.filter;

import com.demo.auth.service.AuthService;
import com.demo.auth.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * JWT Authentication Filter with blacklist support
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final AuthService authService;
    private final RedisTemplate<String, Object> redisTemplate;

    // Blacklist key prefix
    private static final String BLACKLIST_PREFIX = "token:blacklist:";

    public JwtAuthenticationFilter(JwtUtil jwtUtil, @Lazy AuthService authService,
                                   RedisTemplate<String, Object> redisTemplate) {
        this.jwtUtil = jwtUtil;
        this.authService = authService;
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        // 跳過公開接口（不需要認證的接口）
        String requestURI = request.getRequestURI();
        if (requestURI.startsWith("/api/auth/") ||
            requestURI.startsWith("/api/public/") ||
            requestURI.contains("/swagger-ui/") ||
            requestURI.contains("/v3/api-docs/") ||
            requestURI.equals("/actuator/health") ||
            requestURI.startsWith("/api/users/")) {
            log.info("跳過認證端點: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        boolean isTokenExpired = false;

        try {
            String jwt = getJwtFromRequest(request);
            log.debug("Extracted JWT from request: {}", jwt);

            if (StringUtils.hasText(jwt)) {
                // Check if token is blacklisted
                if (isTokenBlacklisted(jwt)) {
                    log.warn("JWT token is blacklisted (user logged out)");
                    // Token 在黑名单中，返回401
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");

                    try {
                        java.util.Map<String, Object> errorResponse = new java.util.HashMap<>();
                        errorResponse.put("status", "error");
                        errorResponse.put("code", "BLACKLISTED_TOKEN");
                        errorResponse.put("message", "Token 已被注销，请重新登录");
                        errorResponse.put("timestamp", System.currentTimeMillis());

                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        response.getWriter().write(mapper.writeValueAsString(errorResponse));
                        response.getWriter().flush();
                    } catch (Exception e) {
                        log.error("Failed to write blacklisted token response: {}", e.getMessage());
                    }
                    return;
                }

                try {
                    String username = jwtUtil.extractUsername(jwt);
                    log.debug("Extracted username from JWT: {}", username);

                    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        UserDetails userDetails = authService.loadUserByUsername(username);

                        if (jwtUtil.validateToken(jwt, username)) {
                            UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                                );
                            authentication.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request)
                            );

                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            log.info("Set authentication for user: {} with roles: {}", username, userDetails.getAuthorities());
                        } else {
                            log.warn("JWT token is invalid for user: {}", username);
                            // Token 无效，返回401
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");

                            try {
                                java.util.Map<String, Object> errorResponse = new java.util.HashMap<>();
                                errorResponse.put("status", "error");
                                errorResponse.put("code", "INVALID_TOKEN");
                                errorResponse.put("message", "Token 无效或已过期");
                                errorResponse.put("timestamp", System.currentTimeMillis());

                                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                                response.getWriter().write(mapper.writeValueAsString(errorResponse));
                                response.getWriter().flush();
                            } catch (Exception e) {
                                log.error("Failed to write invalid token response: {}", e.getMessage());
                            }
                            return;
                        }
                    }
                } catch (io.jsonwebtoken.ExpiredJwtException e) {
                    // Token has expired - auto logout
                    isTokenExpired = true;
                    log.warn("⚠️ JWT token has expired: {}. Auto-logout triggered.", e.getMessage());

                    // Clear security context
                    SecurityContextHolder.clearContext();

                    // 注意：过期的 Token 不要加入黑名单！
                    // 因为用户可能需要用 Refresh Token 来刷新
                    // 只有主动登出时才加入黑名单
                } catch (Exception e) {
                    log.error("Cannot set user authentication: {}", e.getMessage(), e);
                }
            } else {
                log.debug("No JWT token found in request");
                // 没有 Token，返回401
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");

                try {
                    java.util.Map<String, Object> errorResponse = new java.util.HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("code", "MISSING_TOKEN");
                    errorResponse.put("message", "缺少认证 Token");
                    errorResponse.put("timestamp", System.currentTimeMillis());

                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    response.getWriter().write(mapper.writeValueAsString(errorResponse));
                    response.getWriter().flush();
                } catch (Exception e) {
                    log.error("Failed to write missing token response: {}", e.getMessage());
                }
                return;
            }
        } catch (Exception ex) {
            log.error("Cannot set user authentication: {}", ex.getMessage(), ex);
        }

        // If token expired, set response status and headers for frontend to handle
        if (isTokenExpired) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.setHeader("X-Token-Expired", "true");

            try {
                java.util.Map<String, Object> errorResponse = new java.util.HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("code", "TOKEN_EXPIRED");
                errorResponse.put("message", "登录已过期，请重新登录");
                errorResponse.put("timestamp", System.currentTimeMillis());

                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                response.getWriter().write(mapper.writeValueAsString(errorResponse));
                response.getWriter().flush();
            } catch (Exception e) {
                log.error("Failed to write expired token response: {}", e.getMessage());
            }
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT from request header
     * Supports multiple formats:
     * - "Bearer <token>" (standard)
     * - "<token>" (without prefix)
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        log.debug("Authorization header from request: {}", bearerToken);

        if (!StringUtils.hasText(bearerToken)) {
            return null;
        }

        // Support "Bearer <token>" format
        if (bearerToken.startsWith("Bearer ")) {
            String jwt = bearerToken.substring(7);
            log.debug("Extracted JWT (Bearer format): {}", jwt);
            return jwt;
        }

        // Support plain token format (without "Bearer" prefix)
        log.debug("Extracted JWT (plain format): {}", bearerToken);
        return bearerToken;
    }

    /**
     * Check if token is blacklisted
     */
    public boolean isTokenBlacklisted(String jwt) {  // 改为 public
        String blacklistKey = BLACKLIST_PREFIX + jwt;
        Boolean exists = redisTemplate.hasKey(blacklistKey);
        return exists != null && exists;
    }

    /**
     * Add token to blacklist
     * @param jwt the JWT token
     * @param expirationTime expiration time in seconds
     */
    public void blacklistToken(String jwt, long expirationTime) {
        String blacklistKey = BLACKLIST_PREFIX + jwt;
        redisTemplate.opsForValue().set(blacklistKey, "blacklisted", expirationTime, TimeUnit.SECONDS);
        log.info("Token added to blacklist, expires in {} seconds", expirationTime);
    }
}
