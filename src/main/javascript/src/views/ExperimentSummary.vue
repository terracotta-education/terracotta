<template>
  <div>
    <v-container
      v-if="experiment"
    >
      <v-row
        class="sticky my-1"
        justify="space-between"
      >
        <v-col
          class="col-experiment-title"
          cols="8"
        >
          <p class="header ma-0 pa-0">
            <v-img
              src="../../public/terracotta_logo_mark.svg"
              class="mr-6"
              alt="Terracotta Logo"
              height="30"
              max-width="26"
            />
            <span>{{ experiment.title }}</span>
          </p>
        </v-col>
        <div
          class="header ma-0 pa-0"
        >
          <v-btn
            @click="handleDataExportRequest()"
            :disabled="experimentDataExportRequest.polling.active"
            color="primary"
            elevation="0"
            class="mx-1"
          >
            Export Data
          </v-btn>
          <v-btn
            v-if="experimentExportEnabled"
            @click="handleExperimentExport()"
            color="primary"
            elevation="0"
            class="mx-1"
          >
            Export Experiment
          </v-btn>
          <v-btn
            @click="saveExit()"
            color="primary"
            elevation="0"
            class="saveButton ml-4"
          >
            SAVE & EXIT
          </v-btn
          >
        </div>
      </v-row>
      <v-row>
        <div
          v-if="experimentDataExportRequest.showAlert"
          class="alert-data-export-request pb-2 px-3"
        >
          <v-alert
            v-model="experimentDataExportRequest.showAlert"
            @input="handleDataExportRequestAlertDismiss"
            :type="dataExportRequestAlert.type"
            elevation="0"
            dismissible
          >
            {{ dataExportRequestAlert.text }}
            <a
              v-if="dataExportRequestAlert.showDownloadLink"
              @click="handleAlertDataExportDownloadRequest()"
            >
              <b><i>Click here to download</i></b>.
            </a>
            <a
              v-if="dataExportRequestAlert.showRecreateLink"
              @click="handleDataExportRequest()"
            >
              <b><i>Click here to download a new data export</i></b>.
            </a>
          </v-alert>
        </div>
      </v-row>
      <v-row>
        <v-col
          cols="12"
        >
          <v-divider></v-divider>
          <v-tabs
            v-model="tab"
            elevation="0"
          >
            <v-tab
              v-for="item in setupTabs"
              :key="item.tab"
            >
              {{ item.tab }}
            </v-tab>
          </v-tabs>
          <v-divider></v-divider>
          <v-tabs-items
            v-model="tab"
          >
            <v-tab-item
              v-for="item in setupTabs"
              :key="item.tab"
              :class="item.tab"
              :transition="false"
              class="tab-section pt-6"
            >
              <div
                class="tab-heading"
              >
                <v-card
                  v-if="hasPublishedAssignment && item.tab !== 'results'"
                  :key="item.title"
                  class="pt-5 px-5 mx-auto blue lighten-5 rounded-lg"
                  outlined
                >
                  <p
                    class="pb-0"
                  >
                    <strong>Note:</strong> You are currently collecting component submissions. Some setup functionality may not be available to avoid disrupting the experiment.
                  </p>
                </v-card>
                <div
                  v-if="item.tab !== 'results'"
                  class="container-section-summary px-5"
                >
                  <div
                    class="panel-overview py-6"
                  >
                    <div
                      class="panelInformation d-flex flex-column justify-center"
                    >
                      <div>
                        <v-img
                          v-if="item.image"
                          :src="item.image"
                          :alt="item.title"
                          class="icon-section-summary mr-6"
                          style="margin-top: 2px !important; margin-right: 8px !important;"
                        />
                        <h2
                          class="header-section-summary mb-0"
                        >
                          {{ item.title }}
                        </h2>
                      </div>
                      <span
                        v-if="item.description"
                      >
                        {{ item.description }}
                      </span>
                    </div>
                  </div>
                </div>
                <template
                  v-if="item.tab === 'status'"
                >
                  <experiment-summary-status
                    :experiment="experiment"
                  />
                </template>
                <template
                  v-if="item.tab === 'components'"
                >
                  <div
                    class="section-exposure-sets px-5"
                  >
                    <div
                      v-if="!singleConditionExperiment"
                      class="panelInformation d-flex flex-column justify-center"
                    >
                      <h3
                        class="mb-0"
                      >
                        Exposure Sets
                      </h3>
                      <p
                        v-if="exposures"
                        class="pb-0"
                      >
                        Because you have <strong>{{ conditionCount }}</strong> (<a @click="handleEdit('ExperimentDesignConditions', item.tab)" >edit</a>)
                        and would like your students to be <strong>{{ exposureText[experiment.exposureType] }}</strong>
                        ({{ exposureType[experiment.exposureType] }}) (<a @click="handleEdit('ExperimentDesignConditions', item.tab)">edit</a>),
                        we set you up with <strong>{{ exposures.length }} exposure sets</strong>.
                        <v-tooltip
                          top
                        >
                          <template
                            v-slot:activator="{ on, attrs }"
                          >
                            <a
                              v-bind="attrs"
                              v-on="on"
                            >
                              What is an exposure set?
                            </a>
                          </template>
                          <span>
                            <strong
                              class="d-block"
                            >
                              What is an exposure set?
                            </strong>
                            An "exposure set" exposes a student to a specific condition during a specific time period. Students will change conditions between exposure sets, and the order
                            of conditions across exposure sets will be randomly assigned to different students.
                            An exposure set contains one or more components, and there must be an equal number of components in each exposure set in order to balance the experiment.
                          </span>
                        </v-tooltip>
                      </p>
                      <span
                        v-show="showBalanced"
                      >
                        Your exposure sets are currently:
                        <v-chip
                          class="mr-2"
                          label
                          outlined
                        >
                          <span
                            v-if="!balanced"
                            class="label-unbalanced"
                          >
                            <v-icon>mdi-scale-unbalanced</v-icon>
                            Unbalanced
                          </span>
                          <span
                            v-if="balanced"
                          >
                            <v-icon>mdi-scale-balance</v-icon>
                            Balanced
                          </span>
                        </v-chip>
                        <v-tooltip
                          top
                        >
                          <template
                            v-slot:activator="{ on, attrs }"
                          >
                            <a
                              v-bind="attrs"
                              v-on="on"
                            >
                              What does this mean?
                            </a>
                          </template>
                          <span
                            v-if="balanced"
                          >
                            <strong
                              class="d-block"
                            >
                              Balanced Exposure Sets
                            </strong>
                            Your exposure sets contain all the same number components, and components contain the same number of treatments. Great work!
                            Single version {{ isMessagingEnabled ? 'messages and' : '' }} assignments do not count toward balance.
                          </span>
                          <span
                            v-if="!balanced"
                          >
                            <strong
                              class="d-block"
                            >
                              Unbalanced Exposure Sets
                            </strong>
                            A balanced experiment needs to have the same number of {{ isMessagingEnabled ? 'assignments, integrations, and/or messages' : 'assignments and integrations' }} within each exposure set, and a treatment for each condition.
                            This will expose your students to every condition, but in different orders, so you can compare how the different conditions affected each student.
                            Single version {{ isMessagingEnabled ? 'messages and' : '' }} assignments do not count toward balance.
                          </span>
                        </v-tooltip>
                      </span>
                    </div>
                    <experiment-assignments
                      v-if="loaded"
                      :experiment="experiment"
                      :balanced="balanced"
                      :activeExposureSet="exposureSet"
                    />
                  </div>
                </template>
                <template
                  v-if="item.tab !== 'status' && item.tab !== 'components'"
                >
                  <table>
                    <tr
                      v-for="section in sectionValuesMap[item.title]"
                      :key="section.title"
                      class="tableRow"
                    >
                      <td
                        class="leftData col-4"
                      >
                        <template>
                          <div
                            class="detail"
                          >
                            <span
                              class="heading"
                            >
                              {{ section.title }}
                            </span>
                            <a
                              @click="handleEdit(section.editSection, item.tab)"
                            >
                              EDIT
                            </a>
                          </div>
                        </template>
                      </td>
                      <td
                        class="col-7 rightData"
                      >
                        <!-- String Data -->
                        <!-- For Experiment Title and Description -->
                        <template
                          v-if="section.type === 'string'"
                        >
                          {{ section.description }}
                        </template>
                        <!-- Array data -->
                        <!-- For Experiment Condition Details -->
                        <template
                          v-if="section.type === 'array'"
                          class="arrayData"
                        >
                          <label
                            v-for="(condition, index) in section.description"
                            :key="condition.conditionId"
                            :for="`condition-${condition.conditionId}`"
                            class="text-left conditionLabel"
                          >
                            <span
                              class="conditionName"
                            >
                              Condition {{ index + 1 }}
                            </span>
                            <br />
                            <v-chip
                              :color="conditionColorMapping[condition.name]"
                              label
                            >
                              {{ condition.name }}
                            </v-chip>
                            <v-chip
                              v-show="condition.defaultCondition"
                              class="px-3 py-1  ml-3 defaultPill"
                              color="primary"
                            >
                              <v-icon>mdi-check</v-icon>
                              <span>Default</span>
                            </v-chip>
                          </label>
                        </template>
                        <!-- Constant values -->
                        <!-- For Experiment Type -->
                        <template
                          v-if="section.type === 'constant'"
                        >
                          <template
                            v-if="section.description === 'WITHIN'"
                          >
                            <img
                              src="@/assets/all_conditions.svg"
                              alt="all conditions"
                              class="constantImage mb-2"
                            />
                            <span
                              class="conditionType mb-2"
                            >
                              All conditions
                            </span>
                            <p
                              class="conditionDetail"
                            >
                              All students are exposed to every condition, in different orders. This way you can compare how the different conditions affected each individual
                              student. This is called a within-subject design.
                            </p>
                          </template>
                          <template
                            v-if="section.description === 'BETWEEN'"
                          >
                            <img
                              src="@/assets/one_condition.svg"
                              alt="one conditions"
                              class="constantImage mb-2"
                            />
                            <span
                              class="conditionType mb-2"
                            >
                              Only one condition
                            </span>
                            <p
                              class="conditionDetail"
                            >
                              Each student is only exposed to one condition, so that you can compare how the different conditions affected different students. This is called a
                              between-subjects design.
                            </p>
                          </template>
                        </template>
                        <!-- Participation data -->
                        <template
                          v-if="section.type === 'participation'"
                        >
                          <!-- Consent Participation -->
                          <template
                            v-if="section.description === 'CONSENT'"
                          >
                            Informed Consent
                            <button
                              v-if="!pdfLoading"
                              @click="openPDF"
                              class="pdfButton"
                            >
                              {{ experiment.consent.title }}
                            </button>
                            <Spinner
                              v-if="pdfLoading"
                            />
                          </template>
                          <!-- Manual Participation -->
                          <template
                            v-else-if="section.description === 'MANUAL'"
                          >
                            Manual
                            <br />
                            <span>
                              {{ experiment.acceptedParticipants }} students selected to participate out of {{ experiment.potentialParticipants }} students enrolled
                            </span>
                          </template>
                          <!-- All Participation -->
                          <template
                            v-else
                          >
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
              <template
                v-if="item.tab === 'results'"
              >
                <ResultsDashboard />
              </template>
            </v-tab-item>
          </v-tabs-items>
        </v-col>
      </v-row>
      <vue-pdf-embed
        v-if="displayConsentFile"
        :source="'data:application/pdf;base64,' + pdfFile"
      />
    </v-container>
    <v-container v-else>
      no experiment
    </v-container>
  </div>
</template>

<script>
import { mapGetters, mapActions } from "vuex";
import { EventBus } from "@/helpers/event-bus";
import ExperimentAssignments from "@/views/ExperimentAssignments";
import ExperimentSummaryStatus from "@/views/ExperimentSummaryStatus";
import ResultsDashboard from "@/views/dashboard/results/ResultsDashboard";
import Spinner from "@/components/Spinner";
import store from "@/store";
import VuePdfEmbed from "vue-pdf-embed/dist/vue2-pdf-embed";

export default {
  name: "ExperimentSummary",
  components: {
    ExperimentSummaryStatus,
    ExperimentAssignments,
    ResultsDashboard,
    Spinner,
    VuePdfEmbed
  },
  data: () => ({
    tab: null,
    items: [
      "design",
      "participant",
      "components",
      "status",
      "results"
    ],
    conditionTreatments: {},
    conditionColors: [""],
    isLoading: true,
    exposureSet: 0, // exposure set tab index
    loadPdfFrame: false,
    pdfFile: null,
    pdfLoading: false,
    experimentDataExportRequest: {
      showAlert: false,
      downloadLinkClicked: false,
      polling: {
        active: false,
        id: null
      }
    }
  }),
  watch: {
    pdfFile() {
      this.loadPdfFrame = true;
      this.pdfLoading = false;
    },
    experimentDataExportRequest: {
      handler: function (newExperimentDataExportRequest) {
        if (newExperimentDataExportRequest.polling.active) {
          // create export data request polling scheduler
          this.experimentDataExportRequest.polling.id = window.setInterval(() => {
            this.handleDataExportRequestPolling()
          }, 5000);
        } else {
          // clear export data request polling scheduler
          this.experimentDataExportRequest.polling.id = window.clearInterval(this.experimentDataExportRequest.polling.id);
        }
      },
      immediate: false
    }
  },
  computed: {
    ...mapGetters({
      experiment: "experiment/experiment",
      conditions: "experiment/conditions",
      exposures: "exposures/exposures",
      assignments: "assignment/assignments",
      conditionColorMapping: "condition/conditionColorMapping",
      editMode: "navigation/editMode",
      dataExportRequests: "dataexportrequest/dataExportRequests",
      configurations: "configuration/get",
      allMessageContainers: "messagingMessageContainer/messageContainers"
    }),
    setupTabs() {
      // Expansion Tab Header Values
      return [
        {
          title: "Design",
          tab: "design",
          description: "The basic design of your experiment",
          image: require("@/assets/design_summary.svg"),
        },
        {
          title: "Participant",
          tab: "participant",
          description: "How students in your class become participants in your experiment",
          image: require("@/assets/participants_summary.svg"),
        },
        {
          title: "Components",
          tab: "components",
          description: `Terracotta populates ${this.lmsTitle} assignments with learning activities and
                        materials that change depending on who's looking at them, automatically
                        managing experimental variation within the treatments. Just create different
                        treatments within each component. To your students, it will look like
                        they're completing assignments as usual within ${this.lmsTitle}.`,
          image: require("@/assets/assignments_summary.svg"),
        },
        {
          title: "Status",
          tab: "status",
          description: "Once your experiment is running, you will see status updates below",
        },
        {
          title: "Results",
          tab: "results"
        }
      ];
    },
    // Higher Level Section Values
    sectionValuesMap() {
      return {
        Design: this.designDetails,
        Participant: this.participantDetails,
        Component: this.assignmentDetails,
      };
    },
    exposureType() {
      return {
        WITHIN: "within-subject",
        BETWEEN: "between",
      };
    },
    exposureText() {
      return {
        WITHIN: "exposed to every condition",
        BETWEEN: "exposed to only one condition",
      };
    },
    balanced() {
      return this.exposures
        .map(
          (exposure) => {
            return this.assignments
              .filter((assignment) => assignment.exposureId === exposure.exposureId)
              .filter((assignment) => assignment.treatments.length > 1)
              .length
              +
              (
                this.isMessagingEnabled ?
                this.allMessageContainers
                .filter((messageContainer) => messageContainer.exposureId === exposure.exposureId)
                .filter((messageContainer) => messageContainer.messages.length > 1)
                .length
                :
                0
              );
          }
        )
        .every((v, i, arr) => v === arr[0]);
    },
    // Design Expansion View Values
    designDetails() {
      return [
        {
          title: "Experiment Title",
          description: this.experiment.title,
          editSection: "ExperimentDesignTitle",
          type: "string",
        },
        {
          title: "Description",
          description: this.experiment.description,
          editSection: "ExperimentDesignDescription",
          type: "string",
        },
        {
          title: "Conditions",
          description: this.experiment.conditions,
          editSection: "ExperimentDesignConditions",
          type: "array",
        },
        {
          title: "Experiment Type",
          description: this.experiment.exposureType,
          editSection: "ExperimentDesignType",
          type: "constant",
        },
      ];
    },
    conditionCount() {
      return `${this.conditions.length} condition${
        this.singleConditionExperiment ? "" : "s"
      }`;
    },
    // Participation Expansion View Values
    participantDetails() {
      return [
        {
          title: "Selection Method",
          description: this.experiment.participationType,
          editSection: "ExperimentParticipationSelectionMethod",
          type: "participation",
        },
      ];
    },
    // Assignment Expansion View Values
    assignmentDetails() {
      return [
        {
          title: "Your Components",
          description: this.getAssignmentDetails(),
          editSection: "AssignmentExposureSets",
          type: "assignments",
        },
      ];
    },
    hasPublishedAssignment() {
      return this.assignments?.filter((a) => a.published).length;
    },
    activeTab() {
      // if active tab was previously selected, return it, otherwise default to components tab
      return this.editMode?.callerPage?.tab || "components";
    },
    activeExposureSet() {
      // if active tab was previously selected, return it, otherwise default to components tab
      return this.editMode?.callerPage?.exposureSet || 0;
    },
    loaded() {
      return !this.isLoading;
    },
    showBalanced() {
      return this.exposures?.length > 1;
    },
    singleConditionExperiment() {
      return this.conditions.length === 1;
    },
    experimentId() {
      return this.experiment.experimentId;
    },
    displayConsentFile() {
      return this.tab === this.setupTabs.findIndex((setupTab) => setupTab.tab === "participant") && this.loadPdfFrame;
    },
    dataExport() {
      const dataExportRequest = this.dataExportRequest;

      if (dataExportRequest?.ready || dataExportRequest?.downloaded) {
        return {
          status: "Files ready to download",
          color: "success",
          icon: "mdi-check",
          showStatus: this.showDataExportRequestStatus
        }
      }

      if (dataExportRequest?.processing || dataExportRequest?.reprocessing) {
        return {
          status: "Files are being processed",
          color: "info",
          icon: "mdi-clock",
          showStatus: this.showDataExportRequestStatus
        }
      }

      if (dataExportRequest?.error) {
        return {
          status: "File processing error",
          color: "error",
          icon: "mdi-exclamation",
          showStatus: this.showDataExportRequestStatus
        }
      }

      return {
        show: false
      }
    },
    showDataExportRequestStatus() {
      const dataExportRequest = this.dataExportRequest;

      return !this.experimentDataExportRequest.showAlert &&
        [
          dataExportRequest?.processing,
          dataExportRequest?.reprocessing,
          dataExportRequest?.ready,
          dataExportRequest?.downloaded,
          dataExportRequest?.outdated
        ].some(e => e === true);
    },
    dataExportRequestAlert() {
      const dataExportRequest = this.dataExportRequest;

      if (dataExportRequest?.ready) {
        return {
          showDownloadLink: true,
          showRecreateLink: false,
          text: `Your data export for experiment "${dataExportRequest.experimentTitle}" is ready.`,
          type: "success"
        }
      }

      if (dataExportRequest?.processing || dataExportRequest?.reprocessing) {
        return {
          showDownloadLink: false,
          showRecreateLink: false,
          text: `The data export for experiment "${dataExportRequest.experimentTitle}" is being processed. Please do not navigate away from this page.`,
          type: "info"
        }
      }

      if (dataExportRequest?.outdated) {
        return {
          showDownloadLink: false,
          showRecreateLink: true,
          text: `There have been updates since the last requested data export for experiment "${dataExportRequest.experimentTitle}".`,
          type: "warning"
        }
      }

      if (dataExportRequest?.error) {
        return {
          showDownloadLink: false,
          showRecreateLink: false,
          text: `There was an error processing the requested data export for experiment "${dataExportRequest.experimentTitle}". Please try again or contact support.`,
          type: "error"
        }
      }

      return {};
    },
    dataExportRequest() {
      return this.dataExportRequests?.find(dataExportRequest => dataExportRequest.experimentId === parseInt(this.experimentId));
    },
    experimentExportEnabled() {
      return this.configurations?.experimentExportEnabled;
    },
    isMessagingEnabled() {
      return this.configurations?.messagingEnabled || false;
    },
    lmsTitle() {
      return this.configurations?.lmsTitle || "your LMS";
    }
  },
  methods: {
    ...mapActions({
      fetchExposures: "exposures/fetchExposures",
      fetchAssignmentsByExposure: "assignment/fetchAssignmentsByExposure",
      checkTreatment: "treatment/checkTreatment",
      createTreatment: "treatment/createTreatment",
      createAssessment: "assessment/createAssessment",
      getConsentFile: "consent/getConsentFile",
      resetAssignments: "assignment/resetAssignments",
      saveEditMode: "navigation/saveEditMode",
      deleteEditMode: "navigation/deleteEditMode",
      retrieveDataExportRequest: "dataexportrequest/retrieve",
      prepareDataExportRequest: "dataexportrequest/prepare",
      resetDataExportRequest: "dataexportrequest/reset",
      pollDataExportRequest: "dataexportrequest/poll",
      pollDataExportRequests: "dataexportrequest/pollList",
      dataExportRequestAcknowledge: "dataexportrequest/acknowledge",
      exportExperiment: "experiment/exportExperiment",
      importExperiment: "experiment/importExperiment",
      getAllMessageContainers: "messagingMessageContainer/getAll"
    }),
    saveExit() {
      this.$router.push({ name: "Home" });
    },
    async handleExperimentExport() {
      await this.exportExperiment(this.experimentId);
    },
    // Navigate to EDIT section
    async handleEdit(componentName, currentTab) {
      await this.saveEditMode({
        initialPage: componentName,
        callerPage: {
          name: "ExperimentSummary",
          tab: currentTab
        }
      });
      this.$router.push({
        name: componentName
      });
    },
    openPDF() {
      if (this.pdfLoading || this.loadPdfFrame) {
        return;
      }
      this.pdfLoading = true;
      this.handleConsentFileDownload();
    },
    async getAssignmentDetails() {
      await this.fetchExposures(this.experimentId);
      return this.exposures;
    },
    async handleCreateTreatment(conditionId, assignmentId) {
      // POST TREATMENT
      try {
        return await this.createTreatment([
          this.experimentId,
          conditionId,
          assignmentId,
        ]);
      } catch (error) {
        console.error("handleCreateTreatment | catch", { error });
      }
    },
    async handleCreateAssessment(conditionId, treatment) {
      // POST ASSESSMENT TITLE & HTML (description)
      try {
        return await this.createAssessment([
          this.experimentId,
          conditionId,
          treatment.treatmentId,
        ]);
      } catch (error) {
        console.error("handleCreateAssessment | catch", { error });
      }
    },
    async goToBuilder(conditionId, assignmentId) {
      // create the treatment
      const treatment = await this.handleCreateTreatment(
        conditionId,
        assignmentId
      );

      if (![200, 201].includes(treatment.status)) {
        this.$swal(
          `There was a problem creating your treatment: ${treatment.data}`
        );
        return false;
      }

      // create the assessment
      const assessment = await this.handleCreateAssessment(
        conditionId,
        treatment?.data
      );

      if (![200, 201].includes(assessment.status)) {
        this.$swal(
          `There was a problem creating your assessment: ${assessment.data}`
        );
        return false;
      }

      // send user to builder with the treatment and assessment ids
      this.$router.push({
        name: "TerracottaBuilder",
        params: {
          experimentId: this.experimentId,
          conditionId: conditionId,
          treatmentId: treatment?.data?.treatmentId,
          assessmentId: assessment?.data?.assessmentId,
        },
      });
    },
    // For Mapping Sorted Group Name with associated Condition
    groupNameConditionMapping(groupConditionList) {
      const groupConditionMap = {};
      groupConditionList?.map(
        (group) => (groupConditionMap[group.groupName] = group.conditionName)
      );
      return groupConditionMap;
    },
    // For Sorting Group Names
    sortedGroups(groupConditionList) {
      const newGroups = groupConditionList?.map((group) => group.groupName);
      return newGroups?.sort();
    },
    handleConsentFileDownload() {
      this.getConsentFile(this.experimentId)
        .then((file) => {
          this.pdfFile = encodeURI(file);
        });
    },
    async handleAlertDataExportDownloadRequest() {
      this.experimentDataExportRequest = {
        ...this.experimentDataExportRequest,
        downloadLinkClicked: true
      };
      await this.handleDataExportRequest();
    },
    async handleDataExportRequest() {
      let dataExportRequest = this.dataExportRequest;
      await this.pollDataExportRequest([
        this.experimentId,
        dataExportRequest ? (dataExportRequest.ready || dataExportRequest.downloaded) : false
      ]);

      dataExportRequest = this.dataExportRequest;

      if (dataExportRequest?.ready || dataExportRequest?.readyAcknowledged || dataExportRequest?.downloaded) {
        // retrieve file
        await this.retrieveDataExportRequest([
          this.experimentId,
          dataExportRequest
        ]);

        if (dataExportRequest?.ready || dataExportRequest?.readyAcknowledged || dataExportRequest?.downloaded) {
          // file has been delivered
          return;
        }
      }

      if (dataExportRequest?.processing) {
        this.$swal({
          icon: "info",
          text: `The data export for experiment "${dataExportRequest.experimentTitle}" is still being processed. You will be notified when the export is ready for download.
            Please do not navigate away from this page.`,
          confirmButtonText: "OK"
        });
        return;
      }

      if (dataExportRequest?.reprocessing) {
        this.$swal({
          icon: "info",
          text: `New submissons have occurred since the requested set of exported data for experiment "${dataExportRequest.experimentTitle}" was processed. A new export is being created.
            You will be notified when the export is ready for download. Please do not navigate away from this page.`,
          confirmButtonText: "OK"
        });
        return;
      }

      const dataExportRequestConfirm = await this.$swal({
        icon: "info",
        text: `Depending on its size, it could take several minutes to retrieve your data export.
          You will see an alert when the export is ready to download. After you click "ok," please stay on this page until your download is ready.`,
        showCancelButton: true,
        confirmButtonText: "OK"
      });

      if (dataExportRequestConfirm.isConfirmed) {
        await this.prepareDataExportRequest([
          this.experimentId
        ]);

        dataExportRequest = this.dataExportRequest;

        this.experimentDataExportRequest = {
          showAlert: dataExportRequest?.processing || dataExportRequest?.reprocessing,
          downloadLinkClicked: false,
          polling: {
            active: dataExportRequest?.processing || dataExportRequest?.reprocessing,
            id: null
          }
        }
      }
    },
    async handleDataExportRequestPolling() {
      await this.pollDataExportRequest([
        this.experimentId,
        false
      ]);

      const dataExportRequest = this.dataExportRequest;

      this.experimentDataExportRequest = {
        showAlert: dataExportRequest?.processing || dataExportRequest?.reprocessing || dataExportRequest.ready || dataExportRequest.error,
        downloadLinkClicked: false,
        polling: {
          ...this.experimentDataExportRequest.polling,
          active: dataExportRequest?.processing || dataExportRequest?.reprocessing
        }
      }
    },
    async handleDataExportRequestAlertDismiss() {
      let dataExportRequest = this.dataExportRequest;

      if (this.dataExportRequest?.processing || this.dataExportRequest?.reprocessing) {
        // data export is still being processed; just dismiss alert
        this.experimentDataExportRequest = {
          showAlert: false,
          downloadLinkClicked: false,
          polling: {
            ...this.experimentDataExportRequest.polling
          }
        }
        return;
      }

      this.experimentDataExportRequest = {
        showAlert: false,
        downloadLinkClicked: false,
        polling: {
          ...this.experimentDataExportRequest.polling,
          active: false
        }
      }

      dataExportRequest = this.dataExportRequest;

      if (dataExportRequest?.error) {
        this.dataExportRequestAcknowledge([
          this.experimentId,
          dataExportRequest.id,
          "ERROR_ACKNOWLEDGED"
        ]);
      }

      if (dataExportRequest?.ready) {
        this.dataExportRequestAcknowledge([
        this.experimentId,
        dataExportRequest.id,
        "READY_ACKNOWLEDGED"
        ]);
      }

      if (dataExportRequest?.outdated) {
        this.dataExportRequestAcknowledge([
          this.experimentId,
          dataExportRequest.id,
          "OUTDATED_ACKNOWLEDGED"
        ]);
      }
    }
  },
  async mounted() {
    await this.resetAssignments();
    this.resetDataExportRequest();
    this.tab = this.setupTabs.findIndex((s) => s.tab === this.activeTab);
    this.exposureSet = this.activeExposureSet;
    await this.saveEditMode(null);

    // process experiment data exports
    await this.pollDataExportRequests([
      [this.experimentId],
      false
    ]);
    const dataExportRequest = this.dataExportRequest;
    this.experimentDataExportRequest = {
      ...this.experimentDataExportRequest,
      showAlert: dataExportRequest ? (dataExportRequest.ready || dataExportRequest.processing || dataExportRequest.reprocessing || dataExportRequest.outdated || dataExportRequest.error) : false,
    }

    await this.fetchExposures(this.experimentId);

    for (const exposure of this.exposures) {
      // add submissions to assignments request
      const submissions = true;
      await this.fetchAssignmentsByExposure(
        [
          this.experimentId,
          exposure.exposureId,
          submissions,
        ]
      );
      await this.getAllMessageContainers(
        [
          this.experimentId,
          exposure.exposureId
        ]
      );
    }

    this.getAssignmentDetails();
    this.isLoading = false;
  },
  created() {
    // status page nav from Results Dashboard > Outcomes > Input
    EventBus.$on("statusPageNav", () => { this.tab = this.setupTabs.findIndex((s) => s.tab === "status") });
  },
  beforeRouteEnter(to, from, next) {
    return store
      .dispatch("experiment/fetchExperimentById", to.params.experimentId)
      .then(next, next);
  },
  beforeRouteUpdate(to, from, next) {
    return store
      .dispatch("experiment/fetchExperimentById", to.params.experimentId)
      .then(next, next);
  },
  beforeDestroy() {
    // clear file request polling scheduler
    if (this.experimentDataExportRequest.polling.id !== null) {
      window.clearInterval(this.experimentDataExportRequest.polling.id);
    }
  }
};
</script>

<style lang="scss" scoped>
.header {
  display: flex;
  flex-direction: row;
  align-items: center;
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
.v-application .v-sheet--outlined.blue.lighten-5 {
  border-color: rgba(29, 157, 255, 0.6) !important;
}
.v-tooltip__content {
  max-width: 400px;
  opacity: 1.0 !important;
  background-color: rgba(55,61,63, 1.0) !important;
  a {
    color: #afdcff;
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
div.icon-section-summary,
h2.header-section-summary {
  display: inline !important;
  float: left;
}
div.icon-section-summary {
  width: 24px;
  height: 24px;
}
.label-unbalanced {
  text-transform: none !important;
  opacity: 0.87 !important;
  color: #E06766 !important;
}
.sticky {
  position: sticky;
  position: -webkit-sticky;
  top: 0;
  left: 0;
  width: 100%;
  height: 100px;
  padding: 30px 0;
  z-index: 100;
  background: white;
  margin: 0 !important;
}
div.container {
  padding-top: 0 !important;
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
  box-shadow: 0 3px 1px -2px rgba(0,0,0,.2),0 2px 2px 0 rgba(0,0,0,.14),0 1px 5px 0 rgba(0,0,0,.12);
}
div.results {
  padding-top: 0 !important;
}
.alert-data-export-request {
  min-width: 100%;
  > .v-alert {
    margin: 0 auto;
    & a {
      color: white;
    }
  }
}
</style>
