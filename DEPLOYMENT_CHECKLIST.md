# Deployment Checklist

## Pre-Deployment Checklist

### EC2 Setup
- [ ] EC2 instance launched (Ubuntu 22.04 LTS, t3.medium or higher)
- [ ] Security group configured (ports: 22, 8080, 8081, 8082, 8083)
- [ ] Docker installed on EC2
- [ ] Docker Compose installed on EC2
- [ ] Deployment directory created: `/opt/workflow-system`
- [ ] SSH connectivity tested from Jenkins server

### SSH Keys
- [ ] SSH key pair generated (`jenkins-ec2-key`)
- [ ] Public key added to EC2 `~/.ssh/authorized_keys`
- [ ] Private key stored in Jenkins credentials as `ec2-ssh-key`
- [ ] SSH connection tested: `ssh -i jenkins-ec2-key ubuntu@EC2_IP`

### Jenkins Configuration
- [ ] Required plugins installed:
  - [ ] Docker Pipeline
  - [ ] SSH Agent
  - [ ] Email Extension Plugin
  - [ ] Slack Notification (optional)
  - [ ] Pipeline Utility Steps
- [ ] Maven configured in Global Tools (`Maven 3.8.4`)
- [ ] JDK configured in Global Tools (`OpenJDK 11`)

### Jenkins Credentials
- [ ] `docker-username` - Docker Hub username
- [ ] `docker-password` - Docker Hub password/access token
- [ ] `docker-credentials` - Docker Hub login (Username with password type)
- [ ] `ec2-host` - EC2 public IP address
- [ ] `ec2-user` - EC2 SSH username (ubuntu/ec2-user)
- [ ] `ec2-ssh-key` - SSH private key file
- [ ] `mysql-root-password` - MySQL root password
- [ ] `mysql-database` - Database name (workflow_db)
- [ ] `mysql-user` - Database user (workflow_user)
- [ ] `mysql-password` - Database user password

### Version Control
- [ ] GitLab/GitHub repository accessible from Jenkins
- [ ] Webhook configured to trigger Jenkins on push
- [ ] Main branch protected (if needed)

## Deployment Execution

### Manual Trigger
- [ ] Navigate to Jenkins job
- [ ] Click "Build Now"
- [ ] Monitor build progress

### Automatic Trigger
- [ ] Commit changes to main branch
- [ ] Push to repository
- [ ] Verify webhook triggered Jenkins build

## Post-Deployment Verification

### Service Health Checks
- [ ] Run verification script: `./scripts/verify-deployment.sh EC2_IP ubuntu jenkins-ec2-key`
- [ ] All containers running: `docker-compose ps`
- [ ] API Gateway accessible: http://EC2_IP:8080/actuator/health
- [ ] Workflow Service accessible: http://EC2_IP:8081/actuator/health
- [ ] Notification Service accessible: http://EC2_IP:8082/actuator/health
- [ ] Auth Module accessible: http://EC2_IP:8083/actuator/health

### Functional Tests
- [ ] Test user authentication via API Gateway
- [ ] Create a test workflow
- [ ] Verify Kafka message processing
- [ ] Check notification delivery
- [ ] Verify database records created

### Monitoring
- [ ] Check container logs for errors
- [ ] Monitor resource usage: `docker stats`
- [ ] Verify disk space: `df -h`
- [ ] Check memory usage: `free -h`

## Rollback Plan (If Needed)

### Manual Rollback
```bash
# SSH to EC2
ssh -i jenkins-ec2-key ubuntu@EC2_IP

# Navigate to deployment directory
cd /opt/workflow-system

# Stop current deployment
docker-compose down

# Start previous version (update docker-compose.yml with previous image tags)
docker-compose up -d

# Verify rollback
docker-compose ps
```

### Automatic Rollback
- [ ] Jenkins pipeline will attempt automatic rollback on failure
- [ ] Check Jenkins console output for rollback status

## Documentation Updates

- [ ] Update deployment logs with version and timestamp
- [ ] Document any issues encountered
- [ ] Update runbook if procedures changed
- [ ] Notify team of successful deployment

## Monitoring Setup (Optional)

- [ ] CloudWatch alarms configured
- [ ] Prometheus metrics collection enabled
- [ ] Grafana dashboards created
- [ ] Alert notifications configured

## Security Review

- [ ] SSH access restricted to necessary IPs only
- [ ] Firewall rules configured (UFW)
- [ ] SSL/TLS certificates installed (if using HTTPS)
- [ ] Database passwords are strong and stored securely
- [ ] Docker registry credentials are secure

## Performance Optimization (Post-Deployment)

- [ ] JVM heap sizes tuned for each service
- [ ] Database indexes optimized
- [ ] Connection pool sizes configured appropriately
- [ ] Cache settings reviewed
- [ ] Log rotation configured

---

**Deployment Completed By:** _________________  
**Date:** _________________  
**Version:** _________________  
**Status:** ☐ Success ☐ Failed ☐ Rolled Back

**Notes:**
_________________________________________________
_________________________________________________
_________________________________________________
