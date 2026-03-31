package com.demo.auth.filter;

import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * XSS（跨站腳本攻擊）過濾器
 * 過濾惡意腳本注入，保護應用程式安全
 */
@Component
@Slf4j
public class XssFilter implements Filter {
    public  static Logger logger = LogManager.getLogger(XssFilter.class);
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("【安全過濾器】XSS 過濾器已啟動");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                        FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestPath = httpRequest.getRequestURI();
        logger.info("The requested url："+ requestPath);
        String method = httpRequest.getMethod();

        // 跳過靜態資源和特定路徑
        if (isExcluded(requestPath)) {
            chain.doFilter(request, response);
            return;
        }

        log.debug("【XSS 檢查】{} {} | 開始檢查", method, requestPath);

        // 包裝請求，進行 XSS 檢查和過濾
        XssHttpServletRequestWrapper wrappedRequest = new XssHttpServletRequestWrapper(httpRequest);

        try {
            // 執行過濾鏈
            chain.doFilter(wrappedRequest, httpResponse);
        } catch (Exception e) {
            log.error("【XSS 過濾器】處理請求時發生錯誤：{}", e.getMessage());
            throw e;
        }

        log.debug("【XSS 檢查】{} {} | 檢查通過", method, requestPath);
    }

    /**
     * 判斷是否需要排除的路徑
     */
    private boolean isExcluded(String uri) {
        // 排除靜態資源
        if (uri.endsWith(".js") || uri.endsWith(".css") || uri.endsWith(".png") ||
            uri.endsWith(".jpg") || uri.endsWith(".jpeg") || uri.endsWith(".gif") ||
            uri.endsWith(".svg") || uri.endsWith(".ico") || uri.endsWith(".woff") ||
            uri.endsWith(".woff2") || uri.endsWith(".ttf") || uri.endsWith(".eot")) {
            return true;
        }

        // 排除 Swagger 文檔
        if (uri.contains("/swagger") || uri.contains("/v3/api-docs") ||
            uri.contains("/webjars")) {
            return true;
        }

        // 排除 H2 控制台
        if (uri.contains("/h2-console")) {
            return true;
        }

        // 排除文件上傳接口（避免影響 multipart 數據）
        if (uri.contains("/uploadFiles") || uri.contains("/upload")) {
            return true;
        }

        return false;
    }

    @Override
    public void destroy() {
        log.info("【安全過濾器】XSS 過濾器已關閉");
    }
}
