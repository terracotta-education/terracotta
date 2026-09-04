<template>
<v-data-table
  :headers="tableHeaders"
  :items="computedTableData"
  :items-per-page="-1"
  class="v-data-table-alt"
  item-value="title"
>
  <template #bottom></template>
</v-data-table>
</template>

<script setup>
import { computed } from "vue";
import { timeFormat, percent } from "@/helpers/dashboard/utils.js";

defineOptions({
  name: "DataTable"
});

const props = defineProps({
  tableData: {
    type: Array,
    required: true
  },
  titleHeader: {
    type: String,
    required: true
  },
  outcomeType: {
    type: String,
    required: true
  }
});

const computedTableData = computed(() => {
  return (props.tableData ?? []).map((t) => {
    switch (props.outcomeType) {
      case "TIME_ON_TASK":
        return {
          title: t.title,
          number: t.number,
          standardDeviation: timeFormat(t.standardDeviation),
          mean: timeFormat(t.mean)
        };

      case "AVERAGE_ASSIGNMENT_SCORE":
      case "STANDARD":
      default:
        return {
          title: t.title,
          number: t.number,
          standardDeviation: percent(t.standardDeviation) + "%",
          mean: percent(t.mean) + "%"
        };
    }
  });
});

const tableHeaders = computed(() => {
  return [
    {
      title: props.titleHeader,
      key: "title",
      align: "start",
      width: "35%",
      sortable: false
    },
    {
      title: "N",
      key: "number",
      align: "center",
      sortable: false
    },
    {
      title: "Mean",
      key: "mean",
      align: "center",
      sortable: false
    },
    {
      title: "Standard deviation",
      key: "standardDeviation",
      align: "center",
      sortable: false
    }
  ];
});
</script>
