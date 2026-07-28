<template>
  <div>
    <v-row>
      <v-col cols="6">
        <v-text-field
          v-model="title"
          label="Component name"
          variant="outlined"
        />
      </v-col>
    </v-row>

    <p>
      This will create an unpublished component shell in {{ lmsTitle }} and will
      be the way Terracotta will deliver treatments to students.
    </p>

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

    <v-btn
      v-if="!editMode"
      :disabled="contDisabled"
      elevation="0"
      color="primary"
      class="mr-4"
      @click="saveNext('AssignmentYourAssignments')"
    >
      Continue
    </v-btn>
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

import { statusAlert, createStatusAlert } from "@/helpers/ui-utils";

import AssignmentSettings from "@/views/assignment/AssignmentSettings.vue";

import { assignment as assignmentModule } from "@/store/assignment.module";
import { navigation as navigationModule } from "@/store/navigation.module";
import { configuration as configurationModule } from "@/store/configuration.module";
import { alert as alertModule } from "@/store/alert.module";

defineOptions({
  name: "AssignmentEditor"
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
const navigationStore = navigationModule();
const configurationStore = configurationModule();
const alertStore = alertModule();

const tab = ref("settings");

const assignment = computed(() => {
  return assignmentStore.assignment;
});

const title = computed({
  get: () => assignment.value?.title || "",

  set: newTitle => {
    if (!assignment.value) {
      return;
    }

    assignment.value.title = newTitle;
  }
});

const editMode = computed(() => {
  return navigationStore.editMode;
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

const assignmentId = computed(() => {
  return Number.parseInt(route.params.assignmentId, 10);
});

const exposureId = computed(() => {
  return Number.parseInt(route.params.exposureId, 10);
});

const resolvedExposureId = computed(() => {
  return Number.isNaN(exposureId.value)
    ? route.params.exposureId
    : exposureId.value;
});

const contDisabled = computed(() => {
  return !assignment.value?.title;
});

const getSaveExitPage = computed(() => {
  return editMode.value?.callerPage?.name || "Home";
});

const lmsTitle = computed(() => {
  return configurations.value?.lmsTitle || "LMS";
});

const handleSaveAssignment = async () => {
  const response = await assignmentStore.updateAssignment([
    experimentId.value,
    exposureId.value,
    assignmentId.value,
    {
      ...assignment.value
    }
  ]);

  if (response?.status === 400) {
    await Swal.fire(response.data);
    return false;
  }

  return response;
};

const saveExit = async () => {
  if (contDisabled.value) {
    return;
  }

  const savedAssignment = await handleSaveAssignment();

  if (!savedAssignment) {
    return;
  }

  createStatusAlert(
    statusAlert(alertStatuses.value.success, "Assignment saved successfully.")
  );

  router.push({
    name: getSaveExitPage.value,
    params: {
      experiment: props.experiment.experimentId
    }
  });
};

const saveNext = async routeName => {
  const savedAssignment = await handleSaveAssignment();

  if (!savedAssignment) {
    return;
  }

  createStatusAlert(
    statusAlert(alertStatuses.value.success, "Assignment saved successfully.")
  );

  router.push({
    name: routeName,
    params: {
      experiment: props.experiment.experimentId,
      exposureId: resolvedExposureId.value
    }
  });
};

onMounted(async () => {
  await assignmentStore.fetchAssignment([
    props.experiment.experimentId,
    exposureId.value,
    assignmentId.value
  ]);
});

defineExpose({
  saveExit
});
</script>
