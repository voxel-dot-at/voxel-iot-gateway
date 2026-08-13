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


// Plugins
import axiosPlugin from "@/plugins/axios";
import iconsPlugin from "@/plugins/icons";
import storePlugin from "@/plugins/store";
import preferencePlugin from "@/plugins/preference";
import vuetify from "@/plugins/vuetify";
import dialog from "@/plugins/dialog";
import dialogPrompt from "@/plugins/dialog-prompt";
import ssePlugin from "@/plugins/sse";
import router from "@/router";

export function registerPlugins(app) {
    app
        .use(iconsPlugin)
        .use(storePlugin)
        .use(preferencePlugin)
        .use(vuetify)
        .use(dialog)
        .use(dialogPrompt) // Must be after dialog to extend it
        .use(ssePlugin)
        .use(axiosPlugin)
        .use(router)
}