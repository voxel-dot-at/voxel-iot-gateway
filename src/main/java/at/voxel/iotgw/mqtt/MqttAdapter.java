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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MQTT Adapter using Eclipse Paho client
 * 
 * @author simon
 */
public class MqttAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(MqttAdapter.class);

    private final MqttClient client;
    private final Map<String, MqttMessageListener> subscriptions = new ConcurrentHashMap<>();

    public MqttAdapter(String host, int port, String username, String pass) {
        String clientId = "voxel-iot-gateway-" + UUID.randomUUID().toString().substring(0, 8);
        String serverURI = "tcp://" + host + ":" + port;
        
        try {
            client = new MqttClient(serverURI, clientId);
            
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setUserName(username);
            options.setPassword(pass.toCharArray());
            options.setConnectionTimeout(30);
            options.setKeepAliveInterval(60);
            
            // Set callback
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    LOG.warn("MQTT connection lost: " + cause.getMessage(), cause);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    LOG.debug("Message arrived on topic: " + topic);
                    
                    // Notify all listeners for this topic
                    for (Map.Entry<String, MqttMessageListener> entry : subscriptions.entrySet()) {
                        String pattern = entry.getKey();
                        MqttMessageListener listener = entry.getValue();
                        if (matchesTopic(pattern, topic)) {
                            try {
                                listener.onMessage(topic, new String(message.getPayload()));
                            } catch (Exception e) {
                                LOG.error("Error in message listener for topic " + pattern, e);
                            }
                        }
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    LOG.debug("Message delivery complete");
                }
            });
            
            // Connect
            client.connect(options).waitForCompletion();
            
            LOG.info("MQTT connected to " + serverURI + " as " + clientId);
            
            // Setup subscribes or start publishing
            subscribeAll();
            
        } catch (MqttException e) {
            LOG.error("MQTT connection failed to " + serverURI, e);
            throw new RuntimeException("Failed to connect to MQTT broker", e);
        }
    }

    public MqttAdapter(String host, int port) {
        String clientId = "voxel-iot-gateway-" + UUID.randomUUID().toString().substring(0, 8);
        String serverURI = "tcp://" + host + ":" + port;
        
        try {
            client = new MqttClient(serverURI, clientId);
            
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setConnectionTimeout(30);
            options.setKeepAliveInterval(60);
            
            // Set callback
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    LOG.warn("MQTT connection lost: " + cause.getMessage(), cause);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    LOG.debug("Message arrived on topic: " + topic);
                    
                    // Notify all listeners for this topic
                    for (Map.Entry<String, MqttMessageListener> entry : subscriptions.entrySet()) {
                        String pattern = entry.getKey();
                        MqttMessageListener listener = entry.getValue();
                        if (matchesTopic(pattern, topic)) {
                            try {
                                listener.onMessage(topic, new String(message.getPayload()));
                            } catch (Exception e) {
                                LOG.error("Error in message listener for topic " + pattern, e);
                            }
                        }
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    LOG.debug("Message delivery complete");
                }
            });
            
            // Connect
            client.connect(options).waitForCompletion();
            
            LOG.info("MQTT connected to " + serverURI + " as " + clientId);
            
            // Setup subscribes or start publishing
            subscribeAll();
            
        } catch (MqttException e) {
            LOG.error("MQTT connection failed to " + serverURI, e);
            throw new RuntimeException("Failed to connect to MQTT broker", e);
        }
    }

    /**
     * Subscribe to a topic pattern with a message listener
     * @param pattern The topic pattern to subscribe to (e.g., "iot/#")
     * @param listener The listener to receive messages
     */
    public void subscribe(String pattern, MqttMessageListener listener) {
        try {
            int qos = 1;
            client.subscribe(pattern, qos);
            subscriptions.put(pattern, listener);
            LOG.info("MQTT subscribed to: " + pattern + " (QoS: " + qos + ")");
        } catch (MqttException e) {
            LOG.error("MQTT subscribe failed for pattern: " + pattern, e);
        }
    }

    /**
     * Subscribe to all IoT topics
     */
    private void subscribeAll() {
        subscribe("ind/#", new MqttMessageListener() {
            @Override
            public void onMessage(String topic, String payload) {
                handleIndication(topic, payload);
            }
        });
    }

    /**
     * Publish a message to a topic
     * @param topic The topic to publish to
     * @param payload The message payload
     */
    public void publish(String topic, String payload) {
        try {
            MqttMessage message = new MqttMessage(payload.getBytes());
            message.setQos(1); // QoS 1: At least once
            client.publish(topic, message);
            LOG.info("MQTT published to: " + topic);
        } catch (MqttException e) {
            LOG.error("MQTT publish failed for topic: " + topic, e);
        }
    }

    /**
     * Handle indication messages
     * @param topic The topic the message arrived on
     * @param payload The message payload
     */
    private void handleIndication(String topic, String payload) {
        LOG.info("Handling indication from topic: " + topic);
        LOG.debug("Payload: " + payload);
        // TODO: Implement indication handling logic
    }

    /**
     * Check if a topic matches a pattern
     * @param pattern The topic pattern (e.g., "iot/#")
     * @param topic The actual topic
     * @return true if the topic matches the pattern
     */
    private boolean matchesTopic(String pattern, String topic) {
        if (pattern.equals("#")) {
            return true;
        }
        if (pattern.endsWith("/#")) {
            String prefix = pattern.substring(0, pattern.length() - 2);
            return topic.startsWith(prefix) && topic.length() > prefix.length();
        }
        if (pattern.equals(topic)) {
            return true;
        }
        if (pattern.contains("/")) {
            String[] patternParts = pattern.split("/");
            String[] topicParts = topic.split("/");
            if (patternParts.length != topicParts.length) {
                return false;
            }
            for (int i = 0; i < patternParts.length; i++) {
                if (!patternParts[i].equals(topicParts[i]) && !patternParts[i].equals("+")) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Disconnect from the MQTT broker
     */
    public void disconnect() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
                client.close();
                LOG.info("MQTT client disconnected");
            }
        } catch (MqttException e) {
            LOG.error("Error disconnecting MQTT client", e);
        }
    }

    /**
     * Check if connected to the MQTT broker
     * @return true if connected
     */
    public boolean isConnected() {
        return client != null && client.isConnected();
    }
}
