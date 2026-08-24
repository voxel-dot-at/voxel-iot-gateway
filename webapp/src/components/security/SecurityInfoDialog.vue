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
  <v-dialog
    v-model="show"
    :scrim="false"
    fullscreen
    transition="dialog-bottom-transition"
  >
    <v-card>
      <!-- Title -->
      <v-card-title class="text-h5 bg-grey-lighten-2">
        <span>{{
          editMode ? "Edit Security Information" : "Add Security Information"
        }}</span>
      </v-card-title>

      <!-- Form -->
      <v-card-text>
        <v-form ref="form" v-model="valid">
          <v-text-field
            v-model="securityInfo.endpoint"
            :rules="[(v) => !!v || 'Endpoint is required']"
            label="Endpoint"
            required
            :autofocus="!editMode"
            :disabled="editMode"
          ></v-text-field>
          <security-info-input
            :tls="securityInfo.tls"
            :oscore="securityInfo.oscore"
            @update:tls="securityInfo.tls = $event"
            @update:oscore="securityInfo.oscore = $event"
          />
        </v-form>
      </v-card-text>
      <!-- Buttons -->
      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn
          variant="text"
          @click="handleSave"
          :disabled="!isValid"
        >
          {{ editMode ? "Save" : "Add" }}
        </v-btn>
        <v-btn variant="text" @click="show = false"> Cancel </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>
<script setup>
import { ref, computed, watch } from 'vue';
import SecurityInfoInput from './SecurityInfoInput.vue';

const props = defineProps({
  modelValue: {
    type: Boolean,
    required: true
  },
  initialValue: {
    type: Object,
    default: () => ({})
  }
});

const emit = defineEmits(['update:modelValue', 'new', 'edit']);

const form = ref(null);
const valid = ref(true);
const editMode = ref(false);
const securityInfo = ref({});

// Reset form when dialog opens
watch(() => props.modelValue, (newValue) => {
  if (newValue) {
    // Reset validation and set initial value when dialog opens
    form.value?.resetValidation();
    
    if (props.initialValue && Object.keys(props.initialValue).length > 0) {
      // Do a deep copy
      editMode.value = true;
      securityInfo.value = JSON.parse(JSON.stringify(props.initialValue));
    } else {
      // Default value for creation
      editMode.value = false;
      securityInfo.value = {
        endpoint: '',
        tls: { mode: 'psk', details: {} }
      };
    }
  }
});

// Computed show property for v-model binding
const show = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
});

// Computed isValid property
const isValid = computed(() => {
  return securityInfo.value && 
    (securityInfo.value.tls || securityInfo.value.oscore) && 
    valid.value;
});

// Handle save/add
const handleSave = () => {
  const event = editMode.value ? 'edit' : 'new';
  emit(event, securityInfo.value);
};
</script>
