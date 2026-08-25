package at.voxel.iotgw.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MQTT Server implementation for IoT Gateway.
 */
public class MqttServer {
    private static final Logger LOG = LoggerFactory.getLogger(MqttServer.class);

    private final MqttClient client;
    private boolean connected = false;

    /**
     * Creates and configures an MQTT client.
     *
     * @return A configured MqttClient instance, or null if connection fails
     */
    public static MqttServer createServer() {
        return new MqttServer();
    }

    private MqttServer() {
        System.out.println("Setting up MQTT client...");
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
            String topic = "iot/#";
            int qos = 1;
            client.subscribe(topic, qos);
            LOG.info("Subscribed to topic: {} with QoS: {}", topic, qos);
        } catch (MqttException e) {
            LOG.error("Error setting up MQTT client: {}", e.getMessage());
            throw new RuntimeException("Failed to setup MQTT client", e);
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
