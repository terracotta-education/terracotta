<template>
<v-container
  v-show="display"
  :style="containerStyles"
  class="page-loading-container"
>
  <div
    :style="spinnerStyles"
    class="spinner-container"
  >
    <h1>{{ message }}</h1>
    <spinner
      :width="'100px'"
      :height="'100px'"
      class="spinner"
    >
    </spinner>
  </div>
</v-container>
</template>

<script>
import Spinner from "@/components/Spinner";

export default {
  name: "PageLoading",
  components: {
    Spinner
  },
  props: {
    message: {
      type: String,
      default: "Loading. Please wait."
    },
    display: {
      type: Boolean,
      default: false
    },
    containerStyles: {
      type: String
    },
    spinnerStyles: {
      type: String
    }
  }
}
</script>

<style lang="scss" scoped>
@import "~@/styles/variables";

div.spinner-container {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 600px;
  height: 300px;
  padding: 0;
  border: thin solid rgba(0,0,0,.12) !important;
  border-radius: 10px !important;
  text-align: center;
  background-color: white;
  h1 {
    width: 100%;
    margin-top: 20px;
    color: map-get($blue, "base");
  }
  .spinner {
    position: relative;
    top: 25%;
    transform: translate(0, -50%);
  }
}
</style>
