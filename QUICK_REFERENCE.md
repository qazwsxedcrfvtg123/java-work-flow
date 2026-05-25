# Quick Reference Card - EC2 Deployment

## 🔑 Essential Commands

### SSH to EC2
```bash
ssh -i jenkins-ec2-key ubuntu@YOUR_EC2_IP
```

### Check All Services
```bash
cd /opt/workflow-system
docker-compose ps
```

### View Logs
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f api-gateway
docker-compose logs -f workflow-service
docker-compose logs -f notification-service
docker-compose logs -f auth-module
```

### Restart Services
```bash
# All services
docker-compose restart

# Specific service
docker-compose restart workflow-service
```

### Stop/Start
```bash
# Stop all
docker-compose down

# Start all
docker-compose up -d

# Start with fresh images
docker-compose pull && docker-compose up -d
```

## 🌐 Service URLs

Replace `YOUR_EC2_IP` with your actual EC2 public IP.

| Service | URL | Port |
|---------|-----|------|
| API Gateway | http://YOUR_EC2_IP:8080 | 8080 |
| Workflow Service | http://YOUR_EC2_IP:8081 | 8081 |
| Notification Service | http://YOUR_EC2_IP:8082 | 8082 |
| Auth Module | http://YOUR_EC2_IP:8083 | 8083 |

### Health Checks
```bash
curl http://YOUR_EC2_IP:8080/actuator/health
curl http://YOUR_EC2_IP:8081/actuator/health
curl http://YOUR_EC2_IP:8082/actuator/health
curl http://YOUR_EC2_IP:8083/actuator/health
```

## 🗄️ Database Operations

### Backup
```bash
docker exec workflow-mysql mysqldump -u root -p'PASSWORD' workflow_db > backup_$(date +%Y%m%d).sql
```

### Restore
```bash
docker exec -i workflow-mysql mysql -u root -p'PASSWORD' workflow_db < backup_20260525.sql
```

### Connect to MySQL
```bash
docker exec -it workflow-mysql mysql -u root -p'PASSWORD' workflow_db
```

## 📊 Monitoring

### Container Stats
```bash
docker stats
```

### System Resources
```bash
# Memory
free -h

# Disk
df -h

# CPU
top
```

### Docker System
```bash
# Disk usage
docker system df

# Clean up
docker system prune -f
docker image prune -a --filter "until=168h"
```

## 🔧 Troubleshooting

### Check if Services are Running
```bash
docker ps
```

### Check Specific Container
```bash
docker inspect api-gateway
docker inspect workflow-mysql
```

### View Last 100 Lines of Logs
```bash
docker-compose logs --tail=100 api-gateway
```

### Check Network
```bash
docker network ls
docker network inspect workflow-network
```

### Test Connectivity
```bash
# From host to container
docker exec api-gateway ping workflow-mysql

# From one container to another
docker exec workflow-service curl http://api-gateway:8080/actuator/health
```

## 🚀 Deployment

### Trigger Deployment
Push code to main branch:
```bash
git add .
git commit -m "Your changes"
git push origin main
```

### Verify Deployment
```bash
./scripts/verify-deployment.sh YOUR_EC2_IP ubuntu jenkins-ec2-key
```

### Manual Rollback
```bash
ssh -i jenkins-ec2-key ubuntu@YOUR_EC2_IP
cd /opt/workflow-system
docker-compose down
# Edit docker-compose.yml with previous image tags
docker-compose up -d
```

## 🔐 Security

### Update Firewall Rules
```bash
sudo ufw status
sudo ufw allow 8080/tcp
sudo ufw reload
```

### Change Passwords
Edit `.env` file in `/opt/workflow-system`:
```bash
MYSQL_ROOT_PASSWORD=newpassword
MYSQL_PASSWORD=newpassword
```

Then restart:
```bash
docker-compose down
docker-compose up -d
```

## 📦 Docker Images

### List Images
```bash
docker images
```

### Pull Latest
```bash
docker-compose pull
```

### Remove Old Images
```bash
docker rmi $(docker images --filter "dangling=true" -q)
```

## 🔍 Debugging

### Enter Running Container
```bash
docker exec -it api-gateway bash
docker exec -it workflow-mysql bash
```

### Check Environment Variables
```bash
docker exec api-gateway env
```

### Test Database Connection
```bash
docker exec workflow-service curl http://workflow-mysql:3306
```

### Check Kafka Topics
```bash
docker exec workflow-kafka kafka-topics --list --bootstrap-server localhost:9092
```

## ⚙️ Configuration

### Edit docker-compose.yml
```bash
cd /opt/workflow-system
vim docker-compose.yml
```

### Edit Environment Variables
```bash
cd /opt/workflow-system
vim .env
```

### Apply Changes
```bash
docker-compose down
docker-compose up -d
```

## 📋 Common Issues & Fixes

### Issue: Port Already in Use
```bash
# Find process
sudo lsof -i :8080

# Kill process
sudo kill -9 PID
```

### Issue: Out of Memory
```bash
# Check memory
free -h

# Restart services
docker-compose restart

# Limit container memory in docker-compose.yml
```

### Issue: Disk Space Full
```bash
# Check disk usage
df -h

# Clean Docker
docker system prune -af

# Remove old logs
sudo journalctl --vacuum-time=7d
```

### Issue: Container Won't Start
```bash
# Check logs
docker logs api-gateway

# Check dependencies
docker-compose ps

# Recreate container
docker-compose up -d --force-recreate api-gateway
```

## 🛠️ Maintenance Tasks

### Weekly
```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Clean Docker
docker system prune -f

# Check logs for errors
docker-compose logs --tail=1000 | grep ERROR
```

### Monthly
```bash
# Backup database
docker exec workflow-mysql mysqldump -u root -p'PASSWORD' workflow_db > backup_$(date +%Y%m%d).sql

# Rotate logs
docker-compose logs --tail=0 > /dev/null

# Review security updates
sudo unattended-upgrades --dry-run
```

## 📞 Emergency Contacts

### Jenkins Server
- URL: http://YOUR_JENKINS_IP:8080
- Admin credentials: Stored in password manager

### EC2 Instance
- IP: YOUR_EC2_IP
- SSH Key: jenkins-ec2-key
- User: ubuntu

### Docker Hub
- Username: YOUR_DOCKER_USERNAME
- Repository: YOUR_DOCKER_USERNAME/workflow-*

## 🔗 Useful Links

- Jenkins Dashboard: http://YOUR_JENKINS_IP:8080
- API Documentation: http://YOUR_EC2_IP:8080/swagger-ui.html
- Monitoring: (Add your monitoring URL)
- Git Repository: (Add your GitLab/GitHub URL)

---

**Print this page and keep it handy for quick reference!**

Last Updated: May 25, 2026
