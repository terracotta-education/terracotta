<template>
<v-container
  v-show="display"
  fluid
  :style="containerStyles"
  class="page-loading-container"
>
  <div
    :style="spinnerStyles"
    class="spinner-container"
  >
    <h1>{{ message }}</h1>
    <spinner
      width="100px"
      height="100px"
      class="spinner"
    />
  </div>
</v-container>
</template>

<script setup>
import Spinner from "@/components/Spinner";

defineOptions({
  name: "PageLoading"
});

defineProps({
  message: {
    type: String,
    default: "Loading. Please wait."
  },
  display: {
    type: Boolean,
    default: false
  },
  containerStyles: {
    type: String,
    default: ""
  },
  spinnerStyles: {
    type: String,
    default: ""
  }
});
</script>

<style lang="scss" scoped>
.page-loading-container {
  position: fixed;
  inset: 0;
  z-index: 999;
  background-color: rgba(0, 0, 0, 0.5);
}

div.spinner-container {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 600px;
  height: fit-content;
  z-index: 1000;
  padding: 20px;
  border: thin solid rgba(0,0,0,.12) !important;
  border-radius: 10px !important;
  text-align: center;
  background-color: white;
  h1 {
    width: 100%;
    margin-top: 20px;
    color: map.get($blue, "base");
  }
  .spinner {
    position: relative;
    top: 25%;
    transform: translate(0, -50%);
  }
}
</style>
