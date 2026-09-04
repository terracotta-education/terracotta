<template>
<div
  class="container-progress-bar w-100 mx-auto"
  :style="dynamicStyles"
>
  <div class="progress-text">{{ progressValue }}%</div>
  <v-progress-linear
    :model-value="progressValue"
    color="#fbc62f"
    bg-color="rgba(0,0,0,0.12)"
    rounded
    height="6"
  />
</div>
</template>

<script setup>
import { computed } from "vue";

defineOptions({
  name: "PercentBar"
});

const props = defineProps({
  value: {
    type: Number,
    required: false,
    default: 0
  }
});

const progressValue = computed(() => {
  if (props.value === null || Number.isNaN(props.value) || props.value < 0) {
    return 0;
  }

  if (props.value > 100) {
    return 100;
  }

  return props.value;
});

const dynamicStyles = computed(() => {
  return {
    "--percent-label-width": `${progressValue.value + 6}%`
  };
});
</script>

<style scoped>
div.container-progress-bar {
  & .progress-text {
    width: var(--percent-label-width);
    text-align: right;
    font-size: 0.85em;
    line-height: 1.4;
  }
}
</style>
