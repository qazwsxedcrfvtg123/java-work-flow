package com.demo.auth.service;

import com.demo.auth.entity.UserEntity;
import com.demo.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @description:
 * @Creator: 阿昇
 * @CreateTime: 2026-10-15 20:02
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.from:qazwsxedcrfvtg123@gmail.com}")
    private String mailFrom;

    private static final String KEY_PREFIX = "pwd:reset:";
    private static final long CODE_TTL_MINUTES = 15;
    private static final int MAX_ATTEMPTS = 5;      // 防止暴力猜碼

    /**
     * 產生驗證碼、存 Redis、寄信
     */
    public void sendResetCode(String email) {
        UserEntity user = userRepository.findByEmail(email.trim().toLowerCase())
            .orElse(null);

        // 安全：找不到也照樣跑完流程（只是不寄信），避免被列舉帳號
        if (user == null) {
            log.warn("forgot-password requested for unregistered email: {}", email);
            return;
        }

        String code = generateCode();
        String key = KEY_PREFIX + user.getId();

        // 覆蓋舊碼（同一人重複點「發送」只保留最新一組）
        redisTemplate.opsForValue().set(key, code, CODE_TTL_MINUTES, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(KEY_PREFIX + "attempts:" + user.getId(), "0",
            CODE_TTL_MINUTES, TimeUnit.MINUTES);

        // 非同步寄信，避免 SMTP 延遲卡死前端 HTTP 請求
        CompletableFuture.runAsync(() -> sendMail(user.getEmail(), code));
        log.info("reset code generated for userId={}, code={}（上線後刪除此行）", user.getId(), code);
    }

    /**
     * 驗碼（不消耗）
     */
    public void verifyCode(String email, String code) {
        Long userId = resolveUserId(email);
        String key = KEY_PREFIX + userId;

        String stored = redisTemplate.opsForValue().get(key);
        if (stored == null) {
            throw new RuntimeException("驗證碼已過期，請重新索取");
        }
        if (!stored.equals(code.trim())) {
            incAttempts(userId);
            throw new RuntimeException("驗證碼錯誤");
        }
    }

    /**
     * 重設密碼：驗碼 → 存新密碼 → 刪碼（一次性）
     */
    @Transactional
    public void resetPassword(String email, String code, String newPassword) {
        Long userId = resolveUserId(email);
        String key = KEY_PREFIX + userId;

        String stored = redisTemplate.opsForValue().get(key);
        if (stored == null) {
            throw new RuntimeException("驗證碼已過期或不存在，請重新索取");
        }
        if (!stored.equals(code.trim())) {
            incAttempts(userId);
            throw new RuntimeException("驗證碼錯誤");
        }

        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("用戶不存在"));

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new RuntimeException("新密碼不可與舊密碼相同");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // 一次性：用完就刪
        redisTemplate.delete(key);
        redisTemplate.delete(KEY_PREFIX + "attempts:" + userId);

        log.info("password reset success: userId={}", userId);
    }

    // ---------- 內部工具 ----------

    private String generateCode() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000);   // 6 位數字
        return String.valueOf(code);
    }

    private Long resolveUserId(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase())
            .map(UserEntity::getId)
            .orElseThrow(() -> new RuntimeException("驗證碼無效"));
    }

    private void incAttempts(Long userId) {
        String k = KEY_PREFIX + "attempts:" + userId;
        Long c = redisTemplate.opsForValue().increment(k);
        if (c != null && c >= MAX_ATTEMPTS) {
            redisTemplate.delete(KEY_PREFIX + userId);   // 猜太多次直接作廢
            throw new RuntimeException("嘗試次數過多，請重新索取驗證碼");
        }
    }

    private void sendMail(String to, String code) {
        if (mailSender == null) {
            log.warn("JavaMailSender 未配置，略過實際寄信（驗證碼為: {}）", code);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom("Photography-UU 系統通知 <" + mailFrom + ">");
            msg.setTo(to);
            msg.setSubject("【photography-uu】重設密碼驗證碼");
            msg.setText("您的驗證碼為：" + code + "\n有效時間 15 分鐘，請勿提供給他人。");
            mailSender.send(msg);
            log.info("密碼重設驗證碼已發送至: {}", to);
        } catch (Exception e) {
            log.error("寄送驗證碼郵件失敗: {}", e.getMessage(), e);
        }
    }
}
