# Jenkins + EC2 自動化部署快速指南

## 📋 概述

本指南說明如何將您的 Java Spring Boot 專案透過 Jenkins 自動部署到 AWS EC2。

## 🔄 CI/CD 流程

```
Git Push → Jenkins → Build & Test → Docker Build → Docker Push → Deploy to EC2
```

## ✅ 前置準備清單

### 1. EC2 設定

**建立 EC2 執行個體：**
- AMI: Ubuntu 22.04 LTS
- 類型: t3.medium 或更高（至少 2GB RAM）
- 儲存: 最少 20GB
- 安全群組: 開啟端口 8080, 8081, 8082, 8083, 22 (SSH)

**在 EC2 上安裝必要軟體：**

```bash
# SSH 連接到 EC2
ssh -i your-key.pem ubuntu@YOUR_EC2_IP

# 更新系統
sudo apt update && sudo apt upgrade -y

# 安裝 Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# 安裝 Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 驗證安裝
docker --version
docker-compose --version

# 建立部署目錄
sudo mkdir -p /opt/workflow-system
sudo chown $USER:$USER /opt/workflow-system

# 安裝 curl（用於健康檢查）
sudo apt install -y curl
```

### 2. SSH 金鑰設定

**產生 SSH 金鑰（在 Jenkins 伺服器上）：**

```bash
ssh-keygen -t rsa -b 4096 -C "jenkins-deploy" -f jenkins-ec2-key
```

**將公鑰添加到 EC2：**

```bash
ssh-copy-id -i jenkins-ec2-key.pub ubuntu@YOUR_EC2_IP
```

**測試連線：**

```bash
ssh -i jenkins-ec2-key ubuntu@YOUR_EC2_IP
```

### 3. Jenkins 憑證設定

進入 **Jenkins Dashboard** → **Manage Jenkins** → **Credentials** → **System** → **Global credentials** → **Add Credentials**

**需要添加的憑證：**

| 憑證 ID | 類型 | 說明 | 值 |
|--------|------|------|-----|
| `docker-username` | Secret text | Docker Hub 用戶名 | 你的 Docker Hub 用戶名 |
| `docker-password` | Secret password | Docker Hub 密碼 | 你的 Docker Hub 密碼 |
| `ec2-host` | Secret text | EC2 公網 IP | 例如: 54.123.45.67 |
| `ec2-user` | Secret text | EC2 SSH 用戶名 | ubuntu（Amazon Linux 用 ec2-user） |
| `ec2-ssh-key` | Secret file | SSH 私鑰 | 上傳 jenkins-ec2-key 檔案 |
| `mysql-root-password` | Secret password | MySQL root 密碼 | 設定強密碼 |
| `mysql-database` | Secret text | MySQL 資料庫名 | workflow_db |
| `mysql-user` | Secret text | MySQL 應用用戶 | workflow_user |
| `mysql-password` | Secret password | MySQL 用戶密碼 | 設定強密碼 |

**Docker 憑證替代方案：**

建立 ID 為 `docker-credentials` 的憑證：
- **類型**: Username with password
- **用戶名**: Docker Hub 用戶名
- **密碼**: Docker Hub 密碼或 access token

### 4. Jenkins 外掛程式

安裝以下外掛：

1. **Docker Pipeline** - Docker 操作
2. **SSH Agent** - SSH 連線
3. **Email Extension Plugin** - 郵件通知
4. **Slack Notification Plugin** (選配) - Slack 通知
5. **Pipeline Utility Steps** - 工具函數

安裝路徑：**Manage Jenkins** → **Plugins** → **Available plugins**

### 5. Jenkins 全域工具設定

路徑：**Manage Jenkins** → **Global Tool Configuration**

**設定 Maven：**
- **名稱**: `Maven 3.8.4`
- **版本**: 從下拉選單選擇或自動安裝

**設定 JDK：**
- **名稱**: `OpenJDK 11`
- **版本**: JDK 11 或指定 JAVA_HOME

### 6. GitLab/GitHub Webhook 設定

**GitLab：**
1. 進入倉庫 → **Settings** → **Webhooks**
2. URL: `http://YOUR_JENKINS_URL/gitlab-webhook/post`
3. 觸發事件: **Push events** → **All branches**

**GitHub：**
1. 進入倉庫 → **Settings** → **Webhooks**
2. URL: `http://YOUR_JENKINS_URL/github-webhook/`
3. 事件: **Pushes**

## 🚀 使用方式

### 第一次部署

1. **提交程式碼到 main 分支：**

```bash
git add .
git commit -m "Initial deployment to EC2"
git push origin main
```

2. **Jenkins 會自動：**
   - ✅ 拉取最新程式碼
   - ✅ 執行程式碼品質檢查
   - ✅ 執行單元測試
   - ✅ 編譯打包所有服務
   - ✅ 建立 Docker 映像
   - ✅ 推送到 Docker Hub
   - ✅ 透過 SSH 部署到 EC2
   - ✅ 啟動所有容器

3. **驗證部署：**

```bash
# 使用驗證腳本
chmod +x scripts/verify-deployment.sh
./scripts/verify-deployment.sh YOUR_EC2_IP ubuntu jenkins-ec2-key
```

### 訪問服務

部署成功後，您可以通過以下網址訪問服務：

- **API Gateway**: http://YOUR_EC2_IP:8080
- **Workflow Service**: http://YOUR_EC2_IP:8081
- **Notification Service**: http://YOUR_EC2_IP:8082
- **Auth Module**: http://YOUR_EC2_IP:8083

## 📊 監控與維護

### 檢查服務狀態

```bash
# SSH 到 EC2
ssh -i jenkins-ec2-key ubuntu@YOUR_EC2_IP

# 查看所有容器狀態
cd /opt/workflow-system
docker-compose ps

# 查看特定服務日誌
docker-compose logs -f api-gateway
docker-compose logs -f workflow-service

# 查看資源使用情況
docker stats
```

### 重新啟動服務

```bash
cd /opt/workflow-system
docker-compose restart
```

### 停止所有服務

```bash
cd /opt/workflow-system
docker-compose down
```

### 清理舊映像

```bash
# 刪除未使用的映像
docker image prune -a --filter "until=168h"

# 刪除已停止的容器
docker container prune -f
```

### 備份 MySQL 資料

```bash
# 備份資料庫
docker exec workflow-mysql mysqldump -u root -p'$MYSQL_ROOT_PASSWORD' workflow_db > backup_$(date +%Y%m%d).sql

# 還原資料庫
docker exec -i workflow-mysql mysql -u root -p'$MYSQL_ROOT_PASSWORD' workflow_db < backup_20260525.sql
```

## 🔧 疑難排解

### 問題 1：無法透過 SSH 連接 EC2

**解決方案：**
- 檢查安全群組是否允許 SSH（端口 22）
- 確認 SSH 金鑰權限：`chmod 600 jenkins-ec2-key`
- 確認 EC2 正在運行且有公網 IP

### 問題 2：Docker 容器啟動失敗

**解決方案：**

```bash
# 查看日誌
docker-compose logs

# 檢查磁碟空間
df -h

# 重啟服務
docker-compose down
docker-compose up -d
```

### 問題 3：服務無法連接 MySQL/Kafka

**解決方案：**
- 確認服務按正確順序啟動（檢查 docker-compose.yml 中的 `depends_on`）
- 等待健康檢查通過
- 檢查網路連線：`docker network inspect workflow-network`

### 問題 4：Jenkins 構建在 Docker push 時失敗

**解決方案：**
- 驗證 Jenkins 中的 Docker 憑證
- 手動測試登入：`docker login -u USERNAME -p PASSWORD`
- 檢查 Docker Hub 速率限制

### 問題 5：端口已被佔用

**解決方案：**

```bash
# 查找使用端口的進程
sudo lsof -i :8080

# 殺死進程或更改 docker-compose.yml 中的端口
sudo kill -9 PID
```

## 📝 Jenkinsfile 主要功能

### 1. 並行構建
多個服務同時構建，加快構建速度

### 2. 程式碼品質檢查
- Checkstyle
- SpotBugs
- PMD

### 3. 單元測試
自動執行測試並生成報告

### 4. Docker 映像管理
- 自動標記版本號和 latest
- 推送到 Docker Hub

### 5. 自動部署
- 透過 SSH 連接 EC2
- 自動生成 docker-compose.yml
- 滾動更新容器

### 6. 自動回滾
如果部署失敗，自動回滾到上一個穩定版本

### 7. 通知機制
- Slack 通知（如果配置）
- 郵件通知
- 構建狀態追蹤

## 🔐 安全最佳實踐

1. **使用 IAM 角色** 而非硬編碼憑證
2. **定期輪換 SSH 金鑰**
3. **啟用 UFW 防火牆**：

```bash
sudo ufw allow 22/tcp
sudo ufw allow 8080/tcp
sudo ufw enable
```

4. **使用 HTTPS** 保護 Jenkins
5. **限制 SSH 訪問** 僅允許 Jenkins IP
6. **使用強密碼** 並儲存在 Jenkins 憑證中
7. **定期更新** EC2 作業系統和 Docker

## 📈 擴展建議

生產環境考慮：

1. 使用 **AWS ECS/EKS** 取代單一 EC2
2. 添加 **負載平衡器** (ALB/NLB)
3. 使用 **RDS** 取代容器化 MySQL
4. 使用 **MSK** (Managed Kafka) 取代容器化 Kafka
5. 實施 **自動擴展** 群組
6. 添加 **監控** (CloudWatch, Prometheus, Grafana)
7. 使用 **S3** 儲存檔案上傳
8. 實施 **藍綠部署** 策略

## 🎯 下一步

1. ✅ 設定 EC2 並安裝必要軟體
2. ✅ 配置 SSH 金鑰實現無密碼訪問
3. ✅ 在 Jenkins 中添加所有憑證
4. ✅ 安裝必要的 Jenkins 外掛
5. ✅ 配置 Jenkins 全域工具（Maven, JDK）
6. ✅ 在 GitLab/GitHub 設定 webhooks
7. ✅ 推送程式碼到 main 分支測試 pipeline
8. ✅ 監控首次部署並驗證服務
9. ✅ 設定監控和警報
10. ✅ 為運維團隊編寫操作手冊

## 📞 支援

如果遇到問題，請檢查：
- Jenkins 控制台輸出
- EC2 上的容器日誌
- 詳細錯誤訊息

參考完整文檔：[EC2_DEPLOYMENT_SETUP.md](EC2_DEPLOYMENT_SETUP.md)

---

**祝您部署順利！** 🚀
