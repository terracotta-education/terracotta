<template>
<v-row
  class="row-summary"
>
  <v-col>
    <participants
      :participantsData="resultsOverviewParticipants"
    />
  </v-col>
  <v-col>
    <assignments
      :assignmentCount="resultsOverviewSummaryAssignmentsCount"
    />
  </v-col>
  <v-col>
    <conditions
      :conditionCount="resultsOverviewSummaryConditionsCount"
    />
  </v-col>
  <v-col>
    <exposures
      :exposureType="resultsOverviewExposureType"
    />
  </v-col>
</v-row>
</template>

<script setup>
import { computed } from "vue";

import Assignments from "./Assignments.vue";
import Conditions from "./Conditions.vue";
import Exposures from "./Exposures.vue";
import Participants from "./Participants.vue";
import { resultsDashboard as useResultsDashboardStore } from "@/store/dashboard/results.module";

defineOptions({
  name: "ResultsOverviewSummary"
});


const resultsDashboardStore = useResultsDashboardStore();
const overview = computed(() => resultsDashboardStore.overview);

const resultsOverview = computed(() => {
  return overview.value || {};
});

const resultsOverviewParticipants = computed(() => {
  return resultsOverview.value.participants || {};
});

const resultsOverviewConditions = computed(() => {
  return resultsOverview.value.conditions || {};
});

const resultsOverviewExposureType = computed(() => {
  return resultsOverviewConditions.value.exposureType;
});

const resultsOverviewSummaryAssignmentsCount = computed(() => {
  return resultsOverviewParticipants.value.assignmentCount || 0;
});

const resultsOverviewNamedConditions = computed(() => {
  return (resultsOverviewConditions.value.rows || []).filter(
    row => row.title !== "Components with only one version"
  );
});

const resultsOverviewSummaryConditionsCount = computed(() => {
  return resultsOverviewNamedConditions.value.length || 0;
});
</script>

<style lang="scss" scoped>
div.row-summary {
  justify-content: space-between;
  > .v-col {
    max-width: 24%;
    border: thin solid map.get($grey, "lighter");
    border-radius: 10px;
    > .container-summary {
      display: flex;
      flex-direction: column;
      justify-content: space-evenly;
      align-items: stretch;
      font-size: 1.05em;
    }
  }
}
</style>
