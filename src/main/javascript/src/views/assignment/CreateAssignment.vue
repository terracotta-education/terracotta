<template>
  <div>
    <h1>Create your component</h1>

    <p>
      This will create an unpublished component shell in {{ lmsTitle }} and will
      be the way Terracotta will deliver treatments to students.
    </p>

    <v-row>
      <v-col cols="6">
        <v-text-field
          v-model="title"
          :rules="rules"
          label="Component name"
          variant="outlined"
        />
      </v-col>
    </v-row>

    <v-divider />

    <v-tabs
      v-model="tab"
      class="tabs"
    >
      <v-tab value="settings">
        Settings
      </v-tab>
    </v-tabs>

    <v-divider />

    <v-window v-model="tab">
      <v-window-item
        value="settings"
        class="my-5"
      >
        <AssignmentSettings />
      </v-window-item>
    </v-window>
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

import {
  statusAlert,
  createStatusAlert
} from "@/helpers/ui-utils.js";

import AssignmentSettings from "@/views/assignment/AssignmentSettings.vue";

import { assignment as assignmentModule } from "@/store/assignment.module";
import { treatment as treatmentModule } from "@/store/treatment.module";
import { assessment as assessmentModule } from "@/store/assessment.module";
import { configuration as configurationModule } from "@/store/configuration.module";
import { alert as alertModule } from "@/store/alert.module";

defineOptions({
  name: "CreateAssignment"
});

const route = useRoute();
const router = useRouter();

const assignmentStore = assignmentModule();
const treatmentStore = treatmentModule();
const assessmentStore = assessmentModule();
const configurationStore = configurationModule();
const alertStore = alertModule();

const tab = ref("settings");

const rules = [
  value =>
    value && !!value.trim() ||
    "Component Name is required",
  value =>
    (value || "").length <= 255 ||
    "A maximum of 255 characters is allowed"
];

const assignment = computed(() => {
  return assignmentStore.assignment;
});

const configurations = computed(() => {
  return configurationStore.configurations;
});

const alertStatuses = computed(() => {
  return alertStore.statuses;
});

const experimentId = computed(() => {
  return Number.parseInt(route.params.experimentId, 10);
});

const exposureId = computed(() => {
  return Number.parseInt(route.params.exposureId, 10);
});

const conditionIds = computed(() => {
  return JSON.parse(route.query.conditionIds || "[]");
});

const lmsTitle = computed(() => {
  return configurations.value?.lmsTitle || "LMS";
});

const title = computed({
  get: () => assignment.value?.title || "",

  set: (newTitle) => {
    assignmentStore.setAssignment({
      ...assignment.value,
      title: newTitle
    });
  }
});

const handleSaveAssignment = async () => {
  try {
    const response = await assignmentStore.createAssignment([
      experimentId.value,
      exposureId.value,
      assignment.value,
      1
    ]);

    if (response?.status !== 201) {
      await Swal.fire(
        "There was an error creating the assignment. Please try again."
      );

      createStatusAlert(
        statusAlert(
          alertStatuses.value.error,
          "There was an issue creating the assignment. Please try again."
        )
      );

      return false;
    }

    await handleCreateTreatmentsForAssignment(
      experimentId.value,
      response.data.assignmentId
    );

    createStatusAlert(
      statusAlert(alertStatuses.value.success, "Assignment created successfully.")
    );

    router.push({
      name: "ExperimentSummary",
      params: {
        experimentId: experimentId.value
      }
    });

    return true;
  } catch (error) {
    console.error("createAssignment | catch", { error });

    await Swal.fire(
      "There was an error creating the assignment."
    );

    createStatusAlert(
      statusAlert(
        alertStatuses.value.error,
        "There was an issue creating the assignment. Please try again."
      )
    );

    return false;
  }
};

const handleCreateTreatmentsForAssignment = async (
  currentExperimentId,
  assignmentId
) => {
  try {
    const treatments = await Promise.all(
      conditionIds.value.map(conditionId => {
        return treatmentStore.createTreatment([
          currentExperimentId,
          conditionId,
          assignmentId
        ]);
      })
    );

    await Promise.all(
      treatments.map(treatment => {
        return createAssessmentForTreatment(
          treatment.data.conditionId,
          treatment.data.treatmentId
        );
      })
    );
  } catch (error) {
    console.log(
      "CreateAssignment.handleCreateTreatmentsForAssignment | catch",
      error
    );
  }
};

const createAssessmentForTreatment = async (
  conditionId,
  treatmentId
) => {
  try {
    return await assessmentStore.createAssessment([
      experimentId.value,
      conditionId,
      treatmentId
    ]);
  } catch (error) {
    console.error(
      "handleCreateAssessment | catch",
      { error }
    );

    return null;
  }
};

const saveExit = async () => {
  await handleSaveAssignment();
};

onMounted(() => {
  assignmentStore.setAssignment({
    numOfSubmissions: null
  });
});

defineExpose({
  saveExit
});
</script>
