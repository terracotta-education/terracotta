<template>
  <div>
    <v-container v-if="experiment">
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
        <div class="header ma-0 pa-0">
          <v-btn
            color="primary"
            elevation="0"
            @click="exportData()"
          >
            Export Data
          </v-btn>
          <v-btn
            color="primary"
            elevation="0"
            class="saveButton ml-4"
            @click="saveExit()"
          >
            SAVE & EXIT
          </v-btn
          >
        </div>
      </v-row>
      <v-row>
        <v-col cols="12">
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
          <v-tabs-items v-model="tab">
            <v-tab-item
              class="tab-section pt-6"
              v-for="item in setupTabs"
              :key="item.tab"
              :class="item.tab"
            >
              <div class="tab-heading">
                <!-- Setup Panel -->
                <v-card
                  v-if="hasPublishedAssignment"
                  class="pt-5 px-5 mx-auto blue lighten-5 rounded-lg"
                  outlined
                  :key="item.title"
                >
                  <p class="pb-0">
                    <strong>Note:</strong> You are currently collecting assignment submissions. Some setup functionality may not be available to avoid disrupting the experiment.
                  </p>
                </v-card>
                <div class="container-section-summary px-5">
                  <div class="panel-overview py-6">
                    <div
                      class="panelInformation d-flex flex-column justify-center"
                    >
                      <div>
                        <v-img
                          v-if="item.image"
                          :src="item.image"
                          class="icon-section-summary mr-6"
                          :alt="item.title"
                          style="margin-top: 2px !important; margin-right: 8px !important;"
                        />
                        <h2 class="header-section-summary mb-0">{{ item.title }}</h2>
                      </div>
                      <span v-if="item.description">
                        {{ item.description }}
                      </span>
                    </div>
                  </div>
                </div>
                <template v-if="item.tab === 'status'">
                  <experiment-summary-status
                    :experiment="experiment"
                  />
                </template>
                <template v-if="item.tab === 'assignment'">
                  <div class="section-exposure-sets px-5">
                    <div
                      v-if="!singleConditionExperiment"
                      class="panelInformation d-flex flex-column justify-center"
                    >
                      <h3 class="mb-0">Exposure Sets</h3>
                      <p
                        v-if="exposures"
                        class="pb-0"
                      >
                        Because you have <strong>{{ conditionCount }}</strong> (<a @click="handleEdit('ExperimentDesignConditions', item.tab)" >edit</a>)
                        and would like your students to be <strong>{{ exposureText[experiment.exposureType] }}</strong>
                        ({{ exposureType[experiment.exposureType] }}) (<a @click="handleEdit('ExperimentDesignConditions', item.tab)">edit</a>),
                        we set you up with {{ exposures.length }} exposure sets.
                        <v-tooltip top>
                          <template v-slot:activator="{ on, attrs }">
                            <a v-bind="attrs" v-on="on">
                              What is an exposure set?
                            </a>
                          </template>
                          <span>
                            <strong class="d-block">What is an exposure set?</strong>
                            An "exposure set" exposes a student to a specific condition during a specific time period. Students will change conditions between exposure sets, and the order
                            of conditions across exposure sets will be randomly assigned to different students.
                            An exposure set contains one or more assignments, and there must be an equal number of assignments in each exposure set in order to balance the experiment.
                          </span>
                        </v-tooltip>
                      </p>
                      <span
                        v-show="showBalanced"
                      >
                        Your exposure sets are currently:
                        <v-chip
                          label
                          outlined
                          class="mr-2">
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
                        <v-tooltip top>
                          <template v-slot:activator="{ on, attrs }">
                            <a v-bind="attrs" v-on="on">
                              What does this mean?
                            </a>
                          </template>
                          <span v-if="balanced">
                            <strong class="d-block">Balanced Exposure Sets</strong>
                            Your exposure sets contain all the same number assignments, and assignments contain the same number of treatments. Great work!</span
                          >
                          <span v-if="!balanced">
                            <strong class="d-block">Unbalanced Exposure Sets</strong>
                            A balanced experiment needs to have the same number of assignments within each exposure set, and a treatment for each condition within each assignment.
                            This will allow your students to be exposed to every condition, but in different orders, so you can compare how the different conditions affected each
                            individual student.
                          </span>
                        </v-tooltip>
                      </span>
                    </div>
                    <experiment-assignments
                      :experiment="experiment"
                      :balanced="balanced"
                      :loaded="loaded"
                      :activeExposureSet="exposureSet"
                    />
                  </div>
                </template>
                <template
                  v-if="item.tab !== 'status' && item.tab !== 'assignment'"
                >
                  <table>
                    <tr
                      v-for="section in sectionValuesMap[item.title]"
                      :key="section.title"
                      class="tableRow"
                    >
                      <td class="leftData col-4">
                        <template>
                          <div class="detail">
                            <span class="heading">{{ section.title }}</span>
                            <a @click="handleEdit(section.editSection, item.tab)">EDIT</a>
                          </div>
                        </template>
                      </td>
                      <td class="col-7 rightData">
                        <!-- String Data -->
                        <!-- For Experiment Title and Description -->
                        <template v-if="section.type === 'string'">
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
                            <span class="conditionName">
                              Condition {{ index + 1 }}
                            </span>
                            <br />
                            <v-chip
                              label
                              :color="conditionColorMapping[condition.name]"
                            >
                              {{ condition.name }}
                            </v-chip>
                            <v-chip
                              class="px-3 py-1  ml-3 defaultPill"
                              color="primary"
                              v-show="condition.defaultCondition"
                            >
                              <v-icon>mdi-check</v-icon>
                              <span>Default</span>
                            </v-chip>
                          </label>
                        </template>
                        <!-- Constant values -->
                        <!-- For Experiment Type -->
                        <template v-if="section.type === 'constant'">
                          <template v-if="section.description === 'WITHIN'">
                            <img
                              src="@/assets/all_conditions.svg"
                              alt="all conditions"
                              class="constantImage mb-2"
                            />
                            <span class="conditionType mb-2">
                              All conditions
                            </span>
                            <p class="conditionDetail">
                              All students are exposed to every condition, in different orders. This way you can compare how the different conditions affected each individual
                              student. This is called a within-subject design.
                            </p>
                          </template>
                          <template v-if="section.description === 'BETWEEN'">
                            <img
                              src="@/assets/one_condition.svg"
                              alt="one conditions"
                              class="constantImage mb-2"
                            />
                            <span class="conditionType mb-2">
                              Only one condition
                            </span>
                            <p class="conditionDetail">
                              Each student is only exposed to one condition, so that you can compare how the different conditions affected different students. This is called a
                              between-subjects design.
                            </p>
                          </template>
                        </template>
                        <!-- Participation data -->
                        <template v-if="section.type === 'participation'">
                          <!-- Consent Participation -->
                          <template v-if="section.description === 'CONSENT'">
                            Informed Consent
                            <button
                              v-if="!pdfLoading"
                              class="pdfButton"
                              @click="openPDF"
                            >
                              {{ experiment.consent.title }}
                            </button>
                            <Spinner v-if="pdfLoading"></Spinner>
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
            </v-tab-item>
          </v-tabs-items>
        </v-col>
      </v-row>
      <vue-pdf-embed
        v-if="loadPdfFrame"
        :source="'data:application/pdf;base64,' + pdfFile"
      />
    </v-container>
    <v-container v-else>
      no experiment
    </v-container>
  </div>
</template>

<script>
import store from "@/store";
import { mapGetters, mapActions } from "vuex";
import { saveAs } from "file-saver";
import ExperimentSummaryStatus from "@/views/ExperimentSummaryStatus";
import ExperimentAssignments from "@/views/ExperimentAssignments";
import Spinner from "@/components/Spinner";
import VuePdfEmbed from 'vue-pdf-embed/dist/vue2-pdf-embed';

export default {
  name: "ExperimentSummary",
  components: {
    ExperimentSummaryStatus,
    ExperimentAssignments,
    Spinner,
    VuePdfEmbed
  },
  computed: {
    ...mapGetters({
      experiment: "experiment/experiment",
      conditions: "experiment/conditions",
      exposures: "exposures/exposures",
      assignments: "assignment/assignments",
      exportdata: "exportdata/exportData",
      conditionColorMapping: "condition/conditionColorMapping",
      editMode: "navigation/editMode"
    }),
    // Higher Level Section Values
    sectionValuesMap() {
      return {
        Design: this.designDetails,
        Participants: this.participantDetails,
        Assignments: this.assignmentDetails,
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
        .map((exp) => {
          return this.assignments.filter((a) => a.exposureId === exp.exposureId)
            .length;
        })
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
          title: "Your Assignments",
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
      // if active tab was previously selected, return it, otherwise default to assignment tab
      return this.editMode?.callerPage?.tab || 'assignment';
    },
    activeExposureSet() {
      // if active tab was previously selected, return it, otherwise default to assignment tab
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
    }
  },

  data: () => ({
    tab: null,
    items: ["status", "design", "participant", "assignment"],
    // Expansion Tab Header Values
    setupTabs: [
      {
        title: "Experiment Status",
        tab: "status",
        description:
          "Once your experiment is running, you will see status updates below",
      },
      {
        title: "Design",
        tab: "design",
        description: "The basic design of your experiment",
        image: require("@/assets/design_summary.svg"),
      },
      {
        title: "Participants",
        tab: "participant",
        description:
          "How students in your class become participants in your experiment",
        image: require("@/assets/participants_summary.svg"),
      },
      {
        title: "Assignments",
        tab: "assignment",
        description: `Terracotta populates Canvas assignments with learning activities and
                      materials that change depending on who's looking at them, automatically
                      managing experimental variation within the treatments. Just create different
                      treatments within each assignment. To your students, it will look like
                      they're completing assignments as usual within Canvas.`,
        image: require("@/assets/assignments_summary.svg"),
      },
    ],
    conditionTreatments: {},
    conditionColors: [""],
    isLoading: true,
    exposureSet: 0,
    loadPdfFrame: false,
    pdfFile: null,
    pdfLoading: false
  }),
  watch: {
    pdfFile() {
      this.loadPdfFrame = true;
      this.pdfLoading = false;
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
      getZip: "exportdata/fetchExportData",
      resetAssignments: "assignment/resetAssignments",
      saveEditMode: "navigation/saveEditMode",
      deleteEditMode: "navigation/deleteEditMode"
    }),
    saveExit() {
      this.$router.push({ name: "Home" });
    },
    async exportData() {
      await this.getZip(this.experimentId);
      saveAs(
        this.exportdata,
        `Terracotta Experiment ${this.experiment.title} Export.zip`
      );
    },
    // Navigate to EDIT section
    async handleEdit(componentName, currentTab) {
      await this.saveEditMode({
        initialPage: componentName,
        callerPage: {
          name: 'ExperimentSummary',
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
    hasTreatment(conditionId, assignmentId) {
      const assignmentBasedOnConditions = this.conditionTreatments[
        +conditionId
      ];

      return (
        assignmentBasedOnConditions?.find(
          (assignment) => assignment.assignmentId === assignmentId
        ) !== undefined
      );
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
          experiment_id: this.experimentId,
          condition_id: conditionId,
          treatment_id: treatment?.data?.treatmentId,
          assessment_id: assessment?.data?.assessmentId,
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
    }
  },

  async mounted() {
    await this.resetAssignments();
    this.tab = this.setupTabs.findIndex((s) => s.tab === this.activeTab);
    this.exposureSet = this.activeExposureSet;
    await this.saveEditMode(null);

    await this.fetchExposures(this.experimentId);
    for (const e of this.exposures) {
      // add submissions to assignments request
      const submissions = true;
      await this.fetchAssignmentsByExposure([
        this.experimentId,
        e.exposureId,
        submissions,
      ]);
    }
    for (let c of this.conditions) {
      // find treatment for this condition
      for (let a of this.assignments) {
        this.conditionTreatments[c.conditionId] = a.treatments.find((t) => t.conditionId === c.conditionId);
        if (this.conditionTreatments[c.conditionId]) {
          break;
        }
      }
    }
    this.getAssignmentDetails();
    this.isLoading = false;
  },
  beforeRouteEnter(to, from, next) {
    return store
      .dispatch("experiment/fetchExperimentById", to.params.experiment_id)
      .then(next, next);
  },
  beforeRouteUpdate(to, from, next) {
    return store
      .dispatch("experiment/fetchExperimentById", to.params.experiment_id)
      .then(next, next);
  },
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
.assignmentExpansionPanel {
  &:not(:last-child) {
    border-bottom: 2px solid #e0e0e0 !important;
  }
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
</style>
