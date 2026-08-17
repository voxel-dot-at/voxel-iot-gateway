/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License v1.0 which accompany this distribution.
 *    http://www.eclipse.org/documents/edl-v10.html.
 *******************************************************************************/

/**
 * Vue SSE plugin wrapper
 * Uses vue-sse library for Server-Sent Events
 * Wraps the vue-sse API to match the legacy interface
 */

import { SSEManager } from 'vue-sse';

export default {
  install(app) {
    // Create SSE manager instance
    const sseManager = new SSEManager();
    
    // Create a wrapper that matches the legacy API
    const sseWrapper = {
      create: (options = {}) => {
        // Create SSE client using vue-sse
        const client = sseManager.create(options);
        
        // Return a wrapper that matches the legacy interface
        return {
          on: (event, callback) => {
            client.on(event, callback);
            return this;
          },
          off: (event, callback) => {
            client.off(event, callback);
            return this;
          },
          connect: () => {
            return client.connect();
          },
          disconnect: () => {
            return client.disconnect();
          },
          close: () => {
            return client.disconnect();
          }
        };
      }
    };

    // Provide $sse global property
    app.config.globalProperties.$sse = sseWrapper;
    
    // Also provide it via provide/inject for composables
    app.provide('$sse', sseWrapper);
  }
};
