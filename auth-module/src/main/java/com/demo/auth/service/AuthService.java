package com.demo.auth.service;

import com.demo.auth.dto.AuthRequest;
import com.demo.auth.dto.AuthResponse;
import com.demo.auth.entity.UserEntity;
import com.demo.auth.model.Role;
import com.demo.auth.repository.UserRepository;
import com.demo.auth.util.JwtUtil;
import com.demo.auth.config.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Authentication Service with database support
 */
@Service
@Slf4j
public class AuthService implements UserDetailsService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final JwtTokenProvider jwtTokenProvider;  // 新增
    private final UserRepository userRepository;

    public AuthService(@Lazy AuthenticationManager authenticationManager, 
                      JwtUtil jwtUtil, 
                      JwtTokenProvider jwtTokenProvider,  // 新增
                      UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.jwtTokenProvider = jwtTokenProvider;  // 新增
        this.userRepository = userRepository;
        initializeDefaultUsers();
    }

    /**
     * Initialize default users for demo purposes
     */
    private void initializeDefaultUsers() {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        
        // Create admin user if not exists
        if (!userRepository.existsByUsername("admin")) {
            UserEntity adminUser = UserEntity.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .email("admin@example.com")
                    .role(Role.ROLE_ADMIN)
                    .enabled(true)
                    .build();
            userRepository.save(adminUser);
            log.info("Created default admin user");
        }

        // Create regular user if not exists
        if (!userRepository.existsByUsername("user")) {
            UserEntity regularUser = UserEntity.builder()
                    .username("user")
                    .password(passwordEncoder.encode("user123"))
                    .email("user@example.com")
                    .role(Role.ROLE_USER)
                    .enabled(true)
                    .build();
            userRepository.save(regularUser);
            log.info("Created default user");
        }
    }

    /**
     * Authenticate user and generate token
     */
    public AuthResponse authenticate(AuthRequest request) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (DisabledException e) {
            log.error("User is disabled: {}", request.getUsername());
            throw new DisabledException("User is disabled");
        } catch (BadCredentialsException e) {
            log.error("Invalid credentials for user: {}", request.getUsername());
            throw new BadCredentialsException("Invalid username or password");
        }

        UserEntity user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + request.getUsername()));
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole().name());
        
        // 生成 Access Token（30 分钟）
        String token = jwtUtil.generateToken(user.getUsername(), claims);
        
        // 生成 Refresh Token（7 天）- 新增
        String refreshToken = jwtTokenProvider.generateRefreshToken(
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                user.getUsername(), null,
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name()))
            )
        );
        
        long refreshExpiresIn = jwtTokenProvider.getRefreshExpiration();
        
        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)  // 新增
                .username(user.getUsername())
                .role(user.getRole().name())
                .expiresIn(jwtUtil.extractExpiration(token).getTime() - System.currentTimeMillis())
                .refreshExpiresIn(refreshExpiresIn)  // 新增
                .build();
    }

    /**
     * Register a new user
     */
    public UserEntity registerUser(String username, String password, String email, Role role) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        
        UserEntity newUser = UserEntity.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .email(email)
                .enabled(true)
                .role(role != null ? role : Role.ROLE_USER)
                .build();

        userRepository.save(newUser);
        log.info("User registered successfully: {}", username);
        return newUser;
    }

    /**
     * Load user by username for Spring Security
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return new User(
            user.getUsername(),
            user.getPassword(),
            user.isEnabled(),
            true,
            true,
            true,
            Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name()))
        );
    }

    /**
     * Get user by username
     */
    public Optional<UserEntity> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
