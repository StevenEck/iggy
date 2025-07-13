#!/bin/bash

# Check Java version
java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F. '{print $1}')
if [ "$java_version" -lt 16 ]; then
  echo "Error: Java 16 or later is required (found Java $java_version)"
  echo "Please install a newer JDK and make sure it's on your PATH"
  exit 1
fi

# Navigate to the SDK directory
cd "$(dirname "$0")/../../../foreign/java"

# Set JAVA_HOME if needed (uncomment and adjust if necessary)
# export JAVA_HOME=/path/to/java16/or/newer

# Build and publish the SDK to the local Maven repository
./gradlew publishToMavenLocal

echo "Iggy Java SDK has been published to your local Maven repository."
