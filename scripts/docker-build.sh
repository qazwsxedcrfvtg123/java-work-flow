#!/bin/bash

# Script to build Docker images for all services

set -e

echo "Building Docker images for all services..."

# Build parent project first
cd ..
if [ ! -f "mvnw" ]; then
    echo "Creating Maven wrapper..."
    mvn -N io.takari:maven:0.7.7:wrapper
fi

./mvnw clean package -DskipTests

# Build API Gateway image
echo "Building API Gateway image..."
cd services/api-gateway
if [ ! -f "target/api-gateway-*.jar" ]; then
    ../../mvnw clean package -DskipTests
fi
docker build -t api-gateway:latest .

# Build Workflow Service image
echo "Building Workflow Service image..."
cd ../workflow-service
if [ ! -f "target/workflow-service-*.jar" ]; then
    ../../mvnw clean package -DskipTests
fi
docker build -t workflow-service:latest .

# Build Notification Service image
echo "Building Notification Service image..."
cd ../notification-service
if [ ! -f "target/notification-service-*.jar" ]; then
    ../../mvnw clean package -DskipTests
fi
docker build -t notification-service:latest .

# Build Auth Service image
echo "Building Auth Service image..."
cd ../auth-module
if [ ! -f "target/auth-module-*.jar" ]; then
    ../../mvnw clean package -DskipTests
fi
docker build -t auth-service:latest .

echo "All Docker images built successfully!"

# Return to original directory
cd ../..