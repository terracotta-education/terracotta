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
    <conditions
      v-if="selectedTable === 'condition'"
    />
    <exposures
      v-if="experimentExposures.length > 1 && selectedTable === 'exposure'"
    />
  </v-row>
</div>
</template>

<script setup>
import { ref, computed } from "vue";

import Conditions from "./Conditions.vue";
import Exposures from "./Exposures.vue";
import { exposures as useExposuresStore } from "@/store/exposures.module";

defineOptions({
  name: "OutcomeTables"
});

const emit = defineEmits(["type"]);


const selectedTable = ref("condition");

const exposuresStore = useExposuresStore();

const experimentExposures = computed(() => {
  return exposuresStore.exposures || [];
});

const setSelectedTable = (tableName) => {
  selectedTable.value = tableName;
  emit("type", tableName);
};
</script>

<style scoped>
div.container-tables {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  width: 100%;
  min-width: 100%;

  > .v-row {
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
