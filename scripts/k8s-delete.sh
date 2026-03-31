#!/bin/bash

# Script to delete the application from Kubernetes

set -e

echo "Deleting workflow system from Kubernetes..."

# Delete monitoring
kubectl delete -f deploy/k8s/monitoring/grafana.yml -n workflow-system || true
kubectl delete -f deploy/k8s/monitoring/prometheus.yml -n workflow-system || true

# Delete ingress
kubectl delete -f deploy/k8s/ingress/ingress.yml -n workflow-system || true

# Delete HPA
kubectl delete -f deploy/k8s/notification-service/hpa.yml -n workflow-system || true
kubectl delete -f deploy/k8s/workflow-service/hpa.yml -n workflow-system || true

# Delete services
kubectl delete -f deploy/k8s/notification-service/service.yml -n workflow-system || true
kubectl delete -f deploy/k8s/workflow-service/service.yml -n workflow-system || true
kubectl delete -f deploy/k8s/api-gateway/service.yml -n workflow-system || true

# Delete deployments
kubectl delete -f deploy/k8s/notification-service/deployment.yml -n workflow-system || true
kubectl delete -f deploy/k8s/workflow-service/deployment.yml -n workflow-system || true
kubectl delete -f deploy/k8s/api-gateway/deployment.yml -n workflow-system || true

# Delete Kafka
kubectl delete -f deploy/k8s/kafka/ -n workflow-system || true

# Delete MySQL
kubectl delete -f deploy/k8s/mysql/ -n workflow-system || true

# Delete secrets and configmaps
kubectl delete -f deploy/k8s/secret.yml -n workflow-system || true
kubectl delete -f deploy/k8s/configmap.yml -n workflow-system || true

# Delete namespace
kubectl delete namespace workflow-system || true

echo "Deletion completed!"