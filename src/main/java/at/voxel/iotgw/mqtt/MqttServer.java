/** *****************************************************************************
 * Copyright (c) 2023-2024 VoXel Interaction Design.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 *
 ****************************************************************************** */
package at.voxel.iotgw.mqtt;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.leshan.core.peer.IpPeer;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.server.LeshanServer;
import org.eclipse.leshan.servers.security.NonUniqueSecurityInfoException;
import org.eclipse.leshan.servers.security.SecurityInfo;
import org.eclipse.leshan.servers.security.EditableSecurityStore;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * MQTT Server implementation for IoT Gateway with security store operations.
 * <p>
 * This class extends basic MQTT functionality to expose security store operations
 * via MQTT topics. Clients can read, create, update, and delete security information
 * by publishing JSON messages to specific topics.
 * </p>
 * <p>
 * Topic structure:
 * <ul>
 *   <li><b>iot/sec/read/&lt;endpoint&gt;</b> - Read security info for an endpoint</li>
 *   <li><b>iot/sec/create</b> - Create new security info</li>
 *   <li><b>iot/sec/update</b> - Update existing security info</li>
 *   <li><b>iot/sec/delete/&lt;endpoint&gt;</b> - Delete security info</li>
 *   <li><b>iot/sec/list</b> - List all security entries</li>
 * </ul>
 * </p>
 * <p>
 * Response topics mirror request topics with a "response/" prefix:
 * <ul>
 *   <li><b>iot/sec/response/read/&lt;endpoint&gt;</b></li>
 *   <li><b>iot/sec/response/create</b></li>
 *   <li><b>iot/sec/response/update</b></li>
 *   <li><b>iot/sec/response/delete/&lt;endpoint&gt;</b></li>
 *   <li><b>iot/sec/response/list</b></li>
 * </ul>
 * </p>
 *
 * @author simon
 */
public class MqttServer {

    private static final Logger LOG = LoggerFactory.getLogger(MqttServer.class);

    // Topic prefixes
    public static final String SEC_TOPIC_PREFIX = "iot/sec/";
    public static final String SEC_RESPONSE_PREFIX = "iot/sec/response/";
    public static final String SEC_READ_TOPIC = SEC_TOPIC_PREFIX + "read/+";
    public static final String SEC_CREATE_TOPIC = SEC_TOPIC_PREFIX + "create";
    public static final String SEC_UPDATE_TOPIC = SEC_TOPIC_PREFIX + "update";
    public static final String SEC_DELETE_TOPIC = SEC_TOPIC_PREFIX + "delete/+";
    public static final String SEC_LIST_TOPIC = SEC_TOPIC_PREFIX + "list";

    // JSON field names
    public static final String FIELD_MODE = "mode";
    public static final String FIELD_ENDPOINT = "endpoint";
    public static final String FIELD_IDENTITY = "identity";
    public static final String FIELD_KEY = "key";
    public static final String FIELD_KEY_HEX = "keyHex";
    public static final String FIELD_PSK_IDENTITY = "pskIdentity";
    public static final String FIELD_PRE_SHARED_KEY = "preSharedKey";
    public static final String FIELD_RAW_PUBLIC_KEY = "rawPublicKey";
    public static final String FIELD_USE_X509_CERT = "useX509Cert";
    public static final String FIELD_OSORE_SENDER_ID = "oscoreSenderId";
    public static final String FIELD_OSORE_RECIPIENT_ID = "oscoreRecipientId";
    public static final String FIELD_OSORE_MASTER_SECRET = "oscoreMasterSecret";
    public static final String FIELD_OSORE_AEAD_ALGORITHM = "oscoreAeadAlgorithm";
    public static final String FIELD_OSORE_HKDF_ALGORITHM = "oscoreHkdfAlgorithm";
    public static final String FIELD_OSORE_MASTER_SALT = "oscoreMasterSalt";

    // Security modes
    public static final String MODE_PSK = "PSK";
    public static final String MODE_X509 = "X509";
    public static final String MODE_RPK = "RPK";
    public static final String MODE_OSORE = "OSCORE";

    private final MqttClient client;
    private final EditableSecurityStore securityStore;
    private final LeshanServer leshanServer;
    private boolean connected = false;
    private final ObjectMapper objectMapper;
    private final Map<String, MqttMessageListener> subscriptions = new ConcurrentHashMap<>();

    /**
     * Creates and configures an MQTT client with security store operations.
     *
     * @param securityStore The security store to expose via MQTT
     * @param leshanServer The Leshan server instance (for peer resolution)
     * @return A configured MqttServer instance
     */
    public static MqttServer createServer(EditableSecurityStore securityStore, LeshanServer leshanServer) {
        return new MqttServer(securityStore, leshanServer);
    }

    private MqttServer(EditableSecurityStore securityStore, LeshanServer leshanServer) {
        this.securityStore = securityStore;
        this.leshanServer = leshanServer;
        this.objectMapper = new ObjectMapper();

        System.out.println("Setting up MQTT client with security store support...");
        try {
            // Create MQTT client with a unique client ID
            String brokerUrl = "tcp://localhost:1883"; // Default MQTT broker
            String clientId = "voxel-iot-gateway-" + System.currentTimeMillis();
            client = new MqttClient(brokerUrl, clientId);

            // Set up connection options
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setConnectionTimeout(30);
            options.setKeepAliveInterval(60);

            // Register MQTT callback
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    LOG.error("MQTT connection lost: {}", cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    LOG.info("Received message on topic '{}'", topic);
                    LOG.debug("  Payload: {}", new String(message.getPayload()));
                    LOG.debug("  QoS: {}", message.getQos());
                    LOG.debug("  Retained: {}", message.isRetained());

                    // Route to appropriate handler
                    handleSecurityMessage(topic, new String(message.getPayload(), StandardCharsets.UTF_8));
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    LOG.debug("Message delivery complete");
                }
            });

            // Connect to the broker
            client.connect(options);
            connected = true;
            LOG.info("MQTT client connected successfully");

            // Subscribe to IoT topics
            subscribe("iot/#", 1);
            LOG.info("Subscribed to topic: iot/# with QoS: 1");

            // Subscribe to security topics
            subscribe(SEC_READ_TOPIC, 1);
            subscribe(SEC_CREATE_TOPIC, 1);
            subscribe(SEC_UPDATE_TOPIC, 1);
            subscribe(SEC_DELETE_TOPIC, 1);
            subscribe(SEC_LIST_TOPIC, 1);
            LOG.info("Subscribed to security topics");

        } catch (MqttException e) {
            LOG.error("Error setting up MQTT client: {}", e.getMessage());
            throw new RuntimeException("Failed to setup MQTT client", e);
        }
    }

    /**
     * Handles incoming security-related MQTT messages.
     *
     * @param topic The topic the message arrived on
     * @param payload The message payload
     */
    private void handleSecurityMessage(String topic, String payload) {
        try {
            if (topic.equals(SEC_CREATE_TOPIC)) {
                handleCreate(payload);
            } else if (topic.equals(SEC_UPDATE_TOPIC)) {
                handleUpdate(payload);
            } else if (topic.equals(SEC_LIST_TOPIC)) {
                handleList();
            } else if (topic.startsWith(SEC_TOPIC_PREFIX + "read/")) {
                String endpoint = topic.substring((SEC_TOPIC_PREFIX + "read/").length());
                handleRead(endpoint);
            } else if (topic.startsWith(SEC_TOPIC_PREFIX + "delete/")) {
                String endpoint = topic.substring((SEC_TOPIC_PREFIX + "delete/").length());
                handleDelete(endpoint);
            } else {
                LOG.debug("Ignoring message on non-security topic: {}", topic);
            }
        } catch (Exception e) {
            LOG.error("Error handling security message on topic '{}'", topic, e);
            sendErrorResponse(topic, e.getMessage());
        }
    }

    /**
     * Handles a read request for security info.
     *
     * @param endpoint The endpoint name to look up
     */
    private void handleRead(String endpoint) {
        LOG.info("Read request for endpoint: {}", endpoint);

        SecurityInfo info = securityStore.getByEndpoint(endpoint);
        String responseTopic = SEC_RESPONSE_PREFIX + "read/" + endpoint;

        if (info == null) {
            sendJsonResponse(responseTopic, createErrorResponse("Security info not found for endpoint: " + endpoint));
        } else {
            sendJsonResponse(responseTopic, createSecurityInfoNode(info));
        }
    }

    /**
     * Handles a create request for new security info.
     *
     * @param payload JSON payload containing security info
     */
    private void handleCreate(String payload) {
        LOG.info("Create request received");

        try {
            JsonNode json = objectMapper.readTree(payload);
            SecurityInfo info = parseSecurityInfo(json, true);

            SecurityInfo previous = securityStore.add(info);
            String responseTopic = SEC_RESPONSE_PREFIX + "create";

            if (previous != null) {
                sendJsonResponse(responseTopic, createErrorResponse("Endpoint already exists: " + info.getEndpoint()));
            } else {
                sendJsonResponse(responseTopic, createSuccessResponse("Security info created for endpoint: " + info.getEndpoint()));
            }
        } catch (Exception e) {
            LOG.error("Error creating security info", e);
            sendJsonResponse(SEC_RESPONSE_PREFIX + "create", createErrorResponse("Failed to create security info: " + e.getMessage()));
        }
    }

    /**
     * Handles an update request for existing security info.
     *
     * @param payload JSON payload containing updated security info
     */
    private void handleUpdate(String payload) {
        LOG.info("Update request received");

        try {
            JsonNode json = objectMapper.readTree(payload);
            SecurityInfo info = parseSecurityInfo(json, false);

            // First remove the old entry
            SecurityInfo previous = securityStore.remove(info.getEndpoint(), false);
            if (previous == null) {
                sendJsonResponse(SEC_RESPONSE_PREFIX + "update", createErrorResponse("Endpoint not found for update: " + info.getEndpoint()));
                return;
            }

            // Add the updated entry
            securityStore.add(info);
            sendJsonResponse(SEC_RESPONSE_PREFIX + "update", createSuccessResponse("Security info updated for endpoint: " + info.getEndpoint()));
        } catch (Exception e) {
            LOG.error("Error updating security info", e);
            sendJsonResponse(SEC_RESPONSE_PREFIX + "update", createErrorResponse("Failed to update security info: " + e.getMessage()));
        }
    }

    /**
     * Handles a delete request for security info.
     *
     * @param endpoint The endpoint name to delete
     */
    private void handleDelete(String endpoint) {
        LOG.info("Delete request for endpoint: {}", endpoint);

        SecurityInfo removed = securityStore.remove(endpoint, false);
        String responseTopic = SEC_RESPONSE_PREFIX + "delete/" + endpoint;

        if (removed == null) {
            sendJsonResponse(responseTopic, createErrorResponse("Security info not found for endpoint: " + endpoint));
        } else {
            sendJsonResponse(responseTopic, createSuccessResponse("Security info deleted for endpoint: " + endpoint));
        }
    }

    /**
     * Handles a list request for all security info.
     */
    private void handleList() {
        LOG.info("List request received");

        try {
            com.fasterxml.jackson.databind.node.ArrayNode listNode = objectMapper.createArrayNode();
            securityStore.getAll().forEach(info -> listNode.add(createSecurityInfoNode(info)));

            sendJsonResponse(SEC_RESPONSE_PREFIX + "list", listNode);
        } catch (Exception e) {
            LOG.error("Error listing security info", e);
            sendJsonResponse(SEC_RESPONSE_PREFIX + "list", createErrorResponse("Failed to list security info: " + e.getMessage()));
        }
    }

    /**
     * Parses a JSON node into a SecurityInfo object.
     *
     * @param json The JSON node to parse
     * @param requireEndpoint Whether to require the endpoint field
     * @return The parsed SecurityInfo
     * @throws Exception if parsing fails
     */
    private SecurityInfo parseSecurityInfo(JsonNode json, boolean requireEndpoint) throws Exception {
        String mode = json.has(FIELD_MODE) ? json.get(FIELD_MODE).asText().toUpperCase() : null;
        String endpoint = json.has(FIELD_ENDPOINT) ? json.get(FIELD_ENDPOINT).asText() : null;

        if (requireEndpoint && endpoint == null) {
            throw new IllegalArgumentException("Endpoint is required");
        }

        if (mode == null) {
            throw new IllegalArgumentException("Mode is required (PSK, X509, RPK, or OSCORE)");
        }

        switch (mode) {
            case MODE_PSK:
                return parsePskInfo(json, endpoint);
            case MODE_X509:
                return parseX509Info(json, endpoint);
            case MODE_RPK:
                return parseRpKInfo(json, endpoint);
            case MODE_OSORE:
                return parseOscoreInfo(json, endpoint);
            default:
                throw new IllegalArgumentException("Unknown mode: " + mode);
        }
    }

    /**
     * Parses PSK security info from JSON.
     */
    private SecurityInfo parsePskInfo(JsonNode json, String endpoint) throws Exception {
        String identity = json.has(FIELD_IDENTITY) ? json.get(FIELD_IDENTITY).asText() :
                          json.has(FIELD_PSK_IDENTITY) ? json.get(FIELD_PSK_IDENTITY).asText() : null;
        byte[] key = parseKey(json);

        if (identity == null) {
            throw new IllegalArgumentException("PSK identity is required");
        }
        if (key == null || key.length == 0) {
            throw new IllegalArgumentException("PSK key is required");
        }

        return SecurityInfo.newPreSharedKeyInfo(endpoint, identity, key);
    }

    /**
     * Parses X.509 security info from JSON.
     */
    private SecurityInfo parseX509Info(JsonNode json, String endpoint) throws Exception {
        if (endpoint == null) {
            throw new IllegalArgumentException("Endpoint is required for X.509 mode");
        }
        return SecurityInfo.newX509CertInfo(endpoint);
    }

    /**
     * Parses RPK security info from JSON.
     */
    private SecurityInfo parseRpKInfo(JsonNode json, String endpoint) throws Exception {
        if (endpoint == null) {
            throw new IllegalArgumentException("Endpoint is required for RPK mode");
        }

        byte[] publicKeyBytes = parseKey(json);
        if (publicKeyBytes == null || publicKeyBytes.length == 0) {
            throw new IllegalArgumentException("Raw public key is required for RPK mode");
        }

        // Note: RPK support is limited in the current implementation
        LOG.warn("RPK mode is not fully supported yet. Creating placeholder for endpoint: {}", endpoint);
        return SecurityInfo.newX509CertInfo(endpoint);
    }

    /**
     * Parses OSCORE security info from JSON.
     */
    private SecurityInfo parseOscoreInfo(JsonNode json, String endpoint) throws Exception {
        if (endpoint == null) {
            throw new IllegalArgumentException("Endpoint is required for OSCORE mode");
        }

        byte[] senderId = parseKey(json);
        byte[] recipientId = parseKey(json);
        byte[] masterSecret = parseKey(json);

        if (senderId == null || recipientId == null || masterSecret == null) {
            throw new IllegalArgumentException("OSCORE requires senderId, recipientId, and masterSecret");
        }

        // Note: Full OSCORE support requires additional configuration
        LOG.warn("OSCORE mode is not fully supported yet. Creating placeholder for endpoint: {}", endpoint);
        return SecurityInfo.newX509CertInfo(endpoint);
    }

    /**
     * Parses a key from JSON (supports both hex and base64 formats).
     */
    private byte[] parseKey(JsonNode json) {
        if (json.has(FIELD_KEY_HEX)) {
            return hexToBytes(json.get(FIELD_KEY_HEX).asText());
        } else if (json.has(FIELD_KEY)) {
            String key = json.get(FIELD_KEY).asText();
            // Try hex first, then base64
            try {
                return hexToBytes(key);
            } catch (Exception e) {
                return Base64.getDecoder().decode(key);
            }
        }
        return null;
    }

    /**
     * Converts a hex string to bytes.
     */
    private byte[] hexToBytes(String hex) {
        if (hex == null || hex.isEmpty()) {
            return new byte[0];
        }
        // Remove optional "0x" prefix
        if (hex.startsWith("0x") || hex.startsWith("0X")) {
            hex = hex.substring(2);
        }
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            bytes[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) +
                                   Character.digit(hex.charAt(i + 1), 16));
        }
        return bytes;
    }

    /**
     * Creates a JSON node representing a SecurityInfo object.
     */
    private ObjectNode createSecurityInfoNode(SecurityInfo info) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("endpoint", info.getEndpoint());

        if (info.usePSK()) {
            node.put("mode", MODE_PSK);
            node.put("pskIdentity", info.getPskIdentity());
            node.put("preSharedKey", bytesToHex(info.getPreSharedKey()));
        } else if (info.useX509Cert()) {
            node.put("mode", MODE_X509);
        } else if (info.useOSCORE()) {
            node.put("mode", MODE_OSORE);
            // Note: OSCORE details are not fully exposed for security reasons
        } else {
            node.put("mode", "UNKNOWN");
        }

        return node;
    }

    /**
     * Creates a success response JSON node.
     */
    private ObjectNode createSuccessResponse(String message) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("success", true);
        node.put("message", message);
        return node;
    }

    /**
     * Creates an error response JSON node.
     */
    private ObjectNode createErrorResponse(String message) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("success", false);
        node.put("message", message);
        return node;
    }

    /**
     * Sends a JSON response to an MQTT topic.
     */
    private void sendJsonResponse(String topic, JsonNode json) {
        try {
            String payload = objectMapper.writeValueAsString(json);
            MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
            message.setQos(1);
            client.publish(topic, message);
            LOG.info("Published response to topic: {}", topic);
        } catch (Exception e) {
            LOG.error("Error sending response to topic: {}", topic, e);
        }
    }

    /**
     * Sends an error response to the corresponding response topic.
     */
    private void sendErrorResponse(String requestTopic, String errorMessage) {
        String responseTopic = requestTopic.replace(SEC_TOPIC_PREFIX, SEC_RESPONSE_PREFIX);
        sendJsonResponse(responseTopic, createErrorResponse(errorMessage));
    }

    /**
     * Converts bytes to a hex string.
     */
    private String bytesToHex(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Subscribes to a topic pattern.
     */
    private void subscribe(String topicPattern, int qos) {
        try {
            client.subscribe(topicPattern, qos);
            LOG.info("Subscribed to topic pattern: {} with QoS: {}", topicPattern, qos);
        } catch (MqttException e) {
            LOG.error("Failed to subscribe to topic pattern: {}", topicPattern, e);
        }
    }

    /**
     * Publishes a message to a topic.
     */
    public void publish(String topic, String payload) {
        try {
            MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
            message.setQos(1);
            client.publish(topic, message);
            LOG.info("Published to topic: {}", topic);
        } catch (MqttException e) {
            LOG.error("Failed to publish to topic: {}", topic, e);
        }
    }

    /**
     * Returns the underlying MqttClient instance.
     *
     * @return The MqttClient instance
     */
    public MqttClient getClient() {
        return client;
    }

    /**
     * Checks if the MQTT client is connected.
     *
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return connected && client.isConnected();
    }

    /**
     * Returns the security store.
     *
     * @return The EditableSecurityStore
     */
    public EditableSecurityStore getSecurityStore() {
        return securityStore;
    }

    /**
     * Returns the Leshan server instance.
     *
     * @return The LeshanServer instance
     */
    public LeshanServer getLeshanServer() {
        return leshanServer;
    }

    /**
     * Disconnects and closes the MQTT client.
     */
    public void disconnect() {
        try {
            if (client != null && connected) {
                client.disconnect();
                client.close();
                connected = false;
                LOG.info("MQTT client disconnected and closed");
            }
        } catch (MqttException e) {
            LOG.error("Error disconnecting MQTT client: {}", e.getMessage());
        }
    }
}
