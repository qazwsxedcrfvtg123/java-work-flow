Write-Host "=== 測試認證模塊 ===" -ForegroundColor Cyan

Write-Host "`n1. 註冊新用戶..." -ForegroundColor Yellow
try {
    $registerResponse = Invoke-RestMethod -Uri "http://localhost:8081/api/auth/register" `
      -Method POST `
      -ContentType "application/json" `
      -Body '{"username":"testuser","password":"Test123!","email":"test@example.com"}'
    
    $registerResponse | ConvertTo-Json -Depth 5
    Write-Host "✓ 註冊成功！" -ForegroundColor Green
} catch {
    Write-Host "✗ 註冊失敗：$_" -ForegroundColor Red
}

Start-Sleep -Seconds 2

Write-Host "`n2. 登入..." -ForegroundColor Yellow
try {
    $loginResponse = Invoke-RestMethod -Uri "http://localhost:8081/api/auth/login" `
      -Method POST `
      -ContentType "application/json" `
      -Body '{"username":"testuser","password":"Test123!"}'
    
    $loginResponse | ConvertTo-Json -Depth 5
    
    $token = $loginResponse.token
    Write-Host "`n✓ 登入成功！" -ForegroundColor Green
    Write-Host "Token: $token" -ForegroundColor Gray
    
    Write-Host "`n3. 使用 Token 訪問受保護的接口..." -ForegroundColor Yellow
    try {
        $headers = @{
            "Authorization" = "Bearer $token"
            "Content-Type" = "application/json"
        }
        
        $result = Invoke-RestMethod -Uri "http://localhost:8081/api/workflow/list" `
          -Method GET `
          -Headers $headers
        
        $result | ConvertTo-Json -Depth 5
        Write-Host "✓ API 訪問成功！" -ForegroundColor Green
    } catch {
        Write-Host "API 訪問結果：$_" -ForegroundColor Gray
    }
} catch {
    Write-Host "✗ 登入失敗：$_" -ForegroundColor Red
}

Write-Host "`n=== 測試完成 ===" -ForegroundColor Cyan
