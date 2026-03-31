#!/bin/bash

# Wait for Kafka to start
sleep 10

# Create Kafka topics
kafka-topics --create --topic workflow.created --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
kafka-topics --create --topic workflow.approved --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
kafka-topics --create --topic workflow.rejected --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1

echo "Kafka topics created successfully"