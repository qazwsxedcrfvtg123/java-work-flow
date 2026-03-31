#!/bin/bash

# Script to build all modules in the project

set -e  # Exit immediately if a command exits with a non-zero status

echo "Building all modules..."

# Build parent project and all modules
cd ..

if [ ! -f "mvnw" ]; then
    echo "Creating Maven wrapper..."
    mvn -N io.takari:maven:0.7.7:wrapper
fi

echo "Running Maven clean install..."
./mvnw clean install -DskipTests

echo "Build completed successfully!"