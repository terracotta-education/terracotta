<template>
  <iframe :src="previewUrl" />
</template>

<script setup>
import { computed } from "vue";

defineOptions({
  name: "IntegrationPreview"
});

const props = defineProps({
  url: {
    type: String,
    required: true
  }
});

const previewUrl = computed(() => {
  return window.atob(props.url);
});
</script>

<style lang="scss" scoped>
iframe {
  min-height: 100%;
  min-width: 100%;
  border: none;
}
</style>
