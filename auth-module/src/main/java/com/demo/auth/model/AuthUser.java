package com.demo.auth.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User model for authentication
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthUser {
    private Long id;
    private String username;
    private String password;
    private String email;
    private boolean enabled;
    private Role role;
}
