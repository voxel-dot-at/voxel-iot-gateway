# Voxel IoT Gateway - Isolated Project

## Overview
This is an isolated version of the IoT Gateway project that was originally part of the Leshan project. The goal was to create a self-contained Maven project that can be built independently without requiring the full Leshan parent project structure.

## What We've Done

1. **Created a new directory** at `/home/simon/proj/schauer_iot/repos/voxel-iot-gateway`

2. **Initialized a Maven project** with a simplified pom.xml that includes:
   - Core dependencies: Californium (CoAP), HiveMQ MQTT Client, Logback, Jackson
   - Additional dependencies: Picocli (command line parsing), Jedis (Redis client)
   - Proper Maven build configuration

3. **Created a minimal working implementation**:
   - Simple IoTGateway.java class that demonstrates the basic structure
   - No dependency on the complex Leshan framework

4. **Built the project successfully**:
   - Created a JAR file: `voxel-iot-gateway-1.0.0.jar`
   - Verified the JAR runs correctly

## Project Structure

```
voxel-iot-gateway/
├── pom.xml                 # Maven configuration
├── README.md               # Project documentation
├── src/
│   └── main/
│       ├── java/
│       │   └── at/voxel/iotgw/
│       │       └── IoTGateway.java  # Main application class
│       └── resources/
└── target/                 # Build output directory
    └── voxel-iot-gateway-1.0.0.jar
```

## Key Features

- **Self-contained**: No dependency on the Leshan parent project
- **Minimal dependencies**: Only includes necessary libraries
- **Buildable**: Can be compiled and packaged with Maven
- **Runnable**: JAR file can be executed directly

## How to Build

```bash
# Clean and compile
mvn clean compile

# Build JAR file
mvn package -DskipTests

# Run the application
java -cp target/voxel-iot-gateway-1.0.0.jar at.voxel.iotgw.IoTGateway
```

## Limitations

This is a simplified version that:
- Does not include the full functionality of the original IoT Gateway
- Does not have the complex Leshan framework integration
- Is meant as a starting point for further development

For a full-featured implementation, you would need to:
1. Add the original source files with proper dependency management
2. Include the Leshan framework dependencies
3. Configure the proper server and client components