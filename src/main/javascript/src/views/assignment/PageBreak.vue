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
// Vuetify 3's own .v-divider CSS wins the border-style/border-width tie
// against this rule (a Vue3-only divergence - development's identical
// source, sans !important on these two, was enough against Vuetify 2's own
// divider CSS), rendering a plain solid line - only border-color (already
// !important) was actually taking effect, leaving a faint solid line
// instead of the intended dashed one.
:deep(hr) {
  border-style: dashed !important;
  border-color: map.get($grey, "base") !important;
  border-width: 1px !important;
  // Vuetify 3's own .v-divider default (opacity: 0.12, a semi-transparent
  // Material-style divider) is a separate property untouched by the border
  // overrides above - it composited #e2e2e2 down to an almost-invisible
  // ~#fbfbfb, which is what was still reading as "lighter than development"
  // even with the correct color/style already winning.
  opacity: 1 !important;
}
</style>
