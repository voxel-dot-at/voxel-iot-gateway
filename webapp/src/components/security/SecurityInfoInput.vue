<!-----------------------------------------------------------------------------
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
  ----------------------------------------------------------------------------->
<template>
  <div>
    <!-- (D)TLS security info -->
    <v-switch
      v-model="useDTLS"
      @update:model-value="useDTLSChanged"
      label="Using (D)TLS"
      class="mb-2"
    />

    <v-expansion-panels v-if="useDTLS" class="mb-4">
      <v-expansion-panel>
        <v-expansion-panel-title>
          TLS Configuration
        </v-expansion-panel-title>
        <v-expansion-panel-text>
          <v-select
            v-model="tlsMode"
            :items="tlsModes"
            label="TLS Mode"
            @update:model-value="updateTlsMode"
          />

          <!-- PSK Mode -->
          <div v-if="tlsMode === 'psk'">
            <v-text-field
              v-model="tls.identity"
              label="Identity"
              class="mt-2"
            />
            <v-text-field
              v-model="tls.key"
              label="Pre-shared Key"
              class="mt-2"
            />
          </div>

          <!-- RPK Mode -->
          <div v-if="tlsMode === 'rpK'">
            <v-text-field
              v-model="tls.key"
              label="Public Key"
              class="mt-2"
            />
          </div>

          <!-- X509 Mode -->
          <div v-if="tlsMode === 'x509'">
            <v-text-field
              v-model="tls.identity"
              label="Certificate CN"
              class="mt-2"
            />
          </div>
        </v-expansion-panel-text>
      </v-expansion-panel>
    </v-expansion-panels>

    <!-- OSCORE security info -->
    <v-switch
      v-model="useOSCORE"
      @update:model-value="useOSCOREChanged"
      label="Using OSCORE (Experimental - for now can not be used with DTLS)"
      class="mb-2"
    />

    <v-expansion-panels v-if="useOSCORE">
      <v-expansion-panel>
        <v-expansion-panel-title>
          OSCORE Configuration
        </v-expansion-panel-title>
        <v-expansion-panel-text>
          <v-text-field
            v-model="oscore.sid"
            label="Sender ID (SID)"
            class="mt-2"
          />
          <v-text-field
            v-model="oscore.msec"
            label="Master Secret (MSEC)"
            class="mt-2"
          />
          <v-text-field
            v-model="oscore.rid"
            label="Recipient ID (RID)"
            class="mt-2"
          />
        </v-expansion-panel-text>
      </v-expansion-panel>
    </v-expansion-panels>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue';

const props = defineProps({
  tls: {
    type: Object,
    default: () => ({ mode: 'psk', details: {} })
  },
  oscore: {
    type: Object,
    default: () => ({})
  }
});

const emit = defineEmits(['update:tls', 'update:oscore']);

const tlsModes = ['psk', 'rpK', 'x509'];

// Internal state
const useDTLS = ref(true);
const useOSCORE = ref(false);
const tlsMode = ref('psk');
const tls = ref({ mode: 'psk', details: {} });
const oscore = ref({});

/**
 * Initialize OSCORE state from props
 */
function initOscore(oscoreValue) {
  if (oscoreValue) {
    useOSCORE.value = true;
    oscore.value = JSON.parse(JSON.stringify(oscoreValue));
  } else {
    useOSCORE.value = false;
    oscore.value = undefined;
  }
}

/**
 * Initialize TLS state from props
 */
function initTls(tlsValue) {
  if (tlsValue) {
    useDTLS.value = true;
    tls.value = JSON.parse(JSON.stringify(tlsValue));
    tlsMode.value = tlsValue.mode || 'psk';
  } else {
    useDTLS.value = false;
    tls.value = undefined;
  }
}

/**
 * Handle DTLS toggle change
 */
function useDTLSChanged(value) {
  if (value) useOSCORE.value = !value; // temporary code while we don't support OSCORE over DTLS
  exclusifTlsOrOSCORE();
}

/**
 * Handle OSCORE toggle change
 */
function useOSCOREChanged(value) {
  if (value) useDTLS.value = !value; // temporary code while we don't support OSCORE over DTLS
  exclusifTlsOrOSCORE();
}

/**
 * Ensure exclusive TLS or OSCORE selection
 */
function exclusifTlsOrOSCORE() {
  if (useDTLS.value) {
    emit('update:tls', {
      mode: 'psk',
      details: {},
    });
    emit('update:oscore', undefined);
  } else if (useOSCORE.value) {
    emit('update:tls', undefined);
    emit('update:oscore', {});
  } else {
    emit('update:tls', undefined);
    emit('update:oscore', undefined);
  }
}

/**
 * Update TLS mode
 */
function updateTlsMode(value) {
  tlsMode.value = value;
  tls.value.mode = value;
  emit('update:tls', { ...tls.value, mode: value });
}

// Initialize on mount
initTls(props.tls);
initOscore(props.oscore);

// Watch for external changes
watch(() => props.tls, (newVal) => {
  initTls(newVal);
}, { deep: true });

watch(() => props.oscore, (newVal) => {
  initOscore(newVal);
}, { deep: true });

// Watch for internal changes
watch(tls, (newVal) => {
  emit('update:tls', newVal);
}, { deep: true });

watch(oscore, (newVal) => {
  emit('update:oscore', newVal);
}, { deep: true });
</script>

<style scoped>
.mb-2 {
  margin-bottom: 8px;
}
.mb-4 {
  margin-bottom: 16px;
}
.mt-2 {
  margin-top: 8px;
}
</style>