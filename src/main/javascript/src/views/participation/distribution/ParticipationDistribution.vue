<template>
  <div class="participation-distribution-container">
    <template v-if="experiment">
      <h1 class="mb-5">
        How would you like to distribute your experiment participants?
      </h1>

      <v-expansion-panels
        class="v-expansion-panels--icon mx-auto my-0"
      >
        <v-expansion-panel
          :class="{
            'v-expansion-panel--selected':
              experiment.distributionType === 'EVEN'
          }"
          @click="panelExpansion"
        >
          <v-expansion-panel-title hide-actions>
            <img
              src="@/assets/even.svg"
              alt="even distribution"
            />

            Even
          </v-expansion-panel-title>

          <v-expansion-panel-text>
            <p>
              Equally distribute your students
              across all conditions
            </p>

            <v-btn
              color="primary"
              elevation="0"
              @click="saveType('EVEN')"
            >
              Select
            </v-btn>
          </v-expansion-panel-text>
        </v-expansion-panel>

        <v-expansion-panel
          :class="{
            'v-expansion-panel--selected':
              experiment.distributionType === 'CUSTOM'
          }"
          :disabled="
            experiment.exposureType === 'WITHIN'
          "
          @click="panelExpansion"
        >
          <v-expansion-panel-title hide-actions>
            <img
              src="@/assets/custom.svg"
              alt="custom distribution"
            />

            Custom
          </v-expansion-panel-title>

          <v-expansion-panel-text>
            <p>
              Customize the percentage of
              students who receive each
              condition
            </p>

            <v-btn
              color="primary"
              elevation="0"
              @click="saveType('CUSTOM')"
            >
              Select
            </v-btn>
          </v-expansion-panel-text>
        </v-expansion-panel>

        <v-expansion-panel
          :class="{
            'v-expansion-panel--selected':
              experiment.distributionType === 'MANUAL'
          }"
          :disabled="
            experiment.exposureType === 'WITHIN'
          "
          @click="panelExpansion"
        >
          <v-expansion-panel-title hide-actions>
            <img
              src="@/assets/manual.svg"
              alt="Manual distribution"
            />

            Manual
          </v-expansion-panel-title>

          <v-expansion-panel-text>
            <p>
              Manually select which students
              receive each condition
            </p>

            <v-btn
              color="primary"
              elevation="0"
              @click="saveType('MANUAL')"
            >
              Select
            </v-btn>
          </v-expansion-panel-text>
        </v-expansion-panel>
      </v-expansion-panels>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted } from "vue";
import { useRouter } from "vue-router";

import Swal from "sweetalert2";

import { deleteAttributesFromElement } from "@/helpers/ui-utils.js";

import { api as apiModule } from "@/store/api.module";
import { experiment as experimentModule } from "@/store/experiment.module";
import { navigation as navigationModule } from "@/store/navigation.module";

defineOptions({
  name: "ParticipationDistribution"
});

const props = defineProps({
  experiment: {
    type: Object,
    required: true
  }
});

const router = useRouter();

const apiStore = apiModule();
const experimentStore = experimentModule();
const navigationStore = navigationModule();

const editMode = computed(() => {
  return navigationStore.editMode;
});

const saveExitPage = computed(() => {
  return editMode.value?.callerPage?.name || "Home";
});

const saveType = async type => {
  const updatedExperiment = {
    ...props.experiment,
    distributionType: type
  };

  const experimentId =
    updatedExperiment.experimentId;

  const step = "distribution_type";

  try {
    const response =
      await experimentStore.updateExperiment(
        updatedExperiment
      );

    if (response?.status === 200) {
      if (!editMode.value) {
        await apiStore.reportStep({
          experimentId,
          step
        });
      }

      switch (
        updatedExperiment.distributionType
      ) {
        case "EVEN":
          router.push({
            name: "ParticipationSummary",
            params: {
              experiment: experimentId
            }
          });
          break;

        case "CUSTOM":
          router.push({
            name:
              "ParticipationCustomDistribution",
            params: {
              experiment: experimentId
            }
          });
          break;

        case "MANUAL":
          router.push({
            name:
              "ParticipationManualDistribution",
            params: {
              experiment: experimentId
            }
          });
          break;

        default:
          await Swal.fire(
            "Select a distribution type"
          );
      }

      return;
    }

    if (response?.message) {
      await Swal.fire(
        `Error: ${response.message}`
      );
      return;
    }

    await Swal.fire(
      "There was an error saving your experiment."
    );
  } catch (error) {
    console.error(
      "updateExperiment | catch",
      error
    );
  }
};

const saveExit = () => {
  router.push({
    name: saveExitPage.value,
    params: {
      experiment:
        props.experiment.experimentId
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
.v-expansion-panel--disabled {
  opacity: 0.3 !important;
}
</style>