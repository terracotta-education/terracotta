<template>
<v-container
  class="container-results-dashboard pa-0"
  fluid
>
  <v-row>
    <v-col
      cols="12"
      class="px-0"
    >
      <v-tabs v-model="tab">
        <v-tab
          v-for="item in setupTabs"
          :key="item.tab"
          :value="item.tab"
        >
          {{ item.title }}
        </v-tab>
      </v-tabs>
      <v-divider />
      <v-window v-model="tab">
        <v-window-item
          v-for="item in setupTabs"
          :key="item.tab"
          :value="item.tab"
          :class="item.tab"
          class="tab-section pt-0"
        >
          <Overview v-if="item.tab === 'overview'" />
          <Outcomes v-if="item.tab === 'outcomes'" />
        </v-window-item>
      </v-window>
    </v-col>
  </v-row>
</v-container>
</template>

<script setup>
import { ref } from "vue";

import Overview from "./overview/Overview.vue";
import Outcomes from "./outcomes/Outcomes.vue";

defineOptions({
  name: "ResultsDashboard"
});

const tab = ref("overview");

const setupTabs = [
  {
    title: "Overview",
    tab: "overview"
  },
  {
    title: "Outcomes",
    tab: "outcomes"
  }
];
</script>

<style lang="scss" scoped>
div.container-results-dashboard {
  flex-direction: column;
  justify-content: space-evenly;
}
</style>
