<template>
  <div>
    <v-container v-if="experiment" fluid>
      <v-row
        class="sticky my-1"
        justify="space-between"
      >
        <v-col
          class="col-experiment-title d-flex align-center"
          cols="8"
        >
          <div class="header ma-0 pa-0">
            <v-img
              :src="terracottaLogoMark"
              class="mr-6"
              alt="Terracotta Logo"
              width="26"
              height="30"
              max-width="26"
            />
            <span>{{ experiment.title }}</span>
          </div>
        </v-col>

        <div class="header ma-0 pa-0">
          <v-btn
            @click="handleDataExportRequest"
            :disabled="experimentDataExportRequest.polling.active"
            color="primary"
            elevation="0"
            class="mx-1"
          >
            Export Data
          </v-btn>

          <v-btn
            v-if="experimentExportEnabled"
            @click="handleExperimentExport"
            color="primary"
            elevation="0"
            class="mx-1"
          >
            Export Experiment
          </v-btn>

          <Help />

          <v-btn
            @click="saveExit"
            color="primary"
            elevation="0"
            class="saveButton ml-4"
          >
            SAVE & EXIT
          </v-btn>
        </div>
      </v-row>

      <v-row v-if="experimentDataExportRequest.showAlert">
        <div
          class="alert-data-export-request pb-2 px-3"
        >
          <v-alert
            v-model="experimentDataExportRequest.showAlert"
            :type="dataExportRequestAlert.type"
            :color="dataExportRequestAlert.color"
            @update:model-value="handleDataExportRequestAlertDismiss"
            elevation="0"
            closable
            variant="outlined"
          >
            {{ dataExportRequestAlert.text }}

            <a
              v-if="dataExportRequestAlert.showDownloadLink"
              @click="handleAlertDataExportDownloadRequest"
            >
              <b><i>Click here to download</i></b>.
            </a>

            <a
              v-if="dataExportRequestAlert.showRecreateLink"
              @click="handleDataExportRequest"
            >
              <b><i>Click here to download a new data export</i></b>.
            </a>
          </v-alert>
        </div>
      </v-row>

      <v-row>
        <v-col cols="12">
          <v-divider />

          <v-tabs
            v-model="tab"
            elevation="0"
            :show-arrows="true"
          >
            <v-tab
              v-for="item in setupTabs"
              :key="item.tab"
              :value="item.tab"
            >
              {{ item.tab }}
            </v-tab>
          </v-tabs>

          <v-divider />

          <v-window v-model="tab">
            <v-window-item
              v-for="item in setupTabs"
              :key="item.tab"
              :value="item.tab"
              :class="item.tab"
              class="tab-section pt-6"
            >
              <div class="tab-heading">
                <v-card
                  v-if="hasPublishedAssignment && item.tab !== 'results'"
                  :key="item.title"
                  class="pt-5 px-5 mx-auto published-note rounded-lg"
                  variant="outlined"
                >
                  <p class="text">
                    <strong>Note:</strong>
                    You are currently collecting component submissions. Some setup functionality may not be available to avoid disrupting the experiment.
                  </p>
                </v-card>

                <div
                  v-if="item.tab !== 'results'"
                  class="container-section-summary px-5"
                >
                  <div class="panel-overview py-6">
                    <div class="panel-information d-flex flex-column justify-center">
                      <div
                        class="sub-header d-flex flex-row align-center justify-start"
                      >
                        <v-img
                          v-if="item.image"
                          :src="item.image"
                          :alt="item.title"
                          class="mr-2"
                          width="24"
                          height="24"
                          max-width="24"
                        />

                        <h2 class="header-section-summary my-0 py-0">
                          {{ item.title }}
                        </h2>
                      </div>

                      <span v-if="item.description">
                        {{ item.description }}
                      </span>
                    </div>
                  </div>
                </div>

                <ExperimentSummaryStatus
                  v-if="item.tab === 'status'"
                  :experiment="experiment"
                />

                <template v-if="item.tab === 'components'">
                  <div class="section-exposure-sets px-5">
                    <div
                      v-if="!singleConditionExperiment"
                      class="panel-information d-flex flex-column justify-center"
                    >
                      <h3 class="my-0">
                        Exposure Sets
                      </h3>

                      <p
                        v-if="exposures"
                        class="exposure-set-explanation pb-0"
                      >
                        Because you have
                        <strong>{{ conditionCount }}</strong>
                        (<a @click="handleEdit('ExperimentDesignConditions', item.tab)" tabindex="0">edit</a>)
                        and would like your students to be
                        <strong>{{ exposureText[experiment.exposureType] }}</strong>
                        ({{ exposureType[experiment.exposureType] }})
                        (<a @click="handleEdit('ExperimentDesignConditions', item.tab)" tabindex="0">edit</a>),
                        we set you up with
                        <strong>{{ exposures.length }} exposure sets</strong>.

                        <ToolTip
                          header="What is an exposure set?"
                          content="An 'exposure set' exposes a student to a specific condition during a specific time period. Students will change conditions between exposure sets, and the order of conditions across exposure sets will be randomly assigned to different students. An exposure set contains one or more components, and there must be an equal number of components in each exposure set in order to balance the experiment."
                          aria-label="Exposure set explanation tooltip"
                          alignment="top"
                          activator-type="link"
                          activator-content="What is an exposure set?"
                          attach=".exposure-set-explanation"
                          ref="exposureSetTooltip"
                        />
                      </p>

                      <span
                        v-show="showBalanced"
                        class="exposure-set-balanced-status"
                      >
                        Your exposure sets are currently:

                        <v-chip
                          class="mr-2"
                          label
                          variant="outlined"
                        >
                          <span
                            v-if="!balanced"
                            class="label-unbalanced"
                          >
                            <v-icon>mdi-scale-unbalanced</v-icon>
                            Unbalanced
                          </span>

                          <span v-if="balanced">
                            <v-icon>mdi-scale-balance</v-icon>
                            Balanced
                          </span>
                        </v-chip>

                        <ToolTip
                          :header="balanceTooltipHeader"
                          :content="balanceTooltipContent"
                          aria-label="Balance explanation tooltip"
                          alignment="top"
                          activator-type="link"
                          activator-content="What does this mean?"
                          attach=".exposure-set-balanced-status"
                          ref="balancedTooltip"
                        />
                      </span>
                    </div>

                    <ExperimentAssignments
                      v-if="loaded"
                      :experiment="experiment"
                      :balanced="balanced"
                      :active-exposure-set="exposureSet"
                    />
                  </div>
                </template>

                <template v-if="item.tab !== 'status' && item.tab !== 'components'">
                  <table>
                    <tr
                      v-for="section in sectionValuesMap[item.title]"
                      :key="section.title"
                      class="tableRow"
                    >
                      <td class="leftData col-4">
                        <div class="detail">
                          <span class="heading">
                            {{ section.title }}
                          </span>

                          <v-btn
                            @click="handleEdit(section.editSection, item.tab)"
                            class="edit-section-link"
                            tabindex="0"
                            variant="plain"
                          >
                            EDIT
                          </v-btn>
                        </div>
                      </td>

                      <td class="col-7 rightData">
                        <template v-if="section.type === 'string'">
                          {{ section.description }}
                        </template>

                        <template v-if="section.type === 'array'">
                          <label
                            v-for="(condition, index) in section.description"
                            :key="condition.conditionId"
                            :for="`condition-${condition.conditionId}`"
                            class="text-left conditionLabel"
                          >
                            <span class="conditionName">
                              Condition {{ index + 1 }}
                            </span>

                            <br />

                            <v-chip
                              :color="conditionColorMapping[condition.name]"
                              density="compact"
                              variant="flat"
                              label
                            >
                              {{ condition.name }}
                            </v-chip>

                            <v-chip
                              v-show="condition.defaultCondition"
                              class="ml-3 defaultPill"
                              color="primary"
                              density="compact"
                              variant="flat"
                            >
                              <v-icon>mdi-check</v-icon>
                              <span>Default</span>
                            </v-chip>
                          </label>
                        </template>

                        <template v-if="section.type === 'constant'">
                          <template v-if="section.description === 'WITHIN'">
                            <img
                              :src="allConditionsIcon"
                              alt="all conditions"
                              class="constantImage mb-2"
                            />

                            <span class="conditionType mb-2">
                              All conditions
                            </span>

                            <p class="conditionDetail">
                              All students are exposed to every condition, in different orders. This way you can compare how the different conditions affected each individual student. This is called a within-subject design.
                            </p>
                          </template>

                          <template v-if="section.description === 'BETWEEN'">
                            <img
                              :src="oneConditionIcon"
                              alt="one condition"
                              class="constantImage mb-2"
                            />

                            <span class="conditionType mb-2">
                              Only one condition
                            </span>

                            <p class="conditionDetail">
                              Each student is only exposed to one condition, so that you can compare how the different conditions affected different students. This is called a between-subjects design.
                            </p>
                          </template>
                        </template>

                        <template v-if="section.type === 'participation'">
                          <template v-if="section.description === 'CONSENT'">
                            Informed Consent

                            <button
                              v-if="!pdfLoading"
                              @click="openPDF"
                              class="pdfButton"
                            >
                              {{ consentTitle }}
                            </button>

                            <Spinner v-if="pdfLoading" />
                          </template>

                          <template v-else-if="section.description === 'MANUAL'">
                            Manual

                            <br />

                            <span>
                              {{ experiment.acceptedParticipants }} students selected to participate out of {{ experiment.potentialParticipants }} students enrolled
                            </span>
                          </template>

                          <template v-else>
                            Include All Students

                            <br />

                            <span>
                              {{ experiment.potentialParticipants }} students selected to participate out of {{ experiment.potentialParticipants }} students enrolled
                            </span>
                          </template>
                        </template>
                      </td>
                    </tr>
                  </table>
                </template>
              </div>

              <ResultsDashboard v-if="item.tab === 'results'" />
            </v-window-item>
          </v-window>
        </v-col>
      </v-row>

      <VuePdfEmbed
        v-if="displayConsentFile"
        :source="`data:application/pdf;base64,${pdfFile}`"
      />
    </v-container>

    <v-container v-else fluid>
      no experiment
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
} from "vue";

import {
  useRouter,
  useRoute,
  onBeforeRouteUpdate
} from "vue-router";

import Swal from "sweetalert2";
import { EventBus } from "@/helpers/event-bus";
import { statusAlert, createStatusAlert, getColor } from "@/helpers/ui-utils";

import ExperimentAssignments from "@/views/ExperimentAssignments.vue";
import ExperimentSummaryStatus from "@/views/ExperimentSummaryStatus.vue";
import Help from "@/components/Help.vue";
import ResultsDashboard from "@/views/dashboard/results/ResultsDashboard.vue";
import Spinner from "@/components/Spinner.vue";
import ToolTip from "@/components/ToolTip.vue";
import VuePdfEmbed from "vue-pdf-embed";

import designSummary from "@/assets/design_summary.svg";
import participantsSummary from "@/assets/participants_summary.svg";
import assignmentsSummary from "@/assets/assignments_summary.svg";
import allConditionsIcon from "@/assets/all_conditions.svg";
import oneConditionIcon from "@/assets/one_condition.svg";
import terracottaLogoMark from "@/assets/terracotta_logo_mark.svg";

import { experiment as experimentModule } from "@/store/experiment.module";
import { exposures as exposuresModule } from "@/store/exposures.module";
import { assignment as assignmentModule } from "@/store/assignment.module";
import { condition as conditionModule } from "@/store/condition.module";
import { navigation as navigationModule } from "@/store/navigation.module";
import { dataExportRequest as dataExportRequestModule } from "@/store/experiment-data-export.module";
import { configuration as configurationModule } from "@/store/configuration.module";
import { alert as alertModule } from "@/store/alert.module";
import { consent as consentModule } from "@/store/consent.module";
import { treatment as treatmentModule } from "@/store/treatment.module";
import { assessment as assessmentModule } from "@/store/assessment.module";
import { container as messagingContainerModule } from "@/store/messaging/container.module";

defineOptions({
  name: "ExperimentSummary"
});

const router = useRouter();
const route = useRoute();


const experimentStore = experimentModule();
const exposuresStore = exposuresModule();
const assignmentStore = assignmentModule();
const conditionStore = conditionModule();
const navigationStore = navigationModule();
const dataExportRequestStore = dataExportRequestModule();
const configurationStore = configurationModule();
const alertStore = alertModule();
const consentStore = consentModule();
const treatmentStore = treatmentModule();
const assessmentStore = assessmentModule();
const messagingContainerStore = messagingContainerModule();

const tab = ref(null);
const exposureSet = ref(0);
const isLoading = ref(true);
const loadPdfFrame = ref(false);
const pdfFile = ref(null);
const pdfLoading = ref(false);

const experimentDataExportRequest = ref({
  showAlert: false,
  downloadLinkClicked: false,
  polling: {
    active: false,
    id: null
  }
});

const experiment = computed(() => experimentStore.experiment);
const conditions = computed(() => experimentStore.conditions);
const exposures = computed(() => exposuresStore.exposures);
const assignments = computed(() => assignmentStore.assignments);
const conditionColorMapping = computed(() => conditionStore.conditionColorMapping);
const editMode = computed(() => navigationStore.editMode);
const dataExportRequests = computed(() => dataExportRequestStore.dataExportRequests);
const configurations = computed(() => configurationStore.get);
const allMessageContainers = computed(() => messagingContainerStore.messageContainers);
const alertStatuses = computed(() => alertStore.statuses);

const setupTabs = computed(() => [
  {
    title: "Design",
    tab: "design",
    description: "The basic design of your experiment",
    image: designSummary
  },
  {
    title: "Participant",
    tab: "participant",
    description: "How students in your class become participants in your experiment",
    image: participantsSummary
  },
  {
    title: "Components",
    tab: "components",
    description: `Terracotta populates ${lmsTitle.value} assignments with learning activities and materials that change depending on who's looking at them, automatically managing experimental variation within the treatments. Just create different treatments within each component. To your students, it will look like they're completing assignments as usual within ${lmsTitle.value}.`,
    image: assignmentsSummary
  },
  {
    title: "Status",
    tab: "status",
    description: "Once your experiment is running, you will see status updates below"
  },
  {
    title: "Results",
    tab: "results"
  }
]);

const sectionValuesMap = computed(() => ({
  Design: designDetails.value,
  Participant: participantDetails.value,
  Components: assignmentDetails.value
}));

const exposureType = {
  WITHIN: "within-subject",
  BETWEEN: "between"
};

const exposureText = {
  WITHIN: "exposed to every condition",
  BETWEEN: "exposed to only one condition"
};

const isMessagingEnabled = computed(() => configurations.value?.messagingEnabled || false);

const balanced = computed(() => {
  if (!exposures.value?.length) {
    return false;
  }

  return exposures.value
    .map(exposure => {
      const assignmentCount = assignments.value
        .filter(assignment => assignment.exposureId === exposure.exposureId)
        .filter(assignment => assignment.treatments.length > 1)
        .length;

      const messageCount = isMessagingEnabled.value
        ? allMessageContainers.value
            .filter(messageContainer => messageContainer.exposureId === exposure.exposureId)
            .filter(messageContainer => messageContainer.messages.length > 1)
            .length
        : 0;

      return assignmentCount + messageCount;
    })
    .every((value, index, array) => value === array[0]);
});

const designDetails = computed(() => {
  return [
    {
      title: "Experiment Title",
      description: experiment.value.title,
      editSection: "ExperimentDesignTitle",
      type: "string"
    },
    {
      title: "Description",
      description: experiment.value.description,
      editSection: "ExperimentDesignDescription",
      type: "string"
    },
    {
      title: "Conditions",
      description: experiment.value.conditions,
      editSection: "ExperimentDesignConditions",
      type: "array"
    },
    {
      title: "Experiment Type",
      description: experiment.value.exposureType,
      editSection: "ExperimentDesignType",
      type: "constant"
    }
  ]
});

const singleConditionExperiment = computed(() => conditions.value.length === 1);

const conditionCount = computed(() => {
  return `${conditions.value.length} condition${singleConditionExperiment.value ? "" : "s"}`;
});

const participantDetails = computed(() => [
  {
    title: "Selection Method",
    description: experiment.value.participationType,
    editSection: "ExperimentParticipationSelectionMethod",
    type: "participation"
  }
]);

const assignmentDetails = computed(() => [
  {
    title: "Your Components",
    description: getAssignmentDetails(),
    editSection: "AssignmentExposureSets",
    type: "assignments"
  }
]);

const hasPublishedAssignment = computed(() => {
  return assignments.value?.filter(assignment => assignment.published).length;
});

const activeTab = computed(() => editMode.value?.callerPage?.tab || "components");
const activeExposureSet = computed(() => editMode.value?.callerPage?.exposureSet || 0);
const loaded = computed(() => !isLoading.value);
const showBalanced = computed(() => exposures.value?.length > 1);
const experimentId = computed(() => experiment.value.experimentId);

const displayConsentFile = computed(() => {
  return tab.value === "participant" && loadPdfFrame.value;
});

const dataExportRequest = computed(() => {
  return dataExportRequests.value?.find(
    request => request.experimentId === parseInt(experimentId.value)
  );
});

const dataExportRequestAlert = computed(() => {
  const request = dataExportRequest.value;

  if (request?.ready) {
    return {
      showDownloadLink: true,
      showRecreateLink: false,
      text: `Your data export for experiment "${request.experimentTitle}" is ready.`,
      type: "success",
      color: getColor("--green-base")
    };
  }

  if (request?.processing || request?.reprocessing) {
    return {
      showDownloadLink: false,
      showRecreateLink: false,
      text: `The data export for experiment "${request.experimentTitle}" is being processed. Please do not navigate away from this page.`,
      type: "info",
      color: getColor("--blue-primary")
    };
  }

  if (request?.outdated) {
    return {
      showDownloadLink: false,
      showRecreateLink: true,
      text: `There have been updates since the last requested data export for experiment "${request.experimentTitle}".`,
      type: "warning"
    };
  }

  if (request?.error) {
    return {
      showDownloadLink: false,
      showRecreateLink: false,
      text: `There was an error processing the requested data export for experiment "${request.experimentTitle}". Please try again or contact support.`,
      type: "error",
      color: getColor("--red-base")
    };
  }

  return {
    showDownloadLink: false,
    showRecreateLink: false,
    text: "Your data export is being processed. Please do not navigate away from this page.",
    type: "info"
  };
});

const experimentExportEnabled = computed(() => configurations.value?.experimentExportEnabled);
const lmsTitle = computed(() => configurations.value?.lmsTitle || "your LMS");

const balanceTooltipHeader = computed(() => {
  return balanced.value ? "Balanced Exposure Sets" : "Unbalanced Exposure Sets";
});

const balanceTooltipContent = computed(() => {
  if (balanced.value) {
    return `Your exposure sets contain all the same number components, and components contain the same number of treatments. Great work! Single version ${isMessagingEnabled.value ? "messages and" : ""} assignments do not count toward balance.`;
  }

  return `A balanced experiment needs to have the same number ${isMessagingEnabled.value ? "of assignments, integrations, and/or messages" : "of assignments and integrations"} within each exposure set, and a treatment for each condition. This will expose your students to every condition, but in different orders, so you can compare how the different conditions affected each student. Single version ${isMessagingEnabled.value ? "messages and" : ""} assignments do not count toward balance.`;
});

const isConsentType = computed(() => experiment.value?.participationType === "CONSENT");
const consentTitle = computed(() => experiment.value?.consent?.title || "");

watch(pdfFile, () => {
  loadPdfFrame.value = true;
  pdfLoading.value = false;
});

watch(
  experimentDataExportRequest,
  newRequest => {
    if (newRequest.polling.active) {
      if (!newRequest.polling.id) {
        experimentDataExportRequest.value.polling.id = window.setInterval(() => {
          handleDataExportRequestPolling();
        }, 5000);
      }
    } else if (newRequest.polling.id) {
      experimentDataExportRequest.value.polling.id =
        window.clearInterval(newRequest.polling.id);
    }
  },
  { deep: true }
);

const saveExit = () => {
  createStatusAlert(
    statusAlert(alertStatuses.value.success, "Experiment saved successfully")
  );

  router.push({ name: "Home" });
};

const handleExperimentExport = async () => {
  await experimentStore.exportExperiment(experimentId.value);
};

const handleEdit = async (componentName, currentTab) => {
  await navigationStore.saveEditMode({
    initialPage: componentName,
    callerPage: {
      name: "ExperimentSummary",
      tab: currentTab
    }
  });

  router.push({
    name: componentName
  });
};

const openPDF = () => {
  if (pdfLoading.value || loadPdfFrame.value) {
    return;
  }

  pdfLoading.value = true;
  handleConsentFileDownload();
};

const getAssignmentDetails = async () => {
  await exposuresStore.fetchExposures(experimentId.value);
  return exposures.value;
};

const handleCreateTreatment = async (conditionId, assignmentId) => {
  try {
    return await treatmentStore.createTreatment([
      experimentId.value,
      conditionId,
      assignmentId
    ]);
  } catch (error) {
    console.error("handleCreateTreatment | catch", { error });
    return null;
  }
};

const handleCreateAssessment = async (conditionId, treatment) => {
  try {
    return await assessmentStore.createAssessment([
      experimentId.value,
      conditionId,
      treatment.treatmentId
    ]);
  } catch (error) {
    console.error("handleCreateAssessment | catch", { error });
    return null;
  }
};

const goToBuilder = async (conditionId, assignmentId, exposureId) => {
  const treatment = await handleCreateTreatment(conditionId, assignmentId);

  if (![200, 201].includes(treatment?.status)) {
    Swal.fire(`There was a problem creating your treatment: ${treatment?.data}`);
    return false;
  }

  const assessment = await handleCreateAssessment(conditionId, treatment?.data);

  if (![200, 201].includes(assessment?.status)) {
    Swal.fire(`There was a problem creating your assessment: ${assessment?.data}`);
    return false;
  }

  router.push({
    name: "TerracottaBuilder",
    params: {
      experimentId: experimentId.value,
      exposureId,
      assignmentId,
      conditionId,
      treatmentId: treatment?.data?.treatmentId,
      assessmentId: assessment?.data?.assessmentId
    }
  });
};

const groupNameConditionMapping = groupConditionList => {
  const groupConditionMap = {};

  groupConditionList?.forEach(group => {
    groupConditionMap[group.groupName] = group.conditionName;
  });

  return groupConditionMap;
};

const sortedGroups = groupConditionList => {
  return groupConditionList
    ?.map(group => group.groupName)
    ?.sort();
};

const handleConsentFileDownload = () => {
  consentStore.getConsentFile(experimentId.value)
    .then(file => {
      pdfFile.value = encodeURI(file);
    });
};

const handleAlertDataExportDownloadRequest = async () => {
  experimentDataExportRequest.value = {
    ...experimentDataExportRequest.value,
    downloadLinkClicked: true
  };

  await handleDataExportRequest();
};

const handleDataExportRequest = async () => {
  let request = dataExportRequest.value;

  await dataExportRequestStore.poll([
    experimentId.value,
    request ? request.ready || request.downloaded : false
  ]);

  request = dataExportRequest.value;

  if (request?.ready || request?.readyAcknowledged || request?.downloaded) {
    await dataExportRequestStore.retrieve([
      experimentId.value,
      request
    ]);

    if (request?.ready || request?.readyAcknowledged || request?.downloaded) {
      return;
    }
  }

  if (request?.processing) {
    Swal.fire({
      icon: "info",
      text: `The data export for experiment "${request.experimentTitle}" is still being processed. You will be notified when the export is ready for download.
        Please do not navigate away from this page.`,
      confirmButtonText: "OK"
    });
    experimentDataExportRequest.value = {
      ...experimentDataExportRequest.value,
      showAlert: true,
      polling: { ...experimentDataExportRequest.value.polling, active: true }
    };
    return;
  }

  if (request?.reprocessing) {
    Swal.fire({
      icon: "info",
      text: `New submissons have occurred since the requested set of exported data for experiment "${request.experimentTitle}" was processed. A new export is being created.
        You will be notified when the export is ready for download. Please do not navigate away from this page.`,
      confirmButtonText: "OK"
    });
    experimentDataExportRequest.value = {
      ...experimentDataExportRequest.value,
      showAlert: true,
      polling: { ...experimentDataExportRequest.value.polling, active: true }
    };
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

  await dataExportRequestStore.prepare([experimentId.value]);

  experimentDataExportRequest.value = {
    showAlert: true,
    downloadLinkClicked: false,
    polling: {
      active: true,
      id: null
    }
  };
};

const handleDataExportRequestPolling = async () => {
  await dataExportRequestStore.poll([
    experimentId.value,
    false
  ]);

  const request = dataExportRequest.value;

  experimentDataExportRequest.value = {
    showAlert:
      request?.processing ||
      request?.reprocessing ||
      request?.ready ||
      request?.error,
    downloadLinkClicked: false,
    polling: {
      ...experimentDataExportRequest.value.polling,
      active: request?.processing || request?.reprocessing
    }
  };
};

const handleDataExportRequestAlertDismiss = async () => {
  let request = dataExportRequest.value;

  if (request?.processing || request?.reprocessing) {
    experimentDataExportRequest.value = {
      showAlert: false,
      downloadLinkClicked: false,
      polling: {
        ...experimentDataExportRequest.value.polling
      }
    };
    return;
  }

  experimentDataExportRequest.value = {
    showAlert: false,
    downloadLinkClicked: false,
    polling: {
      ...experimentDataExportRequest.value.polling,
      active: false
    }
  };

  request = dataExportRequest.value;

  if (request?.error) {
    dataExportRequestStore.acknowledge([
      experimentId.value,
      request.id,
      "ERROR_ACKNOWLEDGED"
    ]);
  }

  if (request?.ready) {
    dataExportRequestStore.acknowledge([
      experimentId.value,
      request.id,
      "READY_ACKNOWLEDGED"
    ]);
  }

  if (request?.outdated) {
    dataExportRequestStore.acknowledge([
      experimentId.value,
      request.id,
      "OUTDATED_ACKNOWLEDGED"
    ]);
  }
};

const handleConsentExperimentWithoutConsent = () => {
  if (isConsentType.value && !experiment.value?.consent) {
    handleEdit(participantDetails.value[0].editSection, "participant");
  }
};

const fetchExperiment = async routeToUse => {
  await experimentStore.fetchExperimentById(routeToUse.params.experimentId);
};

const statusPageNavHandler = () => {
  tab.value = "status";
};

onMounted(async () => {
  EventBus.on("statusPageNav", statusPageNavHandler);

  await fetchExperiment(route);

  await assignmentStore.resetAssignments();
  dataExportRequestStore.reset();

  tab.value = activeTab.value;
  exposureSet.value = activeExposureSet.value;

  await navigationStore.saveEditMode(null);

  await Promise.all([
    dataExportRequestStore.pollList([[experimentId.value], false]),
    exposuresStore.fetchExposures(experimentId.value)
  ]);

  const request = dataExportRequest.value;

  experimentDataExportRequest.value = {
    ...experimentDataExportRequest.value,
    showAlert: request
      ? request.ready ||
        request.processing ||
        request.reprocessing ||
        request.outdated ||
        request.error
      : false,
    polling: {
      ...experimentDataExportRequest.value.polling,
      active: !!(request?.processing || request?.reprocessing)
    }
  };

  messagingContainerStore.reset();

  await Promise.all(
    exposures.value.flatMap(exposure => [
      assignmentStore.fetchAssignmentsByExposure([
        experimentId.value,
        exposure.exposureId,
        true
      ]),
      messagingContainerStore.getAll([
        experimentId.value,
        exposure.exposureId
      ])
    ])
  );

  getAssignmentDetails();

  isLoading.value = false;

  handleConsentExperimentWithoutConsent();
});

onBeforeRouteUpdate(async (to, from, next) => {
  await fetchExperiment(to);
  next();
});

onBeforeUnmount(() => {
  if (experimentDataExportRequest.value.polling.id !== null) {
    window.clearInterval(experimentDataExportRequest.value.polling.id);
  }

  EventBus.off("statusPageNav", statusPageNavHandler);
});

defineExpose({
  saveExit,
  goToBuilder,
  groupNameConditionMapping,
  sortedGroups
});
</script>

<style lang="scss" scoped>
// development zeroed this via a legacy `.container` class Vuetify 2's v-container also
// applied alongside v-container; Vuetify 3's v-container only applies v-container (whose
// default padding: 16px on every side - see VGrid/_mixins.sass make-container - then went
// unopposed once that selector stopped matching anything during the migration).
.v-container {
  padding-top: 0 !important;
}

.header {
  display: flex;
  flex-direction: row;
  align-items: center;
  /* the Export Data/Export Experiment/Help/Save & Exit button group didn't
     wrap between its own children, so once the outer v-row wrapped it below
     the title it still overflowed off the right edge on its own line
     instead of stacking further. */
  flex-wrap: wrap;
  row-gap: 16px;
}

.panel-overview {
  display: inline-flex;
}

.saveButton {
  background: none !important;
  border: none;
  padding: 0 !important;
  color: #069 !important;
  cursor: pointer;
}

.published-note {
  background-color: map.get($blue, "lighten-5") !important;
  border-color: rgba(29, 157, 255, 0.6) !important;
  > .text {
    padding-bottom: 0 !important;
  }
}

table {
  font-size: 16px;
  color: black;
  border-spacing: 0 25px;
  margin-left: 50px;

  .leftData {
    white-space: nowrap;
    text-align: left;
    vertical-align: top;
    padding: 0 25px;
    width: auto;

    .detail {
      display: inline-flex;
      flex-direction: column;

      .heading {
        font-size: 18px;
        font-weight: 700;
      }
    }
  }

  .rightData {
    display: flex;
    max-width: max-content;
    flex-direction: column;
    text-align: left;
    border-left: 1px solid #e6e6e6;
    padding: 0 12px !important;

    .conditionLabel:not(:last-child) {
      margin-bottom: 10px;
    }

    .assignmentExpansion:not(:last-child) {
      margin-bottom: 20px;
    }

    .defaultPill {
      color: white;

      .v-icon {
        color: white;
        margin-right: 5px;
        font-size: 18px;
        vertical-align: text-bottom;
      }
    }

    .conditionType,
    .exposureSetName,
    .conditionName {
      font-size: 16px;
      font-weight: 700;
    }

    .constantImage {
      height: 36px;
      width: 36px;
    }

    .conditionDetail {
      margin-bottom: 0;
      padding-bottom: 0;
    }

    .assignmentConditionName {
      text-align: left;
    }

    .pdfButton {
      background: none !important;
      border: none;
      padding: 0 !important;
      color: #069;
      text-decoration: underline;
      cursor: pointer;
      text-align: left;
    }
  }
}

div.container-section-summary {
  padding-bottom: 40px;

  div.panel-overview {
    padding-bottom: 0 !important;
  }
}


.label-unbalanced {
  text-transform: none !important;
  opacity: 0.87 !important;
  color: map.get($red, "base") !important;
}

.sticky {
  position: sticky;
  position: -webkit-sticky;
  top: 0;
  left: 0;
  width: 100%;
  /* min-height (not height): once .header wraps to more than one line at
     narrow widths, this needs to grow to fit it instead of clipping. */
  min-height: 100px;
  padding: 30px 0;
  z-index: 100;
  background: white;
  margin: 0 !important;
}

div.col-experiment-title,
div.col-experiment-title > p {
  max-width: fit-content;
}

div.vue-pdf-embed {
  width: 98%;
  margin: 20px auto;
  min-height: 300px;
  max-height: 600px;
  overflow-y: scroll;
  box-shadow:
    0 3px 1px -2px rgba(0, 0, 0, 0.2),
    0 2px 2px 0 rgba(0, 0, 0, 0.14),
    0 1px 5px 0 rgba(0, 0, 0, 0.12);
}

div.results {
  padding-top: 0 !important;
}

.alert-data-export-request {
  min-width: 100%;

  > .v-alert {
    margin: 0 auto;

    a {
      color: inherit !important;
      cursor: pointer;
    }
  }
}

:deep(.edit-section-link) {
  max-width: fit-content !important;
  max-height: fit-content !important;
  padding: 0 !important;
  margin-left: 0 !important;

  &:hover,
  &:focus {
    text-decoration: underline;
    background: none !important;
  }

  .v-btn__content {
    color: map.get($blue, "primary") !important;
    opacity: 1 !important;
    padding: 0 !important;
    justify-content: left !important;
  }
}

.header-section-summary {
  padding-bottom: 0 !important;
}

.panel-information {
  & h3 {
    padding-top: 0 !important;
  }
}
</style>
