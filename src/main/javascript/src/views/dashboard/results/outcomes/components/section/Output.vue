<template>
  <div
    class="container-output"
  >
  <v-row
      v-if="!displayOutput"
      class="my-0 mt-2 px-0"
    >
      <v-card
        class="no-outcomes-selected pt-5 px-5 mx-auto blue lighten-5 rounded-lg"
        outlined
      >
        <p
          class="pb-0"
        >
          Results will appear here when you choose an outcome for your exposure set(s).
        </p>
      </v-card>
    </v-row>
    <v-row>
      <Graph
        :displayOutput="displayOutput"
        :type="getType"
      />
    </v-row>
    <v-row
      v-if="displayOutput"
    >
      <Tables
        @type="changeType"
      />
    </v-row>
  </div>
</template>

<script>
import Graph from "./subsection/output/graph/Graph.vue";
import Tables from "./subsection/output/table/Tables.vue";

export default {
  name: "Output",
  components: {
    Graph,
    Tables
  },
  props: [
    "showOutputPanel"
  ],
  data: () => ({
    type: null
  }),
  computed: {
    getType() {
      return this.type || "condition";
    },
    displayOutput() {
      return this.showOutputPanel || false;
    }
  },
  methods: {
    changeType(newType) {
      this.type = newType;
    }
  }
}
</script>

<style scoped>
div.container-output {
  > .row {
    width: 100%;
    margin: 10px 0;
  }
  & .no-outcomes-selected {
      width: 100%;
      &.v-sheet--outlined.blue.lighten-5 {
        border-color: rgba(29, 157, 255, 0.6) !important;
      }
    }
}
</style>
