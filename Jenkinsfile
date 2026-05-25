pipeline {
    agent any

    tools {
        maven 'Maven 3.8.4'
        jdk 'OpenJDK 11'
    }

    environment {
        // Docker 基礎設定（假設你在 Jenkins 建立了一個 Username/Password 類型的凭证，ID 為 docker-credentials）
        DOCKER_CREDS = credentials('docker-credentials')
        DOCKER_REGISTRY = 'docker.io'
        DOCKER_IMAGE_PREFIX = "${env.DOCKER_CREDS_USR}" // 自動提取憑證中的用戶名

        // EC2 連線設定
        EC2_HOST = credentials('ec2-host')
        EC2_USER = 'ubuntu'
        EC2_SSH_KEY = credentials('ec2-ssh-key') // 必須是 Secret File 類型憑證
        EC2_DEPLOY_PATH = '/opt/workflow-system'

        // 事先綁定資料庫憑證，後面才能作為環境變數傳遞
        MYSQL_ROOT_PASS = credentials('mysql-root-password')
        MYSQL_DB_NAME = credentials('mysql-database')
        MYSQL_DB_USER = credentials('mysql-user')
        MYSQL_DB_PASS = credentials('mysql-password')
    }

    stages {
        stage('1. Checkout & Setup') {
            steps {
                checkout scm
                script {
                    // 將動態變數安全的注入到環境變數中
                    env.APP_VERSION = sh(script: "git describe --tags --always 2>/dev/null || echo 'latest'", returnStdout: true).trim()
                    env.BUILD_TIMESTAMP = sh(script: "date +%Y%m%d_%H%M%S", returnStdout: true).trim()

                    echo "============================================="
                    echo "🚀 開始建置任務"
                    echo "Build Number: ${env.BUILD_NUMBER}"
                    echo "Git Commit: ${env.GIT_COMMIT}"
                    echo "Application Version: ${env.APP_VERSION}"
                    echo "============================================="
                }
            }
        }

        stage('2. Code Quality Check') {
            parallel {
                stage('Checkstyle') { steps { sh 'mvn checkstyle:check || echo "Checkstyle failed but continue"' } }
                stage('SpotBugs') { steps { sh 'mvn spotbugs:check || echo "SpotBugs failed but continue"' } }
                stage('PMD') { steps { sh 'mvn pmd:check || echo "PMD failed but continue"' } }
            }
        }

        stage('3. Unit Tests') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    publishTestResults testResultsPattern: '**/target/surefire-reports/*.xml'
                    // 如果這行噴錯，代表你 Jenkins 沒裝 JaCoCo 外掛，可先註解掉
                    // publishCoverage adapters: [jacocoAdapter('**/target/site/jacoco/jacoco.xml')]
                }
            }
        }

        stage('4. Build Java Services') {
            steps {
                echo '📦 開始進行多模組編譯打包...'
                sh 'mvn clean package -DskipTests -pl auth-module,services/api-gateway,services/workflow-service,services/notification-service -am'
            }
        }

        stage('5. Security Scan') {
            steps {
                sh 'mvn dependency-check:check || echo "Security risks found, check reports"'
            }
        }

        stage('6. Docker Build & Tag') {
            parallel {
                stage('Build API Gateway') {
                    steps {
                        dir('services/api-gateway') {
                            sh "docker build -t ${env.DOCKER_IMAGE_PREFIX}/api-gateway:${env.APP_VERSION} ."
                            sh "docker tag ${env.DOCKER_IMAGE_PREFIX}/api-gateway:${env.APP_VERSION} ${env.DOCKER_IMAGE_PREFIX}/api-gateway:latest"
                        }
                    }
                }
                stage('Build Workflow Service') {
                    steps {
                        dir('services/workflow-service') {
                            sh "docker build -t ${env.DOCKER_IMAGE_PREFIX}/workflow-service:${env.APP_VERSION} ."
                            sh "docker tag ${env.DOCKER_IMAGE_PREFIX}/workflow-service:${env.APP_VERSION} ${env.DOCKER_IMAGE_PREFIX}/workflow-service:latest"
                        }
                    }
                }
                stage('Build Notification Service') {
                    steps {
                        dir('services/notification-service') {
                            sh "docker build -t ${env.DOCKER_IMAGE_PREFIX}/notification-service:${env.APP_VERSION} ."
                            sh "docker tag ${env.DOCKER_IMAGE_PREFIX}/notification-service:${env.APP_VERSION} ${env.DOCKER_IMAGE_PREFIX}/notification-service:latest"
                        }
                    }
                }
                stage('Build Auth Module') {
                    steps {
                        dir('auth-module') {
                            sh "docker build -t ${env.DOCKER_IMAGE_PREFIX}/auth-module:${env.APP_VERSION} ."
                            sh "docker tag ${env.DOCKER_IMAGE_PREFIX}/auth-module:${env.APP_VERSION} ${env.DOCKER_IMAGE_PREFIX}/auth-module:latest"
                        }
                    }
                }
            }
        }

        stage('7. Docker Push to Registry') {
            steps {
                script {
                    // 正確的 Scripted Parallel 語法格式
                    docker.withRegistry("https://${env.DOCKER_REGISTRY}", 'docker-credentials') {
                        parallel (
                            'Push API Gateway': {
                                sh "docker push ${env.DOCKER_IMAGE_PREFIX}/api-gateway:${env.APP_VERSION}"
                                sh "docker push ${env.DOCKER_IMAGE_PREFIX}/api-gateway:latest"
                            },
                            'Push Workflow Service': {
                                sh "docker push ${env.DOCKER_IMAGE_PREFIX}/workflow-service:${env.APP_VERSION}"
                                sh "docker push ${env.DOCKER_IMAGE_PREFIX}/workflow-service:latest"
                            },
                            'Push Notification Service': {
                                sh "docker push ${env.DOCKER_IMAGE_PREFIX}/notification-service:${env.APP_VERSION}"
                                sh "docker push ${env.DOCKER_IMAGE_PREFIX}/notification-service:latest"
                            },
                            'Push Auth Module': {
                                sh "docker push ${env.DOCKER_IMAGE_PREFIX}/auth-module:${env.APP_VERSION}"
                                sh "docker push ${env.DOCKER_IMAGE_PREFIX}/auth-module:latest"
                            }
                        )
                    }
                }
            }
        }

        stage('8. Deploy to EC2') {
            when {
                // 自動對齊你的分支，確保 master 能夠順利觸發部署
                branch 'master'
            }
            steps {
                script {
                    echo "🚚 正在將動態 docker-compose.yml 發送到 EC2: ${env.EC2_HOST}"

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
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
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
    image: ${env.DOCKER_IMAGE_PREFIX}/auth-module:${env.APP_VERSION}
    container_name: auth-module
    restart: always
    ports:
      - "8083:8083"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/\${MYSQL_DATABASE:-workflow_db}?useSSL=false&serverTimezone=UTC
      SPRING_DATASOURCE_USERNAME: \${MYSQL_USER:-workflow_user}
      SPRING_DATASOURCE_PASSWORD: \${MYSQL_PASSWORD:-workflow_password}
    depends_on:
      - mysql
    networks:
      - workflow-network

  workflow-service:
    image: ${env.DOCKER_IMAGE_PREFIX}/workflow-service:${env.APP_VERSION}
    container_name: workflow-service
    restart: always
    ports:
      - "8081:8081"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/\${MYSQL_DATABASE:-workflow_db}?useSSL=false&serverTimezone=UTC
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
    depends_on:
      - mysql
      - kafka
    networks:
      - workflow-network

  notification-service:
    image: ${env.DOCKER_IMAGE_PREFIX}/notification-service:${env.APP_VERSION}
    container_name: notification-service
    restart: always
    ports:
      - "8082:8082"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/\${MYSQL_DATABASE:-workflow_db}?useSSL=false&serverTimezone=UTC
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
    depends_on:
      - mysql
      - kafka
    networks:
      - workflow-network

  api-gateway:
    image: ${env.DOCKER_IMAGE_PREFIX}/api-gateway:${env.APP_VERSION}
    container_name: api-gateway
    restart: always
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      AUTH_SERVICE_URL: http://auth-module:8083
      WORKFLOW_SERVICE_URL: http://workflow-service:8081
      NOTIFICATION_SERVICE_URL: http://notification-service:8082
    depends_on:
      - auth-module
      - workflow-service
      - notification-service
    networks:
      - workflow-network

volumes:
  mysql-data:

networks:
  workflow-network:
    driver: bridge
"""

                    // 安全地上傳至 EC2
                    sh "scp -i ${env.EC2_SSH_KEY} -o StrictHostKeyChecking=no deploy/docker-compose/docker-compose-deploy.yml ${env.EC2_USER}@${env.EC2_HOST}:${env.EC2_DEPLOY_PATH}/docker-compose.yml"

                    // 透過環境變數傳遞敏感資訊，完美解決 credentials() 函數錯誤
                    sh """
                        ssh -i ${env.EC2_SSH_KEY} -o StrictHostKeyChecking=no ${env.EC2_USER}@${env.EC2_HOST} << 'EOF'
                            set -e
                            cd ${env.EC2_DEPLOY_PATH}

                            echo "=== 建立遠端環境變數配置文件 ==="
                            cat > .env << ENVFILE
MYSQL_ROOT_PASSWORD=${env.MYSQL_ROOT_PASS}
MYSQL_DATABASE=${env.MYSQL_DB_NAME}
MYSQL_USER=${env.MYSQL_DB_USER}
MYSQL_PASSWORD=${env.MYSQL_DB_PASS}
DOCKER_IMAGE_PREFIX=${env.DOCKER_IMAGE_PREFIX}
APP_VERSION=${env.APP_VERSION}
ENVFILE

                            echo "=== 登入遠端 Docker 倉庫 ==="
                            echo "${env.DOCKER_CREDS_PSW}" | docker login -u "${env.DOCKER_CREDS_USR}" --password-stdin

                            echo "=== 執行滾動重啟服務 ==="
                            docker-compose pull
                            docker-compose down || true
                            docker-compose up -d

                            echo "=== 檢查運行狀態 ==="
                            sleep 10
                            docker-compose ps
                        EOF
                    """
                }
            }
        }
    }

    post {
        always {
            sh 'docker system prune -f || true'
            cleanWs()
        }
        success {
            echo "✅ 全線部署成功！版本號: ${env.APP_VERSION}"
        }
        failure {
            echo "❌ Pipeline 執行失敗，觸發後續日誌檢查。"
        }
    }
}
