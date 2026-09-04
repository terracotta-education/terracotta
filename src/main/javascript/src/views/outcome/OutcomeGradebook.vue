<template>
  <div
    v-if="experiment && exposureId"
    class="outcome-gradebook-container"
  >
    <PageLoading
      v-if="!isLoaded"
      :display="!isLoaded"
      :message="`Loading gradebook items from ${lmsTitle}. Please wait.`"
    />

    <h1 class="mb-6">
      Select gradebook item(s)
    </h1>

    <form @submit.prevent="saveExit">
      <v-table class="mb-9 v-data-table--light-header">
        <thead>
          <tr>
            <th style="width: 50px;">
              <v-checkbox
                v-model="selectAll"
                color="primary"
                aria-label="Select all gradebook items"
                hide-details
                @update:model-value="handleSelectAll"
              />
            </th>

            <th class="text-left">
              Gradebook Item
            </th>

            <th
              class="text-left"
              style="width: 250px;"
            >
              Total Points
            </th>
          </tr>
        </thead>

        <tbody>
          <tr
            v-for="outcomePotential in outcomePotentials"
            :key="outcomePotential.assignmentId"
          >
            <td>
              <template v-if="!hasExistingOutcome(outcomePotential)">
                <v-checkbox
                  v-model="selectedAssignmentIds"
                  :value="outcomePotential.assignmentId"
                  :aria-label="`Select ${outcomePotential.name} for outcome creation`"
                  hide-details
                />
              </template>

              <template v-else>
                <v-icon>mdi-check</v-icon>
              </template>
            </td>

            <td>{{ outcomePotential.name }}</td>
            <td>{{ outcomePotential.pointsPossible }}</td>
          </tr>
        </tbody>
      </v-table>
    </form>
  </div>
</template>

<script setup>
import {
  ref,
  computed,
  onMounted
} from "vue";

import {
  useRoute,
  useRouter
} from "vue-router";

import Swal from "sweetalert2";

import { statusAlert } from "@/helpers/ui-utils.js";
import PageLoading from "@/components/PageLoading.vue";

import { experiment as experimentModule } from "@/store/experiment.module";
import { outcome as outcomeModule } from "@/store/outcome.module";
import { alert as alertModule } from "@/store/alert.module";
import { configuration as configurationModule } from "@/store/configuration.module";

defineOptions({
  name: "OutcomeGradebook"
});

const route = useRoute();
const router = useRouter();

const experimentStore = experimentModule();
const outcomeStore = outcomeModule();
const alertStore = alertModule();
const configurationStore = configurationModule();

const selectedAssignmentIds = ref([]);
const selectAll = ref(false);
const isLoaded = ref(false);

const experiment = computed(() => {
  return experimentStore.experiment;
});

const outcomePotentials = computed(() => {
  return outcomeStore.outcomePotentials || [];
});

const outcomes = computed(() => {
  return outcomeStore.outcomes || [];
});

const alertStatuses = computed(() => {
  return alertStore.statuses;
});

const configurations = computed(() => {
  return configurationStore.configurations;
});

const exposureId = computed(() => {
  return Number.parseInt(route.params.exposureId, 10);
});

const experimentId = computed(() => {
  return Number.parseInt(route.params.experimentId, 10);
});

const lmsTitle = computed(() => {
  return configurations.value?.lmsTitle || "your LMS";
});

const hasExistingOutcome = outcomePotential => {
  return outcomes.value.some(outcome => {
    return (
      outcome.lmsOutcomeId === outcomePotential.assignmentId &&
      outcome.exposureId === exposureId.value
    );
  });
};

const selectableOutcomePotentials = computed(() => {
  return outcomePotentials.value.filter(
    outcomePotential => !hasExistingOutcome(outcomePotential)
  );
});

const handleSelectAll = () => {
  selectedAssignmentIds.value = selectAll.value
    ? selectableOutcomePotentials.value.map(
        outcomePotential => outcomePotential.assignmentId
      )
    : [];
};

const saveExit = async () => {
  try {
    const results = await Promise.all(
      selectedAssignmentIds.value.map(assignmentId => {
        const outcomePotential = outcomePotentials.value.find(outcome => {
          return Number.parseInt(outcome.assignmentId, 10) ===
            Number.parseInt(assignmentId, 10);
        });

        return outcomeStore.createOutcome([
          experimentId.value,
          exposureId.value,
          outcomePotential.name,
          outcomePotential.pointsPossible,
          true,
          outcomePotential.type,
          Number.parseInt(assignmentId, 10)
        ]);
      })
    );

    if (results.some(result => !result)) {
      throw new Error("Failed to create one or more outcomes");
    }

    let params = {};

    if (selectedAssignmentIds.value.length > 0) {
      params = {
        ...statusAlert(
          alertStatuses.value.success,
          "Outcomes created successfully."
        )
      };
    }

    router.push({
      name: "ExperimentSummary",
      params
    });
  } catch {
    Swal.fire({
      text: "An error occurred while creating outcomes. Please try again.",
      icon: "error"
    });
  }
};

onMounted(async () => {
  await outcomeStore.resetOutcomePotentials();
  await outcomeStore.fetchOutcomes([
    experimentId.value,
    exposureId.value
  ]);
  await outcomeStore.fetchOutcomePotentials(
    experimentId.value
  );

  isLoaded.value = true;
});

defineExpose({
  saveExit
});
</script>
