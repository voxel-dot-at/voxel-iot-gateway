package at.voxel.iotgw;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.model.VersionedModelProvider;
// import org.eclipse.leshan.transport.californium.PrincipalMdcConnectionListener;
// import org.eclipse.leshan.transport.californium.server.endpoint.CaliforniumServerEndpointsProvider;
// import org.eclipse.leshan.transport.californium.server.endpoint.coap.CoapOscoreServerEndpointFactory;
// import org.eclipse.leshan.transport.californium.server.endpoint.coap.CoapServerProtocolProvider;
// import org.eclipse.leshan.transport.californium.server.endpoint.coaps.CoapsServerProtocolProvider;
// import org.eclipse.leshan.transport.javacoap.server.coaptcp.endpoint.JavaCoapTcpServerEndpointsProvider;
// import org.eclipse.leshan.transport.javacoap.server.coaptcp.endpoint.JavaCoapsTcpServerEndpointsProvider;
// import org.eclipse.leshan.transport.javacoap.server.endpoint.JavaCoapServerEndpointsProvider;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Simple IoT Gateway implementation that demonstrates the basic structure
 * with LWM2M server functionality
 */
public class SimpleIoTGateway {
    
    public static void main(String[] args) {
        System.out.println("Voxel IoT Gateway - Minimal Implementation");
        System.out.println("Version: 1.0.0");
        System.out.println("Running with basic dependencies...");
        
        // Initialize and start LWM2M server
        LeshanServer server = startLwm2mServer();
        
        // Setup MQTT client
        MqttClient mqttClient = setupMqttClient();
        
        // In a real implementation, this would:
        // 1. Initialize MQTT connection
        // 2. Set up CoAP server
        // 3. Handle device registration
        // 4. Process IoT data
        
        System.out.println("Gateway initialized successfully");
        
        // Simulate gateway running
        try {
            System.out.println("Gateway running... Press Ctrl+C to stop");
            TimeUnit.MINUTES.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Stop the server when shutting down
        stopLwm2mServer(server);
        
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
        System.out.println("Gateway shutting down...");
    }
    
    /**
     * Starts the LWM2M server
     * @return The started LWM2M server instance
     */
    private static LeshanServer startLwm2mServer() {
        System.out.println("Starting LWM2M server...");
        LeshanServerBuilder builder = new LeshanServerBuilder();

        // Define model provider
        List<ObjectModel> models = ObjectLoader.loadAllDefault();
        // models.addAll(ObjectLoader.loadDdfResources("/models/", LwM2mDemoConstant.modelPaths));
        // if (cli.main.modelsFolder != null) {
        //     models.addAll(ObjectLoader.loadObjectsFromDir(cli.main.modelsFolder, true));
        // }
        LwM2mModelProvider modelProvider = new VersionedModelProvider(models);
        builder.setObjectModelProvider(modelProvider);




        // // Create Californium Endpoints Provider:
        // // ------------------
        // // Create Server Endpoints Provider
        // CaliforniumServerEndpointsProvider.Builder endpointsBuilder = new CaliforniumServerEndpointsProvider.Builder(
        //         // Add coap Protocol support
        //         new CoapServerProtocolProvider(),

        //         // Add coaps protocol support
        //         new CoapsServerProtocolProvider(c -> {
        //             // Add MDC for connection logs
        //             if (cli.helpsOptions.getVerboseLevel() > 0)
        //                 c.setConnectionListener(new PrincipalMdcConnectionListener());

        //         }));

        // // Create Californium Configuration
        // Configuration serverCoapConfig = endpointsBuilder.createDefaultConfiguration();


        LeshanServer server = builder.build();       
        
        server.start();

        System.out.println("LWM2M server started successfully");
        return server;
    }
    
    /**
     * Stops the LWM2M server
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
