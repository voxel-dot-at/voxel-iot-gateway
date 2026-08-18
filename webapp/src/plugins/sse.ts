/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/documents/edl-v10.html.
 *******************************************************************************/

/**
 * Vue SSE plugin wrapper
 * Uses vue-sse library for Server-Sent Events
 * Wraps the vue-sse API to match the legacy interface
 */

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const sseModule = require("vue-sse") as any;
const { SSEManager } = sseModule;

import type { App } from "vue";

interface SSEClient {
  on: (event: string, callback: (...args: any[]) => void) => void;
  off: (event: string, callback: (...args: any[]) => void) => void;
  connect: () => Promise<void>;
  disconnect: () => Promise<void>;
}

interface SSEResponse {
  on: (event: string, callback: (...args: any[]) => void) => SSEResponse;
  off: (event: string, callback: (...args: any[]) => void) => SSEResponse;
  connect: () => Promise<void>;
  disconnect: () => Promise<void>;
  close: () => Promise<void>;
}

interface SSEWrapper {
  create: (options?: Record<string, unknown>) => SSEResponse;
}

interface SSEPlugin {
  install: (app: App) => void;
}

function createSSEResponse(client: SSEClient): SSEResponse {
  const response: SSEResponse = {
    on: (event: string, callback: (...args: any[]) => void): SSEResponse => {
      client.on(event, callback);
      return response;
    },
    off: (event: string, callback: (...args: any[]) => void): SSEResponse => {
      client.off(event, callback);
      return response;
    },
    connect: (): Promise<void> => {
      return client.connect();
    },
    disconnect: (): Promise<void> => {
      return client.disconnect();
    },
    close: (): Promise<void> => {
      return client.disconnect();
    },
  };
  return response;
}

const ssePlugin: SSEPlugin = {
  install(app: App): void {
    // Create SSE manager instance
    const sseManager = new SSEManager();

    // Create a wrapper that matches the legacy API
    const sseWrapper: SSEWrapper = {
      create: (options: Record<string, unknown> = {}): SSEResponse => {
        // Create SSE client using vue-sse
        const client = sseManager.create(options);

        // Return a wrapper that matches the legacy interface
        return createSSEResponse(client);
      },
    };

    // Provide $sse global property
    app.config.globalProperties.$sse = sseWrapper;

    // Also provide it via provide/inject for composables
    app.provide("$sse", sseWrapper);
  },
};

export default ssePlugin;
