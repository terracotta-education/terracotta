<template>
<div
  class="container-input"
>
  <v-row
    class="description"
    density="compact"
  >
    <h3
      class="my-0"
    >
      Select Outcomes
    </h3>
    <tool-tip
      header="What is an outcome?"
      content="An outcome (also known as a dependent variable) is a variable that may be affected by the experimental manipulation."
      activatorType="link"
      activatorContent="What is an outcome?"
      alignment="top"
    />
    <span>{{ experimentDetailsText }}</span>
    <span>
      {{ selectOutcomesText }} Outcomes are
      <a
        role="button"
        href="#"
        tabindex="0"
        @click.prevent="handleStatusPageNav()"
        @keydown.space.prevent="handleStatusPageNav()"
      >
        added on the status page.
      </a>
    </span>
    <span>
      <selector
        @hasCleared="handleClearedSelection"
        @hasSelections="handleGetOutcomes"
      />
    </span>
  </v-row>
</div>
</template>

<script setup>
import { ref, computed, watch } from "vue";

import { EventBus } from "@/helpers/event-bus";
import Selector from "./subsection/input/Selector.vue";
import ToolTip from "@/components/ToolTip.vue";
import { experiment as useExperimentStore } from "@/store/experiment.module";
import { exposures as useExposuresStore } from "@/store/exposures.module";
import { resultsDashboard as useResultsDashboardStore } from "@/store/dashboard/results.module";

defineOptions({
  name: "SectionInput"
});

const emit = defineEmits(["hasSelection"]);


const hasSelectedOption = ref(false);

const experiment = computed(() => useExperimentStore().experiment);
const conditions = computed(() => useExperimentStore().conditions);
const exposures = computed(() => useExposuresStore().exposures);

const experimentId = computed(() => experiment.value?.experimentId);

const experimentConditions = computed(() => conditions.value || []);
const experimentExposures = computed(() => exposures.value || []);

const experimentConditionCount = computed(() => experimentConditions.value.length);
const experimentExposureCount = computed(() => experimentExposures.value.length);

const experimentDetailsText = computed(() => {
  let text = `Your experiment has ${experimentConditionCount.value} conditions`;

  if (experimentExposureCount.value > 1) {
    text += ` and ${experimentExposureCount.value} exposure sets`;
  }

  return `${text}.`;
});

const selectOutcomesText = computed(() => {
  let text = "Select the outcomes you want to compare between conditions";

  if (experimentExposureCount.value > 1) {
    text += "/exposure sets";
  }

  return `${text}.`;
});

watch(hasSelectedOption, value => {
  emit("hasSelection", value);
});

const clearOutcomes = () => {
  useResultsDashboardStore().clearOutcomes();
};

const getOutcomes = payload => {
  return useResultsDashboardStore().getOutcomes( payload);
};

const handleGetOutcomes = async (selectedOutcomes) => {
  hasSelectedOption.value = true;

  await getOutcomes([
    experimentId.value,
    selectedOutcomes
  ]);
};

const handleClearedSelection = () => {
  clearOutcomes();
  hasSelectedOption.value = false;
};

const handleStatusPageNav = () => {
  EventBus.emit("statusPageNav");
};
</script>

<style lang="scss" scoped>
div.container-input {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding-bottom: 60px;
  > .v-row {
    margin: 0 !important;
    &.description {
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      & > * {
        padding: 8px;
        max-width: 100%;
      }
      > h3 {
        font-weight: bold;
      }
    }
  }
  & a.tooltip-outcome {
    text-decoration: none;
    border-bottom:1px dotted;
  }
}
</style>
