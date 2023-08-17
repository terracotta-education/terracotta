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
        @click="setSelectedTable('condition')"
      >
        By condition
      </v-btn>
      <v-btn
        :class="selectedTable === 'exposure' ? 'btn-selected' : ''"
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
import Conditions from "./Conditions.vue";
import Exposures from "./Exposures.vue"

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
    display: flex;
    flex-direction: row;
    justify-content: space-between;
    margin: 0;
    > button {
      width: 48%;
      &.v-btn--is-elevated {
        box-shadow: none;
      }
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
    > .v-data-table {
      width: 100%;
      min-width: 100%;
    }
  }
}
</style>
