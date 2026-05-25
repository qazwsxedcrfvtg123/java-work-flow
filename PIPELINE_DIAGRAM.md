# CI/CD Pipeline Flow Diagram

## Complete Deployment Flow

```mermaid
graph TB
    A[Developer] -->|git push| B[GitLab/GitHub]
    B -->|Webhook| C[Jenkins Server]
    C -->|1. Checkout| D[Source Code]
    D -->|2. Quality Check| E[Code Analysis]
    E -->|3. Unit Tests| F[Test Reports]
    F -->|4. Build| G[Maven Packages]
    G -->|5. Security Scan| H[Vulnerability Report]
    H -->|6. Docker Build| I[Docker Images]
    I -->|7. Docker Push| J[Docker Hub Registry]
    J -->|8. Deploy via SSH| K[AWS EC2 Instance]
    K -->|9. Docker Compose| L[Running Containers]
    L -->|10. Health Check| M[Services Ready]
    M -->|Notification| N[Slack/Email]
```

## Jenkins Pipeline Stages

```mermaid
graph LR
    A[Checkout] --> B[Quality Check]
    B --> C[Unit Tests]
    C --> D[Build Services]
    D --> E[Security Scan]
    E --> F[Docker Build]
    F --> G[Docker Push]
    G --> H[Deploy to EC2]
    H --> I[Post Actions]
    
    style A fill:#e1f5ff
    style B fill:#fff4e1
    style C fill:#fff4e1
    style D fill:#e1ffe1
    style E fill:#fff4e1
    style F fill:#ffe1f5
    style G fill:#ffe1f5
    style H fill:#e1e1ff
    style I fill:#f5e1ff
```

## Parallel Execution

```mermaid
graph TB
    subgraph "Code Quality (Parallel)"
        A1[Checkstyle] 
        A2[SpotBugs]
        A3[PMD]
    end
    
    subgraph "Docker Build (Parallel)"
        B1[API Gateway]
        B2[Workflow Service]
        B3[Notification Service]
        B4[Auth Module]
    end
    
    subgraph "Docker Push (Parallel)"
        C1[Push API Gateway]
        C2[Push Workflow Service]
        C3[Push Notification Service]
        C4[Push Auth Module]
    end
    
    A1 & A2 & A3 --> D[Build Services]
    D --> B1 & B2 & B3 & B4
    B1 & B2 & B3 & B4 --> C1 & C2 & C3 & C4
    C1 & C2 & C3 & C4 --> E[Deploy to EC2]
```

## EC2 Deployment Architecture

```mermaid
graph TB
    subgraph "AWS EC2 Instance"
        subgraph "Docker Network: workflow-network"
            DB[(MySQL<br/>Port 3306)]
            ZK[Zookeeper<br/>Port 2181]
            KF[Kafka<br/>Port 9092]
            AUTH[Auth Module<br/>Port 8083]
            WF[Workflow Service<br/>Port 8081]
            NOTIF[Notification Service<br/>Port 8082]
            GW[API Gateway<br/>Port 8080]
        end
        
        VOL[(MySQL Volume<br/>Persistent Storage)]
    end
    
    Internet -->|Port 8080| GW
    GW -->|HTTP| AUTH
    GW -->|HTTP| WF
    GW -->|HTTP| NOTIF
    
    WF -->|JDBC| DB
    NOTIF -->|JDBC| DB
    AUTH -->|JDBC| DB
    
    WF -->|Events| KF
    NOTIF -->|Events| KF
    
    KF --> ZK
    DB --> VOL
    
    style GW fill:#4CAF50
    style AUTH fill:#2196F3
    style WF fill:#2196F3
    style NOTIF fill:#2196F3
    style KF fill:#FF9800
    style ZK fill:#FF9800
    style DB fill:#9C27B0
```

## Service Dependencies

```mermaid
graph LR
    subgraph "Infrastructure"
        MYSQL[(MySQL)]
        ZK[Zookeeper]
        KAFKA[Kafka]
    end
    
    subgraph "Microservices"
        AUTH[Auth Module]
        WORKFLOW[Workflow Service]
        NOTIFY[Notification Service]
        GATEWAY[API Gateway]
    end
    
    MYSQL --> AUTH
    MYSQL --> WORKFLOW
    MYSQL --> NOTIFY
    
    ZK --> KAFKA
    KAFKA --> WORKFLOW
    KAFKA --> NOTIFY
    
    AUTH --> GATEWAY
    WORKFLOW --> GATEWAY
    NOTIFY --> GATEWAY
    
    style MYSQL fill:#9C27B0
    style ZK fill:#FF9800
    style KAFKA fill:#FF9800
    style AUTH fill:#2196F3
    style WORKFLOW fill:#2196F3
    style NOTIFY fill:#2196F3
    style GATEWAY fill:#4CAF50
```

## Deployment Timeline

```mermaid
gantt
    title CI/CD Pipeline Timeline
    dateFormat X
    axisFormat %s
    
    section Build & Test
    Checkout           :0, 10
    Quality Checks     :10, 60
    Unit Tests         :10, 90
    Maven Build        :100, 120
    Security Scan      :220, 30
    
    section Docker
    Build Images       :230, 120
    Push to Registry   :350, 60
    
    section Deploy
    Upload to EC2      :410, 20
    Start Containers   :430, 60
    Health Checks      :490, 30
    
    section Notify
    Send Notifications :520, 10
```

## Branch Strategy

```mermaid
graph TB
    subgraph "Development"
        DEV[develop branch]
        FEAT[feature branches]
    end
    
    subgraph "Production"
        MAIN[main branch]
    end
    
    FEAT -->|PR/MR| DEV
    DEV -->|Merge| MAIN
    MAIN -->|Trigger| PIPELINE[Jenkins Pipeline]
    
    PIPELINE -->|deploy| EC2[AWS EC2]
    
    style MAIN fill:#4CAF50
    style DEV fill:#2196F3
    style FEAT fill:#FF9800
    style EC2 fill:#9C27B0
```

## Failure Handling & Rollback

```mermaid
graph TB
    A[Deployment Starts] --> B{Success?}
    B -->|Yes| C[Send Success Notification]
    B -->|No| D{Branch is main?}
    D -->|Yes| E[Auto Rollback]
    D -->|No| F[Skip Rollback]
    E --> G[Rollback Success?]
    G -->|Yes| H[Notify Rollback Complete]
    G -->|No| I[Alert Manual Intervention]
    F --> J[Send Failure Notification]
    
    style C fill:#4CAF50
    style E fill:#FF9800
    style H fill:#4CAF50
    style I fill:#f44336
    style J fill:#f44336
```

## Data Flow

```mermaid
graph LR
    subgraph "Client Requests"
        CLIENT[User/Browser]
    end
    
    subgraph "EC2 Instance"
        GATEWAY[API Gateway:8080]
        AUTH[Auth Module:8083]
        WORKFLOW[Workflow Service:8081]
        NOTIFY[Notification Service:8082]
        DB[(MySQL)]
        KAFKA[Kafka]
    end
    
    CLIENT -->|HTTP/HTTPS| GATEWAY
    GATEWAY -->|Authenticate| AUTH
    GATEWAY -->|Create Workflow| WORKFLOW
    GATEWAY -->|Get Status| WORKFLOW
    
    WORKFLOW -->|Store Data| DB
    WORKFLOW -->|Publish Event| KAFKA
    
    KAFKA -->|Consume Event| NOTIFY
    NOTIFY -->|Update Status| DB
    
    style CLIENT fill:#E3F2FD
    style GATEWAY fill:#4CAF50
    style AUTH fill:#2196F3
    style WORKFLOW fill:#2196F3
    style NOTIFY fill:#2196F3
    style DB fill:#9C27B0
    style KAFKA fill:#FF9800
```

## Security Layers

```mermaid
graph TB
    subgraph "Layer 1: Network Security"
        SG[AWS Security Group]
        FW[UFW Firewall]
    end
    
    subgraph "Layer 2: Access Control"
        SSH[SSH Key Authentication]
        CREDS[Jenkins Credentials]
    end
    
    subgraph "Layer 3: Application Security"
        DOCKER[Docker Isolation]
        NET[Docker Network]
        ENV[Environment Variables]
    end
    
    subgraph "Layer 4: Data Security"
        DB_CREDS[Database Passwords]
        ENCRYPT[Encrypted Secrets]
    end
    
    SG --> FW
    FW --> SSH
    SSH --> CREDS
    CREDS --> DOCKER
    DOCKER --> NET
    NET --> ENV
    ENV --> DB_CREDS
    DB_CREDS --> ENCRYPT
    
    style SG fill:#f44336
    style FW fill:#f44336
    style SSH fill:#FF9800
    style CREDS fill:#FF9800
    style DOCKER fill:#4CAF50
    style NET fill:#4CAF50
    style ENV fill:#4CAF50
    style DB_CREDS fill:#9C27B0
    style ENCRYPT fill:#9C27B0
```

## Monitoring & Observability

```mermaid
graph TB
    subgraph "EC2 Instance"
        CONTAINERS[Running Containers]
        LOGS[Docker Logs]
        METRICS[System Metrics]
    end
    
    subgraph "Monitoring Tools"
        PROM[Prometheus]
        GRAF[Grafana]
        ALERT[Alerting]
    end
    
    subgraph "Notifications"
        SLACK[Slack]
        EMAIL[Email]
    end
    
    CONTAINERS --> LOGS
    CONTAINERS --> METRICS
    
    LOGS --> PROM
    METRICS --> PROM
    
    PROM --> GRAF
    PROM --> ALERT
    
    ALERT --> SLACK
    ALERT --> EMAIL
    
    style CONTAINERS fill:#2196F3
    style PROM fill:#FF9800
    style GRAF fill:#4CAF50
    style SLACK fill:#9C27B0
    style EMAIL fill:#9C27B0
```

## Resource Utilization

```mermaid
pie title Typical Resource Distribution
    "MySQL" : 30
    "Kafka" : 20
    "Java Services" : 40
    "System Overhead" : 10
```

## Cost Optimization

```mermaid
graph LR
    subgraph "Cost Factors"
        EC2_TYPE[EC2 Instance Type]
        STORAGE[EBS Volume Size]
        TRANSFER[Data Transfer]
    end
    
    subgraph "Optimization Strategies"
        RIGHT_SIZE[Right-sizing]
        SPOT[Spot Instances]
        AUTO_SCALE[Auto Scaling]
        CACHE[Caching]
    end
    
    subgraph "Results"
        LOWER_COST[Lower Costs]
        BETTER_PERF[Better Performance]
    end
    
    EC2_TYPE --> RIGHT_SIZE
    STORAGE --> RIGHT_SIZE
    TRANSFER --> CACHE
    
    RIGHT_SIZE --> LOWER_COST
    SPOT --> LOWER_COST
    AUTO_SCALE --> LOWER_COST
    AUTO_SCALE --> BETTER_PERF
    CACHE --> BETTER_PERF
    
    style LOWER_COST fill:#4CAF50
    style BETTER_PERF fill:#4CAF50
```

---

**Note:** These diagrams provide visual representations of the CI/CD pipeline and deployment architecture. Refer to the detailed documentation for implementation specifics.
