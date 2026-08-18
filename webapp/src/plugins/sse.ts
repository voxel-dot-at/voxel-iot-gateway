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

import { SSEManager } from "vue-sse";
import type { App } from "vue";

interface SSEClient {
  on: (event: string, callback: (...args: any[]) => void) => void;
  off: (event: string, callback: (...args: any[]) => void) => void;
  connect: () => Promise<void>;
  disconnect: () => Promise<void>;
  close: () => Promise<void>;
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

const ssePlugin: SSEPlugin = {
  install(app: App): void {
    const sseManager = new SSEManager();
    const sseWrapper: SSEWrapper = {
      create: (options: Record<string, unknown> = {}): SSEResponse => {
        const client = sseManager.create(options);
        const response: SSEResponse = {
          on: (event: string, callback: (...args: any[]) => void): SSEResponse => {
            client.on(event, callback);
            return response;
          },
          off: (event: string, callback: (...args: any[]) => void): SSEResponse => {
            client.off(event, callback);
            return response;
          },
          connect: (): Promise<void> => client.connect(),
          disconnect: (): Promise<void> => client.disconnect(),
          close: (): Promise<void> => client.disconnect(),
        };
        return response;
      },
    };
    app.config.globalProperties.$sse = sseWrapper;
    app.provide("$sse", sseWrapper);
  },
};

export default ssePlugin;
