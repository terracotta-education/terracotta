<template>
<div
  class="container-summary"
>
  <summary-data
    :title="title"
    :value="count"
    :message="tooltip"
    :icon="headerIcon"
    :showTooltip="true"
    iconBgColor="#fef8e6"
  />
  <percent-bar
    :value="toPercent(consentRate)"
    class="progress-bar"
  />
</div>
</template>

<script setup>
import { computed } from "vue";

import { percent } from "@/helpers/dashboard/utils.js";
import icon from "@/assets/participants.svg";
import PercentBar from "./components/PercentBar.vue";
import SummaryData from "./components/SummaryData.vue";

defineOptions({
  name: "OverviewParticipantsSummary"
});

const props = defineProps({
  participantsData: {
    type: Object,
    required: false
  }
});

const tooltip = "The number of people who consented to participate in the experiment";
const headerIcon = icon;
const title = "Participants";

const sectionData = computed(() => {
  return props.participantsData || {};
});

const count = computed(() => {
  return sectionData.value.count || 0;
});

const consentRate = computed(() => {
  return sectionData.value.consentRate || 0;
});

const toPercent = (value) => {
  return percent(value);
};
</script>

<style scoped>
.progress-bar {
  width: 90%;
}
</style>
