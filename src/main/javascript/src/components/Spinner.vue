<template>
  <svg
    class="spinner"
    :width="getWidth"
    :height="getHeight"
    :viewBox="getViewBox"
    xmlns="http://www.w3.org/2000/svg"
  >
    <circle
      class="path"
      fill="none"
      stroke-width="6"
      stroke-linecap="round"
      :cx="getCx"
      :cy="getCy"
      :r="getR"
    >
    </circle>
  </svg>
</template>

<script>
  export default {
    name: "Spinner",
    props: [
      "width",
      "height",
      "viewBox",
      "cx",
      "cy",
      "r"
    ],
    computed: {
      getWidth() {
        return this.width || "28px";
      },
      getHeight() {
        return this.height || "28px";
      },
      getViewBox() {
        return this.viewBox || "0 0 66 66";
      },
      getCx() {
        return this.cx || "33";
      },
      getCy() {
        return this.cy || "33";
      },
      getR() {
        return this.r || "30";
      }
    }
  }
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
    stroke-dashoffset: $offset/4;
    transform:rotate(135deg);
  }
  100% {
    stroke-dashoffset: $offset;
    transform:rotate(450deg);
  }
}
</style>