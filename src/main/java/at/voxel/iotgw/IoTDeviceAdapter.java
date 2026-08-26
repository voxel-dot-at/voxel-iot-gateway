/*******************************************************************************
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
 * Contributors:
 *     Simon Vogl - initial implementation
 *******************************************************************************/
package at.voxel.iotgw;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.leshan.core.LwM2m.Version;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.server.LeshanServer;
import org.eclipse.leshan.server.observation.ObservationListener;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationListener;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import at.voxel.iotgw.mqtt.MqttAdapter;

/**
 * Device handler for external devices allowed to communicate with the backend.
 *
 * @author simon
 */
public class IoTDeviceAdapter implements RegistrationListener, ObservationListener {

    private static final Logger LOG = LoggerFactory.getLogger(IoTDeviceAdapter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String TOPIC = "m2m/device";

    public enum RegState {
        unregistered, registered, awake, asleep, offline
    };

    public enum DeviceAction {
        registering, updated, deregistering, internal
    };

    private final Map<String, RegInfo> regInfo = new HashMap<>();
    private final LeshanServer server;

    private final MqttAdapter broker;

    public IoTDeviceAdapter(LeshanServer server, MqttAdapter broker) {
        this.server = server;
        this.broker = broker;
    }

    /**
     * Get or create RegInfo for a device by endpoint
     */
    private RegInfo getRegInfo(String endpoint) {
        return regInfo.computeIfAbsent(endpoint, k -> new RegInfo());
    }

    /**
     * Get RegInfo for a device by endpoint
     */
    public RegInfo getRegInfo(Registration registration) {
        return regInfo.get(registration.getEndpoint());
    }

    /**
     * Get all registered device endpoints
     */
    public Set<String> getEndpoints() {
        return regInfo.keySet();
    }

    /**
     * Get the number of registered devices
     */
    public int getDeviceCount() {
        return regInfo.size();
    }

    /**
     * handle device registration - subscribe to device updates interesting to us.
     *
     * @param registration
     * @param previousReg
     * @param previousObservations
     */
    @Override
    public void registered(Registration registration, Registration previousReg,
            Collection<Observation> previousObservations) {
        String endpoint = registration.getEndpoint();
        LOG.info("=== registered() called for endpoint: {} ===", endpoint);
        LOG.debug("previousReg: {}", previousReg != null ? previousReg.getEndpoint() : "null");
        LOG.debug("previousObservations: {}", previousObservations != null ? previousObservations.size() : "null");

        RegInfo reg = getRegInfo(endpoint);
        reg.regState = RegState.registered;
        reg.regId = registration.getEndpoint();
        reg.action = DeviceAction.registering;

        LOG.info("Device registered: id={}, endpoint={}", registration.getId(), registration.getEndpoint());

        try {
            broker.publish(TOPIC + "/reg/" + endpoint, objectMapper.writeValueAsString(reg));
            LOG.debug("Published registration state to MQTT topic: {}", TOPIC);
        } catch (JsonProcessingException e) {
            LOG.warn("register() Failed to publish for endpoint " + endpoint, e);
        }

        Map<Integer, Version> m = registration.getSupportedObject();
        Set<Integer> keys = m.keySet();
        LOG.debug("Supported objects count: {}", keys.size());
        for (int i = 0; i < keys.size(); i++) {
            LOG.debug("Supported object: {}", keys.toArray()[i]);
        }

        Map<String, String> att = registration.getAdditionalRegistrationAttributes();
        Object[] atts = att.keySet().toArray();
        LOG.debug("Additional attributes count: {}", atts.length);
        for (Object att1 : atts) {
            String key = (String) att1;
            LOG.debug("Additional attribute: {} = {}", key, att.get(key));
        }

        //
        // get some data..
        // /3/0/0 manufacturer
        // /3/0/1 model nr
        // /3/0/2 serial number
        // /3/0/3 firmware version
        //
        // /3/0/7 power source voltage
        // /3/0/9 battery level (%)
        // HERE: synchronous send/response
        String fw = "fw_?.?.?";
        String serNo = "serno_1234";
        try {
            LOG.debug("Fetching firmware version...");
            fw = getFirmwareVersion(registration);
            LOG.debug("Fetching serial number...");
            serNo = getSerialNo(registration);
        } catch (InterruptedException ex) {
            LOG.warn("Failed to read getFirmwareVersion!", ex);
        }
        LOG.info("Device info: fw={}, serial={}", fw, serNo);

        // register for updates
        ReadResponse response;
        try {
            LOG.debug("Sending ReadRequest(3, 0, 13)...");
            response = server.send(registration, new ReadRequest(3, 0, 13));

            if (response.isSuccess()) {
                LOG.debug("Device time: {}", ((LwM2mResource) response.getContent()).getValue());
            } else {
                LOG.warn("Failed to read device time: {} {}", response.getCode(), response.getErrorMessage());
            }

            LOG.debug("Sending ObserveRequest(3, 0, 13)...");
            ObserveResponse oresp = server.send(registration, new ObserveRequest(3, 0, 13));

            if (oresp.isSuccess()) {
                LOG.debug("ObserveResponse success: {}", ((LwM2mResource) oresp.getContent()).getValue());
            } else {
                LOG.warn("ObserveResponse failed: {} {}", oresp.getCode(), oresp.getErrorMessage());
            }
        } catch (InterruptedException e) {
            LOG.warn("register() Failed to get data for endpoint " + endpoint, e);
        }
        LOG.info("=== registered() completed ===");
    }

    /**
     * synchronous read request to get the serial number
     *
     * @param registration
     * @return
     * @throws InterruptedException
     */
    public String getSerialNo(Registration registration) throws InterruptedException {
        LOG.debug("=== getSerialNo() called for endpoint: {} ===", registration.getEndpoint());
        LOG.debug("Sending ReadRequest(3, 0, 2)...");
        ReadResponse response = server.send(registration, new ReadRequest(3, 0, 2));

        if (response.isSuccess()) {
            String value = (String) ((LwM2mResource) response.getContent()).getValue();
            LOG.debug("getSerialNo success: {}", value);
            LOG.debug("=== getSerialNo() completed ===");
            return value;
        } else {
            LOG.warn("getSerialNo Failed to read: {} {}", response.getCode(), response.getErrorMessage());
            LOG.debug("=== getSerialNo() completed with error ===");
            return null;
        }
    }

    /**
     * synchronous read request to get the firmware version
     *
     * @param registration
     * @return
     * @throws InterruptedException
     */
    public String getFirmwareVersion(Registration registration) throws InterruptedException {
        LOG.debug("=== getFirmwareVersion() called for endpoint: {} ===", registration.getEndpoint());
        LOG.debug("Sending ReadRequest(3, 0, 3)...");
        ReadResponse response = server.send(registration, new ReadRequest(3, 0, 3));

        if (response.isSuccess()) {
            String value = (String) ((LwM2mResource) response.getContent()).getValue();
            LOG.debug("getFirmwareVersion success: {}", value);
            LOG.debug("=== getFirmwareVersion() completed ===");
            return value;
        } else {
            LOG.warn("getFirmwareVersion Failed to read: {} {}", response.getCode(), response.getErrorMessage());
            LOG.debug("=== getFirmwareVersion() completed with error ===");
            return null;
        }
    }

    @Override
    public void unregistered(Registration registration, Collection<Observation> observations, boolean expired,
            Registration newReg) {
        String endpoint = registration.getEndpoint();
        LOG.info("=== unregistered() called for endpoint: {} ===", endpoint);
        LOG.debug("observations: {}", observations != null ? observations.size() : "null");
        LOG.debug("expired: {}", expired);
        LOG.debug("newReg: {}", newReg != null ? newReg.getEndpoint() : "null");

        RegInfo reg = getRegInfo(endpoint);
        reg.regState = RegState.unregistered;
        reg.regId = registration.getEndpoint();
        reg.action = DeviceAction.deregistering;

        LOG.info("Device unregistered: id={}, endpoint={}, expired={}", registration.getId(), registration.getEndpoint(), expired);

        try {
            broker.publish(TOPIC + "/unreg/" + endpoint, objectMapper.writeValueAsString(reg));
            LOG.debug("Published unregistration state to MQTT topic: {}", TOPIC);
        } catch (JsonProcessingException e) {
            LOG.warn("unregistered() Failed to publish for endpoint " + endpoint, e);
        }
        LOG.info("=== unregistered() completed ===");
    }

    @Override
    public void updated(RegistrationUpdate update, Registration updatedReg, Registration previousReg) {
        String endpoint = updatedReg.getEndpoint();
        LOG.info("=== updated() called for endpoint: {} ===", endpoint);
        LOG.debug("update: {}", update);
        LOG.debug("previousReg: {}", previousReg != null ? previousReg.getEndpoint() : "null");

        RegInfo reg = getRegInfo(endpoint);
        reg.regId = updatedReg.getId();
        reg.action = DeviceAction.updated;

        LOG.info("Device updated: id={}, endpoint={}", updatedReg.getId(), updatedReg.getEndpoint());

        try {
            broker.publish(TOPIC, objectMapper.writeValueAsString(reg));
            LOG.debug("Published update state to MQTT topic: {}", TOPIC);
        } catch (JsonProcessingException e) {
            LOG.warn("updated() Failed to publish for endpoint " + endpoint, e);
        }
        LOG.info("=== updated() completed ===");
    }

    public void onAwake(Registration registration) {
        String endpoint = registration.getEndpoint();
        LOG.info("=== onAwake() called for endpoint: {} ===", endpoint);

        RegInfo reg = getRegInfo(endpoint);
        reg.regId = registration.getId();
        reg.regState = RegState.awake;
        reg.action = DeviceAction.internal;

        LOG.info("Device awake: id={}, endpoint={}", registration.getId(), registration.getEndpoint());

        try {
            broker.publish(TOPIC, objectMapper.writeValueAsString(reg));
            LOG.debug("Published awake state to MQTT topic: {}", TOPIC);
        } catch (JsonProcessingException e) {
            LOG.warn("onAwake() Failed to publish for endpoint " + endpoint, e);
        }
        LOG.info("=== onAwake() completed ===");
    }

    public void onSleeping(Registration registration) {
        String endpoint = registration.getEndpoint();
        LOG.info("=== onSleeping() called for endpoint: {} ===", endpoint);

        RegInfo reg = getRegInfo(endpoint);
        reg.regId = registration.getId();
        reg.regState = RegState.asleep;
        reg.action = DeviceAction.internal;

        LOG.info("Device sleeping: id={}, endpoint={}", registration.getId(), registration.getEndpoint());

        try {
            broker.publish(TOPIC, objectMapper.writeValueAsString(reg));
            LOG.debug("Published sleeping state to MQTT topic: {}", TOPIC);
        } catch (JsonProcessingException e) {
            LOG.warn("onSleeping() Failed to publish for endpoint " + endpoint, e);
        }
        LOG.info("=== onSleeping() completed ===");
    }

    @Override
    public void newObservation(Observation observation, Registration registration) {
        String endpoint = registration.getEndpoint();
        LOG.info("=== newObservation() called for endpoint: {} ===", endpoint);
        
        // Extract observed resource path from observation
        String resourcePath = "unknown";
        if (observation instanceof org.eclipse.leshan.core.observation.SingleObservation) {
            resourcePath = ((org.eclipse.leshan.core.observation.SingleObservation) observation).getPath().toString();
        } else if (observation instanceof org.eclipse.leshan.core.observation.CompositeObservation) {
            resourcePath = ((org.eclipse.leshan.core.observation.CompositeObservation) observation).getPaths().toString();
        }
        LOG.info("Observed Resource Path: {}", resourcePath);
        LOG.info("Note: Actual transmitted values are received in onResponse() callback");
        
        // Print observation context data
        LOG.info("Observation ID: {}", observation.getId());
        LOG.info("Observation Registration ID: {}", observation.getRegistrationId());
        LOG.info("Observation Context: {}", observation.getContext());
        LOG.info("Observation Protocol Data: {}", observation.getProtocolData());
        
        // Print registration context data
        LOG.info("Registration ID: {}", registration.getId());
        LOG.info("Registration Endpoint: {}", registration.getEndpoint());
        LOG.info("Registration Lifetime: {}", registration.getLifeTimeInSec());
        LOG.info("Registration Binding: {}", registration.getBindingMode());
        LOG.info("Registration Last Update: {}", registration.getLastUpdate());
        
        // Print supported objects
        Map<Integer, Version> supportedObjects = registration.getSupportedObject();
        LOG.info("Supported Objects Count: {}", supportedObjects.size());
        for (Map.Entry<Integer, Version> entry : supportedObjects.entrySet()) {
            LOG.info("  Supported Object: {} (version: {})", entry.getKey(), entry.getValue());
        }
        
        // Print additional attributes
        Map<String, String> additionalAttrs = registration.getAdditionalRegistrationAttributes();
        if (!additionalAttrs.isEmpty()) {
            LOG.info("Additional Registration Attributes:");
            for (Map.Entry<String, String> entry : additionalAttrs.entrySet()) {
                LOG.info("  {} = {}", entry.getKey(), entry.getValue());
            }
        }
        
        // Build and publish context data to MQTT
        try {
            // Create a comprehensive JSON payload with all context data
            com.fasterxml.jackson.databind.node.ObjectNode contextData = objectMapper.createObjectNode();
            
            // Observation details
            com.fasterxml.jackson.databind.node.ObjectNode obsNode = objectMapper.createObjectNode();
            obsNode.put("id", observation.getId().toString());
            obsNode.put("registrationId", observation.getRegistrationId());
            obsNode.put("resourcePath", resourcePath);
            if (!observation.getContext().isEmpty()) {
                com.fasterxml.jackson.databind.node.ObjectNode contextNode = objectMapper.createObjectNode();
                for (Map.Entry<String, String> entry : observation.getContext().entrySet()) {
                    contextNode.put(entry.getKey(), entry.getValue());
                }
                obsNode.set("context", contextNode);
            }
            if (!observation.getProtocolData().isEmpty()) {
                com.fasterxml.jackson.databind.node.ObjectNode protoNode = objectMapper.createObjectNode();
                for (Map.Entry<String, String> entry : observation.getProtocolData().entrySet()) {
                    protoNode.put(entry.getKey(), entry.getValue());
                }
                obsNode.set("protocolData", protoNode);
            }
            contextData.set("observation", obsNode);
            
            // Registration details
            com.fasterxml.jackson.databind.node.ObjectNode regNode = objectMapper.createObjectNode();
            regNode.put("id", registration.getId());
            regNode.put("endpoint", registration.getEndpoint());
            regNode.put("lifetime", registration.getLifeTimeInSec());
            regNode.put("binding", registration.getBindingMode() != null ? registration.getBindingMode().toString() : "null");
            regNode.put("lastUpdate", registration.getLastUpdate() != null ? registration.getLastUpdate().toString() : "null");
            contextData.set("registration", regNode);
            
            // Supported objects
            com.fasterxml.jackson.databind.node.ArrayNode supportedObjs = objectMapper.createArrayNode();
            for (Map.Entry<Integer, Version> entry : supportedObjects.entrySet()) {
                com.fasterxml.jackson.databind.node.ObjectNode objNode = objectMapper.createObjectNode();
                objNode.put("id", entry.getKey());
                objNode.put("version", entry.getValue() != null ? entry.getValue().toString() : "null");
                supportedObjs.add(objNode);
            }
            contextData.set("supportedObjects", supportedObjs);
            
            // Additional attributes
            if (!additionalAttrs.isEmpty()) {
                com.fasterxml.jackson.databind.node.ObjectNode attrsNode = objectMapper.createObjectNode();
                for (Map.Entry<String, String> entry : additionalAttrs.entrySet()) {
                    attrsNode.put(entry.getKey(), entry.getValue());
                }
                contextData.set("attributes", attrsNode);
            }
            
            // Publish to MQTT
            String topic = TOPIC + "/obs/" + endpoint;
            String payload = objectMapper.writeValueAsString(contextData);
            broker.publish(topic, payload);
            LOG.info("Published observation context to MQTT topic: {}", topic);
            LOG.debug("Payload: {}", payload);
            
        } catch (JsonProcessingException e) {
            LOG.warn("Failed to serialize observation context for endpoint: {}", endpoint, e);
        } catch (Exception e) {
            LOG.warn("Failed to publish observation context for endpoint: {}", endpoint, e);
        }
        
        LOG.info("=== newObservation() completed ===");
    }

    @Override
    public void cancelled(Observation observation) {
        LOG.info("=== cancelled() called ===");
        LOG.debug("observation: {}", observation);
        LOG.info("Observation cancelled: id={}", observation.getId());
        LOG.info("=== cancelled() completed ===");
    }

    @Override
    public void onResponse(org.eclipse.leshan.core.observation.SingleObservation observation, Registration registration, org.eclipse.leshan.core.response.ObserveResponse response) {
        String endpoint = registration.getEndpoint();
        LOG.info("=== onResponse() called for endpoint: {} ===", endpoint);
        LOG.info("Observation ID: {}", observation.getId());
        LOG.info("Response code: {}", response.getCode());
        LOG.info("Response success: {}", response.isSuccess());
        
        if (response.isSuccess()) {
            // Extract the LwM2mNode content
            org.eclipse.leshan.core.node.LwM2mNode content = response.getContent();
            if (content != null) {
                LOG.info("Content type: {}", content.getClass().getSimpleName());
                
                // Build JSON payload with observation and value data
                try {
                    com.fasterxml.jackson.databind.node.ObjectNode payload = objectMapper.createObjectNode();
                    
                    // Observation details
                    com.fasterxml.jackson.databind.node.ObjectNode obsNode = objectMapper.createObjectNode();
                    obsNode.put("id", observation.getId().toString());
                    obsNode.put("path", observation.getPath() != null ? observation.getPath().toString() : "null");
                    payload.set("observation", obsNode);
                    
                    // Value data - cast to LwM2mResource to get the value
                    com.fasterxml.jackson.databind.node.ObjectNode valueNode = objectMapper.createObjectNode();
                    valueNode.put("path", observation.getPath() != null ? observation.getPath().toString() : "null");
                    valueNode.put("type", content.getClass().getSimpleName());
                    
                    if (content instanceof org.eclipse.leshan.core.node.LwM2mResource) {
                        org.eclipse.leshan.core.node.LwM2mResource resource = (org.eclipse.leshan.core.node.LwM2mResource) content;
                        Object value = resource.getValue();
                        valueNode.put("value", value != null ? value.toString() : "null");
                    } else {
                        valueNode.put("value", "null");
                    }
                    payload.set("value", valueNode);
                    
                    // Publish to MQTT
                    String topic = TOPIC + "/resp/" + endpoint;
                    String jsonPayload = objectMapper.writeValueAsString(payload);
                    broker.publish(topic, jsonPayload);
                    LOG.info("Published response value to MQTT topic: {}", topic);
                    LOG.debug("Payload: {}", jsonPayload);
                    
                } catch (JsonProcessingException e) {
                    LOG.warn("Failed to serialize response value for endpoint: {}", endpoint, e);
                }
            } else {
                LOG.warn("Response content is null for endpoint: {}", endpoint);
            }
        } else {
            LOG.warn("Response failed for endpoint: {} - Code: {}, Error: {}", 
                endpoint, response.getCode(), response.getErrorMessage());
        }
        
        LOG.info("=== onResponse() completed ===");
    }

    @Override
    public void onResponse(org.eclipse.leshan.core.observation.CompositeObservation observation, Registration registration, org.eclipse.leshan.core.response.ObserveCompositeResponse response) {
        String endpoint = registration.getEndpoint();
        LOG.info("=== onResponse() called for composite observation, endpoint: {} ===", endpoint);
        LOG.info("Observation ID: {}", observation.getId());
        LOG.info("Response code: {}", response.getCode());
        LOG.info("Response success: {}", response.isSuccess());
        
        if (response.isSuccess()) {
            // Extract the Map<LwM2mPath, LwM2mNode> content
            java.util.Map<org.eclipse.leshan.core.node.LwM2mPath, org.eclipse.leshan.core.node.LwM2mNode> content = response.getContent();
            if (content != null && !content.isEmpty()) {
                LOG.info("Composite content size: {}", content.size());
                
                // Build JSON payload with observation and all values
                try {
                    com.fasterxml.jackson.databind.node.ObjectNode payload = objectMapper.createObjectNode();
                    
                    // Observation details
                    com.fasterxml.jackson.databind.node.ObjectNode obsNode = objectMapper.createObjectNode();
                    obsNode.put("id", observation.getId().toString());
                    payload.set("observation", obsNode);
                    
                    // All value data
                    com.fasterxml.jackson.databind.node.ObjectNode valuesNode = objectMapper.createObjectNode();
                    for (java.util.Map.Entry<org.eclipse.leshan.core.node.LwM2mPath, org.eclipse.leshan.core.node.LwM2mNode> entry : content.entrySet()) {
                        org.eclipse.leshan.core.node.LwM2mPath path = entry.getKey();
                        org.eclipse.leshan.core.node.LwM2mNode node = entry.getValue();
                        
                        com.fasterxml.jackson.databind.node.ObjectNode valueNode = objectMapper.createObjectNode();
                        valueNode.put("path", path.toString());
                        valueNode.put("type", node.getClass().getSimpleName());
                        
                        if (node instanceof org.eclipse.leshan.core.node.LwM2mResource) {
                            org.eclipse.leshan.core.node.LwM2mResource resource = (org.eclipse.leshan.core.node.LwM2mResource) node;
                            Object value = resource.getValue();
                            valueNode.put("value", value != null ? value.toString() : "null");
                        } else {
                            valueNode.put("value", "null");
                        }
                        valuesNode.set(path.toString(), valueNode);
                    }
                    payload.set("values", valuesNode);
                    
                    // Publish to MQTT
                    String topic = TOPIC + "/resp/" + endpoint;
                    String jsonPayload = objectMapper.writeValueAsString(payload);
                    broker.publish(topic, jsonPayload);
                    LOG.info("Published composite response values to MQTT topic: {}", topic);
                    LOG.debug("Payload: {}", jsonPayload);
                    
                } catch (JsonProcessingException e) {
                    LOG.warn("Failed to serialize composite response values for endpoint: {}", endpoint, e);
                }
            } else {
                LOG.warn("Composite response content is null or empty for endpoint: {}", endpoint);
            }
        } else {
            LOG.warn("Composite response failed for endpoint: {} - Code: {}, Error: {}", 
                endpoint, response.getCode(), response.getErrorMessage());
        }
        
        LOG.info("=== onResponse() completed ===");
    }

    @Override
    public void onError(Observation observation, Registration registration, Exception error) {
        LOG.warn("=== onError() called for endpoint: {} ===", registration.getEndpoint());
        LOG.debug("observation: {}", observation);
        LOG.warn("Error in observation: id={}, error={}", observation.getId(), error.getMessage());
        LOG.warn("=== onError() completed ===", error);
    }
}
