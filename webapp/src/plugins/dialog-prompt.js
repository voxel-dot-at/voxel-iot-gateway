/*******************************************************************************
 * Copyright (c) 2025 Sierra Wireless and others.
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
import PromptDialog from "@/components/PromptDialog.vue";

/**
 * Extension plugin to add prompt method to dialog
 */
export default {
  install(app) {
    // Extend $dialog with prompt method
    app.config.globalProperties.$dialog.prompt = function (options = {}) {
      const dialogState = this._state;
      
      return new Promise((resolve) => {
        dialogState.component.value = PromptDialog;
        dialogState.props.value = {
          title: options.title || 'Prompt',
          text: options.text || 'Enter value',
          placeholderText: options.placeholderText || '',
          cancelText: options.cancelText || 'Cancel',
          confirmationText: options.confirmationText || 'OK',
          onClose: (value) => {
            dialogState.visible.value = false;
            resolve(value);
            dialogState.component.value = null;
            dialogState.props.value = {};
          }
        };
        dialogState.visible.value = true;
      });
    };
  }
};
