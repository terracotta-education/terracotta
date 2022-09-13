<template>
  <div>
    <v-container class="px-0" v-if="experiment">
      <v-row>
        <v-col cols="12">
          <v-tabs v-model="tab" elevation="0" show-arrows>
            <v-tab v-for="(exposure, index) in exposures" :key="index">
              <div class="d-flex flex-column align-start ">
               <div class="">Set {{ index + 1 }}</div>
                <div class="d-block red--text mt-2">
                  {{ getAssignmentsForExposure(exposure).length }} assignments
                </div>
              </div>
            </v-tab>
          </v-tabs>
          <v-divider></v-divider>
          <v-tabs-items v-model="tab">
            <v-tab-item
              class="py-3 px-3"
              v-for="(exposure, index) in exposures"
              :key="index"
            >
              <h3>Assignments</h3>
              <!--<pre>{{ assignments }}</pre>-->
              <template>
                <v-data-table
                  :headers="assignmentHeaders"
                  :items="getAssignmentsForExposure(exposure)"
                  :single-expand="singleExpand"
                  :expanded.sync="expanded"
                  hide-default-footer
                  item-key="title"
                  show-expand
                  class="mx-3 mb-5 mt-3"
                >
                  <template v-slot:expanded-item="{ item }">
                    <td
                      :colspan="assignmentHeaders.length"
                      class="treatments-table-container"
                    >
                      <v-data-table
                        :headers="treatmentHeaders"
                        :items="item.treatments"
                        hide-default-header
                        hide-default-footer
                        item-key="title"
                        class="grey lighten-5"
                      >
                        <template v-slot:item.title="{ item }">
                          {{ item.assessmentDto.title }}
                        </template>
                        <template v-slot:item.actions="{ item }">
                          <template
                            v-if="
                              hasTreatment(item.conditionId, item.assignmentId)
                            "
                          >
                            <v-btn
                              text
                              tile
                              @click="
                                goToBuilder(item.conditionId, item.assignmentId)
                              "
                            >
                              <v-icon>mdi-pencil</v-icon>
                              Edit
                            </v-btn>
                          </template>
                        </template>
                      </v-data-table>
                    </td>
                  </template>
                  <!-- eslint-disable-next-line -->
                  <template v-slot:item.treatments="{ item }">
                    {{ item.treatments.length }} / {{ conditions.length }}
                  </template>
                  <template v-slot:item.actions>
                    <v-btn
                      text
                      tile
                      @click="handleEdit('AssignmentExposureSets')"
                    >
                      <v-icon>mdi-pencil</v-icon>
                      Edit
                    </v-btn>
                    <v-menu offset-y>
                      <template v-slot:activator="{ on, attrs }">
                        <v-btn icon text tile v-bind="attrs" v-on="on">
                          <v-icon>mdi-dots-horizontal</v-icon>
                        </v-btn>
                      </template>
                      <v-list>
                        <v-list-item link>
                          <v-list-item-title
                            ><v-icon>mdi-content-duplicate</v-icon
                            >Duplicate</v-list-item-title
                          >
                        </v-list-item>
                        <v-list-item link>
                          <v-list-item-title
                            ><v-icon>mdi-delete</v-icon
                            >Delete</v-list-item-title
                          >
                        </v-list-item>
                      </v-list>
                    </v-menu>
                  </template>
                </v-data-table>
              </template>
              <!--
              <template>
                <div
                  :key="exposure.exposureId"
                  class="assignmentExpansion mx-3"
                >
                  <v-expansion-panels class="v-expansion-panels--outlined" flat>
                    <v-expansion-panel
                      class="assignmentExpansionPanel"
                      v-for="assignment in assignments.filter(
                        (a) => a.exposureId === exposure.exposureId
                      )"
                      :key="assignment.assignmentId"
                    >
                      <v-expansion-panel-header
                        style="display:flex;flex-direction: row"
                      >
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
                              <p class="ma-0 pa-0 assignmentConditionName">
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
              </template>-->
              <h3 class="my-4">Design</h3>
              <v-card
                class="px-5 py-5 rounded-lg mx-3 mb-5 d-inline-block"
                outlined
              >
                <div
                  class="groupNames"
                  :key="group"
                  v-for="group in sortedGroups(exposure.groupConditionList)"
                >
                  {{ group }} will receive
                  <v-chip
                    class="ma-2"
                    :color="
                      conditionColorMapping[
                        groupNameConditionMapping(exposure.groupConditionList)[
                          group
                        ]
                      ]
                    "
                    label
                    :key="group"
                  >
                    <!-- Sorted Group Names -->
                    {{
                      groupNameConditionMapping(exposure.groupConditionList)[
                        group
                      ]
                    }}</v-chip
                  >
                </div>
                <a href="" class="text-decoration-none">+ MORE</a>
              </v-card>
            </v-tab-item>
          </v-tabs-items>
        </v-col>
      </v-row>
    </v-container>
  </div>
</template>

<script>
import store from "@/store";
import { mapGetters, mapActions } from "vuex";
import { saveAs } from "file-saver";

export default {
  name: "ExperimentAssignments",
  props: ["experiment"],
  computed: {
    ...mapGetters({
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
        Assignments: this.assignmentDetails,
      };
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
    conditionCount() {
      return `${this.experiment.conditions.length} condition${
        this.experiment.conditions.length > 1 ? "s" : ""
      }`;
    },
  },

  data: () => ({
    tab: 0,
    minTreatments: 2,
    conditionTreatments: {},
    conditionColors: [""],
    expanded: [],
    singleExpand: true,
    assignmentHeaders: [
      {
        text: "Assignment Name",
        align: "start",
        sortable: false,
        value: "title",
      },
      {
        text: "Treatments",
        value: "treatments",
      },
      {
        text: "Due Date",
        value: "dueDate",
      },
      {
        text: "Status",
        value: "status",
      },
      {
        text: "Actions",
        value: "actions",
      },
      { text: "", value: "data-table-expand" },
    ],
    treatmentHeaders: [
      {
        text: "Treatment Name",
        align: "start",
        sortable: false,
        value: "title",
      },
      {
        text: "Actions",
        align: "end",
        value: "actions",
      },
    ],
  }),
  methods: {
    ...mapActions({
      fetchExposures: "exposures/fetchExposures",
      fetchAssignmentsByExposure: "assignment/fetchAssignmentsByExposure",
      checkTreatment: "treatment/checkTreatment",
      createTreatment: "treatment/createTreatment",
      createAssessment: "assessment/createAssessment",
      getConsentFile: "consent/getConsentFile",
      getZip: "exportdata/fetchExportData",
    }),
    saveExit() {
      this.$router.push({ name: "Home" });
    },
    getAssignmentsForExposure(exp) {
      return this.assignments.filter((a) => a.exposureId === exp.exposureId);
    },
    async exportData() {
      await this.getZip(this.experiment.experimentId);
      saveAs(
        this.exportdata,
        `Terracotta Experiment ${this.experiment.title} Export.zip`
      );
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
    await this.fetchExposures(this.experiment.experimentId);
    for (const e of this.exposures) {
      // add submissions to assignments request
      const submissions = true;
      await this.fetchAssignmentsByExposure([
        this.experiment.experimentId,
        e.exposureId,
        submissions,
      ]);
    }
    for (let c of this.conditions) {
      const t = await this.checkTreatment([
        this.experiment.experimentId,
        c.conditionId,
        this.assignments[0].assignmentId,
      ]);
      this.conditionTreatments[c.conditionId] = t?.data;
    }
    this.getAssignmentDetails();
    await this.getZip(this.experiment.experimentId);
    if (this.experiment.participationType === "CONSENT") {
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

<style lang="scss">
.v-data-table__wrapper {
  // border-radius: 0;
  table tbody {
  }
}

.v-data-table
  > .v-data-table__wrapper
  tbody
  tr.v-data-table__expanded__content {
  box-shadow: none;
  border-bottom: thin solid rgba(0, 0, 0, 0.12) !important;

  > td {
    background-color: #fafafa !important;
    .v-data-table__wrapper {
      border: none;
      border-radius: 0;
    }
  }
}
</style>