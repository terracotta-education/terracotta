<template>
  <v-row>
    <v-col cols="1">
      &nbsp;
    </v-col>

    <v-col cols="10">
      <v-card
        :class="borderClass"
        variant="outlined"
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

<script setup>
import { computed } from "vue";

defineOptions({
  name: "ResponseRow"
});

const props = defineProps({
  correct: {
    type: Boolean,
    default: null
  }
});

const borderClass = computed(() => {
  if (props.correct === true) {
    return "green-border";
  }

  if (props.correct === false) {
    return "red-border";
  }

  return "";
});
</script>

<style lang="scss" scoped>
.green-border {
  border-color: map.get($green, "base") !important;
  border-width: 2px !important;
}

.red-border {
  border-color: map.get($red, "base") !important;
  border-width: 2px !important;
}

.v-card-title {
  font-weight: bold;
}
</style>
