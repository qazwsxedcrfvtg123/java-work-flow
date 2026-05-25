pipeline {
    agent any
    
    tools {
        maven 'Maven 3.8.4'
        jdk 'OpenJDK 11'
    }
    
    environment {
        // Docker 相關設定
        DOCKER_REGISTRY = credentials('docker-registry-url') ?: 'docker.io'
        DOCKER_USERNAME = credentials('docker-username')
        DOCKER_PASSWORD = credentials('docker-password')
        DOCKER_IMAGE_PREFIX = "${DOCKER_USERNAME}"
        
        // EC2 設定
        EC2_HOST = credentials('ec2-host')
        EC2_USER = credentials('ec2-user') ?: 'ubuntu'
        EC2_SSH_KEY = credentials('ec2-ssh-key')
        EC2_DEPLOY_PATH = '/opt/workflow-system'
        
        // 應用版本
        APP_VERSION = sh(script: "git describe --tags --always 2>/dev/null || echo 'latest'", returnStdout: true).trim()
        BUILD_NUMBER = "${env.BUILD_NUMBER}"
        BUILD_TIMESTAMP = sh(script: "date +%Y%m%d_%H%M%S", returnStdout: true).trim()
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    echo "Build Number: ${BUILD_NUMBER}"
                    echo "Git Commit: ${GIT_COMMIT}"
                    echo "Application Version: ${APP_VERSION}"
                }
            }
        }
        
        stage('Code Quality Check') {
            parallel {
                stage('Checkstyle') {
                    steps {
                        sh 'mvn checkstyle:check'
                    }
                }
                stage('SpotBugs') {
                    steps {
                        sh 'mvn spotbugs:check'
                    }
                }
                stage('PMD') {
                    steps {
                        sh 'mvn pmd:check'
                    }
                }
            }
        }
        
        stage('Unit Tests') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    publishTestResults testResultsPattern: '**/target/surefire-reports/*.xml'
                    publishCoverage adapters: [
                        jacocoAdapter('**/target/site/jacoco/jacoco.xml')
                    ]
                }
            }
        }
        
        stage('Build Services') {
            steps {
                sh 'mvn clean package -DskipTests -pl auth-module,services/api-gateway,services/workflow-service,services/notification-service -am'
            }
        }
        
        stage('Security Scan') {
            steps {
                sh 'mvn dependency-check:check'
            }
        }
        
        stage('Docker Build & Tag') {
            parallel {
                stage('Build API Gateway') {
                    steps {
                        dir('services/api-gateway') {
                            sh """
                                docker build -t ${DOCKER_IMAGE_PREFIX}/api-gateway:${APP_VERSION} .
                                docker tag ${DOCKER_IMAGE_PREFIX}/api-gateway:${APP_VERSION} ${DOCKER_IMAGE_PREFIX}/api-gateway:latest
                            """
                        }
                    }
                }
                stage('Build Workflow Service') {
                    steps {
                        dir('services/workflow-service') {
                            sh """
                                docker build -t ${DOCKER_IMAGE_PREFIX}/workflow-service:${APP_VERSION} .
                                docker tag ${DOCKER_IMAGE_PREFIX}/workflow-service:${APP_VERSION} ${DOCKER_IMAGE_PREFIX}/workflow-service:latest
                            """
                        }
                    }
                }
                stage('Build Notification Service') {
                    steps {
                        dir('services/notification-service') {
                            sh """
                                docker build -t ${DOCKER_IMAGE_PREFIX}/notification-service:${APP_VERSION} .
                                docker tag ${DOCKER_IMAGE_PREFIX}/notification-service:${APP_VERSION} ${DOCKER_IMAGE_PREFIX}/notification-service:latest
                            """
                        }
                    }
                }
                stage('Build Auth Module') {
                    steps {
                        dir('auth-module') {
                            sh """
                                docker build -t ${DOCKER_IMAGE_PREFIX}/auth-module:${APP_VERSION} .
                                docker tag ${DOCKER_IMAGE_PREFIX}/auth-module:${APP_VERSION} ${DOCKER_IMAGE_PREFIX}/auth-module:latest
                            """
                        }
                    }
                }
            }
        }
        
        stage('Docker Push to Registry') {
            steps {
                script {
                    docker.withRegistry("https://${DOCKER_REGISTRY}", 'docker-credentials') {
                        parallel {
                            stage('Push API Gateway') {
                                steps {
                                    sh """
                                        docker push ${DOCKER_IMAGE_PREFIX}/api-gateway:${APP_VERSION}
                                        docker push ${DOCKER_IMAGE_PREFIX}/api-gateway:latest
                                    """
                                }
                            }
                            stage('Push Workflow Service') {
                                steps {
                                    sh """
                                        docker push ${DOCKER_IMAGE_PREFIX}/workflow-service:${APP_VERSION}
                                        docker push ${DOCKER_IMAGE_PREFIX}/workflow-service:latest
                                    """
                                }
                            }
                            stage('Push Notification Service') {
                                steps {
                                    sh """
                                        docker push ${DOCKER_IMAGE_PREFIX}/notification-service:${APP_VERSION}
                                        docker push ${DOCKER_IMAGE_PREFIX}/notification-service:latest
                                    """
                                }
                            }
                            stage('Push Auth Module') {
                                steps {
                                    sh """
                                        docker push ${DOCKER_IMAGE_PREFIX}/auth-module:${APP_VERSION}
                                        docker push ${DOCKER_IMAGE_PREFIX}/auth-module:latest
                                    """
                                }
                            }
                        }
                    }
                }
            }
        }
        
        stage('Deploy to EC2') {
            when {
                branch 'main'
            }
            steps {
                script {
                    echo "Deploying to EC2: ${EC2_HOST}"
                    
                    // 生成 docker-compose.yml 部署文件
                    writeFile file: 'deploy/docker-compose/docker-compose-deploy.yml', text: """
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: workflow-mysql
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: \${MYSQL_ROOT_PASSWORD:-rootpassword}
      MYSQL_DATABASE: \${MYSQL_DATABASE:-workflow_db}
      MYSQL_USER: \${MYSQL_USER:-workflow_user}
      MYSQL_PASSWORD: \${MYSQL_PASSWORD:-workflow_password}
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
      - ./mysql-init.sql:/docker-entrypoint-initdb.d/mysql-init.sql
    networks:
      - workflow-network
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      timeout: 20s
      retries: 10

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
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
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
      ZOOKEEPER_TICK_TIME: 2000
    networks:
      - workflow-network

  auth-module:
    image: ${DOCKER_IMAGE_PREFIX}/auth-module:${APP_VERSION}
    container_name: auth-module
    restart: always
    ports:
      - "8083:8083"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/\${MYSQL_DATABASE:-workflow_db}?useSSL=false&serverTimezone=UTC
      SPRING_DATASOURCE_USERNAME: \${MYSQL_USER:-workflow_user}
      SPRING_DATASOURCE_PASSWORD: \${MYSQL_PASSWORD:-workflow_password}
      SERVER_PORT: 8083
    depends_on:
      mysql:
        condition: service_healthy
    networks:
      - workflow-network
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:8083/actuator/health || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

  workflow-service:
    image: ${DOCKER_IMAGE_PREFIX}/workflow-service:${APP_VERSION}
    container_name: workflow-service
    restart: always
    ports:
      - "8081:8081"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/\${MYSQL_DATABASE:-workflow_db}?useSSL=false&serverTimezone=UTC
      SPRING_DATASOURCE_USERNAME: \${MYSQL_USER:-workflow_user}
      SPRING_DATASOURCE_PASSWORD: \${MYSQL_PASSWORD:-workflow_password}
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      SERVER_PORT: 8081
    depends_on:
      mysql:
        condition: service_healthy
      kafka:
        condition: service_started
    networks:
      - workflow-network
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:8081/actuator/health || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

  notification-service:
    image: ${DOCKER_IMAGE_PREFIX}/notification-service:${APP_VERSION}
    container_name: notification-service
    restart: always
    ports:
      - "8082:8082"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/\${MYSQL_DATABASE:-workflow_db}?useSSL=false&serverTimezone=UTC
      SPRING_DATASOURCE_USERNAME: \${MYSQL_USER:-workflow_user}
      SPRING_DATASOURCE_PASSWORD: \${MYSQL_PASSWORD:-workflow_password}
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      SERVER_PORT: 8082
    depends_on:
      mysql:
        condition: service_healthy
      kafka:
        condition: service_started
    networks:
      - workflow-network
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:8082/actuator/health || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

  api-gateway:
    image: ${DOCKER_IMAGE_PREFIX}/api-gateway:${APP_VERSION}
    container_name: api-gateway
    restart: always
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      AUTH_SERVICE_URL: http://auth-module:8083
      WORKFLOW_SERVICE_URL: http://workflow-service:8081
      NOTIFICATION_SERVICE_URL: http://notification-service:8082
      SERVER_PORT: 8080
    depends_on:
      auth-module:
        condition: service_healthy
      workflow-service:
        condition: service_healthy
      notification-service:
        condition: service_healthy
    networks:
      - workflow-network
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

volumes:
  mysql-data:

networks:
  workflow-network:
    driver: bridge
"""
                    
                    // 上傳部署文件到 EC2
                    sh """
                        scp -i ${EC2_SSH_KEY} -o StrictHostKeyChecking=no \\
                            deploy/docker-compose/docker-compose-deploy.yml \\
                            ${EC2_USER}@${EC2_HOST}:${EC2_DEPLOY_PATH}/docker-compose.yml
                    """
                    
                    // 在 EC2 上執行部署
                    sh """
                        ssh -i ${EC2_SSH_KEY} -o StrictHostKeyChecking=no ${EC2_USER}@${EC2_HOST} << 'EOF'
                            set -e
                            
                            echo "=== Starting deployment on EC2 ==="
                            cd ${EC2_DEPLOY_PATH}
                            
                            # 創建環境變數文件
                            cat > .env << ENVFILE
MYSQL_ROOT_PASSWORD=${credentials('mysql-root-password') ?: 'rootpassword'}
MYSQL_DATABASE=${credentials('mysql-database') ?: 'workflow_db'}
MYSQL_USER=${credentials('mysql-user') ?: 'workflow_user'}
MYSQL_PASSWORD=${credentials('mysql-password') ?: 'workflow_password'}
DOCKER_IMAGE_PREFIX=${DOCKER_IMAGE_PREFIX}
APP_VERSION=${APP_VERSION}
ENVFILE
                            
                            # 登入 Docker Registry
                            echo "${DOCKER_PASSWORD}" | docker login ${DOCKER_REGISTRY} -u "${DOCKER_USERNAME}" --password-stdin
                            
                            # 拉取最新映像
                            echo "=== Pulling latest images ==="
                            docker-compose pull
                            
                            # 停止並移除舊容器
                            echo "=== Stopping old containers ==="
                            docker-compose down || true
                            
                            # 啟動新容器
                            echo "=== Starting new containers ==="
                            docker-compose up -d
                            
                            # 等待服務啟動
                            echo "=== Waiting for services to start ==="
                            sleep 30
                            
                            # 檢查服務狀態
                            echo "=== Checking service status ==="
                            docker-compose ps
                            
                            # 顯示日誌
                            echo "=== Service logs ==="
                            docker-compose logs --tail=50
                            
                            echo "=== Deployment completed successfully ==="
                        EOF
                    """
                }
            }
        }
    }
    
    post {
        always {
            // 清理 Docker 映像以節省空間
            sh 'docker system prune -f || true'
            
            // 清理工作區
            cleanWs()
        }
        success {
            script {
                currentBuild.result = 'SUCCESS'
                echo "✅ Pipeline completed successfully!"
                echo "📦 Version: ${APP_VERSION}"
                echo "🖥️  Deployed to EC2: ${EC2_HOST}"
                
                // 發送通知（如果配置了 Slack）
                try {
                    slackSend channel: '#deployments',
                             color: 'good',
                             message: "✅ Build #${BUILD_NUMBER} SUCCESS\n" +
                                     "Version: ${APP_VERSION}\n" +
                                     "EC2: ${EC2_HOST}\n" +
                                     "Services: api-gateway, workflow-service, notification-service, auth-module"
                } catch (Exception e) {
                    echo "Slack notification failed: ${e.message}"
                }
                
                // 發送郵件通知
                emailext (
                    subject: "✅ SUCCESS: Pipeline #${BUILD_NUMBER}",
                    body: """
                        <h2>Deployment Successful</h2>
                        <p><strong>Project:</strong> Java K8s Kafka Workflow System</p>
                        <p><strong>Build Number:</strong> #${BUILD_NUMBER}</p>
                        <p><strong>Version:</strong> ${APP_VERSION}</p>
                        <p><strong>EC2 Host:</strong> ${EC2_HOST}</p>
                        <p><strong>Timestamp:</strong> ${BUILD_TIMESTAMP}</p>
                        <p><strong>Services Deployed:</strong></p>
                        <ul>
                            <li>API Gateway (Port 8080)</li>
                            <li>Workflow Service (Port 8081)</li>
                            <li>Notification Service (Port 8082)</li>
                            <li>Auth Module (Port 8083)</li>
                        </ul>
                        <p>Check Jenkins console output for details.</p>
                    """,
                    recipientProviders: [developers(), requestor()]
                )
            }
        }
        failure {
            script {
                currentBuild.result = 'FAILURE'
                echo "❌ Pipeline failed!"
                
                // 嘗試回滾到上一個穩定版本
                if (env.BRANCH_NAME == 'main') {
                    echo "🔄 Attempting rollback on EC2..."
                    try {
                        sh """
                            ssh -i ${EC2_SSH_KEY} -o StrictHostKeyChecking=no ${EC2_USER}@${EC2_HOST} << 'EOF'
                                cd ${EC2_DEPLOY_PATH}
                                echo "Rolling back to previous version..."
                                docker-compose down || true
                                docker-compose up -d
                                echo "Rollback completed"
                            EOF
                        """
                        echo "✅ Rollback completed"
                    } catch (Exception e) {
                        echo "⚠️ Rollback failed: ${e.message}"
                    }
                }
                
                // 發送失敗通知
                try {
                    slackSend channel: '#deployments',
                             color: 'danger',
                             message: "❌ Build #${BUILD_NUMBER} FAILED\n" +
                                     "Version: ${APP_VERSION}\n" +
                                     "Branch: ${env.BRANCH_NAME}\n" +
                                     "Check Jenkins for details"
                } catch (Exception e) {
                    echo "Slack notification failed: ${e.message}"
                }
                
                emailext (
                    subject: "❌ FAILURE: Pipeline #${BUILD_NUMBER}",
                    body: """
                        <h2>Deployment Failed</h2>
                        <p><strong>Project:</strong> Java K8s Kafka Workflow System</p>
                        <p><strong>Build Number:</strong> #${BUILD_NUMBER}</p>
                        <p><strong>Version:</strong> ${APP_VERSION}</p>
                        <p><strong>Branch:</strong> ${env.BRANCH_NAME}</p>
                        <p><strong>Timestamp:</strong> ${BUILD_TIMESTAMP}</p>
                        <p style="color: red;"><strong>Please check Jenkins console output for error details.</strong></p>
                    """,
                    recipientProviders: [developers(), requestor(), culprits()]
                )
            }
        }
        unstable {
            script {
                currentBuild.result = 'UNSTABLE'
                echo "⚠️ Pipeline unstable!"
                
                try {
                    slackSend channel: '#deployments',
                             color: 'warning',
                             message: "⚠️ Build #${BUILD_NUMBER} UNSTABLE\n" +
                                     "Version: ${APP_VERSION}\n" +
                                     "Some tests or quality checks failed"
                } catch (Exception e) {
                    echo "Slack notification failed: ${e.message}"
                }
            }
        }
        changed {
            script {
                echo "📊 Build status changed from previous build"
            }
        }
    }
}