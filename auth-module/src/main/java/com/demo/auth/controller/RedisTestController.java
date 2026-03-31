package com.demo.auth.controller;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redis 測試 Controller
 * 用於測試 Redis 連接和基本操作
 */
@RestController
@RequestMapping("/api/redis")
public class RedisTestController {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 測試 Redis 連接
     */
    @GetMapping("/ping")
    public Map<String, Object> ping() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 嘗試寫入和讀取
            ValueOperations<String, Object> ops = redisTemplate.opsForValue();
            String testKey = "test:ping";
            String testValue = "pong";
            
            ops.set(testKey, testValue);
            Object value = ops.get(testKey);
            
            result.put("status", "success");
            result.put("message", "Redis 連接成功");
            result.put("data", value);
            
            // 清理測試數據
            redisTemplate.delete(testKey);
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "Redis 連接失敗：" + e.getMessage());
        }
        return result;
    }

    /**
     * 設置緩存（帶過期時間）
     */
    @PostMapping("/set")
    public Map<String, Object> set(
            @RequestParam String key,
            @RequestParam String value,
            @RequestParam(required = false, defaultValue = "30") long timeout) {
        Map<String, Object> result = new HashMap<>();
        try {
            ValueOperations<String, Object> ops = redisTemplate.opsForValue();
            ops.set(key, value, timeout, TimeUnit.SECONDS);
            
            result.put("status", "success");
            result.put("message", "設置成功");
            result.put("key", key);
            result.put("value", value);
            result.put("timeout", timeout + "秒");
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "設置失敗：" + e.getMessage());
        }
        return result;
    }

    /**
     * 獲取緩存
     */
    @GetMapping("/get/{key}")
    public Map<String, Object> get(@PathVariable String key) {
        Map<String, Object> result = new HashMap<>();
        try {
            ValueOperations<String, Object> ops = redisTemplate.opsForValue();
            Object value = ops.get(key);
            
            result.put("status", "success");
            result.put("key", key);
            result.put("value", value);
            
            if (value == null) {
                result.put("message", "鍵不存在或已過期");
            } else {
                result.put("message", "獲取成功");
            }
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "獲取失敗：" + e.getMessage());
        }
        return result;
    }

    /**
     * 刪除緩存
     */
    @DeleteMapping("/delete/{key}")
    public Map<String, Object> delete(@PathVariable String key) {
        Map<String, Object> result = new HashMap<>();
        try {
            Boolean deleted = redisTemplate.delete(key);
            
            result.put("status", "success");
            result.put("key", key);
            result.put("deleted", deleted);
            result.put("message", deleted ? "刪除成功" : "鍵不存在");
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "刪除失敗：" + e.getMessage());
        }
        return result;
    }

    /**
     * 檢查 Redis 是否可用
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        try {
            redisTemplate.opsForValue().set("test:health", "ok", 1, TimeUnit.SECONDS);
            Object value = redisTemplate.opsForValue().get("test:health");
            
            if ("ok".equals(value)) {
                result.put("status", "success");
                result.put("message", "Redis 服務正常");
                result.put("connected", true);
            } else {
                result.put("status", "error");
                result.put("message", "Redis 回應異常");
                result.put("connected", false);
            }
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "Redis 服務異常：" + e.getMessage());
            result.put("connected", false);
        }
        return result;
    }
}
