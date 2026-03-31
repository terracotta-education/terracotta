<template>
<v-row>
  <v-col
    cols="1"
  >
    &nbsp;
  </v-col>
  <v-col
    cols="10"
  >
    <v-card
      :class="border"
      outlined
    >
      <v-card-title
        class="py-0"
      >
        <slot />
      </v-card-title>
    </v-card>
  </v-col>
</v-row>
</template>

<script>
export default {
  props: {
    correct: {
      type: Boolean,
      default: null
    }
  },
  computed: {
    border() {
      return this.correct ? 'green--border' : this.correct === null ? '' : 'red--border';
    }
  }
};
</script>

<style lang="scss" scoped>
@import "@/styles/variables";

.green--border {
  border-color: map-get($green, "base") !important;
  border-width: 2px !important;
}
.red--border {
  border-color: map-get($red, "base") !important;
  border-width: 2px !important;
}
</style>
