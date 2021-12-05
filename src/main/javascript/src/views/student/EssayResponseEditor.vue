<template>
  <response-row>
    <v-textarea v-model="response" @input="emitValueChanged" />
  </response-row>
</template>

<script>
import ResponseRow from "./ResponseRow.vue";

export default {
  props: ["value"],
  components: { ResponseRow },
  data() {
    return {
      response: this.value,
    };
  },
  methods: {
    emitValueChanged() {
      this.$emit("input", this.response);
    },
  },
  watch: {
    value() {
      this.response = this.value;
    },
  },
};
</script>

<style></style>
