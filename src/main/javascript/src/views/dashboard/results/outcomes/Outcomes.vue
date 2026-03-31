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
      :showOutputPanel="showOutputPanel"
    />
  </v-col>
</div>
</template>

<script>
import SectionInput from "./components/section/Input.vue";
import SectionOutput from "./components/section/Output.vue";

export default {
  name: "Outcomes",
  components: {
    SectionInput,
    SectionOutput
  },
  data: () => ({
    displayOutput: false
  }),
  computed: {
    showOutputPanel() {
      return this.displayOutput || false;
    }
  },
  methods: {
    handleHasSelection(value) {
      this.displayOutput = value;
    }
  }
}
</script>

<style lang="scss" scoped>
@import "~@/styles/variables";

div.container-outcomes {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  > .col {
    padding-top: 40px;
    &.input {
      background-color: map-get($grey, "extreme-light");
      border-right: 1px solid rgba(0, 0, 0, .12);
      width: 30%;
      max-width: 30%;
    }
    &.output {
      width: 65%;
      max-width: 65%;
    }
  }
}
</style>
