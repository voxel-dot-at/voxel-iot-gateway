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
    <v-data-table
      v-if="!loading"
      :headers="headers"
      :items="registrations"
      item-key="endpoint"
      :items-per-page="10"
      class="elevation-0 fill-height ma-3"
      @click:row="openLink"
      :search="search"
      :sort-by="[{ key: 'registrationDate', order: 'asc' }]"
    >
    <template #header.endpoint>
        <strong>Client Endpoint</strong>
    </template>
    <template #header.registrationId>
        <strong>Registration ID</strong>
    </template>
    <template #header.registrationDate>
        <strong>Registration Date</strong>
    </template>
    <template #header.lastUpdate>
        <strong>Last Update</strong>
    </template>


      <template v-slot:top>
        <v-toolbar>
          <v-toolbar-title>Registered Clients</v-toolbar-title
          >
          <v-divider
            class="mx-4"
            inset
            vertical
          ></v-divider>
          <v-text-field
            v-model="search"
            :append-inner-icon="$icons.mdiMagnify"
            label="Search"
            single-line
            hide-details
            class="pa-2"
            clearable
          ></v-text-field>
        </v-toolbar>
      </template>
      <!-- custom display for date column -->
      <template v-slot:item.registrationDate="{ item }">
        {{ dayjs(item.registrationDate).format("MMM D, h:mm:ss A") }}
      </template>
      <template v-slot:item.lastUpdate="{ item }">
        {{ dayjs(item.lastUpdate).format("MMM D, h:mm:ss A") }}
      </template>
      <template v-slot:item.infos="{ item }">
        <client-info :registration="item" tooltipleft />
      </template>
    </v-data-table>
  </div>
</template>

<script>
import ClientInfo from "../components/ClientInfo.vue";
import dayjs from "dayjs";

export default {
  components: { ClientInfo },
  useSSE: true,
  name: "Clients",
  data: () => ({
    loading: true,
    registrations: [],
    dayjs: dayjs,
    headers: [
      { title: "Client Endpoint", key: "endpoint" },
      { title: "Registration ID", key: "registrationId" },
      { title: "Registration Date", key: "registrationDate" },
      { title: "Last Update", key: "lastUpdate" },
      { title: "", key: "infos", sortable: false, align: "end" },
    ],
    search: "",
  }),
  methods: {
    openLink(event, { item }){
      console.log('LINK', item)
      this.$router.push(`/clients/${encodeURIComponent(item.endpoint)}/3`);
    },
  },
  mounted() {
    // listen events to update registration.
    this.sse = this.$sse
      .create({ url: "/api/event" })
      .on("REGISTRATION", (reg) => {
        if (!reg) return;
        this.registrations = this.registrations
          .filter((r) => reg.endpoint !== r.endpoint)
          .concat(reg);
      })
      .on("UPDATED", (msg) => {
        if (!msg || !msg.registration) return;
        let reg = msg.registration;
        this.registrations = this.registrations
          .filter((r) => (reg?.registrationId ?? '') !== r.registrationId)
          .concat(reg);
      })
      .on("DEREGISTRATION", (reg) => {
        if (!reg) return;
        this.registrations = this.registrations.filter(
          (r) => (reg?.registrationId ?? '') !== r.registrationId
        );
      })
      .on("SLEEPING", (reg) => {
        if (!reg) return;
        for (var i = 0; i < this.registrations.length; i++) {
          if (this.registrations[i].endpoint === reg.ep) {
            this.registrations[i].sleeping = true;
          }
        }
      })
      .on("error", (err) => {
        console.error("sse unexpected error", err);
      });
    this.sse.connect().catch((err) => {
      console.error("Failed to connect to server", err);
    });

    // get all registrations
    this.axios
      .get("/api/clients")
      .then(
        (response) => (
          (this.loading = false), (this.registrations = response.data), ( console.log('R', response.data) )
        )
      );
  },
  beforeUnmount() {
    // close eventsource on destroy
    this.sse.disconnect();
  },
};
</script>
