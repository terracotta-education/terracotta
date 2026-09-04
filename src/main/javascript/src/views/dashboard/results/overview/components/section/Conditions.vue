<template>
<v-row
  class="container-table"
>
  <h3>Conditions</h3>
  <data-table
    :tableData="rows"
    :tooltips="tooltips"
    titleHeader="Condition Name"
    noSubmissionsMessage="Data will appear here when participants assigned to this condition have completed the component."
  />
</v-row>
</template>

<script setup>
import { computed } from "vue";

import DataTable from "./subsection/DataTable.vue";

defineOptions({
  name: "OverviewConditionsSection"
});

const props = defineProps({
  conditionsData: {
    type: Object,
    required: false
  }
});

const tooltips = [
  {
    id: "submissionRate",
    message: "This value is calculated by dividing the total number of component submissions for each condition by the total number of consenting participants assigned to those conditions."
  }
];

const sectionData = computed(() => {
  return props.conditionsData || {};
});

const rows = computed(() => {
  return sectionData.value.rows || [];
});
</script>

<style lang="scss" scoped>
h3 {
  font-weight: 700;
  padding: 0;
  margin: 0;
}
</style>