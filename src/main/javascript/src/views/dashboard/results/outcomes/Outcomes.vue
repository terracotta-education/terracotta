<template>
  <div
    class="container-outcomes"
  >
    <v-col
      class="input"
    >
      <Input
        @hasSelection="handleHasSelection"
      />
    </v-col>
    <v-col
      class="output"
    >
      <Output
        :showOutputPanel="showOutputPanel"
      />
    </v-col>
  </div>
</template>

<script>
import Input from "./components/section/Input.vue"
import Output from "./components/section/Output.vue"

export default {
  name: "Outcomes",
  components: {
    Input,
    Output
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

<style scoped>
div.container-outcomes {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  > .col {
    padding-top: 40px;
    &.input {
      background-color: rgba(249, 249, 249, 1);
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
