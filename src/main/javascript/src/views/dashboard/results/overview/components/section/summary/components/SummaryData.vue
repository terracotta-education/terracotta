<template>
<div
  class="summary-data"
>
  <div
    class="header-row"
  >
    <h3>{{ headerTitle }}</h3>
    <v-img
      v-if="headerIcon"
      :style="dynamicStyles"
      :src="headerIcon"
      width="32"
      height="32"
      class="header-icon"
    />
  </div>
  <div
    class="count-row"
  >
    <span
      :style="valueStyle"
      class="summary-count"
    >{{ count }}</span>
    <tool-tip
      v-if="displayTooltip"
      :header="headerTitle"
      :content="tooltip"
      :activatorType="tooltipActivator.type"
      :icon="tooltipActivator.text"
      :aria-label="`${headerTitle} tooltip`"
    />
  </div>
</div>
</template>

<script setup>
import { computed } from "vue";

import ToolTip from "@/components/ToolTip.vue";

defineOptions({
  name: "SummaryCount"
});

const props = defineProps({
  title: {
    type: String,
    required: false
  },
  value: {
    type: [Number, String],
    required: false
  },
  message: {
    type: String,
    required: false
  },
  icon: {
    type: String,
    required: false
  },
  iconBgColor: {
    type: String,
    required: false
  },
  valueFontSize: {
    type: String,
    required: false
  },
  showTooltip: {
    type: Boolean,
    required: false
  }
});

const headerTitle = computed(() => props.title || "N/A");
const headerIcon = computed(() => props.icon || null);
const count = computed(() => props.value || 0);
const tooltip = computed(() => props.message || "N/A");
const displayTooltip = computed(() => props.showTooltip || false);

const tooltipActivator = {
  type: "icon",
  text: "mdi-information-outline"
};

const valueStyle = computed(() => ({
  fontSize: props.valueFontSize || "2em"
}));

const dynamicStyles = computed(() => ({
  "--header-icon-bg-color": props.iconBgColor || "transparent"
}));
</script>

<style scoped>
div.summary-data {
  display: flex;
  flex-direction: column;
  width: 100%;
  padding: 12px;
}
div.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  > h3 {
    max-width: fit-content;
    border-radius: 4px;
    padding-bottom: 0 !important;
    word-break: break-all;
  }
  > .header-icon {
    flex: 0 0 32px;
    width: 32px;
    height: 32px;
    border-radius: 4px;
    background-color: var(--header-icon-bg-color);
  }
}
div.count-row {
  padding-top: 16px;
  display: flex;
  align-items: center;
  gap: 4px;
  > span.summary-count {
    font-size: 2em;
    font-weight: bold;
  }
}
</style>
