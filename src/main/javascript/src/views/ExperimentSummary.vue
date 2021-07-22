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
                            <template v-if="item.type === 'string'">
                              {{ item.description }}
                            </template>
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
                            <template v-if="item.type === 'assignments'">
                              <template v-for="(exposure, index) in exposures">
                                <div :key="exposure.exposureId">
                                  <br />
                                  Exposure Set {{ index + 1 }}
                                  <br />
                                  <template
                                    v-for="group in exposure.groupConditionList"
                                  >
                                    {{ group.groupName }} will receive
                                    <v-chip
                                      class="ma-2"
                                      color="primary"
                                      label
                                      :key="
                                        group.groupName + group.conditionName
                                      "
                                    >
                                      {{ group.conditionName }}</v-chip
                                    >
                                  </template>
                                  <br />
                                  {{
                                    assignments.filter(
                                      (a) =>
                                        a.exposureId === exposure.exposureId
                                    )
                                  }}
                                  <v-expansion-panels
                                    class="v-expansion-panels--outlined mb-7"
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
                                      {{ assignment }}
                                      <br />
                                      {{  checkConditionTreatments(assignment.assignmentId) }}
                                        <!-- <v-list class="pa-0">
                                          <v-list-item
                                            class="justify-center px-0"
                                            v-for="condition in conditions"
                                            :key="condition.conditionId"
                                          >
                                            <v-list-item-content>
                                              <p class="ma-0 pa-0">
                                                {{ condition.name }}
                                              </p>
                                            </v-list-item-content>

                                            <v-list-item-action>
                                              <template>
                                                <v-btn
                                                  icon
                                                  outlined
                                                  text
                                                  tile
                                                  @click="() => console.log('11')"
                                                >
                                                  <v-icon>mdi-pencil</v-icon>
                                                </v-btn>
                                              </template>
                                              <template>
                                                <v-btn
                                                  color="primary"
                                                  outlined
                                                  @click="() => console.log('22')"
                                                  >Select
                                                </v-btn>
                                              </template>
                                            </v-list-item-action>
                                          </v-list-item>
                                        </v-list> -->
                                      </v-expansion-panel-content>
                                    </v-expansion-panel>
                                  </v-expansion-panels>
                                  <!-- <v-expansion-panels
                                    class="v-expansion-panels--outlined mb-7"
                                    flat
                                  >
                                    <v-expansion-panel
                                      class=""
                                      
                                      :key="assignment.assignmentId"
                                    >
                                      <v-expansion-panel-header
                                        >{{ assignment.title }} ({{
                                          (assignment.treatments &&
                                            assignment.treatments.length) ||
                                            0
                                        }}/{{
                                          exposure.groupConditionList.length
                                        }})</v-expansion-panel-header
                                      >
                                      <v-expansion-panel-content>
                                        Hello World
                                        <v-list class="pa-0">
                                          <v-list-item
                                            class="justify-center px-0"
                                            v-for="condition in conditions"
                                            :key="condition.conditionId"
                                          >
                                            <v-list-item-content>
                                              <p class="ma-0 pa-0">
                                                {{ condition.name }}
                                              </p>
                                            </v-list-item-content>

                                            <v-list-item-action>
                                              <template>
                                                <v-btn icon outlined text tile>
                                                  <v-icon>mdi-pencil</v-icon>
                                                </v-btn>
                                              </template>
                                              <template>
                                                <v-btn color="primary" outlined
                                                  >Select
                                                </v-btn>
                                              </template>
                                            </v-list-item-action>
                                          </v-list-item>
                                        </v-list>
                                      </v-expansion-panel-content>
                                    </v-expansion-panel>
                                  </v-expansion-panels> -->
                                  <br />

                                  <!-- {{ assignments }} -->
                                </div>
                              </template>
                            </template>
                          </td>
                        </tr>
                      </table>

                      <br />
                      {{ experiment }}
                      <br />
                      {{ exposures }}
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
          title: "Experiment Title",
          description: this.experiment.title,
          editSection: "ExperimentDesignTitle",
          type: "string",
        },
      ];
    },
    assignmentDetails() {
      return [
        {
          title: "Your Assignments",
          description: this.getAssignmentDetails(),
          editSection: "ExperimentDesignTitle",
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
    conditionTreatments: [],
  }),
  methods: {
    ...mapActions({
      fetchExposures: "exposures/fetchExposures",
      fetchAssignmentsByExposure: "assignment/fetchAssignmentsByExposure",
      deleteAssignment: "assignment/deleteAssignment",
      checkTreatment: "treatment/checkTreatment",
    }),
    handleEdit(componentName) {
      console.log("Hello World", componentName);
      this.$router.push({ name: componentName });
    },
    async getAssignmentDetails() {
      await this.fetchExposures(this.experiment.experimentId);
      console.log("Fetch Exposures: ", this.exposures);
      // const returnObject = [...this.exposures];
      return this.exposures;
    },
    async checkConditionTreatments(assignmentId) {
      // loop conditions and build condition/treatment manifest
      // (templates don't like async methods for conditions)
      console.log('Here11:', assignmentId);
      for (let c of this.conditions) {
        const t = await this.checkTreatment([this.experiment.experimentId, c.conditionId, assignmentId])
        console.log('t: ', JSON.stringify(t))
        if (t?.data?.find(o=>parseInt(o.assignmentId)===assignmentId)) {
          const ctObj = {
            treatment: t.data ? t.data.find(o=>parseInt(o.assignmentId)===assignmentId) : null,
            condition: c
          }

        console.log('ctOBJ', JSON.stringify(ctObj));
          this.conditionTreatments = [
            ...this.conditionTreatments.filter((o) =>
              o.conditionId === ctObj.conditionId &&
              o.treatment.assignmentId === assignmentId
            ),
            {...ctObj}
          ];
        }

        console.log('Here' -  this.conditionTreatments)
        return this.conditionTreatments
      }
    },
    // async getAssignments(exposureId) {
    //   console.log("exposureId", exposureId);
    //   const a = "";
    //   await this.fetchAssignmentsByExposure(
    //     this.experiment.experimentId,
    //     exposureId
    //   ).then((data) => console.log("Data is: ", data));
    //   console.log("a", a, JSON.stringify(this.assignments));
    //   return a;
    // },
  },
  async created() {
    this.tab = this.$router.currentRoute.name === "ExperimentSummary" ? 1 : 0;
    // await this.checkConditionTreatments();
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
  }
}
</style>
