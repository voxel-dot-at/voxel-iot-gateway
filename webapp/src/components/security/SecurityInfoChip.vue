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
  <v-chip>
    {{ securityInfo.mode }}
  </v-chip>
</template>

<script>
export default {
  props: {
    securityInfo: Object,
  },
};
</script>
