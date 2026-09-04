<template>
<div
  class="container-summary"
>
  <summary-data
    :title="title"
    :value="count"
    :message="tooltip"
    :icon="headerIcon"
    iconBgColor="#e5faff"
  />
</div>
</template>

<script setup>
import { computed } from "vue";

import icon from "@/assets/conditions.svg";
import SummaryData from "./components/SummaryData.vue";

defineOptions({
  name: "OverviewConditionsSummary"
});

const props = defineProps({
  conditionCount: {
    type: Number,
    required: false,
    default: 0
  }
});

const tooltip = "";
const headerIcon = icon;
const title = "Conditions";

const count = computed(() => props.conditionCount);
</script>
