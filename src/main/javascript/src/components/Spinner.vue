<template>
<svg
  :width="getWidth"
  :height="getHeight"
  :viewBox="getViewBox"
  xmlns="http://www.w3.org/2000/svg"
  class="spinner"
>
  <circle
    :cx="getCx"
    :cy="getCy"
    :r="getR"
    class="path"
    fill="none"
    stroke-width="6"
    stroke-linecap="round"
  >
  </circle>
</svg>
</template>

<script setup>
import { computed } from "vue";

defineOptions({
  name: "LoadingSpinner"
});

const props = defineProps({
  width: {
    type: String,
    default: "28px"
  },
  height: {
    type: String,
    default: "28px"
  },
  viewBox: {
    type: String,
    default: "0 0 66 66"
  },
  cx: {
    type: String,
    default: "33"
  },
  cy: {
    type: String,
    default: "33"
  },
  r: {
    type: String,
    default: "30"
  }
});

const getWidth = computed(() => props.width);
const getHeight = computed(() => props.height);
const getViewBox = computed(() => props.viewBox);
const getCx = computed(() => props.cx);
const getCy = computed(() => props.cy);
const getR = computed(() => props.r);
</script>

<style lang="scss" scoped>
$offset: 187;
$duration: 0.75s;
.spinner {
  animation: rotator $duration linear infinite;
  margin: 0 auto;
}
@keyframes rotator {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(270deg); }
}
.path {
  stroke-dasharray: $offset;
  stroke-dashoffset: 0;
  transform-origin: center;
  animation:
    dash $duration ease-in-out infinite,
    colors ($duration*4) ease-in-out infinite;
}
@keyframes colors {
  0% { stroke: lightgrey; }
}
@keyframes dash {
  0% {
    stroke-dashoffset: $offset;
  }
  50% {
    stroke-dashoffset: math.div($offset, 4);
    transform:rotate(135deg);
  }
  100% {
    stroke-dashoffset: $offset;
    transform:rotate(450deg);
  }
}
</style>
