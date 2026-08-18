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
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 *******************************************************************************/
import { type App } from "vue";
import { useLocalStorage } from "@vueuse/core";

interface Preferences {
  timeout: number;
  singleFormat: string;
  multiFormat: string;
  compositePathFormat: string;
  compositeNodeFormat: string;
  compositeObjects: Array<{ name: string; paths: string[] }>;
}

const _preferences: Preferences = {
  timeout: useLocalStorage("timeout", 5).value,
  singleFormat: useLocalStorage("singleFormat", "TEXT").value,
  multiFormat: useLocalStorage("multiFormat", "TLV").value,
  compositePathFormat: useLocalStorage("compositePathFormat", "SENML_CBOR").value,
  compositeNodeFormat: useLocalStorage("CompositeNodeFormat", "SENML_CBOR").value,
  compositeObjects: useLocalStorage("compositeObjects", [
    { name: "myCompositeObject", paths: ["/3/0/1", "/3/0/2"] },
  ]).value,
};

export default {
  install: (app: App): void => {
    app.config.globalProperties.$pref = _preferences;
  },
};
