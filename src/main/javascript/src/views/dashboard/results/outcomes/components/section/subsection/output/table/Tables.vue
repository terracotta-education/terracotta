<template>
  <div
    class="container-tables"
  >
    <v-row
      v-if="experimentExposures.length > 1"
      class="buttons"
    >
      <v-btn
        :class="selectedTable === 'condition' ? 'btn-selected' : ''"
        :elevation="0"
        @click="setSelectedTable('condition')"
      >
        By condition
      </v-btn>
      <v-btn
        :class="selectedTable === 'exposure' ? 'btn-selected' : ''"
        :elevation="0"
        @click="setSelectedTable('exposure')"
      >
        By exposure set
      </v-btn>
    </v-row>
    <v-row
      class="tables"
    >
      <Conditions
        v-if="selectedTable === 'condition'"
      />
      <Exposures
        v-if="experimentExposures.length > 1 && selectedTable === 'exposure'"
      />
    </v-row>
  </div>
</template>

<script>
import { mapGetters } from "vuex";
import Conditions from "./Conditions";
import Exposures from "./Exposures"

export default {
    name: "Tables",
    components: {
      Conditions,
      Exposures
    },
    data: () => ({
      selectedTable: "condition"
    }),
    computed: {
      ...mapGetters({
        conditions: "experiment/conditions",
        experiment: "experiment/experiment",
        exposures: "exposures/exposures",
        outcomes: "outcome/outcomes"
      }),
      experimentExposures() {
        return this.exposures || [];
      }
    },
    methods: {
      setSelectedTable(tableName) {
        this.selectedTable = tableName;
        this.$emit("type", tableName);
      }
    }
}
</script>

<style scoped>
  div.container-tables {
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    width: 100%;
    min-width: 100%;
    
    > .row {
      margin: 0;
      width: 100%;
    }
    > .buttons {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 8px;
      margin: 0;
      
      > button {
        width: 100%;
      }
      > .btn-selected {
        background-color: #323A46;
        color: white;
      }
    }
    > .tables {
      width: 100%;
      min-width: 100%;
      margin: 20px auto;
    }
  }
</style>
