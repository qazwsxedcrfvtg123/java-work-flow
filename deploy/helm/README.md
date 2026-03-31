# Helm Charts for Workflow System

This directory contains Helm charts for deploying the workflow system to Kubernetes.

## Structure

- `workflow-system/` - Main Helm chart for the entire workflow system
  - Includes all microservices, Kafka, MySQL, and supporting infrastructure

## Prerequisites

- Kubernetes 1.19+
- Helm 3+

## Quick Start

```bash
# Add the repository
helm repo add workflow-system https://your-repo/workflow-system

# Install the chart
helm install my-release workflow-system/workflow-system

# Or deploy from local chart
helm install my-release ./workflow-system
```

## Configuration

The following table lists the configurable parameters of the workflow-system chart and their default values.

| Parameter | Description | Default |
|-----------|-------------|---------|
| `image.repository` | Image repository | `"workflow-system"` |
| `image.tag` | Image tag | `"latest"` |
| `replicaCount` | Number of replicas | `1` |
| `service.port` | Service port | `80` |

## Values

See `values.yaml` for all configurable options.