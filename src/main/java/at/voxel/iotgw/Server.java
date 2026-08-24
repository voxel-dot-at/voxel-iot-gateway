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
 *    http://www.eclipse.org/documents/edl-v10.html.
 *
 * Contributors:
 *     Simon Vogl - initial implementation
 *******************************************************************************/
package at.voxel.iotgw;

/**
 * Abstract base class for server components.
 */
public abstract class Server {
    protected boolean running = false;

    /**
     * Starts the server.
     * @throws Exception if an error occurs during startup
     */
    public abstract void start() throws Exception;

    /**
     * Stops the server.
     * @throws Exception if an error occurs during shutdown
     */
    public abstract void stop() throws Exception;

    /**
     * Checks if the server is currently running.
     * @return true if running, false otherwise
     */
    public boolean isRunning() {
        return running;
    }
}
