# CI/CD Deployment to EC2 - Complete Guide

## 🎯 Overview

This guide explains how to deploy your Java Spring Boot microservices project to AWS EC2 using Jenkins for complete CI/CD automation.

## 📚 Documentation

| Document | Description | Language |
|----------|-------------|----------|
| [JENKINS_EC2_QUICKSTART.md](JENKINS_EC2_QUICKSTART.md) | Quick start guide with step-by-step instructions | Chinese (中文) |
| [EC2_DEPLOYMENT_SETUP.md](EC2_DEPLOYMENT_SETUP.md) | Comprehensive setup and configuration guide | English |
| [DEPLOYMENT_CHECKLIST.md](DEPLOYMENT_CHECKLIST.md) | Pre-deployment checklist | English |
| [JENKINSFILE_CHANGES.md](JENKINSFILE_CHANGES.md) | Summary of Jenkinsfile changes | English |

## 🗂️ Scripts

| Script | Purpose | Usage |
|--------|---------|-------|
| [scripts/setup-ec2.sh](scripts/setup-ec2.sh) | Automated EC2 environment setup | Run on EC2 instance |
| [scripts/verify-deployment.sh](scripts/verify-deployment.sh) | Verify deployment health | Run after deployment |

## 🚀 Quick Start (5 Steps)

### Step 1: Prepare EC2 Instance

```bash
# SSH to your EC2 instance
ssh -i your-key.pem ubuntu@YOUR_EC2_IP

# Download and run setup script
curl -O https://raw.githubusercontent.com/YOUR_REPO/scripts/setup-ec2.sh
chmod +x setup-ec2.sh
sudo ./setup-ec2.sh
```

### Step 2: Configure SSH Keys

```bash
# On Jenkins server or your local machine
ssh-keygen -t rsa -b 4096 -C "jenkins-deploy" -f jenkins-ec2-key

# Copy public key to EC2
ssh-copy-id -i jenkins-ec2-key.pub ubuntu@YOUR_EC2_IP

# Test connection
ssh -i jenkins-ec2-key ubuntu@YOUR_EC2_IP
```

### Step 3: Configure Jenkins Credentials

Add these credentials in Jenkins (**Manage Jenkins** → **Credentials**):

| Credential ID | Type | Value |
|--------------|------|-------|
| `docker-username` | Secret text | Your Docker Hub username |
| `docker-password` | Secret password | Your Docker Hub password |
| `ec2-host` | Secret text | EC2 public IP (e.g., 54.123.45.67) |
| `ec2-user` | Secret text | `ubuntu` (or `ec2-user`) |
| `ec2-ssh-key` | Secret file | Upload `jenkins-ec2-key` file |
| `mysql-root-password` | Secret password | Strong password |
| `mysql-database` | Secret text | `workflow_db` |
| `mysql-user` | Secret text | `workflow_user` |
| `mysql-password` | Secret password | Strong password |

### Step 4: Install Jenkins Plugins

Install these plugins in Jenkins:
- Docker Pipeline
- SSH Agent
- Email Extension Plugin
- Slack Notification (optional)
- Pipeline Utility Steps

### Step 5: Deploy!

```bash
# Commit and push to main branch
git add .
git commit -m "Deploy to EC2"
git push origin main
```

Jenkins will automatically:
1. ✅ Build and test your code
2. ✅ Create Docker images
3. ✅ Push to Docker Hub
4. ✅ Deploy to EC2
5. ✅ Start all services

## 🏗️ Architecture

```
┌──────────────┐
│ GitLab/      │
│ GitHub       │
└──────┬───────┘
       │ Push to main
       ▼
┌──────────────┐
│   Jenkins    │
│   Server     │
└──────┬───────┘
       │ Build, Test, Package
       │ Push Docker Images
       │ Deploy via SSH
       ▼
┌──────────────────────────┐
│     AWS EC2 Instance     │
│                          │
│  ┌────────────────────┐  │
│  │  Docker Compose    │  │
│  │                    │  │
│  │  • MySQL           │  │
│  │  • Zookeeper       │  │
│  │  • Kafka           │  │
│  │  • Auth Module     │  │
│  │  • Workflow Svc    │  │
│  │  • Notification    │  │
│  │  • API Gateway     │  │
│  └────────────────────┘  │
└──────────────────────────┘
```

## 🔧 Services & Ports

| Service | Port | URL |
|---------|------|-----|
| API Gateway | 8080 | http://EC2_IP:8080 |
| Workflow Service | 8081 | http://EC2_IP:8081 |
| Notification Service | 8082 | http://EC2_IP:8082 |
| Auth Module | 8083 | http://EC2_IP:8083 |
| MySQL | 3306 | Internal only |
| Kafka | 9092 | Internal only |
| Zookeeper | 2181 | Internal only |

## ✅ Verify Deployment

After deployment completes, verify everything is working:

```bash
# Run verification script
./scripts/verify-deployment.sh YOUR_EC2_IP ubuntu jenkins-ec2-key

# Or manually check
ssh -i jenkins-ec2-key ubuntu@YOUR_EC2_IP
cd /opt/workflow-system
docker-compose ps
docker-compose logs -f
```

## 🔍 Monitoring

### Check Service Status

```bash
# SSH to EC2
ssh -i jenkins-ec2-key ubuntu@YOUR_EC2_IP

# View all containers
docker-compose ps

# View logs for specific service
docker-compose logs -f api-gateway

# Monitor resources
docker stats
```

### Health Checks

All services expose health endpoints:

```bash
# API Gateway
curl http://EC2_IP:8080/actuator/health

# Workflow Service
curl http://EC2_IP:8081/actuator/health

# Notification Service
curl http://EC2_IP:8082/actuator/health

# Auth Module
curl http://EC2_IP:8083/actuator/health
```

## 🔄 Rollback

### Automatic Rollback
If deployment fails, Jenkins automatically attempts rollback to previous version.

### Manual Rollback

```bash
# SSH to EC2
ssh -i jenkins-ec2-key ubuntu@YOUR_EC2_IP

# Navigate to deployment directory
cd /opt/workflow-system

# Stop current deployment
docker-compose down

# Start previous version (edit docker-compose.yml with old image tags)
docker-compose up -d

# Verify
docker-compose ps
```

## 🛠️ Troubleshooting

### Issue: Cannot SSH to EC2

**Solution:**
```bash
# Check security group allows port 22
# Verify key permissions
chmod 600 jenkins-ec2-key

# Test connection
ssh -v -i jenkins-ec2-key ubuntu@YOUR_EC2_IP
```

### Issue: Containers won't start

**Solution:**
```bash
# Check logs
docker-compose logs

# Check disk space
df -h

# Check memory
free -h

# Restart
docker-compose down
docker-compose up -d
```

### Issue: Services can't connect to database

**Solution:**
```bash
# Wait for MySQL to be ready (check health)
docker-compose ps

# Check MySQL logs
docker-compose logs workflow-mysql

# Verify network
docker network inspect workflow-network
```

### Issue: Jenkins build fails at Docker push

**Solution:**
- Verify Docker credentials in Jenkins
- Test login: `docker login -u USERNAME -p PASSWORD`
- Check Docker Hub rate limits

## 🔐 Security Best Practices

1. **Use strong passwords** for all credentials
2. **Rotate SSH keys** regularly
3. **Enable firewall** (UFW) on EC2
4. **Restrict SSH access** to Jenkins IP only
5. **Use HTTPS** for production
6. **Regular updates** of OS and Docker
7. **Monitor logs** for suspicious activity
8. **Backup data** regularly

## 💾 Backup & Restore

### Backup MySQL

```bash
# Backup
docker exec workflow-mysql mysqldump -u root -p'PASSWORD' workflow_db > backup_$(date +%Y%m%d).sql

# Restore
docker exec -i workflow-mysql mysql -u root -p'PASSWORD' workflow_db < backup_20260525.sql
```

### Backup Volumes

```bash
# Backup Docker volumes
docker run --rm -v workflow-system_mysql-data:/data -v $(pwd):/backup alpine tar czf /backup/mysql-data.tar.gz -C /data .

# Restore
docker run --rm -v workflow-system_mysql-data:/data -v $(pwd):/backup alpine tar xzf /backup/mysql-data.tar.gz -C /data
```

## 📊 Performance Tuning

### JVM Options

Edit each service's Dockerfile or docker-compose.yml to optimize JVM:

```yaml
environment:
  JAVA_OPTS: "-Xms512m -Xmx1024m -XX:+UseG1GC"
```

### Docker Resources

Limit container resources in docker-compose.yml:

```yaml
deploy:
  resources:
    limits:
      cpus: '1.0'
      memory: 1G
    reservations:
      cpus: '0.5'
      memory: 512M
```

## 🚀 Scaling Considerations

For production environments:

1. **Use AWS ECS/EKS** instead of single EC2
2. **Add Load Balancer** (ALB/NLB)
3. **Use RDS** for managed database
4. **Use MSK** for managed Kafka
5. **Implement auto-scaling**
6. **Add monitoring** (CloudWatch, Prometheus)
7. **Use S3** for file storage
8. **Implement blue-green deployment**

## 📞 Support

### Documentation
- [Quick Start Guide (Chinese)](JENKINS_EC2_QUICKSTART.md)
- [Detailed Setup Guide](EC2_DEPLOYMENT_SETUP.md)
- [Deployment Checklist](DEPLOYMENT_CHECKLIST.md)
- [Jenkinsfile Changes](JENKINSFILE_CHANGES.md)

### Common Commands

```bash
# Verify deployment
./scripts/verify-deployment.sh EC2_IP ubuntu jenkins-ec2-key

# Check service logs
ssh -i jenkins-ec2-key ubuntu@EC2_IP "cd /opt/workflow-system && docker-compose logs -f"

# Restart all services
ssh -i jenkins-ec2-key ubuntu@EC2_IP "cd /opt/workflow-system && docker-compose restart"

# View resource usage
ssh -i jenkins-ec2-key ubuntu@EC2_IP "docker stats"
```

## 📝 License

This project is part of your organization's internal tools.

---

**Happy Deploying!** 🎉

For questions or issues, consult the detailed documentation or contact your DevOps team.
