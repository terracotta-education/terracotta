<template>
  <div>
    <template
      v-if="
        loaded &&
        exposures.length > 0 &&
        assignments
      "
    >
      <h1 class="mb-3">
        Your Components
      </h1>

      <div class="mb-6">
        <v-expansion-panels
          v-for="exposure in exposures"
          :key="exposure.exposureId"
          class="v-expansion-panels--outlined mb-7"
        >
          <v-expansion-panel class="py-3">
            <v-expansion-panel-title>
              <strong>
                {{ exposure.title }}

                <span
                  :class="{
                    'error-text':
                      !assignmentIsBalanced(exposure.exposureId) ||
                      !allComplete(exposure.exposureId)
                  }"
                >
                  ({{ getComplete(exposure.exposureId) }}/{{ getAssignmentsForExposure(exposure.exposureId).length }})
                </span>
              </strong>
            </v-expansion-panel-title>

            <v-expansion-panel-text>
              <v-list class="pa-0 mb-3">
                <v-list-item
                  v-for="assignmentItem in getAssignmentsForExposure(exposure.exposureId)"
                  :key="assignmentItem.assignmentId"
                  class="justify-center px-0"
                >
                  <v-list-item-title>
                    <p class="ma-0 pa-0">
                      {{ assignmentItem.title }}
                      ({{ assignmentItem.treatments?.length || 0 }}/{{ conditions.length || 0 }})
                    </p>
                  </v-list-item-title>

                  <template #append>
                    <v-btn
                      :to="{
                        name: 'AssignmentTreatmentSelect',
                        params: {
                          exposureId: assignmentItem.exposureId,
                          assignmentId: assignmentItem.assignmentId
                        }
                      }"
                      icon="mdi-pencil"
                      variant="outlined"
                    />

                    <v-btn
                      icon="mdi-delete"
                      variant="outlined"
                      @click="handleDeleteAssignment(exposure.exposureId, assignmentItem)"
                    />
                  </template>
                </v-list-item>
              </v-list>

              <div
                v-if="!assignmentIsBalanced(exposure.exposureId)"
                class="error-text mb-3"
              >
                Add a component to balance the experiment
              </div>

              <div
                v-if="!allComplete(exposure.exposureId)"
                class="error-text mb-3"
              >
                Create a treatment for all conditions
              </div>

              <v-btn
                :to="{
                  name: 'AssignmentCreateAssignment',
                  params: {
                    exposureId: Number.parseInt(exposure.exposureId, 10)
                  }
                }"
                elevation="0"
                color="primary"
                class="px-0"
                variant="text"
              >
                add component
              </v-btn>
            </v-expansion-panel-text>
          </v-expansion-panel>
        </v-expansion-panels>

        <v-btn
          :to="{ name: 'ExperimentSummary' }"
          :disabled="finishDisabled"
          elevation="0"
          color="primary"
        >
          Finish
        </v-btn>
      </div>
    </template>
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

import { assignment as assignmentModule } from "@/store/assignment.module";
import { experiment as experimentModule } from "@/store/experiment.module";
import { exposures as exposuresModule } from "@/store/exposures.module";

defineOptions({
  name: "YourAssignments"
});

const props = defineProps({
  experiment: {
    type: Object,
    required: true
  }
});

const route = useRoute();
const router = useRouter();

const assignmentStore = assignmentModule();
const experimentStore = experimentModule();
const exposuresStore = exposuresModule();

const loaded = ref(false);

const exposureId = computed(() => {
  return Number.parseInt(route.params.exposureId, 10);
});

const experimentId = computed(() => {
  return Number.parseInt(props.experiment.experimentId, 10);
});

const assignments = computed(() => {
  return assignmentStore.assignments || [];
});

const conditions = computed(() => {
  return experimentStore.conditions || [];
});

const exposures = computed(() => {
  return exposuresStore.exposures || [];
});

const assignmentCountsByExposure = computed(() => {
  return exposures.value.map(exposure => {
    return getAssignmentsForExposure(exposure.exposureId).length;
  });
});

const shortestLength = computed(() => {
  if (!assignmentCountsByExposure.value.length) {
    return 0;
  }

  return Math.min(...assignmentCountsByExposure.value);
});

const longestLength = computed(() => {
  if (!assignmentCountsByExposure.value.length) {
    return 0;
  }

  return Math.max(...assignmentCountsByExposure.value);
});

const finishDisabled = computed(() => {
  const unbalanced =
    shortestLength.value !== longestLength.value &&
    exposures.value.length !== 1;

  const noAssignments =
    longestLength.value < 1;

  const incompleteTreatments =
    assignments.value.some(assignment => {
      return (
        (assignment.treatments?.length || 0) <
        conditions.value.length
      );
    });

  return (
    unbalanced ||
    noAssignments ||
    incompleteTreatments
  );
});

const getAssignmentsForExposure = exposureIdValue => {
  return assignments.value.filter(assignment => {
    return assignment.exposureId === exposureIdValue;
  });
};

const getComplete = exposureIdValue => {
  return getAssignmentsForExposure(exposureIdValue).filter(assignment => {
    return (
      (assignment.treatments?.length || 0) ===
      conditions.value.length
    );
  }).length;
};

const allComplete = exposureIdValue => {
  return getAssignmentsForExposure(exposureIdValue).every(assignment => {
    return (
      (assignment.treatments?.length || 0) >=
      conditions.value.length
    );
  });
};

const assignmentIsBalanced = exposureIdValue => {
  const currentLength =
    getAssignmentsForExposure(exposureIdValue).length;

  return (
    currentLength > 0 &&
    currentLength >= longestLength.value
  );
};

const handleDeleteAssignment = async (
  exposureIdValue,
  assignmentItem
) => {
  const result = await Swal.fire({
    icon: "question",
    text: `Are you sure you want to delete the assignment "${assignmentItem.title}"?`,
    showCancelButton: true,
    confirmButtonText: "Yes, delete it",
    cancelButtonText: "No, cancel"
  });

  if (!result.isConfirmed) {
    return null;
  }

  try {
    return await assignmentStore.deleteAssignment([
      experimentId.value,
      exposureIdValue,
      assignmentItem.assignmentId
    ]);
  } catch (error) {
    console.error(
      "handleDeleteAssignment | catch",
      { error }
    );

    return null;
  }
};

const saveExit = () => {
  router.push({
    name: "Home"
  });
};

onMounted(async () => {
  await assignmentStore.resetAssignments();

  await exposuresStore.fetchExposures(
    experimentId.value
  );

  for (const exposure of exposures.value) {
    await assignmentStore.fetchAssignmentsByExposure([
      experimentId.value,
      exposure.exposureId
    ]);
  }

  const selectedExposureExists =
    exposures.value.some(exposure => {
      return (
        Number.parseInt(exposure.exposureId, 10) ===
        exposureId.value
      );
    });

  const selectedExposureHasAssignments =
    assignments.value.some(assignment => {
      return (
        Number.parseInt(assignment.exposureId, 10) ===
        exposureId.value
      );
    });

  if (
    exposureId.value &&
    selectedExposureExists &&
    !selectedExposureHasAssignments
  ) {
    router.push({
      name: "AssignmentCreateAssignment",
      params: {
        exposureId: exposureId.value
      }
    });

    return;
  }

  loaded.value = true;
});

defineExpose({
  saveExit
});
</script>

<style lang="scss" scoped>
.error-text {
  color: map.get($red, "base") !important;
}
</style>
