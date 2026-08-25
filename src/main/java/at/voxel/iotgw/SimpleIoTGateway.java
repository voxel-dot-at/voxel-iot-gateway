package at.voxel.iotgw;

import java.util.concurrent.TimeUnit;

import org.eclipse.leshan.demo.cli.ShortErrorMessageHandler;
import org.eclipse.leshan.server.LeshanServer;

import at.voxel.iotgw.lwm2m.Lwm2mServer;
import at.voxel.iotgw.mqtt.MqttAdapter;
import at.voxel.iotgw.mqtt.MqttServer;
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

            // Setup MQTT adapter
            MqttAdapter mqttAdapter = new MqttAdapter("localhost", 1883);

            // Setup IoT Device Adapter and register it
            IoTDeviceAdapter deviceAdapter = new IoTDeviceAdapter(lwm2mServer, mqttAdapter);
            lwm2mServer.getRegistrationService().addListener(deviceAdapter);
            lwm2mServer.getObservationService().addListener(deviceAdapter);

            // Setup MQTT client
            MqttServer mqttServer = MqttServer.createServer();

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
                    System.out.println("Gateway running... Press Ctrl+C to stop. mqtt: " + mqttServer.isConnected() + " web: " + webServer.isRunning() );
                    
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
            mqttServer.disconnect();
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
}
