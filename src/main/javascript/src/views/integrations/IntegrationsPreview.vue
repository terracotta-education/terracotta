<template>
  <iframe
    :src="previewUrl"
  ></iframe>
</template>

<script>
export default {
  props: {
    url:{
      type: String,
      required: true
    }
  },
  computed: {
    previewUrl() {
      return atob(this.url);
    }
  }
}
</script>

<style lang="scss" scoped>
iframe {
  min-height: 100%;
  min-width: 100%;
  border: none;
}
</style>
