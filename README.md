# Java-K8s-Kafka Workflow System

这是一个基于Spring Boot、Kubernetes和Kafka的微服务工作流系统，包含API网关、工作流服务和通知服务。

## 项目结构

- **common**: 公共DTO、事件和工具类
- **services/api-gateway**: API网关服务
- **services/workflow-service**: 工作流主服务
- **services/notification-service**: 通知服务

## 技术栈

- Spring Boot 2.7+
- Spring Cloud Gateway
- Apache Kafka
- MySQL
- Kubernetes
- Docker

## 快速启动

1. 启动本地环境：
   ```bash
   ./scripts/run-local.sh
   ```

2. 构建所有模块：
   ```bash
   ./scripts/build-all.sh
   ```

## 部署

- 使用Docker Compose进行本地部署
- 使用Kubernetes进行生产部署