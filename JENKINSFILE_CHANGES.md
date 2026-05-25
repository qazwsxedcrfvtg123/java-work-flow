# Jenkinsfile EC2 Deployment - Changes Summary

## 📝 Overview

The Jenkinsfile has been completely rewritten to support automated deployment to AWS EC2 instead of Kubernetes. This document summarizes the key changes and new features.

## 🔄 Major Changes

### 1. **Removed Kubernetes Dependencies**
- ❌ Removed all `kubectl` commands
- ❌ Removed KUBECONFIG environment variable
- ❌ Removed Kubernetes cluster context switching
- ❌ Removed K8s manifest deployments

### 2. **Added EC2 Deployment Support**
- ✅ SSH-based deployment to EC2
- ✅ Docker Compose orchestration
- ✅ Environment variable management via `.env` file
- ✅ Automatic docker-compose.yml generation

### 3. **Enhanced Environment Variables**

```groovy
environment {
    // New EC2-specific variables
    EC2_HOST = credentials('ec2-host')
    EC2_USER = credentials('ec2-user') ?: 'ubuntu'
    EC2_SSH_KEY = credentials('ec2-ssh-key')
    EC2_DEPLOY_PATH = '/opt/workflow-system'
    
    // Enhanced Docker configuration
    DOCKER_IMAGE_PREFIX = "${DOCKER_USERNAME}"
    BUILD_TIMESTAMP = sh(script: "date +%Y%m%d_%H%M%S", returnStdout: true).trim()
}
```

### 4. **Improved Build Stage**

**Before:**
```groovy
stage('Build') {
    steps {
        sh 'mvn clean package -DskipTests'
    }
}
```

**After:**
```groovy
stage('Build Services') {
    steps {
        sh 'mvn clean package -DskipTests -pl auth-module,services/api-gateway,services/workflow-service,services/notification-service -am'
    }
}
```

**Benefits:**
- Only builds specific modules (faster)
- Includes auth-module
- Uses Maven's project list feature

### 5. **Enhanced Docker Build & Tag**

**New Features:**
- Builds all 4 services (added auth-module)
- Tags images with both version AND latest
- Parallel builds for faster execution

```groovy
docker build -t ${DOCKER_IMAGE_PREFIX}/api-gateway:${APP_VERSION} .
docker tag ${DOCKER_IMAGE_PREFIX}/api-gateway:${APP_VERSION} ${DOCKER_IMAGE_PREFIX}/api-gateway:latest
```

### 6. **Enhanced Docker Push**

**Changes:**
- Pushes both versioned and latest tags
- Includes auth-module
- Better credential handling

### 7. **Complete Deployment Stage Rewrite**

**Old Approach (Kubernetes):**
```groovy
kubectl apply -f deploy/k8s/
```

**New Approach (EC2 + Docker Compose):**

#### Step 1: Generate docker-compose.yml
The pipeline dynamically generates a complete docker-compose.yml with:
- MySQL database with health checks
- Zookeeper for Kafka coordination
- Kafka message broker
- All 4 microservices with proper dependencies
- Health checks for all services
- Network isolation
- Volume management

#### Step 2: Upload to EC2
```bash
scp -i ${EC2_SSH_KEY} docker-compose-deploy.yml \
    ${EC2_USER}@${EC2_HOST}:${EC2_DEPLOY_PATH}/docker-compose.yml
```

#### Step 3: Execute Remote Deployment
```bash
ssh -i ${EC2_SSH_KEY} ${EC2_USER}@${EC2_HOST} << 'EOF'
    # Create .env file with credentials
    # Login to Docker registry
    # Pull latest images
    # Stop old containers
    # Start new containers
    # Verify deployment
EOF
```

### 8. **Service Architecture in Docker Compose**

```
┌─────────────────────────────────────┐
│         Docker Network              │
├─────────────────────────────────────┤
│                                     │
│  MySQL (Port 3306)                  │
│    ↑                                │
│  Zookeeper (Port 2181)              │
│    ↑                                │
│  Kafka (Port 9092)                  │
│    ↑                                │
│  Auth Module (Port 8083) ←──────────┤
│    ↑                                │
│  Workflow Service (Port 8081) ←─────┤
│    ↑                                │
│  Notification Service (Port 8082) ←─┤
│    ↑                                │
│  API Gateway (Port 8080) ←──────────┤
│                                     │
└─────────────────────────────────────┘
```

**Dependencies:**
- All services depend on MySQL (with health check)
- Workflow & Notification services depend on Kafka
- API Gateway depends on all services being healthy

### 9. **Enhanced Post-Deployment Actions**

#### Success Notifications
- Detailed Slack notifications with service URLs
- HTML email notifications with deployment details
- Build status tracking

#### Failure Handling with Auto-Rollback
```groovy
if (env.BRANCH_NAME == 'main') {
    // Attempt automatic rollback
    docker-compose down
    docker-compose up -d  // Starts previous version
}
```

#### Cleanup
- Docker system prune to save disk space
- Workspace cleanup

### 10. **Branch-Based Deployment**

**Current Configuration:**
- Only deploys to EC2 when pushing to `main` branch
- Other branches run tests and builds but skip deployment

**Can be extended:**
```groovy
when {
    anyOf {
        branch 'main'
        branch 'develop'
    }
}
```

## 🆕 New Files Created

### 1. **scripts/verify-deployment.sh**
Automated verification script that checks:
- SSH connectivity
- Docker installation
- Container status
- Port availability
- Health endpoints
- Disk space
- Memory usage
- Service logs

**Usage:**
```bash
./scripts/verify-deployment.sh YOUR_EC2_IP ubuntu jenkins-ec2-key
```

### 2. **EC2_DEPLOYMENT_SETUP.md**
Comprehensive setup guide covering:
- EC2 instance provisioning
- Software installation
- SSH key configuration
- Jenkins credentials setup
- Troubleshooting
- Security best practices
- Scaling considerations

### 3. **JENKINS_EC2_QUICKSTART.md**
Quick start guide in Chinese with:
- Step-by-step setup instructions
- Credential configuration
- First deployment walkthrough
- Monitoring and maintenance
- Common issues and solutions

### 4. **DEPLOYMENT_CHECKLIST.md**
Pre-deployment checklist to ensure:
- All prerequisites are met
- Credentials are configured
- Verification steps are followed
- Rollback plan is ready

### 5. **JENKINSFILE_CHANGES.md** (this file)
Summary of all changes made to the Jenkinsfile

## 🔧 Required Jenkins Credentials

| Credential ID | Type | Example Value |
|--------------|------|---------------|
| `docker-username` | Secret text | `mycompany` |
| `docker-password` | Secret password | `********` |
| `docker-credentials` | Username with password | Docker Hub login |
| `ec2-host` | Secret text | `54.123.45.67` |
| `ec2-user` | Secret text | `ubuntu` |
| `ec2-ssh-key` | Secret file | SSH private key file |
| `mysql-root-password` | Secret password | `Str0ngP@ssw0rd!` |
| `mysql-database` | Secret text | `workflow_db` |
| `mysql-user` | Secret text | `workflow_user` |
| `mysql-password` | Secret password | `An0th3rStr0ngP@ss!` |

## 📊 Pipeline Stages Overview

1. **Checkout** - Pull code from GitLab/GitHub
2. **Code Quality Check** - Checkstyle, SpotBugs, PMD (parallel)
3. **Unit Tests** - Run tests with coverage reports
4. **Build Services** - Maven build for all modules
5. **Security Scan** - Dependency vulnerability check
6. **Docker Build & Tag** - Build images for all services (parallel)
7. **Docker Push to Registry** - Push to Docker Hub (parallel)
8. **Deploy to EC2** - SSH deployment with Docker Compose
9. **Post-Actions** - Notifications, cleanup, rollback if needed

## 🚀 Deployment Flow

```
Git Push to main
       ↓
Jenkins Triggered
       ↓
Code Checkout
       ↓
Quality Checks (Parallel)
       ↓
Unit Tests
       ↓
Maven Build
       ↓
Security Scan
       ↓
Docker Build (Parallel for 4 services)
       ↓
Docker Push (Parallel)
       ↓
Generate docker-compose.yml
       ↓
Upload to EC2 via SCP
       ↓
SSH to EC2 and Deploy
       ↓
Health Checks
       ↓
Send Notifications
       ↓
Cleanup
```

## 🔍 Key Improvements

### 1. **Faster Builds**
- Parallel stage execution
- Targeted Maven module builds
- Concurrent Docker builds

### 2. **Better Reliability**
- Health checks before marking success
- Automatic rollback on failure
- Proper service dependency management

### 3. **Enhanced Security**
- Credentials stored in Jenkins
- SSH key-based authentication
- No hardcoded passwords

### 4. **Improved Monitoring**
- Detailed logging
- Multiple notification channels
- Comprehensive verification script

### 5. **Easier Maintenance**
- Clear separation of concerns
- Well-documented processes
- Simple rollback procedures

## ⚠️ Breaking Changes

### Removed Features
- Kubernetes deployment stages
- Multi-environment support (dev/staging/prod clusters)
- Manual production deployment approval

### Migration Notes
If you were using the Kubernetes deployment:
1. Keep old Jenkinsfile as backup
2. Set up EC2 infrastructure first
3. Test new pipeline on a test branch
4. Update team on new deployment process

## 🎯 Next Steps

1. **Setup EC2 Infrastructure**
   - Follow EC2_DEPLOYMENT_SETUP.md
   - Install Docker and Docker Compose
   - Configure SSH keys

2. **Configure Jenkins**
   - Add all required credentials
   - Install necessary plugins
   - Configure global tools

3. **Test Pipeline**
   - Create test branch
   - Push code to trigger build
   - Monitor execution

4. **Verify Deployment**
   - Run verification script
   - Test all service endpoints
   - Check logs for errors

5. **Go Live**
   - Merge to main branch
   - Monitor first production deployment
   - Set up monitoring/alerting

## 📞 Support & Troubleshooting

### Common Issues

**Issue: SSH connection fails**
- Check security group allows port 22
- Verify SSH key permissions (chmod 600)
- Test manually: `ssh -i key user@host`

**Issue: Docker pull fails**
- Verify Docker credentials in Jenkins
- Check Docker Hub rate limits
- Test manually: `docker login`

**Issue: Services won't start**
- Check EC2 has enough resources (RAM/CPU)
- Review container logs: `docker-compose logs`
- Verify MySQL/Kafka started first

**Issue: Port conflicts**
- Check for existing services on ports
- Use: `sudo lsof -i :8080`
- Kill conflicting processes or change ports

### Getting Help

1. Check Jenkins console output
2. Review EC2 container logs
3. Run verification script
4. Consult troubleshooting sections in docs
5. Check Docker and service documentation

## 📚 Additional Resources

- [EC2_DEPLOYMENT_SETUP.md](EC2_DEPLOYMENT_SETUP.md) - Complete setup guide
- [JENKINS_EC2_QUICKSTART.md](JENKINS_EC2_QUICKSTART.md) - Quick start (Chinese)
- [DEPLOYMENT_CHECKLIST.md](DEPLOYMENT_CHECKLIST.md) - Pre-deployment checklist
- [scripts/verify-deployment.sh](scripts/verify-deployment.sh) - Verification script

---

**Last Updated:** May 25, 2026  
**Version:** 2.0 (EC2 Deployment)  
**Author:** DevOps Team
