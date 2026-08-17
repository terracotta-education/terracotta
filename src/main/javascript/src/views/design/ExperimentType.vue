<template>
  <div class="experiment-type-container">
    <template v-if="hasConditions">
      <h1 class="mb-5">
        <span>
          You have defined
          <strong>
            {{ numConditions }} condition{{ numConditions === 1 ? "" : "s" }}
          </strong>
        </span>

        <br />
        <br />

        <span>
          How do you want students to be exposed to these different conditions?
        </span>
      </h1>

      <v-expansion-panels
        v-model="expanded"
        class="v-expansion-panels--icon mx-auto w-50"
        multiple
      >
        <v-expansion-panel
          v-for="(panel, index) in panels"
          :key="index"
          :disabled="hasSelectedExposureType && exposureType !== panel.type"
          :class="{
            'panel-not-selected': exposureType !== panel.type,
            'panel-selected': exposureType === panel.type,
            'pre-selection': !hasSelectedExposureType
          }"
          @click.stop
        >
          <v-expansion-panel-title
            hide-actions
          >
            <img
              :src="panel.img.src"
              :alt="panel.img.alt"
            />

            <strong>{{ panel.header }}</strong>
          </v-expansion-panel-title>

          <v-expansion-panel-text>
            <p>{{ panel.body }}</p>

            <v-btn
              v-if="!hasSelectedExposureType || exposureType === panel.type"
              color="primary"
              elevation="0"
              @click="saveType(panel.type)"
            >
              Select
            </v-btn>
          </v-expansion-panel-text>
        </v-expansion-panel>
      </v-expansion-panels>

      <v-card
        v-if="!hasSelectedExposureType"
        class="pt-5 px-5 mx-auto bg-blue-lighten-5 rounded-lg card-warning"
        variant="outlined"
      >
        <p>
          Please note that you will not be able to switch between “All
          conditions” and “Only one condition” after you click SELECT.
        </p>

        <p>
          Additionally, once you click SELECT to leave this screen, you will be
          able to add, but not delete conditions, so please use the back button
          now to double-check that you have included what you need. To change
          your decisions beyond this point, you will need to create a new
          experiment.
        </p>
      </v-card>

      <v-card
        v-if="hasSelectedExposureType"
        class="pt-5 px-5 mx-auto bg-blue-lighten-5 rounded-lg card-warning"
        variant="outlined"
      >
        <p>
          Please note that you are not able to switch between “All conditions”
          and “Only one condition” as you have previously selected a type. To
          change the experiment type, please create a new experiment.
        </p>
      </v-card>
    </template>

    <template v-else>
      <v-alert
        type="error"
        variant="outlined"
      >
        <v-row align="center">
          <v-col class="grow">
            No conditions found
          </v-col>
        </v-row>
      </v-alert>
    </template>
  </div>
</template>

<script setup>
import {
  ref,
  computed,
  onMounted
} from "vue";

import { useRouter } from "vue-router";
import { storeToRefs } from "pinia";
import Swal from "sweetalert2";

import { deleteAttributesFromElement } from "@/helpers/ui-utils.js";

import allConditionsIcon from "@/assets/all_conditions.svg";
import oneConditionIcon from "@/assets/one_condition.svg";

import { api as apiModule } from "@/store/api.module";
import { experiment as experimentModule } from "@/store/experiment.module";
import { navigation as navigationModule } from "@/store/navigation.module";

defineOptions({
  name: "ExperimentType"
});

const router = useRouter();

const apiStore = apiModule();
const experimentStore = experimentModule();
const navigationStore = navigationModule();

const { experiment, conditions } = storeToRefs(experimentStore);

const initialExposureType = ref(null);
const expanded = ref([0, 1]);

const panels = [
  {
    type: "WITHIN",
    img: {
      src: allConditionsIcon,
      alt: "all conditions"
    },
    header: "All conditions",
    body: "All students are exposed to every condition, in different orders. This way you can compare how the different conditions affected each individual student. This is called a within-subject design."
  },
  {
    type: "BETWEEN",
    img: {
      src: oneConditionIcon,
      alt: "only one condition"
    },
    header: "Only one condition",
    body: "Each student is only exposed to one condition, so that you can compare how the different conditions affected different students. This is called a between-subjects design."
  }
];

const editMode = computed(() => {
  return navigationStore.editMode;
});

const exposureType = computed(() => {
  return experiment.value.exposureType;
});

const numConditions = computed(() => {
  return conditions.value.length;
});

const hasConditions = computed(() => {
  return numConditions.value > 0;
});

const exposureTypes = [
  "WITHIN",
  "BETWEEN"
];

const getSaveExitPage = computed(() => {
  return editMode.value?.callerPage?.name || "Home";
});

const hasSelectedExposureType = computed(() => {
  return (
    initialExposureType.value &&
    initialExposureType.value !== "NOSET"
  );
});

const saveType = async type => {
  initialExposureType.value = type;

  const updatedExperiment = {
    ...experiment.value,
    exposureType: type
  };

  const experimentId = updatedExperiment.experimentId;
  const step = "exposure_type";

  try {
    const response = await experimentStore.updateExperiment(updatedExperiment);

    if (response?.status === 200) {
      if (!editMode.value) {
        await apiStore.reportStep({
          experimentId,
          step
        });
      }

      if (exposureTypes.includes(updatedExperiment.exposureType)) {
        router.push({
          name: "ExperimentDesignDefaultCondition",
          params: {
            experiment: experimentId
          }
        });

        return;
      }

      await Swal.fire("Select an experiment type");
      return;
    }

    if (response?.message) {
      await Swal.fire(`Error: ${response.message}`);
      return;
    }

    await Swal.fire("There was an error saving your experiment.");
  } catch (error) {
    console.error("updateExperiment | catch", { error });
    await Swal.fire("There was an error saving the experiment.");
  }
};

const saveExit = async () => {
  router.push({
    name: getSaveExitPage.value,
    params: {
      experimentId: experiment.value.experimentId
    }
  });
};

onMounted(() => {
  initialExposureType.value = experiment.value?.exposureType;

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
.v-expansion-panel {
  margin-bottom: 30px !important;
}

.panel-selected {
  border-color: rgba(3, 169, 244, 1) !important;
}

.panel-not-selected {
  border-color: map.get($grey, "lighter") !important;

  &:not(.pre-selection) {
    opacity: 0.5 !important;
  }
}

.v-theme--light.v-expansion-panels {
  .v-expansion-panel--disabled {
    color: rgba(0, 0, 0, .38);
  }
}

:deep(.v-expansion-panel-title) {
  flex-direction: column;
  pointer-events: none;
}
</style>