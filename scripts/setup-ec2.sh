#!/bin/bash

# EC2 Setup Script - Automated installation of required software
# Run this script on your EC2 instance to prepare it for deployment

set -e

echo "========================================="
echo "EC2 Deployment Environment Setup"
echo "========================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print success
success() {
    echo -e "${GREEN}✓ $1${NC}"
}

# Function to print info
info() {
    echo -e "${YELLOW}→ $1${NC}"
}

# Function to print error
error() {
    echo -e "${RED}✗ $1${NC}"
}

# Check if running as root
if [ "$EUID" -ne 0 ]; then 
    error "Please run as root or with sudo"
    exit 1
fi

# Update system
info "Updating system packages..."
apt update && apt upgrade -y
success "System updated"
echo ""

# Install prerequisites
info "Installing prerequisites..."
apt install -y \
    apt-transport-https \
    ca-certificates \
    curl \
    gnupg \
    lsb-release \
    software-properties-common \
    unzip \
    git \
    vim \
    htop \
    net-tools \
    jq
success "Prerequisites installed"
echo ""

# Install Docker
info "Installing Docker..."
if ! command -v docker &> /dev/null; then
    curl -fsSL https://get.docker.com -o get-docker.sh
    sh get-docker.sh
    rm get-docker.sh
    success "Docker installed"
else
    success "Docker already installed"
fi
echo ""

# Add current user to docker group
CURRENT_USER=$(who | awk '{print $1}' | head -n1)
if [ -n "$CURRENT_USER" ] && [ "$CURRENT_USER" != "root" ]; then
    info "Adding user '$CURRENT_USER' to docker group..."
    usermod -aG docker $CURRENT_USER
    success "User added to docker group"
fi
echo ""

# Install Docker Compose
info "Installing Docker Compose..."
if ! command -v docker-compose &> /dev/null; then
    DOCKER_COMPOSE_VERSION=$(curl -s https://api.github.com/repos/docker/compose/releases/latest | grep 'tag_name' | cut -d'"' -f4)
    curl -L "https://github.com/docker/compose/releases/download/${DOCKER_COMPOSE_VERSION}/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    chmod +x /usr/local/bin/docker-compose
    success "Docker Compose installed"
else
    success "Docker Compose already installed"
fi
echo ""

# Configure Docker to start on boot
info "Enabling Docker service..."
systemctl enable docker
systemctl start docker
success "Docker service enabled and started"
echo ""

# Create deployment directory
DEPLOY_PATH="/opt/workflow-system"
info "Creating deployment directory: $DEPLOY_PATH"
mkdir -p $DEPLOY_PATH
chmod 755 $DEPLOY_PATH
if [ -n "$CURRENT_USER" ] && [ "$CURRENT_USER" != "root" ]; then
    chown $CURRENT_USER:$CURRENT_USER $DEPLOY_PATH
fi
success "Deployment directory created"
echo ""

# Configure UFW firewall (optional but recommended)
info "Configuring firewall..."
if ! command -v ufw &> /dev/null; then
    apt install -y ufw
fi

# Allow SSH
ufw allow 22/tcp > /dev/null 2>&1 || true

# Allow application ports
ufw allow 8080/tcp > /dev/null 2>&1 || true
ufw allow 8081/tcp > /dev/null 2>&1 || true
ufw allow 8082/tcp > /dev/null 2>&1 || true
ufw allow 8083/tcp > /dev/null 2>&1 || true

# Enable UFW (commented out by default - enable manually if needed)
# ufw --force enable
success "Firewall rules configured (not enabled - enable manually if needed)"
echo ""

# Configure log rotation for Docker
info "Configuring Docker log rotation..."
cat > /etc/docker/daemon.json << 'EOF'
{
    "log-driver": "json-file",
    "log-opts": {
        "max-size": "10m",
        "max-file": "3"
    }
}
EOF
success "Docker log rotation configured"
echo ""

# Restart Docker to apply log rotation config
info "Restarting Docker to apply configuration..."
systemctl restart docker
success "Docker restarted"
echo ""

# Verify installations
info "Verifying installations..."
echo ""

# Check Docker version
if command -v docker &> /dev/null; then
    DOCKER_VERSION=$(docker --version)
    success "$DOCKER_VERSION"
else
    error "Docker installation failed"
    exit 1
fi

# Check Docker Compose version
if command -v docker-compose &> /dev/null; then
    COMPOSE_VERSION=$(docker-compose --version)
    success "$COMPOSE_VERSION"
else
    error "Docker Compose installation failed"
    exit 1
fi

# Check Docker is running
if docker info > /dev/null 2>&1; then
    success "Docker daemon is running"
else
    error "Docker daemon is not running"
    exit 1
fi

echo ""

# Display system information
info "System Information:"
echo "  OS: $(lsb_release -ds 2>/dev/null || cat /etc/os-release | grep PRETTY_NAME | cut -d'"' -f2)"
echo "  Kernel: $(uname -r)"
echo "  CPU: $(nproc) cores"
echo "  Memory: $(free -h | grep Mem | awk '{print $2}')"
echo "  Disk: $(df -h / | awk 'NR==2 {print $2}') total, $(df -h / | awk 'NR==2 {print $4}') available"
echo ""

# Display network information
info "Network Configuration:"
PUBLIC_IP=$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4 2>/dev/null || echo "Unable to determine")
PRIVATE_IP=$(curl -s http://169.254.169.254/latest/meta-data/local-ipv4 2>/dev/null || hostname -I | awk '{print $1}')
echo "  Public IP: $PUBLIC_IP"
echo "  Private IP: $PRIVATE_IP"
echo ""

# Security recommendations
info "Security Recommendations:"
echo "  1. Configure automatic security updates:"
echo "     sudo apt install unattended-upgrades"
echo "     sudo dpkg-reconfigure -plow unattended-upgrades"
echo ""
echo "  2. Enable firewall when ready:"
echo "     sudo ufw enable"
echo ""
echo "  3. Set up fail2ban to prevent brute force attacks:"
echo "     sudo apt install fail2ban"
echo ""
echo "  4. Regularly update system:"
echo "     sudo apt update && sudo apt upgrade"
echo ""

# Next steps
echo "========================================="
echo "Setup Complete!"
echo "========================================="
echo ""
echo "Next Steps:"
echo "  1. Configure SSH keys for Jenkins access"
echo "  2. Test Docker: docker run hello-world"
echo "  3. Test Docker Compose: cd $DEPLOY_PATH && docker-compose --version"
echo "  4. Configure Jenkins credentials"
echo "  5. Trigger your first deployment"
echo ""
echo "Deployment directory: $DEPLOY_PATH"
echo ""
echo "To verify everything is working, run:"
echo "  docker ps"
echo "  docker-compose --version"
echo ""

if [ -n "$CURRENT_USER" ] && [ "$CURRENT_USER" != "root" ]; then
    echo "IMPORTANT: You may need to logout and login again for Docker group permissions to take effect."
    echo "Or run: newgrp docker"
    echo ""
fi

success "EC2 environment is ready for deployment!"
echo ""
