# Voxel IoT Gateway

## Overview
The Voxel IoT Gateway is a self-contained LWM2M server implementation that bridges IoT devices with backend systems via MQTT. It provides device registration management, security handling, and web-based administration.

## Architecture

The gateway consists of several key components:

### Core Components

| Component | Description |
|-----------|-------------|
| `SimpleIoTGateway` | Main entry point and application orchestrator |
| `Lwm2mServer` | LWM2M server configuration and endpoint setup |
| `WebServer` | Jetty-based web server with REST API |
| `MqttServer` | MQTT client for broker communication |
| `MqttAdapter` | MQTT adapter with subscription management |
| `IoTDeviceAdapter` | Device registration listener and handler |
| `SqliteSecurityStore` | SQLite-based security information persistence |

### Package Structure

```
at.voxel.iotgw/
├── SimpleIoTGateway.java      # Main entry point
├── Server.java                # Abstract server base class
├── WebServer.java             # Jetty web server implementation
├── IoTDeviceAdapter.java      # Device registration handler
├── RegInfo.java               # Registration info model
├── cli/                       # Command line interface
├── lwm2m/                     # LWM2M server configuration
├── mqtt/                      # MQTT client and adapter
├── security/                  # Security store implementations
├── servlet/                   # REST API servlets
├── model/                     # Object model utilities
└── servlet/json/              # JSON serialization utilities
```

## Key Features

### LWM2M Server
- **Protocol Support**: CoAP, CoAPS (CoAP over DTLS), CoAP-TCP, CoAPS-TCP
- **Security Modes**: PSK, RPK, X.509, OSCORE
- **Model Management**: Dynamic object model loading from files
- **Registration**: Device registration with lifecycle management

### Security
- **SQLite Persistence**: `SqliteSecurityStore` for persistent security info
- **Redis Support**: Optional Redis-based security store
- **Multiple Auth Modes**: PSK, RPK, X.509 certificate authentication

### MQTT Integration
- **Device Adapter**: Handles device registration/unregistration events
- **State Publishing**: Publishes device state to MQTT topics
- **Subscription Management**: Topic pattern matching and subscription handling
- **Security Store Operations**: Exposes security store CRUD operations via MQTT topics

### Web Interface
- **REST API**: `/api/clients`, `/api/security`, `/api/server`, `/api/event`
- **Vue.js Frontend**: Modern web-based administration interface
- **Real-time Updates**: Event streaming via SSE

## Dependencies

### Core Dependencies
- **Leshan**: LWM2M server framework (2.0.0-SNAPSHOT)
- **Californium**: CoAP implementation
- **Jetty**: Web server (v12.0.37)
- **SQLite**: JDBC driver for security persistence
- **Paho MQTT**: MQTT client library
- **Jackson**: JSON processing
- **Picocli**: Command line parsing

## How to Build

```bash
# Clean and compile
mvn clean compile

# Build JAR file
mvn package

# Run the application
java -cp target/voxel-iot-gateway-1.0.0.jar at.voxel.iotgw.SimpleIoTGateway
```

## Configuration

### Command Line Options

The gateway supports various CLI options:
- `--local-port`: CoAP port (default: 5683)
- `--secure-local-port`: CoAPS port (default: 5684)
- `--web-port`: Web server port (default: 8080)
- `--redis`: Redis connection string (optional)
- `--models-folder`: Custom object models directory

### Security Store

By default, the gateway uses SQLite for security information persistence:
- Database file: `data/security.db`
- Stores PSK keys, certificates, and OSCORE settings

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    SimpleIoTGateway                          │
│                                                               │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────────────┐  │
│  │ Lwm2mServer  │  │ WebServer    │  │ MqttServer        │  │
│  │              │  │              │  │                   │  │
│  │ - CoAP/DTLS  │  │ - Jetty      │  │ - MQTT Client     │  │
│  │ - Endpoints  │  │ - REST API   │  │ - Subscriptions   │  │
│  │ - Security   │  │ - Vue.js UI  │  │ - Security CRUD   │  │
│  └──────────────┘  └──────────────┘  └───────────────────┘  │
│                                                               │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ IoTDeviceAdapter (RegistrationListener)             │    │
│  │ - Device registration/unregistration handling       │    │
│  │ - MQTT state publishing                             │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

## Development Notes

### SQLite Security Store
The `SqliteSecurityStore` replaces the default `FileSecurityStore`:
- Stores serialized `SecurityInfo` objects in SQLite BLOB columns
- Provides persistence across server restarts
- Thread-safe with proper locking

### MQTT Integration
The `MqttAdapter` provides:
- Connection management
- Topic subscription with pattern matching
- Message listener support
- Automatic reconnection handling

### MQTT Security Store Operations
The `MqttServer` exposes security store operations via MQTT topics:
- **Read**: `iot/sec/read/<endpoint>` - Retrieve security info for an endpoint
- **Create**: `iot/sec/create` - Create new security info (PSK, X509, RPK, OSCORE)
- **Update**: `iot/sec/update` - Update existing security info
- **Delete**: `iot/sec/delete/<endpoint>` - Remove security info
- **List**: `iot/sec/list` - List all security entries

Responses are published to `iot/sec/response/...` topics with JSON payloads containing success/error status or security info data.

### Web Server
The `WebServer` provides:
- Static resource serving (Vue.js frontend)
- REST API endpoints for device management
- Security configuration
- Server status monitoring

## License

Eclipse Public License 2.0 / Eclipse Distribution License 1.0

## Contributing

Contributions are welcome. Please ensure:
1. Code compiles with `mvn compile`
2. Tests pass with `mvn test`
3. Changes are documented

---
*Based on Eclipse Leshan framework*
*Version: 1.0.0*
*Date: 2024*

## Limitations

This is a simplified version that:
- Does not include the full functionality of the original IoT Gateway
- Does not have the complex Leshan framework integration
- Is meant as a starting point for further development

For a full-featured implementation, you would need to:
1. Add the original source files with proper dependency management
2. Include the Leshan framework dependencies
3. Configure the proper server and client components