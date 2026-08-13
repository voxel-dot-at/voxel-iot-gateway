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

import { ref, h } from 'vue';
import { VDialog, VCard, VCardText, VCardActions, VBtn, VSpacer } from 'vuetify/components';

/**
 * Simple dialog plugin for Vue 3 / Vuetify 3
 * Provides $dialog.confirm(), $dialog.alert(), and $dialog.prompt() methods
 */
const dialogState = {
  visible: ref(false),
  component: ref(null),
  props: ref({}),
  resolve: ref(null)
};

function showDialog(component, props = {}) {
  return new Promise((resolve) => {
    dialogState.resolve.value = resolve;
    dialogState.component.value = component;
    dialogState.props.value = props;
    dialogState.visible.value = true;
  });
}

function hideDialog(result) {
  dialogState.visible.value = false;
  if (dialogState.resolve.value) {
    dialogState.resolve.value(result);
    dialogState.resolve.value = null;
  }
}

export default {
  install(app) {
    // Store state for extension by dialog-prompt.js
    app.config.globalProperties.$dialog._state = dialogState;
    
    // Provide dialog methods on globalProperties
    app.config.globalProperties.$dialog = {
      confirm: (message, title = 'Confirm') => {
        const ConfirmDialog = {
          props: ['message', 'title'],
          emits: ['close'],
          setup(props, { emit }) {
            return () => h(VCard, [
              h(VCardText, props.message),
              h(VCardActions, [
                h(VSpacer),
                h(VBtn, {
                  variant: 'text',
                  color: 'primary',
                  onClick: () => emit('close', true)
                }, 'OK'),
                h(VBtn, {
                  variant: 'text',
                  onClick: () => emit('close', false)
                }, 'Cancel')
              ])
            ]);
          }
        };
        return showDialog(ConfirmDialog, { message, title });
      },
      alert: (message, title = 'Alert') => {
        const AlertDialog = {
          props: ['message', 'title'],
          emits: ['close'],
          setup(props, { emit }) {
            return () => h(VCard, [
              h(VCardText, props.message),
              h(VCardActions, [
                h(VSpacer),
                h(VBtn, {
                  variant: 'text',
                  color: 'primary',
                  onClick: () => emit('close', true)
                }, 'OK')
              ])
            ]);
          }
        };
        return showDialog(AlertDialog, { message, title });
      },
      prompt: (options = {}) => {
        // Default implementation - will be extended by dialog-prompt.js
        return Promise.resolve(null);
      },
      hide: () => hideDialog(null)
    };

    // Register the dialog wrapper component globally
    const DialogWrapper = {
      setup() {
        return () => {
          if (!dialogState.visible.value || !dialogState.component.value) return null;
          return h(VDialog,
            { modelValue: true, 'onUpdate:modelValue': () => {} },
            [
              h(dialogState.component.value, {
                ...dialogState.props.value,
                onClose: (result) => hideDialog(result)
              })
            ]
          );
        };
      }
    };
    
    app.component('DialogWrapper', DialogWrapper);
  }
};
