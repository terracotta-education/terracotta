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
  // space-between already distributes the horizontal spacing between cards -
  // Vuetify 3's own default v-row gap (24px) stacked on top of that, quietly
  // eating into the slack this row needs to fit 4 cards at exactly 24% each
  // without wrapping (min-width below is a hard floor once gap eats past
  // what space-between leaves for it). row-gap is unrelated to that slack -
  // kept (at the same 24px default) so cards that wrap to their own line
  // don't end up touching the row above.
  column-gap: 0;
  row-gap: 24px;
  > .v-col {
    // max-width alone had no floor, so these 4 columns always fit
    // side-by-side no matter how narrow the row got - each one just kept
    // shrinking until its text wrapped one letter per line instead of ever
    // dropping to fewer per row. min-width wins over a smaller max-width
    // (per spec), so this stays 24% on wide rows but stops shrinking below
    // 220px, letting flex-wrap collapse to fewer columns once they no
    // longer fit.
    min-width: max(24%, 220px);
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
