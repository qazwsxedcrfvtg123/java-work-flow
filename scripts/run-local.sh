#!/bin/bash

# Script to run the entire system locally using docker-compose

set -e

echo "Starting the workflow system locally..."

# Navigate to the docker-compose directory
cd deploy/docker-compose

# Start all services
docker-compose up -d

echo "Waiting for services to start..."
sleep 30

echo "System started successfully!"
echo "API Gateway available at: http://localhost:8080"
echo "Workflow Service available at: http://localhost:8081"
echo "Notification Service available at: http://localhost:8082"

# Tail logs
docker-compose logs -f