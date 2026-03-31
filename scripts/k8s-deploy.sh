#!/bin/bash

# Script to deploy the application to Kubernetes

set -e

echo "Deploying workflow system to Kubernetes..."

# Create namespace
kubectl create namespace workflow-system || true

# Apply secrets and configmaps
kubectl apply -f deploy/k8s/secret.yml
kubectl apply -f deploy/k8s/configmap.yml

# Deploy MySQL
echo "Deploying MySQL..."
kubectl apply -f deploy/k8s/mysql/ -n workflow-system

# Deploy Kafka
echo "Deploying Kafka..."
kubectl apply -f deploy/k8s/kafka/ -n workflow-system

# Wait for Kafka to be ready
echo "Waiting for Kafka to be ready..."
kubectl rollout status deployment/zookeeper -n workflow-system
kubectl rollout status deployment/kafka -n workflow-system

# Deploy services
echo "Deploying services..."

# Deploy API Gateway
kubectl apply -f deploy/k8s/api-gateway/deployment.yml -n workflow-system
kubectl apply -f deploy/k8s/api-gateway/service.yml -n workflow-system

# Deploy Workflow Service
kubectl apply -f deploy/k8s/workflow-service/deployment.yml -n workflow-system
kubectl apply -f deploy/k8s/workflow-service/service.yml -n workflow-system
kubectl apply -f deploy/k8s/workflow-service/hpa.yml -n workflow-system

# Deploy Notification Service
kubectl apply -f deploy/k8s/notification-service/deployment.yml -n workflow-system
kubectl apply -f deploy/k8s/notification-service/service.yml -n workflow-system
kubectl apply -f deploy/k8s/notification-service/hpa.yml -n workflow-system

# Apply ingress
kubectl apply -f deploy/k8s/ingress/ingress.yml -n workflow-system

# Apply monitoring (optional)
kubectl apply -f deploy/k8s/monitoring/prometheus.yml -n workflow-system
kubectl apply -f deploy/k8s/monitoring/grafana.yml -n workflow-system

echo "Deployment completed! Waiting for deployments to be ready..."
kubectl rollout status deployment/api-gateway -n workflow-system
kubectl rollout status deployment/workflow-service -n workflow-system
kubectl rollout status deployment/notification-service -n workflow-system

echo "All services deployed successfully!"
echo "API Gateway External IP:"
kubectl get svc api-gateway -n workflow-system