<template>
  <div>
    <v-container class="px-0" v-if="experiment">
      <v-row>
        <v-col cols="12">
          <v-divider class=""></v-divider>
          <v-tabs v-model="tab" elevation="0" show-arrows>
            <v-tab v-for="(exposure, index) in exposures" :key="index">
              <div class="d-flex flex-column align-start py-1">
                <div class="">Set {{ index + 1 }}</div>
                <div class="d-block mt-4" :class="balanced ? '' : 'red--text'">
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
              <div class="d-flex justify-space-between">
                <h3>Assignments</h3>
                <v-btn
                  color="primary"
                  elevation="0"
                  @click="handleEdit('AssignmentExposureSets')"
                  >Add Assignment</v-btn
                >
              </div>
              <template>
                <v-data-table
                  :headers="assignmentHeaders"
                  :items="getAssignmentsForExposure(exposure)"
                  :single-expand="singleExpand"
                  :expanded.sync="expanded"
                  :sort-by="['assignmentOrder']"
                  hide-default-footer
                  v-sortable-data-table
                  @sorted="
                    (event) =>
                      saveOrder(
                        event,
                        getAssignmentsForExposure(exposure),
                        exposure
                      )
                  "
                  item-key="assignmentId"
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
                        item-key="treatmentId"
                        class="grey lighten-5"
                      >
                        <!-- eslint-disable-next-line -->
                        <template v-slot:item.title="{ item }">
                          Treatment
                          <v-chip
                            label
                            :color="
                              conditionColorMapping[
                                conditionForTreatment(
                                  exposure.groupConditionList,
                                  item.conditionId
                                ).conditionName
                              ]
                            "
                            >{{
                              conditionForTreatment(
                                exposure.groupConditionList,
                                item.conditionId
                              ).conditionName
                            }}</v-chip
                          >
                        </template>
                        <!-- eslint-disable-next-line -->
                        <template v-slot:item.actions="{ item }">
                          <template>
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
                  <!-- eslint-disable-next-line -->
                  <template v-slot:item.drag="{ item }">
                    <span class="dragger"><v-icon>mdi-drag</v-icon></span>
                  </template>
                  <!-- eslint-disable-next-line -->
                  <template v-slot:item.published="{ item }">
                    <span :class="item.published ? '' : 'red--text'">{{
                      item.published ? "Published" : "Unpublished"
                    }}</span>
                  </template>
                  <!-- eslint-disable-next-line -->
                  <template v-slot:item.dueDate="{ item }">
                    {{ item.dueDate }}
                  </template>
                  <!-- eslint-disable-next-line -->
                  <template v-slot:item.actions>
                    <v-btn
                      text
                      tile
                      @click="handleEdit('AssignmentExposureSets')"
                      class="text--lighten-5 text--grey"
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
import { mapGetters, mapActions } from "vuex";
import Sortable from "sortablejs";

export default {
  name: "ExperimentAssignments",
  props: ["experiment", "balanced"],
  directives: {
    sortableDataTable: {
      bind(el, binding, vnode) {
        const options = {
          animation: 150,
          onUpdate: function(event) {
            vnode.child.$emit("sorted", event);
          },
          handle: ".dragger",
        };
        Sortable.create(el.getElementsByTagName("tbody")[0], options);
      },
    },
  },
  computed: {
    ...mapGetters({
      conditions: "experiment/conditions",
      exposures: "exposures/exposures",
      assignments: "assignment/assignments",
      consent: "consent/consent",
      exportdata: "exportdata/exportData",
      conditionColorMapping: "condition/conditionColorMapping",
    }),
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
        text: "",
        align: "start",
        sortable: false,
        value: "drag",
      },
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
        value: "published",
      },
      {
        text: "Actions",
        value: "actions",
        align: "end",
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
      saveAssignmentOrder: "assignment/saveAssignmentOrder",
      checkTreatment: "treatment/checkTreatment",
      createTreatment: "treatment/createTreatment",
      createAssessment: "assessment/createAssessment",
      getConsentFile: "consent/getConsentFile",
      getZip: "exportdata/fetchExportData",
    }),
    saveOrder(event, assignments, exposure) {
      const movedItem = assignments.splice(event.oldIndex, 1)[0];
      assignments.splice(event.newIndex, 0, movedItem);
      const updated = assignments.map((a, idx) => ({
        ...a,
        assignmentOrder: idx + 1,
      }));
      this.saveAssignmentOrder([
        this.experiment.experimentId,
        exposure.exposureId,
        updated,
      ]);
    },
    getAssignmentsForExposure(exp) {
      return this.assignments
        .filter((a) => a.exposureId === exp.exposureId)
        .sort((a, b) => a.assignmentOrder - b.assignmentOrder);
    },
    // Navigate to EDIT section
    handleEdit(componentName) {
      this.$router.push({ name: componentName });
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

    conditionForTreatment(groupConditionList, conditionId) {
      return groupConditionList.find((c) => c.conditionId === conditionId);
    },
    // For Sorting Group Names
    sortedGroups(groupConditionList) {
      const newGroups = groupConditionList?.map((group) => group.groupName);
      return newGroups?.sort();
    },
  },
};
</script>

<style lang="scss">
.v-tabs-bar {
  height: auto;
  .v-tab {
    padding: 16px 16px;
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