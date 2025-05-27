<template>
  <div>
    <v-container
      v-if="experiment"
      class="px-0"
    >
      <v-row>
        <v-col cols="12">
          <v-divider v-if="!singleConditionExperiment"></v-divider>
          <v-tabs
            v-if="!singleConditionExperiment"
            v-model="tab"
            elevation="0"
            show-arrows
          >
            <v-tab
              v-for="(exposure, eidx) in exposures"
              :key="eidx"
            >
              <div class="d-flex flex-column align-start py-1">
                <div class="section-tab-set">Set {{ eidx + 1 }}</div>
                <div
                  class="d-block mt-4"
                  :class="balanced ? 'section-tab-assignments-balanced' : 'section-tab-assignments-unbalanced'"
                >
                  {{ getAssignmentsForExposure(exposure).length }} Component{{ getAssignmentsForExposure(exposure).length === 1 ? '': 's' }}
                </div>
              </div>
            </v-tab>
          </v-tabs>
          <v-divider></v-divider>
          <v-tabs-items v-model="tab">
            <v-tab-item
              v-for="(exposure, eidx) in exposures"
              class="section-assignments py-3 px-3"
              :key="eidx"
            >
              <div class="d-flex justify-space-between">
                <h3>Components</h3>
                <div
                  v-if="loaded && getAssignmentsForExposure(exposure).length"
                >
                  <AddAssignmentDialog
                    @multiple="handleAssignmentMultipleVersions(exposure)"
                    @single="handleAssignmentSingleVersion(exposure)"
                    :hasExistingAssignment="true"
                    :isSingleConditionExperiment="singleConditionExperiment"
                  />
                </div>
              </div>
              <template v-if="!loaded">
                <div class="spinner-container-assignment">
                  <Spinner
                    height="50px"
                    width="50px"
                  />
                </div>
              </template>
              <template v-if="loaded">
                <v-card
                  v-if="!getAssignmentsForExposure(exposure).length"
                  class="no-assignments-yet d-flex flex-column px-5 py-5 rounded-lg mx-3 mb-5 d-inline-block"
                  outlined
                  justify="center"
                >
                  <div class="no-assignments-yet-container">
                    <h4>You don't have any components yet</h4>
                    <AddAssignmentDialog
                      @multiple="handleAssignmentMultipleVersions(exposure)"
                      @single="handleAssignmentSingleVersion(exposure)"
                      :hasExistingAssignment="false"
                      :isSingleConditionExperiment="singleConditionExperiment"
                    />
                  </div>
                </v-card>
                <v-data-table
                  v-if="getAssignmentsForExposure(exposure).length"
                  :headers="assignmentHeaders"
                  :items="getAssignmentsForExposure(exposure)"
                  :expanded="assignmentsExpanded"
                  :sort-by="['assignmentOrder']"
                  :mobile-breakpoint="mobileBreakpoint"
                  :items-per-page="assignmentsCount"
                  :item-class="() => 'assignment-row'"
                  hide-default-footer
                  v-sortable-data-table
                  item-key="assignmentId"
                  show-expand
                  class="v-data-table-alt v-data-table--sorted data-table-assignments mx-3 mb-5 mt-3"
                  @sorted="
                    (event) =>
                      saveOrder(
                        event,
                        getAssignmentsForExposure(exposure),
                        exposure
                      )
                  "
                >
                  <template
                    v-slot:item.title="{ item }"
                  >
                    <v-icon
                      class="component-icon"
                    >
                      mdi-file-outline
                    </v-icon>
                    {{ item.title }}
                    <v-chip
                      v-if="item.treatments.length == 1"
                      label
                      color="lightgrey"
                      class="v-chip--only-one"
                    >
                      Only One Version
                    </v-chip>
                  </template>
                  <template
                    v-slot:expanded-item="{ item }"
                  >
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
                        class="treatment-row grey lighten-5"
                      >
                        <!-- eslint-disable-next-line -->
                        <template
                          v-slot:item.title="{ item }"
                        >
                          <v-icon
                            class="mr-1 component-icon"
                          >
                            {{ item.assessmentDto.integration ? treatmentIcon.integration : treatmentIcon.assignment }}
                          </v-icon>
                          <v-tooltip
                            v-if="!(item.assessmentDto && item.assessmentDto.questions.length)"
                            top
                          >
                            <template #activator="{ on }">
                              <v-icon
                                class="icon-treatment-incomplete"
                                v-on="on"
                              >
                                  mdi-alert-circle-outline
                              </v-icon>
                            </template>
                            <span>Please add content to this treatment.</span>
                          </v-tooltip>
                          <span
                            :class="!(item.assessmentDto && item.assessmentDto.questions.length) ? 'label-treatment-incomplete' : 'label-treatment-complete'"
                          >
                            Treatment
                          </span>
                          <v-chip
                            v-if="!singleConditionExperiment && assignments.find(a => a.assignmentId == item.assignmentId).treatments.length === conditions.length"
                            label
                            :color="
                              conditionColorMapping[
                                conditionForTreatment(
                                  exposure.groupConditionList,
                                  item.conditionId
                                ).conditionName
                              ]
                            "
                          >
                            {{
                              conditionForTreatment(
                                exposure.groupConditionList,
                                item.conditionId
                              ).conditionName
                            }}
                          </v-chip>
                          <div
                            class="treatment-btn-group"
                          >
                            <v-btn
                              text
                              tile
                              @click="goToBuilder(item.conditionId, item.assignmentId)"
                            >
                              <v-icon>mdi-pencil</v-icon>
                              <span class="treatment-btn">Edit</span>
                            </v-btn>
                            <v-btn
                              v-if="item.assessmentDto.integration && !displayTreatmentMenu"
                              :href="integrationsPreviewLaunchUrl(item.assessmentDto.integrationPreviewUrl)"
                              target="_blank"
                              text
                              tile
                            >
                              <v-icon>mdi-eye-outline</v-icon>
                              <span class="treatment-btn">Preview</span>
                            </v-btn>
                            <v-btn
                              v-if="!item.assessmentDto.integration"
                              :disabled="!item.assessmentDto.questions.length"
                              @click="handleTreatmentPreview(item)"
                              text
                              tile
                            >
                              <v-icon>mdi-eye-outline</v-icon>
                              <span class="treatment-btn">Preview</span>
                            </v-btn>
                            <v-menu
                              v-if="item.assessmentDto.integration && displayTreatmentMenu"
                              offset-y
                            >
                              <template
                                v-slot:activator="{ on, attrs }"
                              >
                                <v-btn
                                  icon
                                  text
                                  tile
                                  v-bind="attrs"
                                  v-on="on"
                                  aria-label="treatment actions"
                                >
                                  <v-icon>mdi-dots-horizontal</v-icon>
                                </v-btn>
                              </template>
                              <v-list>
                                <v-list-item
                                  aria-label="preview integration"
                                >
                                  <v-list-item-title>
                                    <v-icon>mdi-eye-outline</v-icon>
                                    <span class="treatment-btn">
                                      <a
                                        :href="integrationsPreviewLaunchUrl(item.assessmentDto.integrationPreviewUrl)"
                                        target="_blank"
                                        class="integration-preview-link"
                                      >
                                        Preview
                                      </a>
                                    </span>
                                  </v-list-item-title>
                                </v-list-item>
                              </v-list>
                            </v-menu>
                          </div>
                        </template>
                      </v-data-table>
                    </td>
                  </template>
                  <!-- eslint-disable-next-line -->
                  <template v-slot:item.treatments="{ item }">
                    <!-- red text if treatment count != condition count or a treatment not having an assessment -->
                    <span
                      :class="item.treatments.filter(treatment => !treatment.assessmentDto || !treatment.assessmentDto.questions || !treatment.assessmentDto.questions.length).length ? 'label-treatment-incomplete' : 'label-treatment-complete'"
                    >
                      {{ item.treatments.length }} / {{ item.treatments.length }}
                      <v-tooltip
                        v-if="item.treatments.filter(treatment => !treatment.assessmentDto || !treatment.assessmentDto.questions || !treatment.assessmentDto.questions.length).length"
                        top
                      >
                        <template #activator="{ on }">
                          <v-icon
                            class="label-treatment-incomplete"
                            v-on="on"
                          >
                            mdi-alert-circle-outline
                          </v-icon>
                        </template>
                        <span>Set up your component by creating {{ item.treatments.length > 1 ? "treatments" : "a treatment" }}.</span>
                      </v-tooltip>
                    </span>
                  </template>
                  <!-- eslint-disable-next-line -->
                  <template v-slot:item.drag="{ item }">
                    <span class="dragger"><v-icon>mdi-drag</v-icon></span>
                  </template>
                  <!-- eslint-disable-next-line -->
                  <template v-slot:item.published="{ item }">
                    <span
                      :class="item.published ? 'label-treatment-complete' : 'label-treatment-incomplete'"
                    >
                      {{ item.published ? "Published" : "Unpublished" }}
                    </span>
                  </template>
                  <!-- eslint-disable-next-line -->
                  <template v-slot:item.dueDate="{ item }">
                    {{ dueDate(item) }}
                  </template>
                  <!-- eslint-disable-next-line -->
                  <template v-slot:item.actions="{ item }">
                    <v-menu offset-y>
                      <template v-slot:activator="{ on, attrs }">
                        <v-btn
                          icon
                          text
                          tile
                          v-bind="attrs"
                          v-on="on"
                          aria-label="assignment actions"
                        >
                          <v-icon>mdi-dots-horizontal</v-icon>
                        </v-btn>
                      </template>
                      <v-list>
                        <v-menu
                          v-if="exposures.length > 1"
                          offset-x
                          :key="exposure.exposureId"
                          open-on-hover
                          transition="slide-x-transition"
                        >
                          <template v-slot:activator="{ on, attrs }">
                            <v-list-item
                              v-bind="attrs"
                              v-on="on"
                              aria-label="assignment move to exposure set"
                            >
                              <v-list-item-title>
                                <v-icon>mdi-arrow-right-top</v-icon> Move
                              </v-list-item-title>
                              <v-list-item-action class="justify-end">
                                <v-icon>mdi-menu-right</v-icon>
                              </v-list-item-action>
                            </v-list-item>
                          </template>
                          <v-list>
                            <template v-for="(exposure, idx) in exposures">
                              <v-list-item
                                v-if="exposure.exposureId !== item.exposureId"
                                :key="exposure.exposureId"
                                :aria-label="`Exposure set ${idx + 1}`"
                                @click="handleMoveAssignment(exposure.exposureId, item)"
                              >
                                <v-list-item-title>
                                  Exposure set {{ idx + 1 }}
                                </v-list-item-title>
                              </v-list-item>
                            </template>
                          </v-list>
                        </v-menu>
                        <v-list-item
                          aria-label="edit assignment"
                          @click="handleEdit(item, exposure.exposureId)">
                          <v-list-item-title>
                            <v-icon>mdi-pencil</v-icon>
                            Edit
                          </v-list-item-title>
                        </v-list-item>
                        <v-list-item
                          aria-label="duplicate assignment"
                          @click="handleDuplicateAssignment(exposure.exposureId, item)">
                          <v-list-item-title>
                            <v-icon>mdi-content-duplicate</v-icon>
                            Duplicate
                          </v-list-item-title>
                        </v-list-item>
                        <v-list-item
                          v-if="canDeleteAssignment"
                          aria-label="delete assignment"
                          @click="handleDeleteAssignment(exposure.exposureId, item)">
                          <v-list-item-title>
                            <v-icon>mdi-delete</v-icon>
                            Delete
                          </v-list-item-title>
                        </v-list-item>
                      </v-list>
                    </v-menu>
                  </template>
                </v-data-table>
              </template>
              <div
                v-if="!singleConditionExperiment"
              >
                <h3 class="my-4">Design</h3>
                <v-card
                  class="data-table-design px-5 py-5 rounded-lg mx-3 mb-5 d-inline-block"
                  outlined
                >
                  <div
                    v-for="group in sortedGroups(exposure.groupConditionList, designExpanded ? null : maxDesignGroups)"
                    class="groupNames"
                    :key="group"
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
                      }}
                    </v-chip>
                  </div>
                  <a
                    v-if="sortedGroups(exposure.groupConditionList).length > maxDesignGroups"
                    @click="designExpanded = !designExpanded"
                    class="text--blue"
                  >
                    <v-icon
                      v-if="!designExpanded"
                      color="blue"
                    >
                      mdi-plus
                    </v-icon>
                    <v-icon
                      v-else
                      color="blue"
                    >
                      mdi-minus
                    </v-icon>
                    <span v-if="!designExpanded">More</span>
                    <span v-else>Less</span>
                  </a>
                </v-card>
              </div>
            </v-tab-item>
          </v-tabs-items>
        </v-col>
      </v-row>
    </v-container>
  </div>
</template>

<script>
import { mapGetters, mapActions } from "vuex";
import AddAssignmentDialog from "@/components/AddAssignmentDialog";
import moment from "moment";
import Sortable from "sortablejs";
import Spinner from "@/components/Spinner";

export default {
  name: "ExperimentAssignments",
  props: [
    "experiment",
    "balanced",
    "loaded",
    "activeExposureSet"
  ],
  components: {
    AddAssignmentDialog,
    Spinner,
  },
  directives: {
    sortableDataTable: {
      bind(el, binding, vnode) {
        const options = {
          animation: 150,
          onUpdate: function(event) {
            vnode.child.$emit("sorted", event);
          },
          handle: ".dragger",
          draggable: ".assignment-row"
        };
        Sortable.create(el.querySelector(".data-table-assignments tbody"), options);
      },
    },
  },
  data: () => ({
    tab: 0,
    minTreatments: 2,
    maxDesignGroups: 2,
    conditionTreatments: {},
    conditionColors: [""],
    assignmentsExpanded: [],
    designExpanded: false,
    mobileBreakpoint: 636,
    assignmentHeaders: [
      {
        text: "",
        align: "start",
        sortable: false,
        value: "drag",
      },
      {
        text: "Component Name",
        align: "start",
        sortable: false,
        value: "title",
      },
      {
        text: "Treatments",
        sortable: false,
        value: "treatments",
      },
      {
        text: "Due Date",
        sortable: false,
        value: "dueDate",
      },
      {
        text: "Status",
        sortable: false,
        value: "published",
      },
      {
        text: "Actions",
        align: "center",
        sortable: false,
        value: "actions",
      },
      {
        text: "",
        sortable: false,
        value: "data-table-expand"
      }
    ],
    treatmentHeaders: [
      {
        text: "Treatment Name",
        align: "start",
        sortable: false,
        value: "title",
      }
    ],
    treatmentIcon: {
      integration: "mdi-application-brackets-outline",
      assignment: "mdi-wrench-outline"
    }
  }),
  watch: {
    assignmentsCount: {
      handler() {
        this.expandAssignments();
      },
      immediate: true
    }
  },
  computed: {
    ...mapGetters({
      conditions: "experiment/conditions",
      exposures: "exposures/exposures",
      assignments: "assignment/assignments",
      assignment: "assignment/assignment",
      consent: "consent/consent",
      exportdata: "exportdata/exportData",
      conditionColorMapping: "condition/conditionColorMapping",
      userId: "api/userId",
    }),
    experimentId() {
      return parseInt(this.experiment.experimentId);
    },
    canDeleteAssignment() {
      return !this.experiment.started;
    },
    assignmentsCount() {
      return this.assignments.length;
    },
    singleConditionExperiment() {
      return this.conditions.length === 1;
    },
    defaultCondition() {
      return this.conditions.find(c => c.defaultCondition);
    },
    displayTreatmentMenu() {
      switch (this.$vuetify.breakpoint.name) {
        case 'xs':
        case 'sm':
        case 'md':
          return true;
        case 'lg':
        case 'xl':
          return false;
        default:
          return false;
      }
    },
  },
  methods: {
    ...mapActions({
      fetchExposures: "exposures/fetchExposures",
      fetchAssignmentsByExposure: "assignment/fetchAssignmentsByExposure",
      saveAssignmentOrder: "assignment/saveAssignmentOrder",
      deleteAssignment: "assignment/deleteAssignment",
      duplicateAssignment: "assignment/duplicateAssignment",
      checkTreatment: "treatment/checkTreatment",
      createTreatment: "treatment/createTreatment",
      createAssessment: "assessment/createAssessment",
      getConsentFile: "consent/getConsentFile",
      moveAssignment: "assignment/moveAssignment",
      setCurrentAssignment: 'assignments/setCurrentAssignment',
      saveEditMode: "navigation/saveEditMode"
    }),
    saveOrder(event, assignments, exposure) {
      const movedItem = assignments.splice(event.oldDraggableIndex, 1)[0];
      assignments.splice(event.newDraggableIndex, 0, movedItem);
      const updated = assignments.map(
        (a, idx) => ({
          ...a,
          assignmentOrder: idx + 1,
        })
      );
      this.saveAssignmentOrder([
        this.experiment.experimentId,
        exposure.exposureId,
        updated,
      ]);
    },
    async handleMoveAssignment(targetExposureId, assignment) {
      try {
        const response = await this.moveAssignment([
          this.experiment.experimentId,
          assignment.exposureId,
          assignment.assignmentId,
          {
            ...assignment,
            assignmentId: null,
            exposureId: targetExposureId
          }
        ]);

        if (response.status === 201) {
          return await this.fetchAssignmentsByExposure([
            this.experimentId,
            targetExposureId,
            true,
          ]);
        }
      } catch (error) {
        console.error("handleMoveAssignment | catch", { error });
      }
    },
    getAssignmentsForExposure(exp) {
      return this.assignments
        .filter((a) => a.exposureId === exp.exposureId)
        .sort((a, b) => a.assignmentOrder - b.assignmentOrder);
    },
    async handleCreateAssignment(exposureId, conditionIds) {
      await this.saveEditMode({
        initialPage: 'AssignmentCreateAssignment',
        callerPage: {
          name: 'ExperimentSummary',
          tab: 'components',
          exposureSet: this.tab
        }
      });
      this.$router.push(
        {
          name: 'AssignmentCreateAssignment',
          params: {
            exposureId: exposureId,
            conditionIds: conditionIds
          }
        }
      )
    },
    // Navigate to EDIT section
    async handleEdit(assignment, exposureId) {
      const reallyEdit = await this.handleAssignmentStartedAlert(assignment.assignmentId);
      if (!reallyEdit) {
        return;
      }
      await this.setCurrentAssignment(assignment);
      await this.saveEditMode({
        initialPage: 'AssignmentEditor',
        callerPage: {
          name: 'ExperimentSummary',
          tab: 'components',
          exposureSet: this.tab
        }
      });
      this.$router.push({
        name: 'AssignmentEditor',
        params: {
          assignmentId: assignment.assignmentId,
          exposureId: exposureId
        }
      });
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
    async handleDeleteAssignment(eid, a) {
      // DELETE ASSIGNMENT
      const reallyDelete = await this.$swal({
        icon: "question",
        text: `Are you sure you want to delete the assignment "${a.title}"?`,
        showCancelButton: true,
        confirmButtonText: "Yes, delete it",
        cancelButtonText: "No, cancel",
      });
      if (reallyDelete?.isConfirmed) {
        try {
          return await this.deleteAssignment([
            this.experimentId,
            eid,
            a.assignmentId,
          ]);
        } catch (error) {
          console.error("handleDeleteQuestion | catch", { error });
        }
      }
    },
    async handleDuplicateAssignment(eid, a) {
      // DUPLICATE ASSIGNMENT experimentId, exposureId, assignmentId

      try {
        const response = await this.duplicateAssignment([
          this.experimentId,
          eid,
          a.assignmentId,
        ]);

        if (response.status === 201) {
          return await this.fetchAssignmentsByExposure([
            this.experimentId,
            eid,
            true,
          ]);
        }
      } catch (error) {
        console.error("handleDuplicateQuestion | catch", { error });
      }
    },
    async goToBuilder(conditionId, assignmentId) {
      const reallyEdit = await this.handleAssignmentStartedAlert(assignmentId);
      if (!reallyEdit) {
        return;
      }
      await this.saveEditMode({
        initialPage: "TerracottaBuilder",
        callerPage: {
          name: "ExperimentSummary",
          tab: "components",
          exposureSet: this.tab
        }
      });
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
          experimentId: this.experiment.experimentId,
          conditionId: conditionId,
          treatmentId: treatment?.data?.treatmentId,
          assessmentId: assessment?.data?.assessmentId,
          current_assignment: JSON.stringify(this.assignments.find((a) => a.assignmentId === assignmentId))
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
    sortedGroups(groupConditionList, limit) {
      const newGroups = groupConditionList?.map((group) => group.groupName);
      return newGroups?.sort().filter((g, i) => limit ? i < limit : true);
    },
    dueDate(item) {
      return item.dueDate != null ? moment(item.dueDate).format('MMM D, YYYY hh:mma') : "";
    },
    async handleAssignmentStartedAlert(assignmentId) {
      var assignment = this.assignments.find((a) => a.assignmentId === assignmentId);
      if (!assignment.started) {
        return true;
      }
      const result = await this.$swal({
        icon: "warning",
        text: "You are currently collecting assignment submissions, and at least one student has submitted the assignment. Making changes could compromise the integrity of your experiment.",
        showCancelButton: true,
        confirmButtonText: "OK",
        cancelButtonText: "Cancel",
        showLoaderOnConfirm: true,
        reverseButtons: true,
        allowOutsideClick: () => !this.$swal.isLoading(),
      });
      return result.isConfirmed;
    },
    expandAssignments() {
      this.assignmentsExpanded = [];
      this.assignments.forEach(assignment => this.assignmentsExpanded.push(assignment));
    },
    async handleAssignmentMultipleVersions(exposure) {
      // create an assignment normal
      await this.handleCreateAssignment(
        exposure.exposureId,
        JSON.stringify(exposure.groupConditionList.map(a => a.conditionId))
      );
    },
    async handleAssignmentSingleVersion(exposure) {
      // create an assignment with the default condition
      await this.handleCreateAssignment(
        exposure.exposureId,
        JSON.stringify([this.defaultCondition.conditionId])
      );
    },
    integrationsPreviewLaunchUrl(url) {
      return `/integrations/preview?url=${btoa(url)}`;
    },
    handleTreatmentPreview(treatment) {
      window.open(`/preview/experiments/${this.experimentId}/conditions/${treatment.conditionId}/treatments/${treatment.treatmentId}?ownerId=${this.userId}`, "_blank");
    }
  },
  async mounted() {
    this.tab = parseInt(this.activeExposureSet);
  }
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
  border-bottom: 0 !important;
  // border-bottom: thin solid rgba(0, 0, 0, 0.12) !important;

  > td {
    background-color: #fafafa !important;
    .v-data-table__wrapper {
      border: none;
      border-radius: 0;
    }
  }
}
.v-tooltip__content {
  max-width: 400px;
  opacity: 1.0 !important;
  background-color: rgba(55,61,63, 1.0) !important;
  a {
    color: #afdcff;
  }
}
.no-assignments-yet {
  width: 100%;
  min-height: 100px;
  background-color: #fffcf7 !important;
  border-color: #ffe0b2 !important;
}
.no-assignments-yet-container {
  width: fit-content;
  margin: 0 auto;
}
.no-assignments-yet-container {
  > h4 {
    width: fit-content;
    margin: 0 auto;
 }
}
.section-tab-set {
  color: black;
  opacity: 0.74;
}
.label-treatment-complete,
.label-treatment-incomplete {
  padding-right: 10px;
}
.treatment-btn,
.label-treatment-complete,
.icon-treatment-incomplete,
.label-treatment-incomplete,
.section-tab-assignments-balanced,
.section-tab-assignments-unbalanced {
  text-transform: none !important;
  opacity: 0.87 !important;
}
.treatment-btn,
.label-treatment-complete,
.section-tab-assignments-balanced {
  color: black !important;
}
.v-btn--disabled {
  .treatment-btn {
    color: rgba(0, 0, 0, 0.26) !important;
  }
}
.icon-treatment-incomplete,
.label-treatment-incomplete,
.section-tab-assignments-unbalanced {
  color: #E06766 !important;
}
div.section-assignments.py-3.px-3 {
  padding-top: 40px !important;
  padding-left: 0 !important;
  padding-right: 0 !important;
  > div.spinner-container-assignment {
    width: 100%;
    height: 100px;
    padding: 0;
    margin-top: 12px !important;
    margin-left: 0 !important;
    list-style: none;
    display: -webkit-box;
    display: -moz-box;
    display: -ms-flexbox;
    display: -webkit-flex;
    display: flex;
    align-items: center;
    justify-content: center;
    border: thin solid rgba(0,0,0,.12) !important;
    border-radius: 8px !important;
  }
}
div.no-assignments-yet.px-5.py-5.mx-3.mb-5,
div.data-table-assignments.mx-3.mb-5.mt-3,
div.data-table-design.px-5.py-5.rounded-lg.mx-3.mb-5.d-inline-block {
  margin-left: 0 !important;
}
div.data-table-assignments.mx-3.mb-5.mt-3 {
  margin-right: 0 !important;
  margin-bottom: 40px !important;
}
td.treatments-table-container td,
td.treatments-table-container td span,
div.data-table-assignments.mx-3.mb-5.mt-3 td,
div.data-table-assignments.mx-3.mb-5.mt-3 th,
div.data-table-assignments.mx-3.mb-5.mt-3 th span {
  min-width: fit-content;
  white-space: nowrap;
}
td.treatments-table-container td,
td.treatments-table-container td span {
  white-space: normal;
}
td.treatments-table-container .v-data-table__wrapper table {
  padding: 0 35px !important;
}
.v-application--is-ltr .v-data-table > .v-data-table__wrapper > table > tbody > tr > th,
.v-application--is-ltr .v-data-table > .v-data-table__wrapper > table > tfoot > tr > th,
.v-application--is-ltr .v-data-table > .v-data-table__wrapper > table > thead > tr > th,
div.data-table-assignments > .v-data-table__wrapper > table > tbody > tr > td:not(.treatments-table-container)  {
  padding: 4px !important;
}
div.data-table-assignments > .v-data-table__wrapper > table > tbody > tr > td:not(.treatments-table-container),
div.data-table-design > div.groupNames > span.v-chip.v-chip--label > span.v-chip__content {
  white-space: normal !important;
}
.treatment-btn-group {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  float: right;
  margin-top: 5px;
  & .v-btn {
    padding: 0 8px;
  }
}
span.v-chip.v-chip--label,
span.v-chip.v-chip--label > span.v-chip__content {
  min-height: fit-content !important;
  height: unset !important;
  max-width: 400px !important;
}
a.integration-preview-link {
  color: rgba(0, 0, 0, .87) !important;
  text-decoration: none;
  font-size: 1rem;
}
.component-icon {
  color: rgba(0, 0, 0, .54) !important;
}
</style>
