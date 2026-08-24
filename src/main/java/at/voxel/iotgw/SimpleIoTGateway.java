package at.voxel.iotgw;

import java.util.concurrent.TimeUnit;

import org.eclipse.leshan.demo.cli.ShortErrorMessageHandler;
import org.eclipse.leshan.server.LeshanServer;

import at.voxel.iotgw.lwm2m.Lwm2mServer;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine;

import at.voxel.iotgw.cli.IoTGatewayCli;

/**
 * Simple IoT Gateway implementation that demonstrates the basic structure
 * with LWM2M server functionality
 */
public class SimpleIoTGateway {

    static {
        // Define a default logback.configurationFile
        String property = System.getProperty("logback.configurationFile");
        if (property == null) {
            System.setProperty("logback.configurationFile", "logback-config.xml");
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(SimpleIoTGateway.class);

    public static void main(String[] args) {
        System.out.println("Voxel IoT Gateway - Minimal Implementation");
        System.out.println("Version: 1.0.0");
        System.out.println("Running with basic dependencies...");

        LOG.debug("test log debug");
        LOG.info("test log info");
        LOG.warn("test log warn");
        IoTGatewayCli cli = new IoTGatewayCli();

        CommandLine command = new CommandLine(cli).setTrimQuotes(true)
                .setParameterExceptionHandler(new ShortErrorMessageHandler());
        // Handle exit code error
        int exitCode = command.execute(args);
        if (exitCode != 0)
            System.exit(exitCode);
        // Handle help or version command
        if (command.isUsageHelpRequested() || command.isVersionHelpRequested())
            System.exit(0);

        // Initialize and start LWM2M server
        LeshanServer lwm2mServer;
        try {
            lwm2mServer = Lwm2mServer.createServer(cli);

            // Setup MQTT client
            MqttClient mqttClient = setupMqttClient();

            // Setup Web server
            at.voxel.iotgw.Server webServer = null;
            if (cli.main.enableWebServer) {
                System.out.println("Starting webServer");
                webServer = new WebServer(cli, lwm2mServer);
            }

            lwm2mServer.start();
            if (webServer != null) {
                webServer.start();
            }
            System.out.println("Gateway initialized successfully");

            // Simulate gateway running
            try {
                while (true) {
                    System.out.println("Gateway running... Press Ctrl+C to stop. mqtt: " + mqttClient.isConnected() + " web: " + webServer.isRunning() );
                    
                    TimeUnit.MINUTES.sleep(1);
                }
            } catch (InterruptedException e) {
                // Thread.currentThread().interrupt();
                LOG.debug("interrupted.");
            }
            if (webServer != null) {
                webServer.stop();
            }

            // Stop the server when shutting down
            stopLwm2mServer(lwm2mServer);

            // Disconnect MQTT client
            if (mqttClient != null) {
                try {
                    mqttClient.disconnect();
                    mqttClient.close();
                    System.out.println("MQTT client disconnected and closed");
                } catch (MqttException e) {
                    System.err.println("Error disconnecting MQTT client: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println("Gateway shutting down...");
    }

    /**
     * Stops the LWM2M server
     * 
     * @param server The LWM2M server instance to stop
     */
    private static void stopLwm2mServer(LeshanServer server) {
        if (server != null) {
            System.out.println("Stopping LWM2M server...");
            server.stop();
            System.out.println("LWM2M server stopped");
        }
    }

    /**
     * Sets up and returns an MQTT client
     * 
     * @return The configured MQTT client instance
     */
    private static MqttClient setupMqttClient() {
        System.out.println("Setting up MQTT client...");
        try {
            // Create MQTT client with a unique client ID
            String brokerUrl = "tcp://localhost:1883"; // Default MQTT broker
            String clientId = "voxel-iot-gateway-" + System.currentTimeMillis();
            MqttClient client = new MqttClient(brokerUrl, clientId);

            // Set up connection options
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setConnectionTimeout(30);
            options.setKeepAliveInterval(60);

            // Register MQTT callback
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.err.println("MQTT connection lost: " + cause.getMessage());
                    cause.printStackTrace();
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    System.out.println("Received message on topic '" + topic + "'");
                    System.out.println("  Payload: " + new String(message.getPayload()));
                    System.out.println("  QoS: " + message.getQos());
                    System.out.println("  Retained: " + message.isRetained());
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    System.out.println("Message delivery complete");
                }
            });

            // Connect to the broker
            client.connect(options);
            System.out.println("MQTT client connected successfully");

            // Subscribe to IoT topics
            String topic = "iot/#";
            int qos = 1;
            client.subscribe(topic, qos);
            System.out.println("Subscribed to topic: " + topic + " with QoS: " + qos);

            return client;
        } catch (MqttException e) {
            System.err.println("Error setting up MQTT client: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
