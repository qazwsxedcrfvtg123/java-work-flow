#!/bin/bash

# EC2 Deployment Verification Script
# This script verifies that all services are running correctly after deployment

set -e

EC2_HOST="${1:-}"
EC2_USER="${2:-ubuntu}"
SSH_KEY="${3:-jenkins-ec2-key}"

if [ -z "$EC2_HOST" ]; then
    echo "Usage: $0 <EC2_HOST> [EC2_USER] [SSH_KEY]"
    echo "Example: $0 54.123.45.67 ubuntu jenkins-ec2-key"
    exit 1
fi

echo "========================================="
echo "EC2 Deployment Verification"
echo "========================================="
echo "EC2 Host: $EC2_HOST"
echo "EC2 User: $EC2_USER"
echo "SSH Key: $SSH_KEY"
echo "========================================="
echo ""

# Function to run commands on EC2
run_on_ec2() {
    ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no "$EC2_USER@$EC2_HOST" "$1"
}

# Check SSH connectivity
echo "1. Checking SSH connectivity..."
if run_on_ec2 "echo 'SSH connection successful'"; then
    echo "✅ SSH connection established"
else
    echo "❌ Cannot connect to EC2 via SSH"
    exit 1
fi
echo ""

# Check Docker is running
echo "2. Checking Docker status..."
if run_on_ec2 "docker info > /dev/null 2>&1 && echo 'Docker is running'"; then
    echo "✅ Docker is running"
else
    echo "❌ Docker is not running"
    exit 1
fi
echo ""

# Check Docker Compose
echo "3. Checking Docker Compose..."
if run_on_ec2 "docker-compose version > /dev/null 2>&1 && echo 'Docker Compose installed'"; then
    echo "✅ Docker Compose is installed"
else
    echo "❌ Docker Compose is not installed"
    exit 1
fi
echo ""

# Check containers status
echo "4. Checking container status..."
CONTAINERS_STATUS=$(run_on_ec2 "cd /opt/workflow-system && docker-compose ps 2>&1")
echo "$CONTAINERS_STATUS"
echo ""

# Check if all required containers are running
echo "5. Verifying required services..."
REQUIRED_CONTAINERS=("api-gateway" "workflow-service" "notification-service" "auth-module" "workflow-mysql" "workflow-kafka" "workflow-zookeeper")

ALL_RUNNING=true
for container in "${REQUIRED_CONTAINERS[@]}"; do
    STATUS=$(run_on_ec2 "docker ps --filter name=$container --format '{{.Status}}' 2>&1")
    if [ -n "$STATUS" ] && echo "$STATUS" | grep -q "Up"; then
        echo "✅ $container is running"
    else
        echo "❌ $container is NOT running"
        ALL_RUNNING=false
    fi
done
echo ""

# Check ports
echo "6. Checking service ports..."
PORTS=(8080 8081 8082 8083 3306 9092 2181)
for port in "${PORTS[@]}"; do
    if run_on_ec2 "sudo lsof -i :$port > /dev/null 2>&1 || ss -tlnp | grep :$port > /dev/null 2>&1"; then
        echo "✅ Port $port is open"
    else
        echo "⚠️  Port $port is not responding"
    fi
done
echo ""

# Test API Gateway health endpoint
echo "7. Testing API Gateway health..."
HEALTH_STATUS=$(run_on_ec2 "curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/actuator/health 2>&1" || echo "failed")
if [ "$HEALTH_STATUS" = "200" ]; then
    echo "✅ API Gateway health check passed (HTTP $HEALTH_STATUS)"
else
    echo "⚠️  API Gateway health check failed (HTTP $HEALTH_STATUS)"
fi
echo ""

# Check disk space
echo "8. Checking disk space..."
DISK_USAGE=$(run_on_ec2 "df -h / | awk 'NR==2 {print \$5}'")
echo "Disk usage: $DISK_USAGE"
if [ "${DISK_USAGE%\%}" -gt 80 ]; then
    echo "⚠️  Warning: Disk usage is above 80%"
else
    echo "✅ Disk usage is acceptable"
fi
echo ""

# Check memory usage
echo "9. Checking memory usage..."
MEMORY_INFO=$(run_on_ec2 "free -h | grep Mem")
echo "$MEMORY_INFO"
echo ""

# Show recent logs for each service
echo "10. Recent logs (last 10 lines) for each service..."
SERVICES_LOGS=("api-gateway" "workflow-service" "notification-service" "auth-module")
for service in "${SERVICES_LOGS[@]}"; do
    echo "--- $service logs ---"
    run_on_ec2 "docker logs --tail 10 $service 2>&1" || echo "No logs available for $service"
    echo ""
done

# Summary
echo "========================================="
echo "Verification Summary"
echo "========================================="
if [ "$ALL_RUNNING" = true ]; then
    echo "✅ All required services are running"
    echo ""
    echo "Access your services:"
    echo "  - API Gateway: http://$EC2_HOST:8080"
    echo "  - Workflow Service: http://$EC2_HOST:8081"
    echo "  - Notification Service: http://$EC2_HOST:8082"
    echo "  - Auth Module: http://$EC2_HOST:8083"
    echo ""
else
    echo "❌ Some services are not running properly"
    echo "Check the logs above for details"
    exit 1
fi

echo "========================================="
