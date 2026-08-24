package at.voxel.iotgw;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.ee10.servlet.DefaultServlet;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;

import org.eclipse.leshan.demo.cli.ShortErrorMessageHandler;
import org.eclipse.leshan.demo.servers.json.servlet.SecurityServlet;
import org.eclipse.leshan.demo.servers.json.servlet.ServerServlet;
import org.eclipse.leshan.server.LeshanServer;
import org.eclipse.leshan.servers.security.EditableSecurityStore;

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
import at.voxel.iotgw.servlet.ClientServlet;
import at.voxel.iotgw.servlet.EventServlet;
import at.voxel.iotgw.servlet.ObjectSpecServlet;

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

            // In a real implementation, this would:
            // 1. Initialize MQTT connection
            // 2. Set up CoAP server
            // 3. Handle device registration
            // 4. Process IoT data

            at.voxel.iotgw.Server webServer = null;
            if (cli.main.enableWebServer) {
                System.out.println("Starting webServer");
                webServer = new at.voxel.iotgw.WebServer(cli, lwm2mServer);
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

    private static Server createJettyServer(IoTGatewayCli cli, LeshanServer lwServer) {
        // Now prepare Jetty
        InetSocketAddress jettyAddr;
        if (cli.main.webhost == null) {
            jettyAddr = new InetSocketAddress(cli.main.webPort);
        } else {
            jettyAddr = new InetSocketAddress(cli.main.webhost, cli.main.webPort);
        }
        System.out.printf("Web Listening on port %d%n", cli.main.webPort);

        Server server = new Server(jettyAddr);
        ServletContextHandler root = new ServletContextHandler("/", true, false);
        server.setHandler(root);

        // Create static Servlet
        DefaultServlet staticServelt = new DefaultServlet();
        ServletHolder staticHolder = new ServletHolder(staticServelt);
        staticHolder.setInitParameter("baseResource",
                SimpleIoTGateway.class.getClassLoader().getResource("webapp").toExternalForm());
        staticHolder.setInitParameter("gzip", "true");
        staticHolder.setInitParameter("cacheControl", "public, max-age=31536000");
        root.addServlet(staticHolder, "/");

        // Create REST API Servlets
        EventServlet eventServlet = new EventServlet(lwServer);
        ServletHolder eventServletHolder = new ServletHolder(eventServlet);
        root.addServlet(eventServletHolder, "/api/event/*");

        ServletHolder clientServletHolder = new ServletHolder(new ClientServlet(lwServer, eventServlet));
        root.addServlet(clientServletHolder, "/api/clients/*");

        ServletHolder securityServletHolder;
        if (cli.identity.isRPK()) {
            securityServletHolder = new ServletHolder(new SecurityServlet(
                    (EditableSecurityStore) lwServer.getSecurityStore(), cli.identity.getPublicKey()));
        } else {
            securityServletHolder = new ServletHolder(new SecurityServlet(
                    (EditableSecurityStore) lwServer.getSecurityStore(), cli.identity.getCertChain()[0]));
        }
        root.addServlet(securityServletHolder, "/api/security/*");

        ServletHolder serverServletHolder;
        if (cli.identity.isRPK()) {
            serverServletHolder = new ServletHolder(
                    new ServerServlet(lwServer.getEndpoints(), cli.identity.getPublicKey()));
        } else {
            serverServletHolder = new ServletHolder(
                    new ServerServlet(lwServer.getEndpoints(), cli.identity.getCertChain()[0]));
        }
        root.addServlet(serverServletHolder, "/api/server/*");

        ServletHolder objectSpecServletHolder = new ServletHolder(
                new ObjectSpecServlet(lwServer.getModelProvider(), lwServer.getRegistrationService()));
        root.addServlet(objectSpecServletHolder, "/api/objectspecs/*");

        return server;
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
