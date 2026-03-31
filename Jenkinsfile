pipeline {
    agent any
    
    tools {
        maven 'Maven 3.8.4'
        jdk 'OpenJDK 11'
    }
    
    environment {
        // Docker 相關設定
        DOCKER_REGISTRY = 'docker.io'
        DOCKER_USERNAME = credentials('docker-username')
        DOCKER_PASSWORD = credentials('docker-password')
        
        // Kubernetes 設定
        KUBECONFIG = '/var/jenkins_home/.kube/config'
        
        // 應用版本
        APP_VERSION = sh(script: "git describe --tags --always", returnStdout: true).trim()
        BUILD_NUMBER = "${env.BUILD_NUMBER}"
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
        
        stage('Build') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }
        
        stage('Security Scan') {
            steps {
                sh 'mvn dependency-check:check'
            }
        }
        
        stage('Docker Build') {
            parallel {
                stage('Build API Gateway') {
                    steps {
                        dir('services/api-gateway') {
                            sh "docker build -t ${DOCKER_REGISTRY}/api-gateway:${APP_VERSION} ."
                        }
                    }
                }
                stage('Build Workflow Service') {
                    steps {
                        dir('services/workflow-service') {
                            sh "docker build -t ${DOCKER_REGISTRY}/workflow-service:${APP_VERSION} ."
                        }
                    }
                }
                stage('Build Notification Service') {
                    steps {
                        dir('services/notification-service') {
                            sh "docker build -t ${DOCKER_REGISTRY}/notification-service:${APP_VERSION} ."
                        }
                    }
                }
            }
        }
        
        stage('Docker Push') {
            steps {
                script {
                    docker.withRegistry("https://${DOCKER_REGISTRY}", 'docker-credentials') {
                        parallel {
                            stage('Push API Gateway') {
                                steps {
                                    sh "docker push ${DOCKER_REGISTRY}/api-gateway:${APP_VERSION}"
                                }
                            }
                            stage('Push Workflow Service') {
                                steps {
                                    sh "docker push ${DOCKER_REGISTRY}/workflow-service:${APP_VERSION}"
                                }
                            }
                            stage('Push Notification Service') {
                                steps {
                                    sh "docker push ${DOCKER_REGISTRY}/notification-service:${APP_VERSION}"
                                }
                            }
                        }
                    }
                }
            }
        }
        
        stage('Integration Tests') {
            steps {
                sh './scripts/run-integration-tests.sh'
            }
        }
        
        stage('Deploy to Dev') {
            when {
                branch 'develop'
            }
            steps {
                script {
                    sh """
                        kubectl config use-context dev-cluster
                        sed -i "s/latest/${APP_VERSION}/g" deploy/k8s/*/deployment.yml
                        kubectl apply -f deploy/k8s/namespace.yml
                        kubectl apply -f deploy/k8s/configmap.yml
                        kubectl apply -f deploy/k8s/secret.yml
                        kubectl apply -f deploy/k8s/mysql/
                        kubectl apply -f deploy/k8s/kafka/
                        kubectl apply -f deploy/k8s/workflow-service/
                        kubectl apply -f deploy/k8s/notification-service/
                        kubectl apply -f deploy/k8s/api-gateway/
                    """
                }
            }
        }
        
        stage('Deploy to Staging') {
            when {
                branch 'staging'
            }
            steps {
                script {
                    sh """
                        kubectl config use-context staging-cluster
                        sed -i "s/latest/${APP_VERSION}/g" deploy/k8s/*/deployment.yml
                        kubectl apply -f deploy/k8s/
                    """
                }
            }
        }
        
        stage('Deploy to Production') {
            when {
                branch 'main'
            }
            steps {
                input message: "Deploy version ${APP_VERSION} to production?",
                       ok: "Deploy"
                
                script {
                    sh """
                        kubectl config use-context prod-cluster
                        sed -i "s/latest/${APP_VERSION}/g" deploy/k8s/*/deployment.yml
                        kubectl apply -f deploy/k8s/
                    """
                }
            }
        }
    }
    
    post {
        always {
            // 清理工作
            cleanWs()
        }
        success {
            script {
                currentBuild.result = 'SUCCESS'
                echo "Pipeline completed successfully!"
                slackSend channel: '#deployments',
                         color: 'good',
                         message: "✅ Build #${BUILD_NUMBER} SUCCESS - ${APP_VERSION}"
            }
        }
        failure {
            script {
                currentBuild.result = 'FAILURE'
                echo "Pipeline failed!"
                slackSend channel: '#deployments',
                         color: 'danger',
                         message: "❌ Build #${BUILD_NUMBER} FAILED - ${APP_VERSION}"
            }
        }
        unstable {
            script {
                currentBuild.result = 'UNSTABLE'
                slackSend channel: '#deployments',
                         color: 'warning',
                         message: "⚠️ Build #${BUILD_NUMBER} UNSTABLE - ${APP_VERSION}"
            }
        }
    }
}