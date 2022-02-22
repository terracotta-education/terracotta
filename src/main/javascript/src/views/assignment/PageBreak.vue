<template>
  <v-row class="my-6 grey--text text--darken-2">
    <v-col :class="dividerColClasses"><v-divider /></v-col>
    <v-col cols="auto">Page break</v-col>
    <v-col :class="dividerColClasses"><v-divider /></v-col>
  </v-row>
</template>

<script>
export default {
  computed: {
    dividerColClasses() {
      return ["d-flex", "flex-column", "justify-center"];
    },
  },
};
</script>

<style lang="scss" scoped>
hr {
  border-style: dashed;
  border-color: map-get($grey, "base") !important;
  border-width: 1px;
}
</style>
