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

    RegInfo reg;
    private final LeshanServer server;

    private final MqttAdapter broker;

    private Registration registration;

    public IoTDeviceAdapter(LeshanServer server, MqttAdapter broker) {
        this.server = server;
        this.broker = broker;
        this.reg = new RegInfo();
    }

    public void setId(String id) {
        reg.id = id;
    }

    public String getId() {
        return reg.id;
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
        LOG.info("=== registered() called for endpoint: {} ===", registration.getEndpoint());
        LOG.debug("previousReg: {}", previousReg != null ? previousReg.getEndpoint() : "null");
        LOG.debug("previousObservations: {}", previousObservations != null ? previousObservations.size() : "null");

        reg.regState = RegState.registered;
        reg.regId = registration.getId();
        reg.action = DeviceAction.registering;
        this.registration = registration;

        LOG.info("Device registered: id={}, endpoint={}", registration.getId(), registration.getEndpoint());

        try {
            broker.publish(TOPIC, objectMapper.writeValueAsString(reg));
            LOG.debug("Published registration state to MQTT topic: {}", TOPIC);
        } catch (JsonProcessingException e) {
            LOG.warn("register() Failed to publish " + getId(), e);
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
            fw = getFirmwareVersion();
            LOG.debug("Fetching serial number...");
            serNo = getSerialNo();
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
            LOG.warn("register() Failed to get data for " + getId(), e);
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
    public String getSerialNo() throws InterruptedException {
        LOG.debug("=== getSerialNo() called ===");
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
    public String getFirmwareVersion() throws InterruptedException {
        LOG.debug("=== getFirmwareVersion() called ===");
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
        LOG.info("=== unregistered() called for endpoint: {} ===", registration.getEndpoint());
        LOG.debug("observations: {}", observations != null ? observations.size() : "null");
        LOG.debug("expired: {}", expired);
        LOG.debug("newReg: {}", newReg != null ? newReg.getEndpoint() : "null");

        reg.regState = RegState.unregistered;
        reg.regId = registration.getId();
        reg.action = DeviceAction.deregistering;

        LOG.info("Device unregistered: id={}, endpoint={}, expired={}", registration.getId(), registration.getEndpoint(), expired);

        try {
            broker.publish(TOPIC, objectMapper.writeValueAsString(reg));
            LOG.debug("Published unregistration state to MQTT topic: {}", TOPIC);
        } catch (JsonProcessingException e) {
            LOG.warn("unregistered() Failed to publish " + getId(), e);
        }
        LOG.info("=== unregistered() completed ===");
    }

    @Override
    public void updated(RegistrationUpdate update, Registration updatedReg, Registration previousReg) {
        LOG.info("=== updated() called for endpoint: {} ===", updatedReg.getEndpoint());
        LOG.debug("update: {}", update);
        LOG.debug("previousReg: {}", previousReg != null ? previousReg.getEndpoint() : "null");

        reg.regId = updatedReg.getId();
        reg.action = DeviceAction.updated;

        LOG.info("Device updated: id={}, endpoint={}", updatedReg.getId(), updatedReg.getEndpoint());

        try {
            broker.publish(TOPIC, objectMapper.writeValueAsString(reg));
            LOG.debug("Published update state to MQTT topic: {}", TOPIC);
        } catch (JsonProcessingException e) {
            LOG.warn("updated() Failed to publish " + getId(), e);
        }
        LOG.info("=== updated() completed ===");
    }

    public void onAwake(Registration registration) {
        LOG.info("=== onAwake() called for endpoint: {} ===", registration.getEndpoint());
        reg.regId = registration.getId();
        reg.regState = RegState.awake;
        reg.action = DeviceAction.internal;

        LOG.info("Device awake: id={}, endpoint={}", registration.getId(), registration.getEndpoint());

        try {
            broker.publish(TOPIC, objectMapper.writeValueAsString(reg));
            LOG.debug("Published awake state to MQTT topic: {}", TOPIC);
        } catch (JsonProcessingException e) {
            LOG.warn("onAwake() Failed to publish " + getId(), e);
        }
        LOG.info("=== onAwake() completed ===");
    }

    public void onSleeping(Registration registration) {
        LOG.info("=== onSleeping() called for endpoint: {} ===", registration.getEndpoint());
        reg.regId = registration.getId();
        reg.regState = RegState.asleep;
        reg.action = DeviceAction.internal;

        LOG.info("Device sleeping: id={}, endpoint={}", registration.getId(), registration.getEndpoint());

        try {
            broker.publish(TOPIC, objectMapper.writeValueAsString(reg));
            LOG.debug("Published sleeping state to MQTT topic: {}", TOPIC);
        } catch (JsonProcessingException e) {
            LOG.warn("onSleeping() Failed to publish " + getId(), e);
        }
        LOG.info("=== onSleeping() completed ===");
    }

    @Override
    public void newObservation(Observation observation, Registration registration) {
        LOG.info("=== newObservation() called for endpoint: {} ===", registration.getEndpoint());
        LOG.debug("observation: {}", observation);
        LOG.info("New observation: id={}, endpoint={}", observation.getId(), registration.getEndpoint());
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
        LOG.info("=== onResponse() called for endpoint: {} ===", registration.getEndpoint());
        LOG.debug("observation: {}", observation);
        LOG.debug("response: {}", response);
        LOG.info("Response received: id={}, success={}", observation.getId(), response.isSuccess());
        LOG.info("=== onResponse() completed ===");
    }

    @Override
    public void onResponse(org.eclipse.leshan.core.observation.CompositeObservation observation, Registration registration, org.eclipse.leshan.core.response.ObserveCompositeResponse response) {
        LOG.info("=== onResponse() called for composite observation, endpoint: {} ===", registration.getEndpoint());
        LOG.debug("observation: {}", observation);
        LOG.debug("response: {}", response);
        LOG.info("Composite response received: success={}", response.isSuccess());
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
