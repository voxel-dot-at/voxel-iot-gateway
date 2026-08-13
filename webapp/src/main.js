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

import { createApp } from "vue";
import App from "./App.vue";
import { registerPlugins } from "./plugins/index";

const app = createApp(App);

/**
 * directive to hide content without changing layout unlike v-show or v-if
 */
app.directive("visible", (el, binding) => {
  el.style.visibility = binding.value ? "visible" : "hidden";
});

registerPlugins(app);

app.mount("#app");
