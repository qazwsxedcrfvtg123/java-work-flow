package com.demo.auth.filter;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.HashMap;
import java.util.Map;

/**
 * XSS 請求包裝器
 * 過濾和清理所有輸入參數，防止 XSS 攻擊  ->執行者
 */
public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {

    private static final Safelist ALLOWED_TAGS = Safelist.relaxed()
            .addTags("section", "article", "header", "footer")
            .addAttributes(":all", "class", "id", "title")
            .addProtocols("a", "href", "http", "https", "mailto")
            .removeTags("script", "iframe", "object", "embed", "form", "input", "textarea");

    public XssHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    @Override
    public String[] getParameterValues(String parameter) {
        String[] values = super.getParameterValues(parameter);
        if (values == null) {
            return null;
        }

        int count = values.length;
        String[] encodedValues = new String[count];
        for (int i = 0; i < count; i++) {
            encodedValues[i] = cleanXSS(values[i]);
        }

        return encodedValues;
    }

    @Override
    public String getParameter(String parameter) {
        String value = super.getParameter(parameter);
        return cleanXSS(value);
    }

    @Override
    public String getHeader(String name) {
        String value = super.getHeader(name);
        return cleanXSS(value);
    }

    /**
     * 清理 XSS 內容
     * 使用 Jsoup 庫進行 HTML 淨化
     */
    private String cleanXSS(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }

        // 如果值是純文本（不包含 HTML），直接進行轉義
        if (!value.contains("<") && !value.contains(">") && !value.contains("&") &&
            !value.contains("\"") && !value.contains("'")) {
            return value;
        }

        try {
            // 使用 Jsoup 清理 HTML，只保留安全的標籤和屬性
            return Jsoup.clean(value, ALLOWED_TAGS);
        } catch (Exception e) {
            // 如果清理失敗，返回原始值（或者可以選擇拋出異常）
            return value;
        }
    }

    /**
     * 獲取請求體（用於 POST/PUT 請求）
     * 注意：不能直接修改 InputStream，需要在過濾器中處理
     */
    // 不需要重寫 getInputStream()，因為我們主要在過濾器層面處理 XSS
}
