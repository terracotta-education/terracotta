<template>
<div
  class="container-outcomes"
>
  <v-col
    class="input"
  >
    <section-input
      @hasSelection="handleHasSelection"
    />
  </v-col>
  <v-col
    class="output"
  >
    <section-output
      :showOutputPanel="displayOutput"
    />
  </v-col>
</div>
</template>

<script setup>
import { ref } from "vue";

import SectionInput from "./components/section/Input.vue";
import SectionOutput from "./components/section/Output.vue";

defineOptions({
  name: "OutcomesDashboard"
});

const displayOutput = ref(false);

const handleHasSelection = (value) => {
  displayOutput.value = value;
};
</script>

<style lang="scss" scoped>
div.container-outcomes {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  > .v-col {
    padding-top: 40px;
    &.input {
      background-color: map.get($grey, "extreme-light");
      border-right: 1px solid rgba(0, 0, 0, .12);
      width: 30%;
      max-width: 30%;
    }
    &.output {
      width: 65%;
      max-width: 65%;
    }
  }

  // matches this app's existing mobile breakpoint (DataTable.vue, ExperimentSteps.vue) - below
  // it there isn't room for input/output side by side, so stack output under input instead
  @media (max-width: 636px) {
    flex-direction: column;

    > .v-col {
      &.input, &.output {
        width: 100%;
        max-width: 100%;
      }
      &.input {
        border-right: none;
      }
    }
  }
}
</style>
