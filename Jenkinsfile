pipeline {
    agent any

    environment {
        // 純本地模式：鏡像名稱定義 🟢
        IMAGE_NAME_PREFIX = "workflow-local"
    }

    stages {
        stage('1. Checkout & Setup') {
            steps {
                checkout scm
                script {
                    env.APP_VERSION = sh(script: "git describe --tags --always 2>/dev/null || git rev-parse --short HEAD", returnStdout: true).trim()
                    echo "============================================="
                    echo "🚀 本地極速模式：航母點火成功！"
                    echo "Build Number: ${env.BUILD_NUMBER}"
                    echo "Application Version: ${env.APP_VERSION}"
                    echo "============================================="
                }
            }
        }

        stage('2. Build Java Services') {
            steps {
                echo '📦 [優化方案] 啟動智慧快取與阿里雲鏡像加速機制...'
                script {
                    sh '''
                    if [ ! -f /tmp/apache-maven-3.8.6-bin.tar.gz ]; then
                        curl -Lfs https://archive.apache.org/dist/maven/maven-3/3.8.6/binaries/apache-maven-3.8.6-bin.tar.gz -o /tmp/apache-maven-3.8.6-bin.tar.gz
                    fi
                    tar -xzf /tmp/apache-maven-3.8.6-bin.tar.gz
                    '''

                    // 🟢 修正了 activeProfile 的 XML 語法錯誤
                    sh '''
                    cat << 'EOF' > ./apache-maven-3.8.6/conf/settings.xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
  <localRepository>/var/jenkins_home/.m2/repository</localRepository>
  <mirrors>
    <mirror>
        <id>aliyunmaven</id>
        <mirrorOf>central</mirrorOf>
        <name>aliyun maven</name>
        <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>
  <profiles>
    <profile>
      <id>downloadProfiles</id>
      <repositories>
        <repository>
          <id>aliyunmaven-repo</id>
          <url>https://maven.aliyun.com/repository/public</url>
          <releases><enabled>true</enabled></releases>
          <snapshots><enabled>false</enabled></snapshots>
        </repository>
      </repositories>
    </profile>
  </profiles>
  <activeProfiles>
    <activeProfile>downloadProfiles</activeProfile>
  </activeProfiles>
</settings>
EOF
                    '''

                    echo '🟢 開始執行全模組編譯安裝...'
                    sh './apache-maven-3.8.6/bin/mvn clean install -DskipTests'
                }
            }
        }

        stage('3. Docker Build Local') {
            parallel {
                stage('Build API Gateway') {
                    steps {
                        dir('services/api-gateway') {
                            sh "docker build -t ${env.IMAGE_NAME_PREFIX}/api-gateway:${env.APP_VERSION} ."
                            sh "docker tag ${env.IMAGE_NAME_PREFIX}/api-gateway:${env.APP_VERSION} ${env.IMAGE_NAME_PREFIX}/api-gateway:latest"
                        }
                    }
                }
                stage('Build Workflow Service') {
                    steps {
                        dir('services/workflow-service') {
                            sh "docker build -t ${env.IMAGE_NAME_PREFIX}/workflow-service:${env.APP_VERSION} ."
                            sh "docker tag ${env.IMAGE_NAME_PREFIX}/workflow-service:${env.APP_VERSION} ${env.IMAGE_NAME_PREFIX}/workflow-service:latest"
                        }
                    }
                }
                stage('Build Notification Service') {
                    steps {
                        dir('services/notification-service') {
                            sh "docker build -t ${env.IMAGE_NAME_PREFIX}/notification-service:${env.APP_VERSION} ."
                            sh "docker tag ${env.IMAGE_NAME_PREFIX}/notification-service:${env.APP_VERSION} ${env.IMAGE_NAME_PREFIX}/notification-service:latest"
                        }
                    }
                }
                stage('Build Auth Module') {
                    steps {
                        dir('auth-module') {
                            sh "docker build -t ${env.IMAGE_NAME_PREFIX}/auth-module:${env.APP_VERSION} ."
                            sh "docker tag ${env.IMAGE_NAME_PREFIX}/auth-module:${env.APP_VERSION} ${env.IMAGE_NAME_PREFIX}/auth-module:latest"
                        }
                    }
                }
            }
        }

        stage('4. Deploy to Local EC2') {
            when { branch 'master' }
            steps {
                script {
                    echo "🚚 正在本地生成 docker-compose-deploy.yml ..."

                    // 🟢 加入了 volumes 陣列，確保 MySQL/Kafka 重啟後資料不遺失
                    writeFile file: 'docker-compose-deploy.yml', text: """
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    container_name: workflow-mysql
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: rootpassword
      MYSQL_DATABASE: workflow_db
    ports:
      - "3306:3306"
    networks:
      - workflow-network
    volumes:
      - mysql_data:/var/lib/mysql

  zookeeper:
    image: confluentinc/cp-zookeeper:7.3.0
    container_name: workflow-zookeeper
    restart: always
    ports:
      - "2181:2181"
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    networks:
      - workflow-network
    volumes:
      - zookeeper_data:/var/lib/zookeeper/data

  kafka:
    image: confluentinc/cp-kafka:7.3.0
    container_name: workflow-kafka
    restart: always
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    networks:
      - workflow-network
    volumes:
      - kafka_data:/var/lib/kafka/data

  auth-module:
    image: ${env.IMAGE_NAME_PREFIX}/auth-module:${env.APP_VERSION}
    container_name: auth-module
    restart: always
    ports:
      - "8083:8083"
    networks:
      - workflow-network

  workflow-service:
    image: ${env.IMAGE_NAME_PREFIX}/workflow-service:${env.APP_VERSION}
    container_name: workflow-service
    restart: always
    ports:
      - "8081:8081"
    networks:
      - workflow-network

  notification-service:
    image: ${env.IMAGE_NAME_PREFIX}/notification-service:${env.APP_VERSION}
    container_name: notification-service
    restart: always
    ports:
      - "8082:8082"
    networks:
      - workflow-network

  api-gateway:
    image: ${env.IMAGE_NAME_PREFIX}/api-gateway:${env.APP_VERSION}
    container_name: api-gateway
    restart: always
    ports:
      - "8080:8080"
    networks:
      - workflow-network

networks:
  workflow-network:
    driver: bridge

# 宣告全局的持久化資料卷 🟢
volumes:
  mysql_data:
  zookeeper_data:
  kafka_data:
"""
                    echo "=== 執行 docker-compose 平滑升級服務 ==="
                    // 🟢 移除了 `down`。直接 `up -d`，Docker 會自動判斷哪些微服務更新了並單獨重啟它們，達到零中斷升級！
                    sh "docker-compose -f docker-compose-deploy.yml up -d"

                    echo "🚀 [SUCCESS] 恭喜！本機所有微服務已平滑更新完畢！"
                }
            }
        }
    }

    post {
        always {
            script {
                try {
                    // 只清理沒有被標籤的廢棄鏡像，不影響運行中的服務
                    sh 'docker image prune -f || true'
                    cleanWs()
                } catch (Exception e) {
                    echo "清理工作區被跳過: ${e.message}"
                }
            }
        }
    }
}
