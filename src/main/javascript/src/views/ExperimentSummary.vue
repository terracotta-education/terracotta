<template>
  <div>
    <v-container v-if="experiment">
      <v-row class="my-1" justify="space-between">
        <v-col cols="8">
          <p class="header ma-0 pa-0">
            <v-img
              src="@/assets/terracotta_logo_mark.svg"
              class="mr-6"
              alt="Terracotta Logo"
              max-height="30"
              max-width="27"
            />
            <span>{{ experiment.title }}</span>
          </p>
        </v-col>
        <div class="header ma-0 pa-0">
          <v-btn color="primary" elevation="0" @click="exportData()"
            >Export Data</v-btn
          >
          <v-btn
            color="primary"
            elevation="0"
            class="saveButton ml-4"
            @click="saveExit()"
            >SAVE & CLOSE</v-btn
          >
        </div>
      </v-row>
      <v-row>
        <v-col cols="12">
          <v-divider></v-divider>
          <v-tabs v-model="tab" elevation="0">
            <v-tab v-for="item in items" :key="item">
              {{ item }}
            </v-tab>
          </v-tabs>
          <v-divider class="mb-6"></v-divider>
          <v-tabs-items v-model="tab">
            <v-tab-item class="py-3" v-for="item in items" :key="item">
              <!-- Status Panel -->
              <template v-if="item === 'status'">
                <experiment-summary-status :experiment="experiment" />
              </template>
              <!-- Setup Panel -->
              <template v-if="item === 'setup'">
                <v-card
                  class="pt-5 px-5 mx-auto blue lighten-5 rounded-lg"
                  outlined
                >
                  <p class="pb-0">
                    <strong>Note:</strong> You are currently collecting
                    assignment submissions. Some setup functionality may be
                    disabled to not disrupt the experiment.
                  </p>
                </v-card>
                <!-- Design, Participants and Assignment Panels -->
                <v-expansion-panels class="mt-5 v-expansion-panels--icon" flat>
                  <v-expansion-panel
                    v-for="panel in setupPanels"
                    :key="panel.title"
                  >
                    <v-expansion-panel-header>
                      <div class="panel-overview">
                        <div class="a1">
                          <v-img
                            :src="panel.image"
                            class="mr-6"
                            :alt="panel.title"
                            min-height="55"
                            min-width="50"
                          />
                        </div>
                        <div class="panelInformation">
                          <h2>{{ panel.title }}</h2>
                          <span>{{ panel.description }}</span>
                        </div>
                      </div>
                    </v-expansion-panel-header>
                    <v-expansion-panel-content>
                      <table>
                        <tr
                          v-for="item in sectionValuesMap[panel.title]"
                          :key="item.title"
                          class="tableRow"
                        >
                          <td class="leftData col-4">
                            <template>
                              <div class="detail">
                                <span class="heading">{{ item.title }}</span>
                                <a @click="handleEdit(item.editSection)"
                                  >EDIT</a
                                >
                              </div>
                            </template>
                          </td>
                          <td class="col-7 rightData">
                            <!-- String Data -->
                            <!-- For Experiment Title and Description -->
                            <template v-if="item.type === 'string'">
                              {{ item.description }}
                            </template>
                            <!-- Array data -->
                            <!-- For Experiment Condition Details -->
                            <template
                              v-if="item.type === 'array'"
                              class="arrayData"
                            >
                              <label
                                v-for="(condition, index) in item.description"
                                :key="condition.conditionId"
                                :for="`condition-${condition.conditionId}`"
                                class="text-left conditionLabel"
                              >
                                <span class="conditionName"
                                  >Condition {{ index + 1 }}</span
                                >
                                <br />
                                <span>{{ condition.name }}</span>
                                <span
                                  class="rounded-pill px-3 py-1 primary ml-3 defaultPill"
                                  v-show="condition.defaultCondition"
                                >
                                  <v-icon>mdi-check</v-icon>
                                  <span>Default</span>
                                </span>
                              </label>
                            </template>
                            <!-- Constant values -->
                            <!-- For Experiment Type -->
                            <template v-if="item.type === 'constant'">
                              <template v-if="item.description === 'WITHIN'">
                                <img
                                  src="@/assets/all_conditions.svg"
                                  alt="all conditions"
                                  class="constantImage mb-2"
                                />
                                <span class="conditionType mb-2"
                                  >All conditions</span
                                >
                                <p class="conditionDetail">
                                  All students are exposed to every condition,
                                  in different orders. This way you can compare
                                  how the different conditions affected each
                                  individual student. This is called a
                                  within-subject design.
                                </p>
                              </template>
                              <template v-if="item.description === 'BETWEEN'">
                                <img
                                  src="@/assets/one_condition.svg"
                                  alt="one conditions"
                                  class="constantImage mb-2"
                                />
                                <span class="conditionType mb-2"
                                  >Only one condition</span
                                >
                                <p class="conditionDetail">
                                  Each student is only exposed to one condition,
                                  so that you can compare how the different
                                  conditions affected different students. This
                                  is called a between-subjects design.
                                </p>
                              </template>
                            </template>
                            <!-- Assignment data -->
                            <template v-if="item.type === 'assignments'">
                              <template v-for="(exposure, index) in exposures">
                                <div
                                  :key="exposure.exposureId"
                                  class="assignmentExpansion"
                                >
                                  <span class="exposureSetName">
                                    Exposure Set {{ index + 1 }}
                                  </span>
                                  <br />
                                  <div
                                    class="groupNames"
                                    :key="group"
                                    v-for="group in sortedGroups(
                                      exposure.groupConditionList
                                    )"
                                  >
                                    {{ group }} will receive
                                    <v-chip
                                      class="ma-2"
                                      :color="conditionColorMapping[groupNameConditionMapping(
                                          exposure.groupConditionList
                                        )[group]]"
                                      label
                                      :key="
                                        group
                                      "
                                    >
                                      <!-- Sorted Group Names -->
                                      {{
                                        groupNameConditionMapping(
                                          exposure.groupConditionList
                                        )[group]
                                      }}</v-chip
                                    >
                                  </div>
                                  <!-- Assignment Expansion Panels -->
                                  <v-expansion-panels
                                    class="v-expansion-panels--outlined"
                                    flat
                                  >
                                    <v-expansion-panel
                                      class="assignmentExpansionPanel"
                                      v-for="assignment in assignments.filter(
                                        (a) =>
                                          a.exposureId === exposure.exposureId
                                      )"
                                      :key="assignment.assignmentId"
                                    >
                                      <v-expansion-panel-header style="display:flex;flex-direction: row">
                                        {{ assignment.title }} ({{
                                          (assignment.treatments &&
                                            assignment.treatments.length) ||
                                            0
                                        }}/{{ conditions.length || 0 }})
                                      </v-expansion-panel-header>
                                      <v-expansion-panel-content>
                                        <v-list class="pa-0">
                                          <v-list-item
                                            class="justify-center px-0"
                                            v-for="condition in conditions"
                                            :key="condition.conditionId"
                                          >
                                            <v-list-item-content>
                                              <p
                                                class="ma-0 pa-0 assignmentConditionName"
                                              >
                                                {{ condition.name }}
                                              </p>
                                            </v-list-item-content>

                                            <v-list-item-action>
                                              <!-- Assignment Edit Button -->
                                              <template
                                                v-if="
                                                  hasTreatment(
                                                    condition.conditionId,
                                                    assignment.assignmentId
                                                  )
                                                "
                                              >
                                                <v-btn
                                                  icon
                                                  outlined
                                                  text
                                                  tile
                                                  @click="
                                                    goToBuilder(
                                                      condition.conditionId,
                                                      assignment.assignmentId
                                                    )
                                                  "
                                                >
                                                  <v-icon>mdi-pencil</v-icon>
                                                </v-btn>
                                              </template>
                                              <!-- Assignment Select Button -->
                                              <template v-else>
                                                <v-btn
                                                  color="primary"
                                                  outlined
                                                  @click="
                                                    goToBuilder(
                                                      condition.conditionId,
                                                      assignment.assignmentId
                                                    )
                                                  "
                                                  >Create
                                                </v-btn>
                                              </template>
                                            </v-list-item-action>
                                          </v-list-item>
                                        </v-list>
                                      </v-expansion-panel-content>
                                    </v-expansion-panel>
                                  </v-expansion-panels>
                                </div>
                              </template>
                            </template>
                            <!-- Participation data -->
                            <template v-if="item.type === 'participation'">
                              <!-- Consent Participation -->
                              <template v-if="item.description === 'CONSENT'">
                                Informed Consent
                                <button class="pdfButton" @click="openPDF">
                                  {{ experiment.consent.title }}
                                </button>
                              </template>
                              <!-- Manual Participation -->
                              <template
                                v-else-if="item.description === 'MANUAL'"
                              >
                                Manual
                                <br />
                                <span
                                  >{{ experiment.acceptedParticipants }}
                                  students selected to participate out of
                                  {{ experiment.potentialParticipants }}
                                  students enrolled
                                </span>
                              </template>
                              <!-- All Participation -->
                              <template v-else>
                                Include All Students
                                <br />
                                <span
                                  >{{ experiment.potentialParticipants }}
                                  students selected to participate out of
                                  {{ experiment.potentialParticipants }}
                                  students enrolled
                                </span>
                              </template>
                            </template>
                          </td>
                        </tr>
                      </table>
                    </v-expansion-panel-content>
                  </v-expansion-panel>
                </v-expansion-panels>
              </template>
            </v-tab-item>
          </v-tabs-items>
        </v-col>
      </v-row>
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

export default {
  name: "ExperimentSummary",
  components: { ExperimentSummaryStatus },
  computed: {
    ...mapGetters({
      experiment: "experiment/experiment",
      conditions: "experiment/conditions",
      exposures: "exposures/exposures",
      assignments: "assignment/assignments",
      consent: "consent/consent",
      exportdata: "exportdata/exportData",
      conditionColorMapping: "condition/conditionColorMapping",
    }),
    // Higher Level Section Values
    sectionValuesMap() {
      return {
        Design: this.designDetails,
        Participants: this.participantDetails,
        Assignments: this.assignmentDetails,
      };
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
  },

  data: () => ({
    tab: null,
    items: ["status", "setup"],
    // Expansion Tab Header Values
    setupPanels: [
      {
        title: "Design",
        description: "The basic design of your experiment",
        image: require("@/assets/design_summary.svg"),
      },
      {
        title: "Participants",
        description:
          "How students in your class become participants in your experiment",
        image: require("@/assets/participants_summary.svg"),
      },
      {
        title: "Assignments",
        description: "All experiment assignments",
        image: require("@/assets/assignments_summary.svg"),
      },
    ],
    conditionTreatments: {},
  }),
  methods: {
    ...mapActions({
      fetchExposures: "exposures/fetchExposures",
      fetchAssignmentsByExposure: 'assignment/fetchAssignmentsByExposure',
      checkTreatment: "treatment/checkTreatment",
      createTreatment: "treatment/createTreatment",
      createAssessment: "assessment/createAssessment",
      getConsentFile: "consent/getConsentFile",
      getZip: "exportdata/fetchExportData",
    }),
    saveExit() {
      this.$router.push({ name: "Home" });
    },
    async exportData() {
      await this.getZip(this.experiment.experimentId);
      saveAs(this.exportdata, `Terracotta Experiment ${this.experiment.title} Export.zip`);
    },
    // Navigate to EDIT section
    handleEdit(componentName) {
      this.$router.push({ name: componentName });
    },
    openPDF() {
      // Second Parameter intentionally left blank
      let pdfWindow = window.open("", "", "_blank");
      pdfWindow.opener = null;
      pdfWindow.document.write(
        "<iframe width='100%' height='100%' src='data:application/pdf;base64, " +
          encodeURI(this.consent.file) +
          "'></iframe>"
      );

      return false;
    },
    async getAssignmentDetails() {
      await this.fetchExposures(this.experiment.experimentId);
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
          this.experiment.experimentId,
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
          this.experiment.experimentId,
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
      // create the assessment
      const assessment = await this.handleCreateAssessment(
        conditionId,
        treatment?.data
      );

      // show an alert if there's a problem creating the treatment or assessment
      if (!treatment || !assessment) {
        this.$swal("There was a problem creating your assessment");
        return false;
      }

      // send user to builder with the treatment and assessment ids
      this.$router.push({
        name: "TerracottaBuilder",
        params: {
          experiment_id: this.experiment.experimentId,
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
  },

  async created() {
    this.tab = this.$router.currentRoute.name === "ExperimentSummary" ? 1 : 0;
    
    await this.fetchExposures(this.experiment.experimentId);
    // Commenting the change for Unblock QA
    // if (this.assignments > 0) {
      for (let c of this.conditions) {
        const t = await this.checkTreatment([
          this.experiment.experimentId,
          c.conditionId,
          this.assignments[0].assignmentId,
        ]);
        this.conditionTreatments[c.conditionId] = t?.data;
      }
      for (const e of this.exposures) {
        // add submissions to assignments request
        const submissions = true
        await this.fetchAssignmentsByExposure([this.experiment.experimentId, e.exposureId, submissions])
      }
      this.getAssignmentDetails();
      await this.getZip(this.experiment.experimentId);
      if(this.experiment.participationType === 'CONSENT') {
        await this.getConsentFile(this.experiment.experimentId);
      }
    // }
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
table {
  font-size: 16px;
  color: black;
  border-spacing: 0 25px;
  .leftData {
    white-space: nowrap;
    text-align: left;
    vertical-align: top;
    padding: 0 25px;
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
</style>
