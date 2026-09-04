<template>
<div
  class="container-summary"
>
  <summary-data
    :title="title"
    :value="count"
    :message="tooltip"
    :icon="headerIcon"
    iconBgColor="#fef3e7"
  />
</div>
</template>

<script setup>
import { computed } from "vue";

import icon from "@/assets/assignments.svg";
import SummaryData from "./components/SummaryData.vue";

defineOptions({
  name: "OverviewAssignmentsSummary"
});

const props = defineProps({
  assignmentCount: {
    type: Number,
    required: false,
    default: 0
  }
});

const tooltip = "";
const headerIcon = icon;
const title = "Components";

const count = computed(() => props.assignmentCount);
</script>
