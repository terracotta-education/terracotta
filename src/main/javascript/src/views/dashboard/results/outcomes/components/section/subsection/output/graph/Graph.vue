<template>
<chart
  :displayChartData="displayChartData"
  :graphData="graphData"
  :outcomeType="outcomeType"
  :type="getType"
/>
</template>

<script setup>
import { computed } from "vue";

import Chart from "./components/Chart.vue";
import { resultsDashboard as useResultsDashboardStore } from "@/store/dashboard/results.module";

defineOptions({
  name: "OutcomeGraph"
});

const props = defineProps({
  displayOutput: {
    type: Boolean,
    default: false
  },
  type: {
    type: String,
    default: "condition"
  }
});


const outcomes = computed(() => useResultsDashboardStore().outcomes);

const conditions = computed(() => {
  return outcomes.value?.conditions?.rows || [];
});

const exposures = computed(() => {
  return outcomes.value?.exposures?.rows || [];
});

const getType = computed(() => {
  return props.type;
});

const displayChartData = computed(() => {
  return props.displayOutput;
});

const outcomeType = computed(() => {
  return outcomes.value?.outcomeType || "";
});

const orderByTitleAsc = (values) => {
  return [...values].sort((a, b) => {
    return a.title.localeCompare(b.title, undefined, {
      sensitivity: "base"
    });
  });
};

const graphData = computed(() => {
  let dataset;

  switch (getType.value) {
    case "exposure":
      dataset = orderByTitleAsc(exposures.value);
      break;
    case "condition":
      dataset = orderByTitleAsc(conditions.value);
      break;
    default:
      dataset = [];
  }

  return dataset
    .filter((ds) => ds.title !== "Overall")
    .map((d) => {
      return {
        title: d.title,
        mean: displayChartData.value ? d.mean : null,
        scores: displayChartData.value
          ? d.scores?.length
            ? d.scores
            : []
          : []
      };
    });
});
</script>
