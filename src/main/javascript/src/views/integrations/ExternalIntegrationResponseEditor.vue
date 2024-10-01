<template>
    <iframe
      v-if="feedbackEnabled"
      :src="feedbackUrl"
    >
    </iframe>
</template>

<script>
export default {
  props: {
    submission:{
      type: Object,
      required: true
    }
  },
  computed: {
    feedbackEnabled() {
      return this.submission ? this.submission.integrationFeedbackEnabled : false;
    },
    feedbackUrl() {
      return this.submission ? this.submission.integrationLaunchUrl : "";
    }
  }
};
</script>

<style lang="scss" scoped>
iframe {
  min-height: 100%;
  min-width: 100%;
  border: none;
}
</style>
