/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *    http://www.eclipse.org/documents/edl-v10.html.
 *******************************************************************************/

/**
 * Native SSE (Server-Sent Events) plugin for Vue 3
 * Provides $sse.create() method that returns an event emitter-like object
 */

class SSEClient {
  constructor(options) {
    this.url = options.url;
    this.format = options.format || 'json';
    this.eventSource = null;
    this.listeners = {};
    this._connected = false;
  }

  connect() {
    return new Promise((resolve, reject) => {
      if (this.eventSource) {
        this.eventSource.close();
      }

      this.eventSource = new EventSource(this.url);

      this.eventSource.onmessage = (event) => {
        let data;
        try {
          data = JSON.parse(event.data);
        } catch (e) {
          data = event.data;
        }

        // Handle different event types
        if (data.type) {
          this._emit(data.type, data);
        } else {
          // Fallback: emit as 'message'
          this._emit('message', data);
        }
      };

      this.eventSource.onerror = (error) => {
        this._emit('error', error);
        this._connected = false;
        reject(error);
      };

      this.eventSource.onopen = () => {
        this._connected = true;
        this._emit('open', null);
        resolve(this);
      };
    });
  }

  on(event, callback) {
    if (!this.listeners[event]) {
      this.listeners[event] = [];
    }
    this.listeners[event].push(callback);
    return this;
  }

  off(event, callback) {
    if (this.listeners[event]) {
      this.listeners[event] = this.listeners[event].filter(cb => cb !== callback);
    }
    return this;
  }

  _emit(event, data) {
    if (this.listeners[event]) {
      this.listeners[event].forEach(callback => callback(data));
    }
  }

  close() {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
    return this;
  }

  // Alias for close() for backward compatibility
  disconnect() {
    return this.close();
  }

  get connected() {
    return this._connected;
  }
}

export default {
  install(app) {
    app.config.globalProperties.$sse = {
      create: (options = {}) => {
        const client = new SSEClient(options);
        client.connect();
        return client;
      }
    };
  }
};
