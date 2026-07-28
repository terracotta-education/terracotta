<template>
  <v-row class="my-6 text-medium-emphasis">
    <v-col class="d-flex flex-column justify-center">
      <v-divider />
    </v-col>

    <v-col cols="auto">
      Page break
    </v-col>

    <v-col class="d-flex flex-column justify-center">
      <v-divider />
    </v-col>
  </v-row>
</template>

<script setup>
defineOptions({
  name: "PageBreak"
});
</script>

<style lang="scss" scoped>
:deep(hr) {
  border-style: dashed;
  border-color: map.get($grey, "base") !important;
  border-width: 1px;
}
</style>
