<template>
  <v-container
    class="container-results-dashboard p-0"
  >
    <v-row>
      <v-col
        cols="12"
        class="px-0"
      >
        <v-tabs
          v-model="tab"
          elevation="0"
        >
          <v-tab
            v-for="item in setupTabs"
            :key="item.tab"
          >
            {{ item.tab }}
          </v-tab>
        </v-tabs>
        <v-divider></v-divider>
        <v-tabs-items
          v-model="tab"
        >
          <v-tab-item
            v-for="item in setupTabs"
            :class="item.tab"
            :key="item.tab"
            :transition="false"
            class="tab-section pt-0"
          >
            <div
              class="tab-heading"
            >
              <template
                v-if="item.tab === 'overview'"
              >
                <Overview />
              </template>
              <template
                v-if="item.tab === 'outcomes'"
              >
                <Outcomes />
              </template>
            </div>
          </v-tab-item>
        </v-tabs-items>
      </v-col>
    </v-row>
  </v-container>
</template>

<script>
import Overview from "./overview/Overview"
import Outcomes from "./outcomes/Outcomes"

export default {
  name: "ResultsDashboard",
  components: {
    Overview,
    Outcomes
  },
  data: () => ({
    tab: null,
    items: ["overview", "outcomes"],
    setupTabs: [
      {
        title: "Overview",
        tab: "overview"
      },
      {
        title: "Outcomes",
        tab: "outcomes"
      }
    ]
  })
};
</script>

<style scoped>
div.container-results-dashboard {
  flex-direction: column;
  justify-content: space-evenly;
  & div.v-tabs-bar {
    > div.v-slide-group__wrapper {
      background-color: rgba(249, 249, 249, 1);
    }
  }
}
</style>
