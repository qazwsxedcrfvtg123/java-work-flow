package com.demo.auth.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.Principal;

/**
 * 自定義攔截器 - 用於日誌記錄、性能監控、權限檢查
 */
@Component
@Slf4j
public class CustomInterceptor implements HandlerInterceptor {

    private static final String START_TIME = "startTime";

    /**
     * 在控制器方法執行前調用
     * 可用於：權限驗證、日誌記錄、參數檢查
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        long startTime = System.currentTimeMillis();
        request.setAttribute(START_TIME, startTime);

        String uri = request.getRequestURI();
        String method = request.getMethod();
        String remoteAddr = getRemoteAddr(request);

        log.info("【請求開始】{} {} | IP: {} | Time: {}", method, uri, remoteAddr, startTime);

        // 可以在這裡進行額外的權限檢查
        // 例如：檢查 token 是否過期、用戶角色等

        return true; // 繼續執行
    }

    /**
     * 在控制器方法執行後，視圖渲染前調用
     * 可用於：資源清理、額外日誌
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, 
                          Object handler, ModelAndView modelAndView) throws Exception {
        
        long startTime = (Long) request.getAttribute(START_TIME);
        long duration = System.currentTimeMillis() - startTime;

        log.info("【請求處理完成】耗时：{} ms", duration);

        // 可以在這裡修改 modelAndView
        if (modelAndView != null) {
            modelAndView.addObject("serverTime", System.currentTimeMillis());
        }
    }

    /**
     * 在整個請求完成後調用（包括視圖渲染）
     * 可用於：資源清理、異常記錄
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                               Object handler, Exception ex) throws Exception {
        
        long startTime = (Long) request.getAttribute(START_TIME);
        long duration = System.currentTimeMillis() - startTime;
        String uri = request.getRequestURI();
        String method = request.getMethod();

        if (ex != null) {
            log.error("【請求異常】{} {} | 耗时：{} ms | 錯誤：{}", 
                     method, uri, duration, ex.getMessage(), ex);
        } else {
            log.info("【請求結束】{} {} | 总耗时：{} ms", method, uri, duration);
        }
    }

    /**
     * 獲取客戶端真實 IP 地址
     */
    private String getRemoteAddr(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 如果是多個 IP，取第一個
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0];
        }
        return ip;
    }
}
