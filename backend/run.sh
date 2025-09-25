#!/bin/bash

echo "Starting Carbon Credit Marketplace Backend..."

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "Maven is not installed. Please install Maven first."
    exit 1
fi

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "Java is not installed. Please install Java 17 or later."
    exit 1
fi

# Clean and compile the project
echo "Building the project..."
mvn clean compile

if [ $? -eq 0 ]; then
    echo "Build successful. Starting the application..."
    mvn spring-boot:run
else
    echo "Build failed. Please check the errors above."
    exit 1
fi
