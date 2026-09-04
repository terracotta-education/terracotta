<template>
<data-table
  :tableData="exposures"
  :outcomeType="outcomeType"
  titleHeader="Exposure"
/>
</template>

<script setup>
import { computed } from "vue";

import DataTable from "./components/DataTable.vue";
import { resultsDashboard as useResultsDashboardStore } from "@/store/dashboard/results.module";

defineOptions({
  name: "OutcomeExposuresTable"
});


const resultsDashboardStore = useResultsDashboardStore();
const outcomes = computed(() => resultsDashboardStore.outcomes);

const exposures = computed(() => {
  return outcomes.value?.exposures?.rows || [];
});

const outcomeType = computed(() => {
  return outcomes.value?.outcomeType || "";
});
</script>
