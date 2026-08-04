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

/**
 * Listener interface for MQTT messages
 * 
 * @author simon
 */
public interface MqttMessageListener {
    
    /**
     * Called when a message is received
     * @param topic The topic the message arrived on
     * @param payload The message payload as a string
     */
    void onMessage(String topic, String payload);
}
