<template>
  <v-container
    v-show="display"
    :style="getContainerStyles"
    class="page-loading-container"
  >
    <div
      :style="getSpinnerStyles"
      class="spinner-container"
    >
      <h1>{{ getMessage }}</h1>
      <Spinner
        :width="'100px'"
        :height="'100px'"
        class="spinner"
      >
      </Spinner>
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
    props: [
      "message",
      "display",
      "containerStyles",
      "spinnerStyles"
    ],
    computed: {
      getMessage() {
        return this.message || "Loading. Please wait.";
      },
      getContainerStyles() {
        return this.containerStyles || "";
      },
      getSpinnerStyles() {
        return this.spinnerStyles || "";
      }
    }
  }
</script>

<style lang="scss" scoped>
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
    color: #0077d2;
  }
  .spinner {
    position: relative;
    top: 25%;
    transform: translate(0, -50%);
  }
}
</style>
