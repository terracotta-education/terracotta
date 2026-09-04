<template>
<div
  class="container-summary"
>
  <summary-data
    :title="title"
    :value="experienced.type"
    :message="tooltip"
    :icon="headerIcon"
    :valueFontSize="valueFontSize"
    iconBgColor="#e8e6fe"
  />
  <p
    class="pl-3"
  >Exposed to {{ experienced.description }} condition</p>
</div>
</template>

<script setup>
import { computed } from "vue";

import icon from "@/assets/experimenttype.svg";
import SummaryData from "./components/SummaryData.vue";

defineOptions({
  name: "OverviewExposuresSummary"
});

const props = defineProps({
  exposureType: {
    type: String,
    required: false
  }
});

const tooltip = "";
const headerIcon = icon;
const title = "Experiment type";
const valueFontSize = "1em";

const experienced = computed(() => {
  switch (props.exposureType) {
    case "BETWEEN":
      return {
        type: "Between-Subject",
        description: "one"
      };
    case "WITHIN":
      return {
        type: "Within-Subject",
        description: "every"
      };
    default:
      return {
        type: "N/A",
        description: "N/A"
      };
  }
});
</script>

<style scoped>
p {
  max-width: fit-content;
  align-self: flex-start;
  font-size: small;
}
</style>
