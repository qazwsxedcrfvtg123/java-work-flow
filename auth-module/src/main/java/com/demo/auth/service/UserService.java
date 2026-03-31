package com.demo.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.auth.entity.UserEntity;
import com.demo.auth.mapper.UserMapper;
import com.demo.auth.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * User Service demonstrating JPA + MyBatis Plus usage
 * 
 * Use Cases:
 * - JPA Repository: Simple CRUD, entity management, relationships
 * - MyBatis Plus: Complex queries, dynamic SQL, batch operations, performance-critical queries
 */
@Service
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    // ==================== JPA Operations ====================

    /**
     * Simple CRUD with JPA
     */
    @Transactional(readOnly = true)
    public Optional<UserEntity> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Save user with JPA
     */
    @Transactional
    public UserEntity saveUser(UserEntity user) {
        log.info("Saving user with JPA: {}", user.getUsername());
        return userRepository.save(user);
    }

    /**
     * Delete user with JPA
     */
    @Transactional
    public void deleteUser(Long id) {
        log.info("Deleting user with JPA: {}", id);
        userRepository.deleteById(id);
    }

    // ==================== MyBatis Plus Operations ====================

    /**
     * Complex query with MyBatis Plus
     */
    @Transactional(readOnly = true)
    public List<UserEntity> findUsersByRoleMyBatis(String role, boolean enabled) {
        log.info("Finding users by role with MyBatis: {}", role);
        return userMapper.selectByRoleAndEnabled(role, enabled);
    }

    /**
     * Dynamic query with MyBatis XML
     */
    @Transactional(readOnly = true)
    public List<UserEntity> searchUsers(String username, String email, String role) {
        log.info("Searching users with MyBatis dynamic SQL");
        return userMapper.selectUsersWithFilters(username, email, role);
    }

    /**
     * MyBatis Plus QueryWrapper (very powerful!)
     */
    @Transactional(readOnly = true)
    public List<UserEntity> findWithQueryWrapper(String usernamePattern, String role) {
        QueryWrapper<UserEntity> wrapper = new QueryWrapper<>();
        
        if (usernamePattern != null) {
            wrapper.like("username", usernamePattern);
        }
        if (role != null) {
            wrapper.eq("role", role);
        }
        wrapper.eq("enabled", true)
               .orderByDesc("created_at");
        
        return userMapper.selectList(wrapper);
    }

    /**
     * Pagination with MyBatis Plus
     */
    @Transactional(readOnly = true)
    public Page<UserEntity> findUsersPaged(int page, int size) {
        Page<UserEntity> pagedPage = new Page<>(page, size);
        return userMapper.selectPage(pagedPage, null);
    }

    /**
     * Batch insert with MyBatis Plus (faster than JPA for bulk inserts)
     */
    @Transactional
    public int batchInsertUsers(List<UserEntity> users) {
        log.info("Batch inserting {} users with MyBatis", users.size());
        int count = 0;
        for (UserEntity user : users) {
            count += userMapper.insert(user);
        }
        return count;
    }

    /**
     * Count users by time range (complex aggregation)
     */
    @Transactional(readOnly = true)
    public long countUsersByTimeRange(Long startTime, Long endTime) {
        return userMapper.countUsersByTimeRange(startTime, endTime);
    }

    /**
     * Update user with MyBatis Plus
     */
    @Transactional
    public int updateUser(UserEntity user) {
        log.info("Updating user with MyBatis: {}", user.getId());
        return userMapper.updateById(user);
    }

    // ==================== Combined Usage Example ====================

    /**
     * Example: Use JPA for read, MyBatis for complex update
     */
    @Transactional
    public void enableUsersByRole(String role) {
        // Use JPA to find users
        List<UserEntity> users = userRepository.findAll();
        
        // Filter and use MyBatis for batch update
        long count = users.stream()
            .filter(u -> u.getRole().name().equals(role))
            .peek(u -> {
                u.setEnabled(true);
                userMapper.updateById(u);
            })
            .count();
        
        log.info("Enabled {} users with role {}", count, role);
    }
}
