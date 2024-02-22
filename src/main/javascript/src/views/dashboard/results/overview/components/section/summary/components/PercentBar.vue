<template>
  <div
    class="container-progress-bar w-100"
    :style="dynamicStyles"
  >
    <ProgressBar
      :val="progressValue"
      :text="progressValue + '%'"
      size="medium"
      text-align="right"
      text-position="top"
      bar-color="#fbc62f"
      bar-border-radius="5"
    />
  </div>
</template>

<script>
import ProgressBar from 'vue-simple-progress'

export default {
  name: "PercentBar",
  props: [
    "value"
  ],
  components: {
    ProgressBar
  },
  computed: {
    progressValue() {
      if (this.value === null || this.value < 0) {
        return 0;
      }
      if (this.value > 100) {
        return 100;
      }
      return this.value;
    },
    dynamicStyles() {
      return {
        "--percent-label-width": (this.progressValue + 6) + '%'
      };
    }
  }
}
</script>

<style scoped>
div.container-progress-bar {
  & .vue-simple-progress-text {
    width: var(--percent-label-width);
  }
  & .vue-simple-progress {
    width: 100%;
  }
}
</style>
