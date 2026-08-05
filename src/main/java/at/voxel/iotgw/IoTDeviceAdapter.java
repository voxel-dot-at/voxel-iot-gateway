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
import org.eclipse.leshan.server.registration.Registration;
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
public class IoTDeviceAdapter {

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
    public void handleRegistered(Registration registration, Registration previousReg,
            Collection<Observation> previousObservations) {
        reg.regState = RegState.registered;
        reg.regId = registration.getId();
        reg.action = DeviceAction.registering;
        this.registration = registration;

        try {
            broker.publish(TOPIC, objectMapper.writeValueAsString(reg));
        } catch (JsonProcessingException e) {
            LOG.warn("register() Failed to publish " + getId(), e);
        }

        Map<Integer, Version> m = registration.getSupportedObject();
        Set<Integer> keys = m.keySet();
        for (int i = 0; i < keys.size(); i++) {
            LOG.debug("reggy new device: " + registration.getId() + " ITER O " + keys.toArray()[i]);
        }
        Map<String, String> att = registration.getAdditionalRegistrationAttributes();
        Object[] atts = att.keySet().toArray();
        for (Object att1 : atts) {
            String key = (String) att1;
            LOG.debug("reggy new device: " + registration.getId() + " ITER A " + att1 + " " + att.get(key));
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
            fw = getFirmwareVersion();
            serNo = getSerialNo();
        } catch (InterruptedException ex) {
            LOG.warn("Failed to read getFirmwareVersion!", ex);
        }
        LOG.debug("fw version " + fw + " serial " + serNo);
        // register for updates
        ReadResponse response;
        try {
            response = server.send(registration, new ReadRequest(3, 0, 13));

            if (response.isSuccess()) {
                LOG.debug("Device time:" + ((LwM2mResource) response.getContent()).getValue());
            } else {
                LOG.warn("Failed to read:" + response.getCode() + " " + response.getErrorMessage());
            }
            ObserveResponse oresp = server.send(registration, new ObserveRequest(3, 0, 13));

            if (oresp.isSuccess()) {
                LOG.debug("Device time:" + ((LwM2mResource) response.getContent()).getValue());
            } else {
                LOG.warn("Failed to read:" + response.getCode() + " " + response.getErrorMessage());
            }
        } catch (InterruptedException e) {
            LOG.warn("register() Failed to get data for " + getId(), e);
        }
    }

    /**
     * synchronous read request to get the firmware version
     *
     * @param registration
     * @return
     * @throws InterruptedException
     */
    public String getSerialNo() throws InterruptedException {
        ReadResponse response = server.send(registration, new ReadRequest(3, 0, 2));

        if (response.isSuccess()) {
            LOG.debug("getSerialNo:" + ((LwM2mResource) response.getContent()).getValue());
            return (String) ((LwM2mResource) response.getContent()).getValue();
        } else {
            LOG.warn("getSerialNo Failed to read:" + response.getCode() + " " + response.getErrorMessage());
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
        ReadResponse response = server.send(registration, new ReadRequest(3, 0, 3));

        if (response.isSuccess()) {
            LOG.debug("getFirmwareVersion:" + ((LwM2mResource) response.getContent()).getValue());
            return (String) ((LwM2mResource) response.getContent()).getValue();
        } else {
            LOG.warn("getFirmwareVersion Failed to read:" + response.getCode() + " " + response.getErrorMessage());
            return null;
        }
    }

    public void handleUnregistered(Registration registration, Collection<Observation> observations, boolean expired,
            Registration newReg) {
        // TODO Auto-generated method stub
        reg.regState = RegState.unregistered;
        reg.regId = registration.getId();
        reg.action = DeviceAction.deregistering;

        try {
            broker.publish(TOPIC, objectMapper.writeValueAsString(reg));
        } catch (JsonProcessingException e) {
            LOG.warn("register() Failed to publish " + getId(), e);
        }
    }

    public void handleUpdatedReg(RegistrationUpdate update, Registration updatedReg, Registration previousReg) {
        reg.regId = updatedReg.getId();
        reg.action = DeviceAction.updated;
        try {
            broker.publish(TOPIC, objectMapper.writeValueAsString(reg));
        } catch (JsonProcessingException e) {
            LOG.warn("register() Failed to publish " + getId(), e);
        }
    }

    public void onAwake(Registration registration) {
        reg.regId = registration.getId();
        reg.regState = RegState.awake;
        reg.action = DeviceAction.internal;

        try {
            broker.publish(TOPIC, objectMapper.writeValueAsString(reg));
        } catch (JsonProcessingException e) {
            LOG.warn("register() Failed to publish " + getId(), e);
        }
    }

    public void onSleeping(Registration registration) {
        reg.regId = registration.getId();
        reg.regState = RegState.asleep;
        reg.action = DeviceAction.internal;

        try {
            broker.publish(TOPIC, objectMapper.writeValueAsString(reg));
        } catch (JsonProcessingException e) {
            LOG.warn("register() Failed to publish " + getId(), e);
        }
    }
}
