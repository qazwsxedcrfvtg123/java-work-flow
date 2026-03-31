XSS 過濾器（XssFilter）
定位：Servlet Filter，位於 Spring Security 過濾鏈中，屬於容器層的「外觀防護」。
職責：決定哪些請求需要檢查、哪些路徑要排除；負責把請求包裝成帶 XSS 防護的請求。
作用範圍：所有匹配的 HTTP 請求（可按路徑/資源類型排除）。
典型工作：在 doFilter 中將 HttpServletRequest 包裝為 XssHttpServletRequestWrapper，再交給後續過濾器和控制器。
XSS 請求包裝器（XssHttpServletRequestWrapper）
定位：對 HttpServletRequest 的裝飾器（Wrapper），屬於應用層的「輸入淨化」。
職責：重寫 getParameter / getParameterValues / getHeader 等方法，在取值時即時清理潛在 XSS 內容。
作用範圍：只有通過過濾器並被包裝後的請求才會生效。
典型工作：使用 Jsoup.clean(value, allowlist) 對含 HTML 的輸入做白名單淨化；純文本且不含特殊字元則不處理。
兩者關係與差異總結
關係：過濾器是「入口與開關」，負責選擇並包裝；包裝器是「執行者」，負責實際清理參數與 Header。
層級：過濾器在 Servlet 容器層面介入請求流程；包裝器在應用代碼讀取參數時才发挥作用。
可控性：過濾器可配置排除路徑（靜態資源、Swagger、H2 控制台等）；包裝器提供具體的清理策略與白名單規則。
限制：包裝器主要處理查詢參數/表單參數與 Header；如需處理請求體（JSON/RAW body），通常要在過濾器層面額外做緩衝與清洗，或在全域異常/反序列化層面統一處理。
