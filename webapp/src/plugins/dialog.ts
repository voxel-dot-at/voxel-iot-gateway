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

import { ref, h, type Ref, type Component, type App } from "vue";
import { VDialog, VCard, VCardText, VCardActions, VBtn, VSpacer } from "vuetify/components";

interface DialogState {
  visible: Ref<boolean>;
  component: Ref<Component | null>;
  props: Ref<Record<string, unknown>>;
  resolve: Ref<((value: unknown) => void) | null>;
}

interface DialogApi {
  confirm: (message: string, title?: string) => Promise<boolean>;
  alert: (message: string, title?: string) => Promise<void>;
  prompt: (options?: Record<string, unknown>) => Promise<unknown>;
  hide: () => void;
  _state: DialogState;
}

const dialogState: DialogState = {
  visible: ref(false),
  component: ref(null),
  props: ref({}),
  resolve: ref(null),
};

function showDialog(component: Component, props: Record<string, unknown> = {}): Promise<unknown> {
  return new Promise((resolve) => {
    dialogState.resolve.value = resolve;
    dialogState.component.value = component;
    dialogState.props.value = props;
    dialogState.visible.value = true;
  });
}

function hideDialog(result: unknown = null): void {
  dialogState.visible.value = false;
  if (dialogState.resolve.value) {
    dialogState.resolve.value(result);
    dialogState.resolve.value = null;
  }
}

export default {
  install(app: App): void {
    // Create the dialog API object first
    const dialogApi: DialogApi = {
      confirm: (message: string, title: string = "Confirm"): Promise<boolean> => {
        const ConfirmDialog: Component = {
          props: ["message", "title"],
          emits: ["close"],
          setup(props: Record<string, unknown>, { emit }: { emit: (event: string, ...args: unknown[]) => void }) {
            return () =>
              h(
                VCard,
                [
                  h(VCardText, props.message as string),
                  h(
                    VCardActions,
                    [
                      h(VSpacer),
                      h(
                        VBtn,
                        {
                          variant: "text",
                          color: "primary",
                          onClick: () => emit("close", true),
                        },
                        "OK"
                      ),
                      h(
                        VBtn,
                        {
                          variant: "text",
                          onClick: () => emit("close", false),
                        },
                        "Cancel"
                      ),
                    ]
                  ),
                ]
              );
          },
        };
        return showDialog(ConfirmDialog, { message, title }) as Promise<boolean>;
      },
      alert: (message: string, title: string = "Alert"): Promise<void> => {
        const AlertDialog: Component = {
          props: ["message", "title"],
          emits: ["close"],
          setup(props: Record<string, unknown>, { emit }: { emit: (event: string, ...args: unknown[]) => void }) {
            return () =>
              h(
                VCard,
                [
                  h(VCardText, props.message as string),
                  h(
                    VCardActions,
                    [
                      h(VSpacer),
                      h(
                        VBtn,
                        {
                          variant: "text",
                          color: "primary",
                          onClick: () => emit("close", true),
                        },
                        "OK"
                      ),
                    ]
                  ),
                ]
              );
          },
        };
        return showDialog(AlertDialog, { message, title }) as Promise<void>;
      },
      prompt: (options: Record<string, unknown> = {}): Promise<unknown> => {
        // Default implementation - will be extended by dialog-prompt.js
        return Promise.resolve(null);
      },
      hide: (): void => hideDialog(null),
      _state: dialogState,
    };

    // Store state for extension by dialog-prompt.js
    (dialogApi as DialogApi & { _state: DialogState })._state = dialogState;

    // Provide dialog methods on globalProperties
    app.config.globalProperties.$dialog = dialogApi;

    // Register the dialog wrapper component globally
    const DialogWrapper: Component = {
      setup() {
        return () => {
          if (!dialogState.visible.value || !dialogState.component.value) return null;
          return h(
            VDialog,
            { modelValue: true, "onUpdate:modelValue": () => {} },
            [
              h(dialogState.component.value, {
                ...dialogState.props.value,
                onClose: (result: unknown) => hideDialog(result),
              }),
            ]
          );
        };
      },
    };

    app.component("DialogWrapper", DialogWrapper);
  },
};
