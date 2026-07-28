<template>
  <iframe
    v-if="feedbackEnabled"
    :src="feedbackUrl"
  />
</template>

<script setup>
import { computed } from "vue";

defineOptions({
  name: "IntegrationFeedback"
});

const props = defineProps({
  submission: {
    type: Object,
    required: true
  }
});

const feedbackEnabled = computed(() => {
  return props.submission?.integrationFeedbackEnabled || false;
});

const feedbackUrl = computed(() => {
  return props.submission?.integrationLaunchUrl || "";
});
</script>

<style lang="scss" scoped>
iframe {
  min-height: 100%;
  min-width: 100%;
  border: none;
}
</style>
