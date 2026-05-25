# EC2 Deployment Setup Guide

This guide will help you configure Jenkins and EC2 for automated CI/CD deployment.

## Prerequisites

### 1. EC2 Instance Setup

#### Launch EC2 Instance
- **AMI**: Ubuntu 22.04 LTS (recommended) or Amazon Linux 2
- **Instance Type**: t3.medium or higher (minimum 2GB RAM)
- **Storage**: At least 20GB SSD
- **Security Group**: Open ports 8080, 8081, 8082, 8083, 22 (SSH)

#### Install Required Software on EC2

SSH into your EC2 instance and run:

```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# Install Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Verify installations
docker --version
docker-compose --version

# Create deployment directory
sudo mkdir -p /opt/workflow-system
sudo chown $USER:$USER /opt/workflow-system

# Install curl for health checks
sudo apt install -y curl

# Enable Docker to start on boot
sudo systemctl enable docker
sudo systemctl start docker
```

### 2. SSH Key Configuration

#### Generate SSH Key Pair (on Jenkins server or locally)

```bash
ssh-keygen -t rsa -b 4096 -C "jenkins-deploy" -f jenkins-ec2-key
```

#### Add Public Key to EC2

```bash
# Copy public key to EC2
ssh-copy-id -i jenkins-ec2-key.pub ubuntu@YOUR_EC2_IP

# Or manually add to authorized_keys
cat jenkins-ec2-key.pub | ssh ubuntu@YOUR_EC2_IP 'mkdir -p ~/.ssh && cat >> ~/.ssh/authorized_keys'
```

#### Test SSH Connection

```bash
ssh -i jenkins-ec2-key ubuntu@YOUR_EC2_IP
```

### 3. Jenkins Credentials Configuration

Go to **Jenkins Dashboard** → **Manage Jenkins** → **Credentials** → **System** → **Global credentials** → **Add Credentials**

#### Required Credentials:

| Credential ID | Type | Description | Value |
|--------------|------|-------------|-------|
| `docker-username` | Secret text | Docker Hub username | Your Docker Hub username |
| `docker-password` | Secret password | Docker Hub password | Your Docker Hub password/access token |
| `docker-registry-url` | Secret text | Docker Registry URL | `docker.io` (or your registry) |
| `ec2-host` | Secret text | EC2 Public IP/DNS | Your EC2 public IP address |
| `ec2-user` | Secret text | EC2 SSH username | `ubuntu` (or `ec2-user` for Amazon Linux) |
| `ec2-ssh-key` | Secret file | SSH private key | Upload `jenkins-ec2-key` file |
| `mysql-root-password` | Secret password | MySQL root password | Strong password for MySQL root |
| `mysql-database` | Secret text | MySQL database name | `workflow_db` |
| `mysql-user` | Secret text | MySQL application user | `workflow_user` |
| `mysql-password` | Secret password | MySQL user password | Strong password for app user |

#### Docker Credentials Alternative:

If using Docker Hub, create credential with ID `docker-credentials`:
- **Kind**: Username with password
- **Username**: Your Docker Hub username
- **Password**: Your Docker Hub password or access token

### 4. Jenkins Plugins Required

Install these plugins in Jenkins:

1. **Docker Pipeline** - For Docker operations in pipeline
2. **SSH Agent** - For SSH connections to EC2
3. **Email Extension Plugin** - For email notifications
4. **Slack Notification Plugin** (optional) - For Slack notifications
5. **Pipeline Utility Steps** - For utility functions
6. **JUnit** - For test reports
7. **JaCoCo** - For code coverage

Install via: **Manage Jenkins** → **Plugins** → **Available plugins**

### 5. Jenkins Global Tool Configuration

Go to **Manage Jenkins** → **Global Tool Configuration**

#### Configure Maven:
- **Name**: `Maven 3.8.4`
- **Version**: Choose from dropdown or install automatically

#### Configure JDK:
- **Name**: `OpenJDK 11`
- **Version**: JDK 11 or point to JAVA_HOME

### 6. GitLab/GitHub Webhook Configuration

#### For GitLab:
1. Go to your repository → **Settings** → **Webhooks**
2. Add webhook URL: `http://YOUR_JENKINS_URL/gitlab-webhook/post`
3. Trigger: **Push events** → **All branches**
4. Add secret token (optional but recommended)

#### For GitHub:
1. Go to repository → **Settings** → **Webhooks**
2. Add webhook URL: `http://YOUR_JENKINS_URL/github-webhook/`
3. Events: **Let me select individual events** → **Pushes**
4. Content type: `application/json`

### 7. Environment Variables (Optional)

In Jenkins job configuration, you can add these environment variables:

```properties
DOCKER_REGISTRY=docker.io
EC2_DEPLOY_PATH=/opt/workflow-system
MYSQL_DATABASE=workflow_db
MYSQL_USER=workflow_user
```

## Deployment Architecture

```
┌─────────────┐
│   GitLab/   │
│   GitHub    │
└──────┬──────┘
       │ Push to main branch
       ▼
┌─────────────┐
│   Jenkins   │
│   Server    │
└──────┬──────┘
       │ Build & Test
       │ Build Docker Images
       │ Push to Registry
       │ Deploy via SSH
       ▼
┌─────────────┐
│   EC2       │
│  Instance   │
└──────┬──────┘
       │ Runs Docker Compose
       ▼
┌──────────────────────────────────────┐
│         Docker Containers            │
├──────────────────────────────────────┤
│ • MySQL (Port 3306)                  │
│ • Zookeeper (Port 2181)              │
│ • Kafka (Port 9092)                  │
│ • Auth Module (Port 8083)            │
│ • Workflow Service (Port 8081)       │
│ • Notification Service (Port 8082)   │
│ • API Gateway (Port 8080)            │
└──────────────────────────────────────┘
```

## First Manual Deployment

Before running the pipeline, you can test manually:

```bash
# On EC2 instance
cd /opt/workflow-system

# Create .env file
cat > .env << EOF
MYSQL_ROOT_PASSWORD=your_root_password
MYSQL_DATABASE=workflow_db
MYSQL_USER=workflow_user
MYSQL_PASSWORD=your_user_password
EOF

# Pull images (after Jenkins pushes them)
docker pull your-dockerhub-username/api-gateway:latest
docker pull your-dockerhub-username/workflow-service:latest
docker pull your-dockerhub-username/notification-service:latest
docker pull your-dockerhub-username/auth-module:latest

# Start services
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f
```

## Monitoring & Maintenance

### Check Service Status

```bash
# SSH to EC2
ssh -i jenkins-ec2-key ubuntu@YOUR_EC2_IP

# Check all containers
docker-compose ps

# View logs
docker-compose logs -f api-gateway
docker-compose logs -f workflow-service

# Check resource usage
docker stats
```

### Backup MySQL Data

```bash
# Backup database
docker exec workflow-mysql mysqldump -u root -p'$MYSQL_ROOT_PASSWORD' workflow_db > backup_$(date +%Y%m%d).sql

# Restore database
docker exec -i workflow-mysql mysql -u root -p'$MYSQL_ROOT_PASSWORD' workflow_db < backup_20260525.sql
```

### Cleanup Old Images

```bash
# Remove unused images
docker image prune -a --filter "until=168h"

# Remove stopped containers
docker container prune -f
```

## Troubleshooting

### Issue: Cannot connect to EC2 via SSH
**Solution**: 
- Check security group allows SSH (port 22)
- Verify SSH key permissions: `chmod 600 jenkins-ec2-key`
- Check EC2 is running and has public IP

### Issue: Docker containers fail to start
**Solution**:
```bash
# Check logs
docker-compose logs

# Check disk space
df -h

# Restart services
docker-compose down
docker-compose up -d
```

### Issue: Services cannot connect to MySQL/Kafka
**Solution**:
- Ensure services start in correct order (check `depends_on` in docker-compose)
- Wait for health checks to pass
- Check network connectivity: `docker network inspect workflow-network`

### Issue: Jenkins build fails at Docker push
**Solution**:
- Verify Docker credentials in Jenkins
- Test login manually: `docker login -u USERNAME -p PASSWORD`
- Check Docker Hub rate limits

### Issue: Port already in use
**Solution**:
```bash
# Find process using port
sudo lsof -i :8080

# Kill process or change port in docker-compose.yml
```

## Security Best Practices

1. **Use IAM Roles** instead of hardcoded credentials where possible
2. **Rotate SSH keys** regularly
3. **Use Docker content trust** for image signing
4. **Enable UFW firewall** on EC2:
   ```bash
   sudo ufw allow 22/tcp
   sudo ufw allow 8080/tcp
   sudo ufw enable
   ```
5. **Use HTTPS** for Jenkins and consider adding SSL certificates
6. **Restrict SSH access** to Jenkins IP only in EC2 security group
7. **Use strong passwords** for MySQL and store in Jenkins credentials
8. **Regular updates**: Keep EC2 OS and Docker updated

## Scaling Considerations

For production environments:

1. **Use AWS ECS/EKS** instead of single EC2 for better scalability
2. **Add load balancer** (ALB/NLB) in front of API Gateway
3. **Use RDS** instead of MySQL container for managed database
4. **Use MSK** (Managed Streaming for Kafka) instead of container Kafka
5. **Implement auto-scaling** groups
6. **Add monitoring** with CloudWatch, Prometheus, Grafana
7. **Use S3** for file uploads instead of local storage
8. **Implement blue-green deployment** strategy

## Next Steps

1. ✅ Set up EC2 instance with required software
2. ✅ Configure SSH keys for passwordless access
3. ✅ Add all credentials to Jenkins
4. ✅ Install required Jenkins plugins
5. ✅ Configure Jenkins global tools (Maven, JDK)
6. ✅ Set up webhooks in GitLab/GitHub
7. ✅ Test pipeline with a commit to main branch
8. ✅ Monitor first deployment and verify services
9. ✅ Set up monitoring and alerting
10. ✅ Document runbook for operations team

---

**Support**: If you encounter issues, check Jenkins console output and EC2 logs for detailed error messages.
