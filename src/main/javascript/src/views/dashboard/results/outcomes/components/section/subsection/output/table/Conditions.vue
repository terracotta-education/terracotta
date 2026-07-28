<template>
<data-table
  :tableData="conditions"
  :outcomeType="outcomeType"
  titleHeader="Condition"
/>
</template>

<script setup>
import { computed } from "vue";

import DataTable from "./components/DataTable.vue";
import { resultsDashboard as useResultsDashboardStore } from "@/store/dashboard/results.module";

defineOptions({
  name: "OutcomeConditionsTable"
});


const outcomes = computed(() => useResultsDashboardStore().outcomes);

const conditions = computed(() => {
  return outcomes.value?.conditions?.rows || [];
});

const outcomeType = computed(() => {
  return outcomes.value?.outcomeType || "";
});
</script>
