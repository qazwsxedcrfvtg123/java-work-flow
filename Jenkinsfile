pipeline {
    agent any

    environment {
        // 純本地模式：直接把鏡像名稱定義好即可，不需要登入憑證 🟢
        IMAGE_NAME_PREFIX = "workflow-local"
        EC2_DEPLOY_PATH = '/opt/workflow-system'
    }

    stages {
        stage('1. Checkout & Setup') {
            steps {
                checkout scm
                script {
                    // 自動取得 Git 版本號，如果沒有標籤就用簡短的 Commit ID
                    env.APP_VERSION = sh(script: "git describe --tags --always 2>/dev/null || git rev-parse --short HEAD", returnStdout: true).trim()

                    echo "============================================="
                    echo "🚀 本地模式航空母艦點火！"
                    echo "Build Number: ${env.BUILD_NUMBER}"
                    echo "Application Version: ${env.APP_VERSION}"
                    echo "============================================="
                }
            }
        }

        stage('2. Build Java Services') {
            steps {
                echo '📦 正在使用內建 Maven Wrapper 進行打包...'
                sh 'chmod +x ./mvnw'
                sh './mvnw clean package -DskipTests -pl auth-module,services/api-gateway,services/workflow-service,services/notification-service -am'
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

                    // 這裡的 image 欄位全部改用我們剛剛在本地做好的 workflow-local/...
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

                    echo "=== 拔除所有登入邏輯，直接本機啟動微服務群 ==="
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
                    // 清理垃圾，只留下最新打包好和正在運行的鏡像
                    sh 'docker image prune -f || true'
                    cleanWs()
                } catch (Exception e) {
                    echo "清理工作區跳過: ${e.message}"
                }
            }
        }
    }
}
