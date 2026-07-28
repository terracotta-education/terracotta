<template>
  <div class="summary-container">
    <h1 class="mb-3">
      <span class="completed-text font-weight-bold">
        You've completed section 1.
      </span>

      <br />

      <span>Here's a summary of your experiment design.</span>
    </h1>

    <template v-if="experiment">
      <div class="summary-panels">
        <v-expansion-panels>
          <v-expansion-panel
            class="py-3 mb-3"
            @click="panelExpansion"
          >
            <v-expansion-panel-title>
              <strong>Title</strong>
            </v-expansion-panel-title>

            <v-expansion-panel-text>
              <p>{{ experiment.title }}</p>
            </v-expansion-panel-text>
          </v-expansion-panel>
        </v-expansion-panels>

        <v-expansion-panels>
          <v-expansion-panel
            class="py-3 mb-3"
            @click="panelExpansion"
          >
            <v-expansion-panel-title>
              <strong>Description</strong>
            </v-expansion-panel-title>

            <v-expansion-panel-text>
              <p>{{ experiment.description }}</p>
            </v-expansion-panel-text>
          </v-expansion-panel>
        </v-expansion-panels>

        <v-expansion-panels v-if="conditions.length > 0">
          <v-expansion-panel
            class="py-3 mb-3"
            @click="panelExpansion"
          >
            <v-expansion-panel-title>
              <strong>Conditions</strong>
            </v-expansion-panel-title>

            <v-expansion-panel-text>
              <v-list class="m-0 p-0">
                <v-list-item
                  v-for="condition in conditions"
                  :key="condition.conditionId"
                  class="mx-0 px-0"
                >
                  <v-list-item-title>
                    {{ condition.name }}
                  </v-list-item-title>

                  <template #append>
                    <v-icon
                      v-if="condition.defaultCondition"
                      class="completed-text"
                    >
                      mdi-check
                    </v-icon>
                  </template>
                </v-list-item>
              </v-list>
            </v-expansion-panel-text>
          </v-expansion-panel>
        </v-expansion-panels>

        <v-expansion-panels>
          <v-expansion-panel
            class="py-3 mb-6"
            @click="panelExpansion"
          >
            <v-expansion-panel-title>
              <strong>Experiment Type</strong>
            </v-expansion-panel-title>

            <v-expansion-panel-text>
              <p>{{ exposureType }}</p>
            </v-expansion-panel-text>
          </v-expansion-panel>
        </v-expansion-panels>
      </div>
    </template>

    <v-btn
      v-if="!editMode"
      elevation="0"
      color="primary"
      class="mr-4"
      @click="nextSection"
    >
      Continue to next section
    </v-btn>
  </div>
</template>

<script setup>
import {
  computed,
  onMounted
} from "vue";

import { useRouter } from "vue-router";

import { deleteAttributesFromElement } from "@/helpers/ui-utils.js";

import { experiment as experimentModule } from "@/store/experiment.module";
import { navigation as navigationModule } from "@/store/navigation.module";

defineOptions({
  name: "DesignSummary"
});

const props = defineProps({
  experiment: {
    type: Object,
    required: true
  }
});

const router = useRouter();

const experimentStore = experimentModule();
const navigationStore = navigationModule();

const conditions = computed(() => {
  return experimentStore.conditions || [];
});

const editMode = computed(() => {
  return navigationStore.editMode;
});

const exposureType = computed(() => {
  return props.experiment.exposureType === "BETWEEN"
    ? "One condition"
    : "All conditions";
});

const getSaveExitPage = computed(() => {
  return editMode.value?.callerPage?.name || "Home";
});

const getNextPage = computed(() => {
  return editMode.value?.callerPage?.name || "ExperimentParticipationIntro";
});

const nextSection = () => {
  router.push({
    name: getNextPage.value,
    params: {
      experiment: props.experiment.experimentId
    }
  });
};

const saveExit = () => {
  router.push({
    name: getSaveExitPage.value,
    params: {
      experiment: props.experiment.experimentId
    }
  });
};

const panelExpansion = () => {
  setTimeout(() => {
    deleteAttributesFromElement(
      ".v-expansion-panel",
      ["aria-expanded"]
    );
  }, 1000);
};

onMounted(() => {
  deleteAttributesFromElement(
    ".v-expansion-panel",
    ["aria-expanded"]
  );
});

defineExpose({
  saveExit
});
</script>

<style lang="scss" scoped>
.completed-text {
  color: map.get($green, "base") !important;
}
</style>
