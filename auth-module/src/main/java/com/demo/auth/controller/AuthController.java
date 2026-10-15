package com.demo.auth.controller;

import com.demo.auth.dto.*;
import com.demo.auth.filter.JwtAuthenticationFilter;
import com.demo.auth.model.Role;
import com.demo.auth.service.AuthService;
import com.demo.auth.service.PasswordResetService;
import com.demo.auth.util.JwtUtil;
import com.demo.auth.config.JwtTokenProvider;
import com.demo.auth.repository.UserRepository;
import com.demo.auth.entity.UserEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Authentication Controller
 */
@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {
    public static final String AUTHORIZATION = "Authorization";

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtUtil jwtUtil;
    private final JwtTokenProvider jwtTokenProvider;  // 新增
    private final UserRepository userRepository;      // 新增

    public AuthController(AuthService authService,
                          PasswordResetService passwordResetService, JwtAuthenticationFilter jwtAuthenticationFilter,
                          JwtUtil jwtUtil,
                          JwtTokenProvider jwtTokenProvider,  // 新增
                          UserRepository userRepository) {      // 新增
        this.authService = authService;
        this.passwordResetService = passwordResetService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtUtil = jwtUtil;
        this.jwtTokenProvider = jwtTokenProvider;  // 新增
        this.userRepository = userRepository;      // 新增
    }


    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
        @RequestParam(required = false) String username,
        @RequestParam(required = false) String password,
        @RequestBody(required = false) AuthRequest jsonRequest, HttpServletRequest request, HttpServletResponse response) {

        //JSON
        String finalUsername = username != null ? username : jsonRequest.getUsername();
        String finalPassword = password != null ? password : jsonRequest.getPassword();

        AuthRequest request_auth = new AuthRequest(finalUsername, finalPassword);
        AuthResponse response_auth = authService.authenticate(request_auth);

        log.info("成功登陆：{}", finalUsername);
        return ResponseEntity.ok(response_auth);
    }

    @GetMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request, HttpServletResponse response) {
        log.info("User logging out");

        try {
            // Get token from request header
            String bearerToken = request.getHeader("Authorization");
            String token = null;

            if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
                token = bearerToken.substring(7);
            } else if (bearerToken != null) {
                token = bearerToken;
            }

            // Add token to blacklist (invalidate it)
            if (token != null) {
                try {
                    // Calculate remaining expiration time from token's expiration date
                    Date expirationDate = jwtUtil.extractExpiration(token);
                    long expirationTime = (expirationDate.getTime() - System.currentTimeMillis()) / 1000;

                    // Ensure expiration time is positive and reasonable (at least 1 hour)
                    if (expirationTime <= 0) {
                        expirationTime = 3600; // Default to 1 hour if token already expired
                    }

                    jwtAuthenticationFilter.blacklistToken(token, expirationTime);
                    log.info("Token blacklisted successfully, will expire in {} seconds", expirationTime);
                } catch (Exception e) {
                    log.warn("Failed to blacklist token: {}", e.getMessage());
                    // If extraction fails, use default expiration time (24 hours)
                    jwtAuthenticationFilter.blacklistToken(token, 86400);
                }
            }

            // Invalidate session
            if (request.getSession(false) != null) {
                request.getSession().invalidate();
                log.debug("Session invalidated");
            }

            // Clear cookie if exists
            if (token != null) {
                Cookie cookie = new Cookie("AUTH_TOKEN", token);
                cookie.setPath("/");
                cookie.setMaxAge(0);  // Set cookie to expire immediately
                cookie.setHttpOnly(true);
                cookie.setSecure(false); // Set to true if using HTTPS
                response.addCookie(cookie);
                log.debug("Auth cookie cleared");
            }

            // Prepare response
            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("message", "退出成功");
            result.put("timestamp", System.currentTimeMillis());

            log.info("User logged out successfully");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error during logout: {}", e.getMessage());
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", "error");
            errorResult.put("message", "退出失败：" + e.getMessage());
            return ResponseEntity.ok(errorResult);
        }
    }

    /**
     * Refresh Access Token using Refresh Token
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken(
            @RequestParam String refreshToken,
            HttpServletRequest request) {

        log.info("Refreshing access token");

        try {
            // Validate Refresh Token
            if (!jwtTokenProvider.validateToken(refreshToken)) {
                log.warn("Invalid refresh token");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("code", "INVALID_REFRESH_TOKEN");
                errorResponse.put("message", "Refresh Token 已失效，请重新登录");
                return ResponseEntity.status(401).body(errorResponse);
            }

            // Check if refresh token is blacklisted
            if (jwtAuthenticationFilter.isTokenBlacklisted(refreshToken)) {
                log.warn("Refresh token is blacklisted");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("code", "BLACKLISTED_TOKEN");
                errorResponse.put("message", "Token 已被注销");
                return ResponseEntity.status(401).body(errorResponse);
            }

            // Extract username from refresh token
            String username = jwtTokenProvider.getUsernameFromToken(refreshToken);

            // Load user details
            UserDetails userDetails = authService.loadUserByUsername(username);

            // Generate new Access Token
            Map<String, Object> claims = new HashMap<>();
            UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
            claims.put("userId", userEntity.getId());
            claims.put("role", userEntity.getRole().name());

            String newAccessToken = jwtUtil.generateToken(username, claims);
            long newExpiresIn = jwtUtil.extractExpiration(newAccessToken).getTime() - System.currentTimeMillis();

            // Response
            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("status", "success");
            successResponse.put("accessToken", newAccessToken);
            successResponse.put("expiresIn", newExpiresIn);
            successResponse.put("tokenType", "Bearer");
            successResponse.put("timestamp", System.currentTimeMillis());

            log.info("Access token refreshed successfully for user: {},userDetails:{}", username,userDetails);
            return ResponseEntity.ok(successResponse);

        } catch (Exception e) {
            log.error("Error refreshing token: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("code", "TOKEN_REFRESH_FAILED");
            errorResponse.put("message", "Token 刷新失败：" + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * (admin only)
     */
    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> registerUser(
            @Valid @RequestBody Map<String, String> request) {

        String username = request.get("username");
        String password = request.get("password");
        String email = request.get("email");
        String roleStr = request.get("role");

        Role role = roleStr != null ? Role.valueOf(roleStr) : Role.ROLE_USER;

        log.info("Registering new user: {}", username);
        authService.registerUser(username, password, email, role);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "User registered successfully");
        response.put("username", username);

        return ResponseEntity.ok(response);
    }

    // 修改密碼 (需帶 JWT Token)
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
        @RequestHeader("Authorization") String token,
        @RequestBody Map<String, String> body
    ) {

        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");

        // 1. 驗證請求參數
        if (oldPassword == null || oldPassword.isBlank()
            || newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "舊密碼與新密碼不可為空"));
        }

        // 2. 不允許新舊密碼相同
        if (oldPassword.equals(newPassword)) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "新密碼不可與舊密碼相同"));
        }

        // 3. 呼叫 Service 驗證舊密碼並更新
        authService.changePassword(token, oldPassword, newPassword);

        // 4. 回傳成功訊息
        return ResponseEntity.ok(
            Map.of("message", "更新密碼成功")
        );
    }

    //忘記密碼 重設三部曲

    // 1. 忘記密碼：送驗證碼
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody @Valid ForgotPasswordRequest req) {
        passwordResetService.sendResetCode(req.getEmail());
        // 安全：不管信箱是否存在都回同一訊息，避免被列舉帳號
        return ResponseEntity.ok(Map.of("message", "若該信箱已註冊，我們將寄送重設密碼的驗證碼"));
    }

    // 2. 驗碼（可選）
    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@RequestBody @Valid VerifyCodeRequest req) {
        passwordResetService.verifyCode(req.getEmail(), req.getCode());
        return ResponseEntity.ok(Map.of("message", "驗證成功"));
    }

    // 3. 重設密碼
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody @Valid ResetPasswordRequest req) {
        passwordResetService.resetPassword(req.getEmail(), req.getCode(), req.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "密碼已重設，請使用新密碼登入"));
    }

    /**
     * Get current user info
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        // This will be handled by JWT filter which sets authentication
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("message", "Authenticated user info");
        return ResponseEntity.ok(userInfo);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "auth-module");
        return ResponseEntity.ok(response);
    }
}
