pipeline {
    agent any

    environment {
        // 純本地模式：鏡像名稱定義，不需要登入 credentials 🟢
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
                        echo '📦 [方案B] 啟動 Jenkins 內部動態下載 Maven 機制...'
                        script {
                            // 1. 用 curl 抓標準無污染的 3.8.6 版本
                            sh 'curl -Lfs https://archive.apache.org/dist/maven/maven-3/3.8.6/binaries/apache-maven-3.8.6-bin.tar.gz -o apache-maven-3.8.6-bin.tar.gz'
                            // 2. 解壓縮
                            sh 'tar -xzf apache-maven-3.8.6-bin.tar.gz'

                            echo '🟢 Maven 下載解壓成功！開始執行全模組編譯打包...'
                            // 關鍵修改：直接對根目錄進行全局 clean package，去掉限制的 -pl 參數，徹底解決 Parent 找不到的難題 🟢
                            sh './apache-maven-3.8.6/bin/mvn clean package -DskipTests'
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
            when {
                branch 'master'
            }
            steps {
                script {
                    echo "🚚 正在本地生成 docker-compose-deploy.yml ..."

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
    networks:
      - workflow-network

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
"""
                    echo "=== 執行 docker-compose 滾動重啟服務 ==="
                    sh "docker-compose -f docker-compose-deploy.yml down || true"
                    sh "docker-compose -f docker-compose-deploy.yml up -d"

                    echo "🚀 [SUCCESS] 恭喜！本機所有微服務與中間件已全部啟動完成！"
                }
            }
        }
    }

    post {
        always {
            script {
                try {
                    sh 'docker image prune -f || true'
                    cleanWs()
                } catch (Exception e) {
                    echo "清理工作區被跳過: ${e.message}"
                }
            }
        }
    }
}
