<!-----------------------------------------------------------------------------
 * Copyright (c) 2021 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License v1.0 which accompany this distribution.
  ----------------------------------------------------------------------------->
<template>
  <v-dialog v-model="show" overlay="false" fullscreen transition="dialog-bottom-transition">
    <v-card>
      <v-card-title class="headline grey-lighten-2">
        Security Information
      </v-card-title>
      <v-card-text>
        <p>Security information dialog - Not implemented</p>
      </v-card-text>
      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn text @click="show = false">
          Cancel
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script>
export default {
  props: {
    value: Boolean,
    initialValue: Object,
  },
  computed: {
    show: {
      get() {
        return this.value;
      },
      set(value) {
        this.$emit("input", value);
      },
    },
  },
};
</script>
