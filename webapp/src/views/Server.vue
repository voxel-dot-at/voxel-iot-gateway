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
<template>
  <v-container>
    <v-card>
      <v-card-title>Server Configuration</v-card-title>
      <v-card-text>
        <v-form>
          <v-text-field
            v-model="pubkeyFileName"
            label="Public Key File Name"
            disabled
          ></v-text-field>
          <v-text-field
            v-model="certFileName"
            label="Certificate File Name"
            disabled
          ></v-text-field>
        </v-form>
      </v-card-text>
    </v-card>
  </v-container>
</template>

<script>
export default {
  props: {
    pubkeyFileName: {
      type: String,
      default: "serverPubKey.der",
    },
    certFileName: {
      type: String,
      default: "serverCertificate.der",
    },
  },
};
</script>
