<template>
  <div>
    <page-loading
      :display="!isLoaded"
      message="Loading experiments. Please wait."
    />
    <page-loading
      :display="isDeletingExperiment"
      message="Please wait..."
    />
    <zero-state
      v-show="isLoaded && !hasExperiments"
      :experimentExportEnabled="experimentExportEnabled"
      :experimentImportRequests="experimentImportRequests"
      :importRequestAlerts="importRequestAlerts"
      @handleImportExperiment="handleImportExperiment"
      @handleImportRequestAlertDismiss="handleImportRequestAlertDismiss"
      @handleImportRequestAlertVisibilityChange="handleImportRequestAlertVisibilityChange"
      @startExperiment="startExperiment"
    />
    <v-container
      v-show="isLoaded && hasExperiments"
      class="container px-12 py-3"
      fluid
    >
      <v-row
        class="mb-5"
        justify="space-between"
      >
        <v-col
          class="d-flex align-center"
        >
          <v-img
            :src="terracottaLogo"
            alt="Terracotta Logo"
            max-width="138"
          />
        </v-col>
        <v-col
          class="right-side text-right"
        >
          <v-btn
            v-if="experimentExportEnabled"
            :disabled="isExperimentImporting"
            @click="handleImportExperiment"
            color="primary"
            elevation="0"
            class="mr-2"
          >
            Import Experiment
          </v-btn>
          <v-btn
            :disabled="isExperimentImporting"
            @click="startExperiment"
            color="primary"
            elevation="0"
          >
            New Experiment
          </v-btn>
          <help />
        </v-col>
      </v-row>
      <v-row
        v-if="dataExportRequestAlerts.length > 0"
      >
        <div
          v-for="dataExportRequestAlert in dataExportRequestAlerts"
          :key="dataExportRequestAlert.experimentId"
          class="alert-request pb-2 px-3"
        >
          <v-alert
            v-model="experimentDataExportRequests[dataExportRequestAlert.experimentId].showAlert"
            @click:close="handleDataExportRequestAlertDismiss(dataExportRequestAlert.experimentId)"
            :aria-label="`data export request alert for experiment ${dataExportRequestAlert.experimentId}`"
            :type="dataExportRequestAlert.type"
            elevation="0"
            role="alert"
            closable
            variant="outlined"
          >
            {{ dataExportRequestAlert.text }}
            <a
              v-if="dataExportRequestAlert.showDownloadLink"
              @click="handleAlertDataExportDownloadRequest(dataExportRequestAlert.experimentId)"
              aria-label="download export file for experiment"
            >
              <b><i>Click here to download</i></b>.
            </a>
            <a
              v-if="dataExportRequestAlert.showRecreateLink"
              @click="handleDataExportRequest(dataExportRequestAlert.experimentId)"
              aria-label="request new data export for experiment"
            >
              <b><i>Click here to download a new data export</i></b>.
            </a>
          </v-alert>
        </div>
      </v-row>
      <v-row
        v-if="importRequestAlerts.length > 0"
        class="pb-2"
      >
        <div
          v-for="importRequestAlert in importRequestAlerts"
          :key="importRequestAlert.id"
          class="alert-request pb-2 px-3"
        >
          <v-alert
            v-if="experimentImportRequests[importRequestAlert.id]"
            v-model="experimentImportRequests[importRequestAlert.id].showAlert"
            @click:close="handleImportRequestAlertDismiss(importRequestAlert.id)"
            :type="importRequestAlert.type"
            elevation="0"
            role="alert"
            closable
            variant="outlined"
          >
            {{ importRequestAlert.text }}
            <ul
              v-if="importRequestAlert.showErrors"
            >
              <li
                v-for="(error, i) in importRequestAlert.errors"
                :key="i"
              >
                {{ error }}
              </li>
            </ul>
          </v-alert>
        </div>
      </v-row>
      <v-row>
        <v-col
          id="terracotta-main"
          tabindex="-1"
          cols="12"
        >
          <h1
            class="mb-3"
          >
            Experiments
          </h1>
          <p>
            All your experiments for this course are here. You can also start a new one or import an existing experiment.
            <br />
            Need a hand? Check out our
            <a
              href="https://www.terracotta.education/help-center/quick-start-guide"
              target="_blank"
              rel="noopener"
              class="user-help-link"
            >Quick Start Guide</a>
            or explore the
            <a
              href="https://terracotta-education.atlassian.net/wiki/spaces/TC/overview"
              target="_blank"
              rel="noopener"
              class="user-help-link"
            >Knowledge Base</a>
            for tips and troubleshooting.
          </p>
          <v-data-table
            :headers="headers"
            :items="experiments || []"
            class="table-experiments v-data-table-alt"
            density="comfortable"
            hover
          >
            <template
              v-slot:item.title="{ item }"
            >
              <button
                v-if="item"
                class="v-data-table__link"
                @click="handleNavigate(item.experimentId)"
              >
                <template
                  v-if="item.title"
                >
                  {{ item.title }}
                </template>
                <template
                  v-else
                >
                  <em>No Title</em>
                </template>
              </button>
            </template>
            <template
              v-slot:item.createdAt="{ item }"
            >
              <span
                v-if="item.createdAt"
              >
                {{ formatDate(item.createdAt) }}
              </span>
            </template>
            <template
              v-slot:item.actions="{ item }"
            >
              <v-menu>
                <template #activator="{ props }">
                  <v-icon
                    color="black"
                    v-bind="props"
                    :aria-label="`actions for experiment ${item.title}`"
                  >
                    mdi-dots-horizontal
                  </v-icon>
                </template>
                <v-list density="compact">
                  <v-list-item
                    v-if="experimentExportEnabled"
                    @click="handleExportExperiment(item)"
                    :disabled="isExportingExperiment"
                    :aria-label="`export experiment ${item.title}`"
                    prepend-icon="mdi-briefcase-download"
                    title="Export Experiment"
                  />
                  <v-list-item
                    @click="handleDataExportRequest(item.experimentId)"
                    :aria-label="`export experiment results ${item.title}`"
                    prepend-icon="mdi-download"
                    title="Export Results"
                  />
                  <v-tooltip
                    :disabled="!item.started"
                    location="top"
                  >
                    <template #activator="{ props: tooltipProps }">
                      <span v-bind="tooltipProps">
                        <v-list-item
                          @click="handleDelete(item)"
                          :aria-label="`delete experiment ${item.title}`"
                          :disabled="item.started"
                          prepend-icon="mdi-delete"
                          title="Delete"
                        />
                      </span>
                    </template>
                    <span>You cannot delete this experiment because at least one student has completed an assignment.</span>
                  </v-tooltip>
                </v-list>
              </v-menu>
            </template>
          </v-data-table>
        </v-col>
      </v-row>
    </v-container>
  </div>
</template>

<script setup>
import {
  ref,
  computed,
  watch,
  onMounted,
  onBeforeUnmount,
  nextTick
} from "vue";

import { useRouter, onBeforeRouteLeave } from "vue-router";
import dayjs from "@/plugins/dayjs";
import Swal from "sweetalert2";
import terracottaLogo from "@/assets/terracotta_logo.svg";

import {
  getColor,
  deleteAttributesFromElement,
  addAttributesToElement,
  getAttributeFromElement
} from "@/helpers/ui-utils.js";

import Help from "@/components/Help.vue";
import PageLoading from "@/components/PageLoading.vue";
import ZeroState from "@/views/ZeroState.vue";

import { experiment as experimentModule } from "@/store/experiment.module";
import { dataExportRequest as dataExportRequestModule } from "@/store/experiment-data-export.module";
import { configuration as configurationModule } from "@/store/configuration.module";
import { consent as consentModule } from "@/store/consent.module";
import { assessment as assessmentModule } from "@/store/assessment.module";
import { assignment as assignmentModule } from "@/store/assignment.module";
import { condition as conditionModule } from "@/store/condition.module";
import { exposures as exposuresModule } from "@/store/exposures.module";
import { outcome as outcomeModule } from "@/store/outcome.module";
import { participants as participantsModule } from "@/store/participants.module";
import { resultsDashboard as resultsDashboardModule } from "@/store/dashboard/results.module";
import { submission as submissionModule } from "@/store/submission.module";
import { treatment as treatmentModule } from "@/store/treatment.module";
import { navigation as navigationModule } from "@/store/navigation.module";
import { container as messagingContainerModule } from "@/store/messaging/container.module";
import { conditionaltext as messagingConditionalTextModule } from "@/store/messaging/conditionaltext.module";

defineOptions({
  name: "HomePage"
});

const router = useRouter();

const experimentStore = experimentModule();
const dataExportRequestStore = dataExportRequestModule();
const configurationStore = configurationModule();
const consentStore = consentModule();
const assessmentStore = assessmentModule();
const assignmentStore = assignmentModule();
const conditionStore = conditionModule();
const exposuresStore = exposuresModule();
const outcomeStore = outcomeModule();
const participantsStore = participantsModule();
const resultsDashboardStore = resultsDashboardModule();
const submissionStore = submissionModule();
const treatmentStore = treatmentModule();
const navigationStore = navigationModule();
const messagingContainerStore = messagingContainerModule();
const messagingConditionalTextStore = messagingConditionalTextModule();

const headers = [
  { title: "Experiment name", key: "title" },
  { title: "Created", key: "createdAt" },
  { title: "Actions", key: "actions", sortable: false }
];

const isLoaded = ref(false);
const isExportingExperiment = ref(false);
const isDeletingExperiment = ref(false);

const experimentDataExportRequests = ref({
  downloadLinkClicked: false
});

const experimentImportRequests = ref({});

const experiments = computed(() => experimentStore.experiments);
const dataExportRequests = computed(() => dataExportRequestStore.dataExportRequests);
const importRequests = computed(() => experimentStore.importRequests);
const configurations = computed(() => configurationStore.get);

const hasExperiments = computed(() => {
  return experiments.value && experiments.value.length > 0;
});

const experimentExportEnabled = computed(() => {
  return configurations.value?.experimentExportEnabled;
});

const isExperimentImporting = computed(() => {
  return Object.values(experimentImportRequests.value).some(
    request => request.polling.active
  );
});

const dataExportRequestAlerts = computed(() => {
  const experimentsToShow = [];

  for (const experimentId in experimentDataExportRequests.value) {
    if (!experimentDataExportRequests.value[experimentId].showAlert) {
      continue;
    }

    const request = dataExportRequest(experimentId);

    if (request?.ready) {
      experimentsToShow.push({
        experimentId: request.experimentId,
        showDownloadLink: true,
        showRecreateLink: false,
        text: `Your data export for experiment "${request.experimentTitle}" is ready.`,
        type: "success"
      });
      continue;
    }

    if (request?.processing || request?.reprocessing) {
      experimentsToShow.push({
        experimentId: request.experimentId,
        showDownloadLink: false,
        showRecreateLink: false,
        text: `The data export for experiment "${request.experimentTitle}" is being processed. Please do not navigate away from this page.`,
        type: "info"
      });
      continue;
    }

    if (request?.outdated) {
      experimentsToShow.push({
        experimentId: request.experimentId,
        showDownloadLink: false,
        showRecreateLink: true,
        text: `There have been updates since the last requested data export for experiment "${request.experimentTitle}".`,
        type: "warning"
      });
      continue;
    }

    if (request?.error) {
      experimentsToShow.push({
        experimentId: request.experimentId,
        showDownloadLink: false,
        showRecreateLink: false,
        text: `There was an error processing the requested data export for experiment "${request.experimentTitle}". Please try again or contact support.`,
        type: "error"
      });
    }
  }

  return experimentsToShow;
});

const importRequestAlerts = computed(() => {
  const experimentsToShow = [];

  for (const id in experimentImportRequests.value) {
    const request = importRequest(id);

    if (request?.complete) {
      experimentsToShow.push({
        id,
        showAlert: true,
        text: `Your import of experiment "${request.sourceTitle}" is complete. The new title is "${request.importedTitle}".`,
        type: "success",
        showErrors: false
      });
      continue;
    }

    if (request?.processing) {
      experimentsToShow.push({
        id,
        showAlert: true,
        text: `Your import of experiment "${request.sourceTitle}" is being processed. Please do not navigate away from this page.`,
        type: "info",
        showErrors: false
      });
      continue;
    }

    if (request?.error) {
      const errorMessages = request.errorMessages || [];

      experimentsToShow.push({
        id,
        showAlert: true,
        text: `There were errors in processing the import of experiment "${request.sourceTitle}". Please try again or contact support. ${errorMessages.length > 0 ? "Errors: " : ""}`,
        type: "error",
        errors: errorMessages.slice(0, 3).map(errorMessage => errorMessage.text),
        showErrors: errorMessages.length > 0
      });
    }
  }

  return experimentsToShow;
});

const formatDate = date => {
  return date ? dayjs(date).fromNow() : "";
};

const dataExportRequest = experimentId => {
  return dataExportRequests.value?.find(
    request => request.experimentId === parseInt(experimentId)
  );
};

const importRequest = id => {
  return importRequests.value.find(request => request.id === id);
};

const experimentImportRequest = id => {
  return experimentImportRequests.value[id];
};

const handleNavigate = experimentId => {
  const selectedExperiment = experiments.value.find(
    experiment => experiment.experimentId === experimentId
  );

  const {
    exposureType,
    participationType,
    distributionType
  } = selectedExperiment;

  const isExperimentIncomplete = [
    exposureType,
    participationType,
    distributionType
  ].some(value => value === "NOSET");

  router.push({
    name: isExperimentIncomplete
      ? "ExperimentDesignIntro"
      : "ExperimentSummary",
    params: {
      experimentId: experimentId
    }
  });
};

const startExperiment = async () => {
  try {
    const response = await experimentStore.createExperiment();

    if (response?.data?.experimentId) {
      router.push({
        name: "ExperimentDesignIntro",
        params: {
          experimentId: response.data.experimentId
        }
      });
      return;
    }

    await Swal.fire({
      text: `Error Status: ${response?.status} - There was an issue creating an experiment`,
      icon: "error"
    });
  } catch (error) {
    console.log("startExperiment -> createExperiment | catch", { error });
  }
};

const handleExportExperiment = async item => {
  isExportingExperiment.value = true;

  try {
    await experimentStore.exportExperiment(item.experimentId);
  } finally {
    isExportingExperiment.value = false;
  }
};

const handleImportExperiment = async () => {
  const { value: file } = await Swal.fire({
    title: "Import experiment from file",
    text: "Please select the experiment file to import",
    input: "file",
    inputAttributes: {
      accept: ".zip"
    },
    showCancelButton: true,
    confirmButtonText: "Import",
    cancelButtonText: "Cancel"
  });

  if (!file) {
    return;
  }

  const newImport = await experimentStore.importExperiment(file);
  const request = importRequest(newImport.id);

  experimentImportRequests.value = {
    ...experimentImportRequests.value,
    [newImport.id]: {
      showAlert: true,
      polling: {
        active: request.processing,
        id: null
      }
    }
  };
};

const handleDelete = async experiment => {
  if (!experiment?.experimentId) {
    return;
  }

  const reallyDelete = await Swal.fire({
    icon: "question",
    text: `Do you really want to delete "${experiment.title}"?`,
    showCancelButton: true,
    confirmButtonText: "Yes, delete it",
    cancelButtonText: "No, cancel",
    cancelButtonColor: getColor("--swal-cancel")
  });

  if (!reallyDelete.isConfirmed) {
    return;
  }

  isDeletingExperiment.value = true;

  try {
    await experimentStore.deleteExperiment(experiment.experimentId);
  } catch {
    await Swal.fire({
      text: "Could not delete experiment.",
      icon: "error"
    });
  } finally {
    isDeletingExperiment.value = false;
  }
};

const handleAlertDataExportDownloadRequest = async experimentId => {
  experimentDataExportRequests.value.downloadLinkClicked = true;
  await handleDataExportRequest(experimentId);
};

const handleDataExportRequest = async experimentId => {
  let request = dataExportRequest(experimentId);

  await dataExportRequestStore.poll([
    experimentId,
    request
      ? request.ready || request?.readyAcknowledged || request.downloaded
      : false
  ]);

  request = dataExportRequest(experimentId);

  if (request?.ready || request?.readyAcknowledged || request?.downloaded) {
    await dataExportRequestStore.retrieve([
      experimentId,
      request
    ]);

    if (request?.ready || request?.readyAcknowledged || request?.downloaded) {
      return;
    }
  }

  if (request?.processing) {
    await Swal.fire({
      icon: "info",
      text: `The data export for experiment "${request.experimentTitle}" is still being processed. You will be notified when the export is ready for download.
        Please do not navigate away from this page.`,
      confirmButtonText: "OK"
    });
    return;
  }

  if (request?.reprocessing) {
    await Swal.fire({
      icon: "info",
      text: `New submissons have occurred since the requested set of exported data for experiment "${request.experimentTitle}" was processed. A new export is being created.
        You will be notified when the export is ready for download. Please do not navigate away from this page.`,
      confirmButtonText: "OK"
    });
    return;
  }

  const confirmation = await Swal.fire({
    icon: "info",
    text: `Depending on its size, it could take several minutes to retrieve your data export.
      You will see an alert when the export is ready to download. After you click "ok," please stay on this page until your download is ready.`,
    showCancelButton: true,
    confirmButtonText: "OK"
  });

  if (!confirmation.isConfirmed) {
    return;
  }

  await dataExportRequestStore.prepare([experimentId]);

  request = dataExportRequest(experimentId);

  experimentDataExportRequests.value = {
    ...experimentDataExportRequests.value,
    [experimentId]: {
      showAlert: request?.processing || request?.reprocessing,
      polling: {
        active: true,
        id: null
      }
    }
  };
};

const handleDataExportRequestPolling = async experimentId => {
  await dataExportRequestStore.poll([
    experimentId,
    false
  ]);

  const request = dataExportRequest(experimentId);

  experimentDataExportRequests.value = {
    ...experimentDataExportRequests.value,
    [experimentId]: {
      showAlert:
        request.ready ||
        request.error ||
        request.processing ||
        request.reprocessing ||
        request.outdated,
      polling: {
        ...experimentDataExportRequests.value[experimentId].polling,
        active: request.processing || request.reprocessing
      }
    }
  };
};

const handleDataExportRequestAlertDismiss = async experimentId => {
  let request = dataExportRequest(experimentId);

  if (request?.processing || request?.reprocessing) {
    experimentDataExportRequests.value = {
      ...experimentDataExportRequests.value,
      [experimentId]: {
        showAlert: false,
        polling: {
          active: experimentDataExportRequests.value[experimentId].polling.active,
          id: experimentDataExportRequests.value[experimentId].polling.id
        }
      }
    };
    return;
  }

  experimentDataExportRequests.value = {
    ...experimentDataExportRequests.value,
    [experimentId]: {
      showAlert: false,
      polling: {
        active: false,
        id: experimentDataExportRequests.value[experimentId].polling.id
      }
    }
  };

  experimentDataExportRequests.value.downloadLinkClicked = false;
  request = dataExportRequest(experimentId);

  if (request?.error) {
    dataExportRequestStore.acknowledge([
      experimentId,
      request.id,
      "ERROR_ACKNOWLEDGED"
    ]);
  }

  if (request?.ready) {
    dataExportRequestStore.acknowledge([
      experimentId,
      request.id,
      "READY_ACKNOWLEDGED"
    ]);
  }

  if (request?.outdated) {
    dataExportRequestStore.acknowledge([
      experimentId,
      request.id,
      "OUTDATED_ACKNOWLEDGED"
    ]);
  }
};

const handleImportRequestPolling = async id => {
  await experimentStore.pollImport([id]);

  const request = importRequest(id);
  const currentRequest = experimentImportRequest(id);

  experimentImportRequests.value = {
    ...experimentImportRequests.value,
    [id]: {
      ...currentRequest,
      showAlert: request.complete || request.error || request.processing,
      polling: {
        ...currentRequest.polling,
        active: request.processing
      }
    }
  };

  if (request?.complete) {
    await experimentStore.fetchExperiments();
  }
};

const handleImportRequestAlertDismiss = async id => {
  const request = importRequest(id);

  if (request?.processing) {
    return;
  }

  if (request?.complete) {
    await experimentStore.acknowledgeImport([
      id,
      "COMPLETE_ACKNOWLEDGED"
    ]);
  }

  if (request?.error) {
    await experimentStore.acknowledgeImport([
      id,
      "ERROR_ACKNOWLEDGED"
    ]);
  }

  const updatedRequests = { ...experimentImportRequests.value };
  delete updatedRequests[id];
  experimentImportRequests.value = updatedRequests;
};

const handleImportRequestAlertVisibilityChange = (id, showAlert) => {
  if (!experimentImportRequests.value[id]) {
    return;
  }

  experimentImportRequests.value = {
    ...experimentImportRequests.value,
    [id]: {
      ...experimentImportRequests.value[id],
      showAlert
    }
  };
};

watch(
  hasExperiments,
  async () => {
    await nextTick();

    if (!hasExperiments.value) {
      isLoaded.value = true;
      return;
    }

    const table = document.querySelector(".table-experiments");

    if (!table) {
      isLoaded.value = true;
      return;
    }

    const sortableColumns = table.querySelectorAll("th.sortable > span:not(.v-icon)");

    sortableColumns.forEach(column => {
      column.setAttribute("tabindex", "0");
      column.addEventListener("keyup", event => {
        if (event.key !== "Enter") {
          return;
        }

        event.target.click();
      });
    });
  }
);

watch(
  experimentDataExportRequests,
  requests => {
    for (const experimentId in requests) {
      const request = requests[experimentId];

      if (!request.polling) {
        continue;
      }

      if (request.polling.active && !request.polling.id) {
        request.polling.id = window.setInterval(() => {
          handleDataExportRequestPolling(experimentId);
        }, 5000);
      } else if (!request.polling.active && request.polling.id) {
        request.polling.id = window.clearInterval(request.polling.id);
      }
    }
  },
  { deep: true }
);

watch(
  experimentImportRequests,
  requests => {
    for (const id in requests) {
      const request = requests[id];

      if (request.polling.active && request.polling.id === null) {
        request.polling.id = window.setInterval(() => {
          handleImportRequestPolling(id);
        }, 5000);
      } else if (!request.polling.active && request.polling.id !== null) {
        request.polling.id = window.clearInterval(request.polling.id);
      }
    }
  },
  { deep: true }
);

watch(
  experiments,
  () => {
    const ariaOwnsId = getAttributeFromElement(
      ".v-data-table-footer__items-per-page .v-select .v-field:first-of-type",
      "aria-owns"
    );

    deleteAttributesFromElement(
      ".v-data-table-footer__items-per-page .v-select .v-field",
      ["role"]
    );

    addAttributesToElement(
      ".v-data-table-footer__items-per-page .v-select .v-field",
      [
        { name: "role", value: "combobox" },
        { name: "aria-controls", value: ariaOwnsId }
      ]
    );
  },
  { immediate: true }
);

onMounted(async () => {
  await consentStore.resetConsent();

  assessmentStore.resetAssessments();
  assignmentStore.resetAssignments();
  assignmentStore.resetAssignment();
  conditionStore.resetConditions();
  dataExportRequestStore.resetExportData?.();
  exposuresStore.resetExposures();
  outcomeStore.resetOutcome();
  outcomeStore.resetOutcomePotentials();
  participantsStore.resetParticipants();
  resultsDashboardStore.resetResultsDashboard();
  submissionStore.resetSubmissions();
  treatmentStore.resetTreatments();
  navigationStore.deleteEditMode();
  dataExportRequestStore.reset();
  experimentStore.resetImportRequests();
  messagingContainerStore.reset();
  messagingConditionalTextStore.reset();

  await experimentStore.fetchExperiments();

  if (experiments.value && experiments.value.length > 0) {
    await dataExportRequestStore.pollList([
      experiments.value.map(experiment => experiment.experimentId),
      false
    ]);

    experiments.value.forEach(experiment => {
      const experimentId = experiment.experimentId;
      const request = dataExportRequest(experimentId);

      experimentDataExportRequests.value = {
        ...experimentDataExportRequests.value,
        [experimentId]: {
          showAlert: request
            ? request.ready ||
              request.processing ||
              request.reprocessing ||
              request.outdated ||
              request.error
            : false,
          polling: {
            active: false,
            id: null
          }
        }
      };
    });
  }

  await experimentStore.pollImports();

  importRequests.value.forEach(request => {
    experimentImportRequests.value = {
      ...experimentImportRequests.value,
      [request.id]: {
        showAlert: true,
        polling: {
          active: request.processing,
          id: null
        }
      }
    };
  });

  isLoaded.value = true;
});

onBeforeUnmount(() => {
  for (const experimentId in experimentDataExportRequests.value) {
    const request = experimentDataExportRequests.value[experimentId];

    if (request?.polling?.id) {
      window.clearInterval(request.polling.id);
    }
  }
});

onBeforeRouteLeave((to, from, next) => {
  for (const id in experimentImportRequests.value) {
    const request = experimentImportRequests.value[id];

    if (request.polling.id !== null) {
      window.clearInterval(request.polling.id);
    }
  }

  next();
});
</script>

<style lang="scss">
// scoped to this page's own table (not .v-data-table generally) - unscoped, this used
// to leak globally into every v-data-table in the app (this <style> block isn't
// `scoped`), overriding e.g. ComponentTable.vue's .label-treatment-incomplete red text
// with black via higher selector specificity.
.table-experiments {
  *:not(.v-icon) {
    color: black !important;
    font-size: 16px !important;
  }
}
.v-data-table {
  &__link {
    text-decoration: none;
    background-color: transparent;
    border-style: none;
    &:focus,
    &:hover {
      text-decoration: underline;
    }
  }
  .v-data-table-footer {
    border-top: none !important;
  }
}
.v-tooltip > .v-overlay__content {
  max-width: 400px;
  opacity: 1.0 !important;
  background-color: rgba(55,61,63, 1.0) !important;
  color: #fff !important;
  a {
    color: map.get($blue, "light");
  }
}
.alert-request {
  min-width: 100%;
  z-index: 1000;
  > .v-alert {
    margin: 0 auto;
    & a {
      color: inherit !important;
      cursor: pointer;
    }
  }
}
a {
  &.user-help-link {
    color: rgba(0, 0, 0, .87) !important;
  }
}
.table-experiments {
  > hr.v-divider {
    display: none;
  }
}
.right-side {
  min-width: fit-content;
}
</style>
