<template>
<div
  class="container-output"
>
<v-row
    v-if="!displayOutput"
    class="my-0 mt-2 px-0"
  >
    <v-card
      class="no-outcomes-selected pt-5 px-5 mx-auto bg-blue-lighten-5 rounded-lg"
      variant="outlined"
    >
      <p
        class="pb-0"
      >
        Results will appear here when you choose an outcome for your exposure set(s).
      </p>
    </v-card>
  </v-row>
  <v-row>
    <graph
      :displayOutput="displayOutput"
      :type="getType"
    />
  </v-row>
  <v-row
    v-if="displayOutput"
  >
    <tables
      @type="changeType"
    />
  </v-row>
</div>
</template>

<script setup>
import { ref, computed } from "vue";

import Graph from "./subsection/output/graph/Graph.vue";
import Tables from "./subsection/output/table/Tables.vue";

defineOptions({
  name: "SectionOutput"
});

const props = defineProps({
  showOutputPanel: {
    type: Boolean,
    required: false
  }
});

const type = ref(null);

const getType = computed(() => {
  return type.value || "condition";
});

const displayOutput = computed(() => {
  return props.showOutputPanel || false;
});

const changeType = (newType) => {
  type.value = newType;
};
</script>

<style scoped>
div.container-output {
  > .v-row {
    width: 100%;
    margin: 10px 0;
  }
  & .no-outcomes-selected {
      width: 100%;
      border-color: rgba(29, 157, 255, 0.6) !important;
    }
}
</style>
