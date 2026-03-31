package com.demo.auth.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.auth.entity.UserEntity;
import com.demo.auth.enums.PhotosType;
import com.demo.auth.service.FileStorageService;
import com.demo.auth.service.UserService;
import com.demo.auth.enums.FileType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *前台页面
 */
@RestController
@RequestMapping("/api/users")
@Slf4j
public class UserController {

    private final UserService userService;
    @Autowired
    private FileStorageService fileStorageService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

//http://localhost:8888/api/users/findPhotos?types=1,2,3
    @GetMapping("/findPhotos")
    public Object findPhotos(@RequestParam(required = false) List<String> types){
        if (types == null || types.isEmpty()) {
            // 没有指定类型，返回所有照片
            return fileStorageService.findAllPhotos();
        }
        List<String>photosTypes = types.stream().collect(Collectors.toList());
        List<PhotosType> photos = fileStorageService.findPhotos(photosTypes);
        return photos;
    }

    @GetMapping("/{username}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getUserByUsername(@PathVariable String username) {
        log.info("Getting user with JPA: {}", username);

        Map<String, Object> response = new HashMap<>();
        userService.findByUsername(username)
            .ifPresent(user -> response.put("user", user));

        if (response.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        response.put("method", "JPA Repository");
        return ResponseEntity.ok(response);
    }

    /**
     * Find users by role (MyBatis)
     */
    @GetMapping("/role/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getUsersByRole(
            @PathVariable String role,
            @RequestParam(defaultValue = "true") boolean enabled) {
        log.info("Getting users by role with MyBatis: {}", role);

        List<UserEntity> users = userService.findUsersByRoleMyBatis(role, enabled);

        Map<String, Object> response = new HashMap<>();
        response.put("users", users);
        response.put("count", users.size());
        response.put("method", "MyBatis Plus - Custom Query");

        return ResponseEntity.ok(response);
    }

    /**
     * Search users with dynamic filters (MyBatis XML)
     */
//    @GetMapping("/search")
//    @PreAuthorize("hasRole('ADMIN')")
//    public ResponseEntity<Map<String, Object>> searchUsers(
//            @RequestParam(required = false) String username,
//            @RequestParam(required = false) String email,
//            @RequestParam(required = false) String role) {
//        log.info("Searching users with MyBatis dynamic SQL");
//
//        List<UserEntity> users = userService.searchUsers(username, email, role);
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("users", users);
//        response.put("count", users.size());
//        response.put("method", "MyBatis XML - Dynamic SQL");
//
//        return ResponseEntity.ok(response);
//    }

    /**
     * Find users with QueryWrapper (MyBatis Plus feature)
     */
    @GetMapping("/query")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> findWithQueryWrapper(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String role) {
        log.info("Finding users with MyBatis QueryWrapper");

        List<UserEntity> users = userService.findWithQueryWrapper(username, role);

        Map<String, Object> response = new HashMap<>();
        response.put("users", users);
        response.put("count", users.size());
        response.put("method", "MyBatis Plus - QueryWrapper");

        return ResponseEntity.ok(response);
    }

    /**
     * Paginated users (MyBatis Plus)
     */
    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getPagedUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Getting paged users with MyBatis Plus");

        Page<UserEntity> userPage = userService.findUsersPaged(page, size);

        Map<String, Object> response = new HashMap<>();
        response.put("users", userPage.getRecords());
        response.put("total", userPage.getTotal());
        response.put("currentPage", userPage.getCurrent());
        response.put("size", userPage.getSize());
        response.put("pages", userPage.getPages());
        response.put("method", "MyBatis Plus - Pagination");

        return ResponseEntity.ok(response);
    }


    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "User service running with JPA + MyBatis Plus");
        return ResponseEntity.ok(response);
    }
}
