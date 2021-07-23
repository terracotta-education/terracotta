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
              <template v-if="item === 'status'">
                <experiment-summary-status :experiment="experiment" />
              </template>
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
                <v-expansion-panels class="mt-5 v-expansion-panels--icon" flat>
                  <v-expansion-panel
                    v-for="panel in setupPanels"
                    :key="panel.title"
                  >
                    <v-expansion-panel-header>
                      <div class="panel-overview">
                        <h2>{{ panel.title }}</h2>
                        <p>{{ panel.description }}</p>
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
                            <!-- string data -->
                            <template v-if="item.type === 'string'">
                              {{ item.description }}
                            </template>
                            <!-- array data -->
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
                            <!-- constant values -->
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
                            <!-- assignment data -->
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
                                    :key="group.groupId"
                                    v-for="group in sortedGroups(
                                      exposure.groupConditionList
                                    )"
                                  >
                                    {{ group }} will receive
                                    <v-chip
                                      class="ma-2"
                                      color="primary"
                                      label
                                      :key="
                                        group.groupName + group.conditionName
                                      "
                                    >
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
                                      v-for="assignment in assignments.filter(
                                        (a) =>
                                          a.exposureId === exposure.exposureId
                                      )"
                                      :key="assignment.assignmentId"
                                    >
                                      <v-expansion-panel-header>
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
                                                  >Select
                                                </v-btn>
                                              </template>
                                            </v-list-item-action>
                                          </v-list-item>
                                        </v-list>
                                      </v-expansion-panel-content>
                                    </v-expansion-panel>
                                  </v-expansion-panels>
                                  <br />

                                  <!-- {{ assignments }} -->
                                </div>
                              </template>
                            </template>
                            <!-- participation data -->
                             <template v-if="item.type === 'participation'">
                              <template v-if="item.description === 'CONSENT'">
                                Informed Consent
                                <br />
                                {{ experiment.consent.title }}
                              </template>
                              <template v-else-if="item.description === 'MANUAL'">
                                Manual
                                <br />
                                10 students selected to participate out of 14 students enrolled
                              </template>
                              <template v-else>
                                Include All Students
                                <br />
                                10 students selected to participate out of 14 students enrolled
                              </template>
                            </template>
                          </td>
                        </tr>
                      </table>

                      <br />
                      {{ experiment }}
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
    }),
    sectionValuesMap() {
      return {
        Design: this.designDetails,
        Participants: this.participantDetails,
        Assignments: this.assignmentDetails,
      };
    },
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
    participantDetails() {
      return [
        {
          title: "SelectionMethod",
          description: 'CONSENT',
          editSection: "ExperimentParticipationSelectionMethod",
          type: "participation",
        },
      ];
    },
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
    setupPanels: [
      {
        title: "Design",
        description: "The basic design of your experiment",
      },
      {
        title: "Participants",
        description:
          "How students in your class become participants in your experiment",
      },
      {
        title: "Assignments",
        description: "All experiment assignments",
      },
    ],
    conditionTreatments: {},
  }),
  methods: {
    ...mapActions({
      fetchExposures: "exposures/fetchExposures",
      fetchAssignmentsByExposure: "assignment/fetchAssignmentsByExposure",
      deleteAssignment: "assignment/deleteAssignment",
      checkTreatment: "treatment/checkTreatment",
      createTreatment: "treatment/createTreatment",
      createAssessment: "assessment/createAssessment",
      fetchAssignment: "assignment/fetchAssignment",
    }),
    handleEdit(componentName) {
      this.$router.push({ name: componentName });
    },
    async getAssignmentDetails() {
      await this.fetchExposures(this.experiment.experimentId);
      console.log("Fetch Exposures: ", JSON.stringify(this.exposures));
      console.log("Assignments", JSON.stringify(this.assignments));

      console.log("Final Treatements:", this.conditionTreatments);
      return this.exposures;
    },
    hasTreatment(conditionId, assignmentId) {
      console.log(
        "\n\n HAS TREATMENTConditionId + assignmentId",
        conditionId,
        assignmentId
      );
      const assignmentBasedOnConditions = this.conditionTreatments[
        +conditionId
      ];

      console.log("Treatments: ", this.assignmentBasedOnConditions);
      console.log("Treatments: ", this.assignmentBasedOnConditions);
      console.log(
        "Return Value: ",
        assignmentBasedOnConditions?.find(
          (assignment) => assignment.assignmentId === assignmentId
        ) !== undefined
      );
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
        alert("There was a problem creating your assessment");
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
    groupNameConditionMapping(groupConditionList) {
      const groupConditionMap = {};
      console.log("GCL: ", groupConditionList);
      groupConditionList?.map(
        (group) => (groupConditionMap[group.groupName] = group.conditionName)
      );
      return groupConditionMap;
    },
    sortedGroups(groupConditionList) {
      const newGroups = groupConditionList?.map((group) => group.groupName);
      console.log("Sorted Group", newGroups?.sort());
      return newGroups?.sort();
    },
  },

  async created() {
    this.tab = this.$router.currentRoute.name === "ExperimentSummary" ? 1 : 0;
    console.log("Created", this.experiment);
    await this.fetchExposures(this.experiment.experimentId);
    console.log("Created-EXP", this.exposures);
    for (let c of this.conditions) {
      console.log("c is: ", JSON.stringify(c));
      const t = await this.checkTreatment([
        this.experiment.experimentId,
        c.conditionId,
        this.assignments[0].assignmentId,
      ]);
      console.log("t: -  ", JSON.stringify(t));
      this.conditionTreatments[c.conditionId] = t?.data;
      console.log("Treatments: ", this.conditionTreatments);
    }
    console.log("Final Treatements:", this.conditionTreatments);
    this.getAssignmentDetails();
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
  flex-direction: column;
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
    // white-space: nowrap;
    max-width: max-content;
    flex-direction: column;
    text-align: left;
    border-left: 1px solid #e6e6e6;
    padding: 0 12px !important;
    .conditionName {
      font-size: 16px;
      font-weight: 700;
    }
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
    .conditionType {
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
    .exposureSetName {
      font-size: 16px;
      font-weight: 700;
    }
    .assignmentConditionName {
      text-align: left;
    }
  }
}
</style>
