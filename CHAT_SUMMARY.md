# Voxel IoT Gateway - Chat Summary

## Session Overview
This document summarizes the changes made during the chat session to implement SQLite-based security storage and improve code organization.

## Date
Generated: 2024-08-24

## Project State

### Current Branch
`feature/vue3`

### Key Changes Made

#### 1. SqliteSecurityStore Implementation
**File**: `src/main/java/at/voxel/iotgw/security/SqliteSecurityStore.java`

- Created `SqliteSecurityStore` class extending `InMemorySecurityStore`
- Persists security information in SQLite database (`data/security.db`)
- Loads all entries from database on startup
- Supports PSK, X.509, and OSCORE security modes
- **Note**: DTLS/PSK authentication is not working yet

**Database Schema**:
```sql
CREATE TABLE security_info (
  endpoint TEXT PRIMARY KEY,
  psk_identity TEXT,
  pre_shared_key BLOB,
  raw_public_key BLOB,
  use_x509_cert INTEGER,
  oscore_sender_id BLOB,
  oscore_recipient_id BLOB,
  oscore_master_secret BLOB,
  oscore_aead_algorithm INTEGER,
  oscore_hkdf_algorithm INTEGER,
  oscore_master_salt BLOB,
  serialized_info BLOB
);
```

#### 2. Code Refactoring

**Lwm2mServer.java** (`src/main/java/at/voxel/iotgw/lwm2m/Lwm2mServer.java`)
- Extracted Leshan server creation logic from `SimpleIoTGateway`
- Contains `createServer(IoTGatewayCli cli)` static method
- Handles all server configuration including:
  - Model provider setup
  - Security store initialization (SQLite or Redis)
  - Californium endpoint configuration
  - DTLS/TLS settings
  - CoAP endpoint creation

**MqttServer.java** (`src/main/java/at/voxel/iotgw/mqtt/MqttServer.java`)
- Extracted MQTT client setup into dedicated class
- Provides `createServer()` factory method
- Handles connection, subscription, and lifecycle management

**SimpleIoTGateway.java**
- Simplified main class with focused responsibilities
- Delegates to `Lwm2mServer`, `MqttServer`, and `WebServer` classes
- Logs security store type and DTLS/PSK status

#### 3. IoTDeviceAdapter Enhancement
**File**: `src/main/java/at/voxel/iotgw/IoTDeviceAdapter.java`

- Implements `RegistrationListener` and `ObservationListener` interfaces
- Registered with both `RegistrationService` and `ObservationService`
- Added comprehensive debug logging for all callbacks:
  - `registered()`: Logs endpoint, previous registration, observations, supported objects, additional attributes, device info
  - `unregistered()`: Logs endpoint, observations, expired status
  - `updated()`: Logs endpoint, update details
  - `onAwake()` / `onSleeping()`: Logs device state changes
  - `newObservation()`: Logs observation creation
  - `cancelled()`: Logs observation cancellation
  - `onResponse()`: Logs observation responses
  - `onError()`: Logs observation errors

#### 4. UI Updates
**File**: `webapp/src/components/security/SecurityInfoInput.vue`

- Updated to use Vue 3 Composition API (`<script setup>`)
- Added mutual exclusivity logic between DTLS and OSCORE
- Added validation for OSCORE fields
- Uses Vuetify components for better UI

#### 5. Utility Scripts
**Files**: `scripts/set_psk.sh` and `scripts/set_psk.py`

- Scripts to set PSK (Pre-Shared Key) from hexadecimal string
- Usage:
  ```bash
  ./scripts/set_psk.sh <endpoint> <psk_identity> <hex_key>
  python3 scripts/set_psk.py <endpoint> <psk_identity> <hex_key>
  ```

### Dependencies Added
- SQLite JDBC Driver (`org.xerial:sqlite-jdbc:3.42.0.0`)

### Known Issues
1. **DTLS/PSK Authentication**: Not working yet - needs investigation
2. **RPK Mode**: Not fully supported in SqliteSecurityStore (logs warning)

### Database Location
`data/security.db`

### Git Commits Made
1. Replace FileSecurityStore with SqliteSecurityStore for SQLite-based persistence
2. Extract Leshan server code into Lwm2mServer class
3. Remove webserver code from SimpleIoTGateway - use WebServer class instead
4. Extract MQTT client setup into MqttServer class
5. Register IoTDeviceAdapter with LeshanServer RegistrationService
6. Add debug messages to all registration and observation callbacks
7. Ensure SqliteSecurityStore loads all entries on start and is actually used
8. Fix loadFromDatabase to read from individual columns instead of serialized_info
9. Add scripts to set PSK from hexadecimal string

## Architecture Overview

```
SimpleIoTGateway.java
├── Lwm2mServer.createServer() → LeshanServer
│   ├── SqliteSecurityStore (security info persistence)
│   └── IoTDeviceAdapter (registered with RegistrationService & ObservationService)
├── MqttServer.createServer() → MQTT client
└── WebServer (optional, based on CLI flag)
```

## Important Notes for Future Work

1. **DTLS/PSK Authentication**: The SqliteSecurityStore correctly loads and persists security information, but DTLS/PSK authentication needs debugging. Check:
   - PSK identity and key format
   - DTLS configuration in Lwm2mServer
   - Client-side PSK configuration

2. **Database Migration**: If migrating from FileSecurityStore, ensure the SQLite database is properly initialized with the correct schema.

3. **Logging**: All registration and observation events are now logged with debug messages. Check logs for:
   - `=== registered() called ===`
   - `=== unregistered() called ===`
   - `=== newObservation() called ===`
   - etc.

4. **Scripts**: Use the provided scripts to manage PSK keys:
   - `scripts/set_psk.sh` (Bash)
   - `scripts/set_psk.py` (Python)

## File Structure
```
src/main/java/at/voxel/iotgw/
├── SimpleIoTGateway.java
├── IoTDeviceAdapter.java
├── lwm2m/
│   └── Lwm2mServer.java
├── mqtt/
│   ├── MqttServer.java
│   └── MqttAdapter.java
├── security/
│   └── SqliteSecurityStore.java
└── cli/
    └── IoTGatewayCli.java
scripts/
├── set_psk.sh
└── set_psk.py
```

#### 6. IoTDeviceAdapter Multi-Device Support
**File**: `src/main/java/at/voxel/iotgw/IoTDeviceAdapter.java`

- Changed `RegInfo reg` (single instance) to `Map<String, RegInfo> regInfo` (HashMap indexed by endpoint)
- Added helper methods:
  - `getRegInfo(String endpoint)` - Get or create RegInfo for a device
  - `getRegInfo(Registration registration)` - Get RegInfo by registration
  - `getEndpoints()` - Get all registered device endpoints
  - `getDeviceCount()` - Get the number of registered devices
- Updated all callback methods to work with the map:
  - `registered()` - Now creates/updates RegInfo per endpoint
  - `unregistered()` - Updates RegInfo for the unregistered endpoint
  - `updated()` - Updates RegInfo for the updated endpoint
  - `onAwake()` / `onSleeping()` - Updates RegInfo state per endpoint
- Updated `getSerialNo()` and `getFirmwareVersion()` to accept `Registration` parameter instead of using instance field
- Removed `setId()` and `getId()` methods (no longer applicable with multiple devices)

## Quick Reference Commands

### Set PSK Key
```bash
# Using bash script
./scripts/set_psk.sh "device1" "device1" "0123456789abcdef0123456789abcdef"

# Using Python script
python3 scripts/set_psk.py "device1" "device1" "0123456789abcdef0123456789abcdef"
```

### Check Database
```bash
sqlite3 data/security.db "SELECT endpoint, psk_identity, length(pre_shared_key) FROM security_info;"
```

### Build Project
```bash
mvn clean package
```

### Run
```bash
java -jar target/voxel-iot-gateway-1.0.0.jar
```