<template>
<tool-tip
  v-if="helpUrl"
  :url="helpUrl"
  icon="mdi-help-circle-outline"
  icon-label="Get help icon"
  icon-color="primary"
  aria-label="Get help here"
  content="Link to Terracotta Knowledge Base"
  alignment="bottom"
/>
</template>

<script setup>
import { computed } from "vue";

import ToolTip from "@/components/ToolTip.vue";
import { configuration } from "@/store/configuration.module";

defineOptions({
  name: "HelpTooltip"
});

const configurationStore = configuration();

const configurations = computed(() => {
  return configurationStore.get;
});

const helpUrl = computed(() => {
  return configurations.value?.helpUrl;
});
</script>
