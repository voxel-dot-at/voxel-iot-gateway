# Voxel IoT Gateway

This is an isolated version of the IoT Gateway project, extracted from the Leshan project.

## Project Structure

- `src/main/java/` - Java source code
- `src/main/resources/` - Configuration files
- `pom.xml` - Maven build configuration

## Dependencies

This project includes the following key dependencies:
- Californium (CoAP library)
- HiveMQ MQTT Client
- Logback (logging)
- Jackson (JSON processing)
- Servlet API (for web components)

## Building

Prerequisites: you need to build leshan first and add the packages to the local Maven cache:

```bash
cd <leshan>
mvn compile
mvn package
mvn install
```

To build the project, run:

```bash
mvn clean package
```

This will create a JAR file in the `target/` directory.
