<template>
  <div>
    <v-container
      v-if="loaded && experiment"
      class="px-0"
    >
      <v-row>
        <v-col
          cols="12"
        >
          <v-divider
            v-if="!singleConditionExperiment"
          />
          <v-tabs
            v-if="!singleConditionExperiment"
            v-model="tab"
            elevation="0"
            show-arrows
          >
            <v-tab
              v-for="(exposure, eidx) in exposures"
              :key="eidx"
              @change="tab = eidx"
            >
              <div
                class="d-flex flex-column align-start py-1"
              >
                <div
                  class="section-tab-set"
                >
                  Set {{ eidx + 1 }}
                </div>
                <div
                  class="d-block mt-4"
                  :class="balanced ? 'section-tab-components-balanced' : 'section-tab-components-unbalanced'"
                >
                  {{ rows[eidx].length }} Component{{ rows[eidx].length === 1 ? '': 's' }}
                </div>
              </div>
            </v-tab>
          </v-tabs>
          <v-divider />
          <v-tabs-items
            v-model="tab"
          >
            <v-tab-item
              v-for="(exposure, eidx) in exposures"
              class="section-components py-3 px-3"
              :key="eidx"
            >
              <div
                class="d-flex justify-space-between"
              >
                <h3>Components</h3>
                <div
                  v-if="loaded && exposureRows.length"
                  class="component-buttons d-flex justify-space-between"
                >
                  <div
                    v-if="isMessagingEnabled"
                    class="mb-5 mr-5"
                  >
                    <add-message-dialog
                      :hasExisting="true"
                      :isSingleConditionExperiment="singleConditionExperiment"
                      @add="handleAddMessage($event, exposure)"
                    />
                  </div>
                  <div>
                    <add-assignment-dialog
                      :hasExisting="true"
                      :isSingleConditionExperiment="singleConditionExperiment"
                      @multiple="handleAssignmentMultipleVersions(exposure)"
                      @single="handleAssignmentSingleVersion(exposure)"
                    />
                  </div>
                </div>
              </div>
              <template
                v-if="!loaded"
              >
                <div
                  class="spinner-container-assignment"
                >
                  <spinner
                    height="50px"
                    width="50px"
                  />
                </div>
              </template>
              <template
                v-if="loaded"
              >
                <v-card
                  v-if="!rows[eidx].length"
                  class="no-assignments-yet d-flex flex-column rounded-lg mb-5 d-inline-block"
                  outlined
                >
                  <div
                    class="no-assignments-yet-container"
                  >
                    <h4>You don't have any components yet</h4>
                    <div
                      class="no-components-yet-buttons d-flex flex-row justify-space-between mx-auto"
                    >
                      <add-assignment-dialog
                        @multiple="handleAssignmentMultipleVersions(exposure)"
                        @single="handleAssignmentSingleVersion(exposure)"
                        :hasExisting="false"
                        :isSingleConditionExperiment="singleConditionExperiment"
                      />
                      <add-message-dialog
                        v-if="isMessagingEnabled"
                        :isSingleConditionExperiment="singleConditionExperiment"
                        :hasExisting="false"
                        @add="handleAddMessage($event, exposure)"
                        class="ml-3"
                      />
                    </div>
                  </div>
                </v-card>
                <v-data-table
                  v-if="rows[eidx].length"
                  :headers="assignmentHeaders"
                  :items="rows[eidx]"
                  :expanded.sync="rowsExpanded[eidx]"
                  :sort-by="['assignmentOrder']"
                  :mobile-breakpoint="mobileBreakpoint"
                  :items-per-page="rows[eidx].length"
                  :item-class="() => 'assignment-row'"
                  :key="componentTableKey"
                  item-key="assignmentId"
                  class="v-data-table-alt v-data-table--sorted data-table-assignments mx-3 mb-5 mt-3"
                  hide-default-footer
                  v-sortable-data-table
                  show-expand
                  @sorted="
                    (event) =>
                      saveOrder(
                        event,
                        rows[eidx],
                        exposure
                      )
                  "
                >
                  <template
                    v-slot:item.title="{ item: row }"
                  >
                    <!-- row slot-->
                    <v-icon>
                      {{ rowIcon(row) }}
                    </v-icon>
                    {{ row.title }}
                    <v-chip
                      v-if="row.treatments.length == 1"
                      label
                      color="lightgrey"
                      class="v-chip--only-one"
                    >
                      Only One Version
                    </v-chip>
                  </template>
                  <template
                    v-slot:expanded-item="{ item: row }"
                  >
                    <!-- expanded row (treatments) slot-->
                    <td
                      :colspan="assignmentHeaders.length"
                      class="treatments-table-container"
                    >
                      <v-data-table
                        :headers="treatmentHeaders"
                        :items="row.treatments"
                        hide-default-header
                        hide-default-footer
                        item-key="treatmentId"
                        class="treatment-row grey lighten-5"
                      >
                        <!-- eslint-disable-next-line -->
                        <template v-slot:item.title="{ item: treatment }">
                          <v-icon
                            class="mr-1 component-icon"
                          >
                            {{ rowTreatmentsIcon(row, treatment) }}
                          </v-icon>
                          <v-tooltip
                            v-if="showTreatmentRowTooltip(row, treatment)"
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
                            <span>{{ treatmentRowTooltipText(row, treatment) }}</span>
                          </v-tooltip>
                          <span
                            :class="treatmentRowClass(row, treatment)"
                          >
                            Treatment
                          </span>
                          <v-chip
                            v-if="!singleConditionExperiment && rows[eidx].find((r) => r.assignmentId === row.assignmentId).treatments.length === conditions.length"
                            label
                            :color="
                              conditionColorMapping[
                                conditionForTreatment(
                                  exposure.groupConditionList,
                                  treatment.conditionId
                                ).conditionName
                              ]
                            "
                          >
                            {{
                              conditionForTreatment(
                                exposure.groupConditionList,
                                treatment.conditionId
                              ).conditionName
                            }}
                          </v-chip>
                          <div
                            class="treatment-btn-group"
                          >
                            <v-btn
                              text
                              tile
                              class="btn-treatment-edit"
                              @click="handleEditTreatment(row, treatment)"
                            >
                              <v-icon>
                                {{ editTreatmentIcon(row, treatment) }}
                              </v-icon>
                              <span
                                class="btn-edit"
                              >
                                {{ editTreatmentText(row, treatment) }}
                              </span>
                            </v-btn>
                            <v-btn
                              v-if="isIntegrationAssignment(row, treatment) && !displayTreatmentMenu"
                              :href="integrationsPreviewLaunchUrl(treatment.assessmentDto.integrationPreviewUrl)"
                              :disabled="!treatment.assessmentDto.questions.length || !treatment.assessmentDto.integrationUrlValid"
                              target="_blank"
                              text
                              tile
                            >
                              <v-icon>mdi-eye-outline</v-icon>
                              <span class="treatment-btn">Preview</span>
                            </v-btn>
                            <v-btn
                              v-if="!isMessage(row) && !treatment.assessmentDto.integration"
                              :disabled="!treatment.assessmentDto.questions.length"
                              @click="handleTreatmentPreview(treatment)"
                              text
                              tile
                            >
                              <v-icon>mdi-eye-outline</v-icon>
                              <span class="treatment-btn">Preview</span>
                            </v-btn>
                            <v-menu
                              v-if="isIntegrationAssignment(row, treatment) && displayTreatmentMenu"
                              :disabled="!treatment.assessmentDto.questions.length || !treatment.assessmentDto.integrationUrlValid"
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
                                        :href="integrationsPreviewLaunchUrl(treatment.assessmentDto.integrationPreviewUrl)"
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
                  <template v-slot:item.treatments="{ item: row }">
                    <!-- red text if treatment count != condition count or a treatment not having an assessment -->
                    <span
                      :class="rowTreatmentsColumnClass(row)"
                    >
                      {{ row.treatments.length }} / {{ row.treatments.length }}
                      <v-tooltip
                        v-if="hasIncompleteTreatments(row)"
                        top
                      >
                        <template
                          #activator="{ on }"
                        >
                          <v-icon
                            class="label-treatment-incomplete"
                            v-on="on"
                          >
                            mdi-alert-circle-outline
                          </v-icon>
                        </template>
                        <span>
                          {{ showRowTreatmentsColumnTooltipText(row) }}
                        </span>
                      </v-tooltip>
                    </span>
                  </template>
                  <!-- eslint-disable-next-line -->
                  <template v-slot:item.drag="{ item: row }">
                    <!-- dragger slot-->
                    <span
                      class="dragger"
                    >
                      <v-icon>mdi-drag</v-icon>
                    </span>
                  </template>
                  <!-- eslint-disable-next-line -->
                  <template v-slot:item.published="{ item: row }">
                    <!-- row published column slot -->
                    <span
                      :class="rowPublishedColumnClass(row)"
                    >
                      {{ rowPublishedColumnText(row) }}
                    </span>
                  </template>
                  <!-- eslint-disable-next-line -->
                  <template v-slot:item.dueDate="{ item: row }">
                    <!-- row due date column slot -->
                    {{ dueDate(row) }}
                  </template>
                  <!-- eslint-disable-next-line -->
                  <template v-slot:item.actions="{ item: row }">
                    <!-- row actions column slot -->
                    <v-menu
                      offset-y
                    >
                      <template
                        v-slot:activator="{ on, attrs }"
                      >
                        <v-btn
                          v-bind="attrs"
                          v-on="on"
                          aria-label="actions"
                          icon
                          text
                          tile
                        >
                          <v-icon>mdi-dots-horizontal</v-icon>
                        </v-btn>
                      </template>
                      <v-list>
                        <v-menu
                          v-if="showMoveAction(row)"
                          :key="exposure.exposureId"
                          transition="slide-x-transition"
                          open-on-hover
                          offset-x
                        >
                          <template
                            v-slot:activator="{ on, attrs }"
                          >
                            <v-list-item
                              v-bind="attrs"
                              v-on="on"
                              aria-label="move to exposure set"
                            >
                              <v-list-item-title>
                                <v-icon>mdi-arrow-right-top</v-icon>
                                Move
                              </v-list-item-title>
                              <v-list-item-action
                                class="justify-end"
                              >
                                <v-icon>mdi-menu-right</v-icon>
                              </v-list-item-action>
                            </v-list-item>
                          </template>
                          <v-list>
                            <template
                              v-for="(exposure, idx) in exposures"
                            >
                              <v-list-item
                                v-if="exposure.exposureId !== row.exposureId"
                                :key="exposure.exposureId"
                                :aria-label="`Exposure set ${idx + 1}`"
                                @click="handleMoveComponent(exposure.exposureId, row)"
                              >
                                <v-list-item-title>
                                  Exposure set {{ idx + 1 }}
                                </v-list-item-title>
                              </v-list-item>
                            </template>
                          </v-list>
                        </v-menu>
                        <v-list-item
                          aria-label="edit"
                          @click="handleEditComponent(exposure.exposureId, row)"
                        >
                          <v-list-item-title>
                            <v-icon>mdi-pencil</v-icon>
                            Edit
                          </v-list-item-title>
                        </v-list-item>
                        <v-list-item
                          aria-label="duplicate"
                          @click="handleDuplicateComponent(exposure.exposureId, row)">
                          <v-list-item-title>
                            <v-icon>mdi-content-duplicate</v-icon>
                            Duplicate
                          </v-list-item-title>
                        </v-list-item>
                        <v-list-item
                          v-if="showDeleteComponent(row)"
                          aria-label="delete"
                          @click="handleDeleteComponent(exposure.exposureId, row)">
                          <v-list-item-title>
                            <v-icon>mdi-delete</v-icon>
                            Delete
                          </v-list-item-title>
                        </v-list-item>
                        <v-list-item
                          v-if="showPublishComponent(row)"
                          aria-label="publish"
                          @click="handlePublishComponent(exposure.exposureId, row)">
                          <v-list-item-title>
                            <v-icon>mdi-publish</v-icon>
                            Publish
                          </v-list-item-title>
                        </v-list-item>
                        <v-list-item
                          v-if="showUnpublishComponent(row)"
                          aria-label="publish"
                          @click="handleUnpublishComponent(exposure.exposureId, row)">
                          <v-list-item-title>
                            <v-icon>mdi-publish-off</v-icon>
                            Unpublish
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
                <h3
                  class="my-4"
                >
                  Design
                </h3>
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
                    <span>
                      {{ designExpanded ? "Less" : "More" }}
                    </span>
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
import { message as messageStatus } from "@/helpers/messaging/status.js";
import AddAssignmentDialog from "@/components/AddAssignmentDialog";
import AddMessageDialog from "@/views/messaging/components/dialog/AddMessageDialog";
import moment from "moment";
import Sortable from "sortablejs";
import Spinner from "@/components/Spinner";

export default {
  name: "ExperimentAssignments",
  props: {
    experiment: {
      type: Object,
      required: true
    },
    balanced: {
      type: Boolean
    },
    activeExposureSet: { // exposure tab index
      type: Number,
      default: 0
    }
  },
  components: {
    AddAssignmentDialog,
    AddMessageDialog,
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
    exposure: null,
    minTreatments: 2,
    maxDesignGroups: 2,
    conditionTreatments: {},
    conditionColors: [""],
    rows: [],
    rowsExpanded: [],
    designExpanded: false,
    loaded: false,
    mobileBreakpoint: 636,
    assignmentHeaders: [
      {
        text: "",
        align: "start",
        sortable: false,
        value: "drag"
      },
      {
        text: "Component Name",
        align: "start",
        sortable: false,
        value: "title"
      },
      {
        text: "Treatments",
        sortable: false,
        value: "treatments"
      },
      {
        text: "Due Date",
        sortable: false,
        value: "dueDate"
      },
      {
        text: "Status",
        sortable: false,
        value: "published"
      },
      {
        text: "Actions",
        align: "center",
        sortable: false,
        value: "actions"
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
        value: "title"
      }
    ],
    treatmentIcon: {
      integration: "mdi-application-brackets-outline",
      assignment: "mdi-wrench-outline",
      file: "mdi-file-outline",
      message: "mdi-message-text-outline"
    },
    componentTableKey: 0,
  }),
  watch: {
    rowsCount: {
      handler() {
        this.calculateRows();
        this.expandRows();
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
      allMessageContainers: "messagingMessageContainer/messageContainers",
      configurations: "configuration/get"
    }),
    experimentId() {
      return parseInt(this.experiment.experimentId);
    },
    canDeleteAssignment() {
      return !this.experiment.started;
    },
    rowsCount() {
      return this.assignments.length + this.allMessageContainers.length;
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
    rowType() {
      return {
        assignment: "assignment",
        message: "message"
      };
    },
    messageStatuses() {
      return messageStatus;
    },
    exposureRows() {
      return this.rows[this.tab] || [];
    },
    isMessagingEnabled() {
      return this.configurations?.messagingEnabled || false;
    }
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
      saveEditMode: "navigation/saveEditMode",
      updateMessageContainer: "messagingMessageContainer/update",
      updateAllMessageContainers: "messagingMessageContainer/updateAll",
      sendMessageContainer: "messagingMessageContainer/send",
      deleteMessageContainer: "messagingMessageContainer/deleteContainer",
      moveMessageContainer: "messagingMessageContainer/move",
      duplicateMessageContainer: "messagingMessageContainer/duplicate"
    }),
    calculateRows() {
      // only display message containers if messaging is enabled
      const messageContainerRows = this.isMessagingEnabled ? this.allMessageContainers.map(
        (messageContainer) => ({
          ...messageContainer,
          assignmentId: messageContainer.id,
          assignmentOrder: messageContainer.configuration.order,
          dueDate: null,
          published: messageContainer.configuration.status === messageStatus.published,
          sent: messageContainer.configuration.status === messageStatus.sent,
          error: messageContainer.configuration.status === messageStatus.error,
          title: messageContainer.configuration.title,
          treatments: messageContainer.messages.map(
            (message) => ({
              ...message,
              status: message.configuration.status,
              treatmentId: message.id, // assignment treatment ID is a number, message ID is a UUID; no collisions possible
              conditionId: message.conditionId,
              assessmentDto: {
                integrationPreviewUrl: "",
                integration: false,
                integrationUrlValid: false,
                integrationIframeInfoUrl: "",
                questions: []
              }
            })
          ).sort((a,b) => a.conditionId - b.conditionId),
          type: this.rowType.message
        })
      ) : [];

        const assignmentRows = this.assignments.map(
          (assignment) => ({
            ...assignment,
            type: this.rowType.assignment
          })
        );

        let rows = [];

        for (let i = 0; i < this.exposures.length; i++) {
          rows.splice(i, 1,
            [
              ...assignmentRows.filter(a => a.exposureId === this.exposures[i].exposureId),
              ...messageContainerRows.filter(m => m.exposureId === this.exposures[i].exposureId)
            ]
            .toSorted((a, b) => a.assignmentOrder - b.assignmentOrder)
          );
        }

        this.rows = rows;
    },
    messageContainers(exposure) {
      if (!exposure) {
        return [];
      }

      return this.allMessageContainers.filter(m => m.exposureId === exposure.exposureId);
    },
    async saveOrder(event, rows, exposure) {
      const movedItem = rows.splice(event.oldDraggableIndex, 1)[0];
      rows.splice(event.newDraggableIndex, 0, movedItem);
      const updated = rows.map(
        (a, idx) => ({
          ...a,
          assignmentOrder: idx + 1,
        })
      );

      Promise.allSettled([
        // update assignments
        await this.saveAssignmentOrder([
          this.experiment.experimentId,
          exposure.exposureId,
          updated.filter((u) => u.type === this.rowType.assignment),
        ]),
        // update messages
        await this.updateAllMessageContainers(
          [
            this.experimentId,
            exposure.exposureId,
            updated
              .filter((u) => u.type === this.rowType.message)
              .map((u) => ({
                ...u,
                configuration: {
                  ...u.configuration,
                  order: u.assignmentOrder
                }
              }))
          ]
        )
      ]).then(this.componentTableKey++); // force refresh of component table
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
    async handleAssignmentEdit(assignment, exposureId) {
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
    dueDate(row) {
      return row.dueDate != null ? moment(row.dueDate).format('MMM D, YYYY hh:mma') : "";
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
    expandRows() {
      this.rowsExpanded = [];
      this.rows.forEach((exposureRows, eidx) => this.rowsExpanded[eidx] = [...exposureRows]);
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
    integrationsPreviewLaunchUrl(url = "http://localhost") {
      return `/integrations/preview?url=${btoa(url)}`;
    },
    handleTreatmentPreview(treatment) {
      window.open(`/preview/experiments/${this.experimentId}/conditions/${treatment.conditionId}/treatments/${treatment.treatmentId}?ownerId=${this.userId}`, "_blank");
    },
     async handleMoveComponent(exposureId, row) {
      switch (row.type) {
        case this.rowType.assignment:
          await this.handleMoveAssignment(exposureId, row);
          break;
        case this.rowType.message:
          this.handleMoveMessageContainer(exposureId, row);
          break;
        default:
          break;
      }
    },
    async handleEditComponent(exposureId, row) {
      switch (row.type) {
        case this.rowType.assignment:
          await this.handleAssignmentEdit(row, exposureId);
          break;
        case this.rowType.message:
          await this.handleEditMessageContainer(row.id, exposureId);
          break;
        default:
          break;
      }
    },
    async handleDuplicateComponent(exposureId, row) {
      switch (row.type) {
        case this.rowType.assignment:
          await this.handleDuplicateAssignment(exposureId, row);
          break;
        case this.rowType.message:
          await this.handleDuplicateMessageContainer(row);
          break;
        default:
          break;
      }
    },
    showDeleteComponent(row) {
      switch (row.type) {
        case this.rowType.assignment:
          return this.canDeleteAssignment;
        case this.rowType.message:
          return ![messageStatus.queued, messageStatus.sent, messageStatus.deleted].includes(row.configuration.status)
            && row.treatments.every(treatment => treatment.status !== messageStatus.sent);
        default:
          return false;
      }
    },
    showPublishComponent(row) {
      switch (row.type) {
        case this.rowType.assignment:
          return false;
        case this.rowType.message:
          return row.configuration.status === messageStatus.unpublished && !this.hasIncompleteTreatments(row);
        default:
          return false;
      }
    },
    showUnpublishComponent(row) {
      switch (row.type) {
        case this.rowType.assignment:
          return false;
        case this.rowType.message:
          return row.configuration.status === messageStatus.published;
        default:
          return false;
      }
    },
    async handleDeleteComponent(exposureId, row) {
      switch (row.type) {
        case this.rowType.assignment:
          await this.handleDeleteAssignment(exposureId, row);
          break;
        case this.rowType.message:
          await this.handleDeleteMessageContainer(row);
          break;
        default:
          break;
      }
    },
    async handlePublishComponent(exposureId, row) {
      switch (row.type) {
        case this.rowType.assignment:
          break;
        case this.rowType.message:
          await this.updateMessageContainer(
            [
              this.experimentId,
              exposureId,
              row.id,
              {
                ...row,
                configuration: {
                  ...row.configuration,
                  status: messageStatus.published
                }
              }
            ]
          );
          break;
        default:
          break;
      }
    },
    async handleUnpublishComponent(exposureId, row) {
      switch (row.type) {
        case this.rowType.assignment:
          break;
        case this.rowType.message:
          await this.updateMessageContainer(
            [
              this.experimentId,
              exposureId,
              row.id,
              {
                ...row,
                configuration: {
                  ...row.configuration,
                  status: messageStatus.unpublished
                }
              }
            ]
          );
          break;
        default:
          break;
      }
    },
    rowIcon(row) {
      switch (row.type) {
        case this.rowType.assignment:
          return this.treatmentIcon.file;
        case this.rowType.message:
          return this.treatmentIcon.message;
        default:
          return "";
      }
    },
    rowTreatmentsIcon(row, treatmentRow) {
      switch (row.type) {
        case this.rowType.assignment:
          return treatmentRow.assessmentDto.integration ? this.treatmentIcon.integration : this.treatmentIcon.assignment;
        case this.rowType.message:
          return ;
        default:
          return "";
      }
    },
    rowTreatmentsColumnClass(row) {
      switch (row.type) {
        case this.rowType.assignment:
        case this.rowType.message:
          return this.hasIncompleteTreatments(row) ? "label-treatment-incomplete" : "label-treatment-complete";
        default:
          return "";
      }
    },
    hasIncompleteTreatments(row) {
      switch (row.type) {
        case this.rowType.assignment:
          if (row.treatments.some(treatment => treatment.assessmentDto.integration && !treatment.assessmentDto.integrationUrlValid)) {
            return true;
          }

          return row.treatments.some(treatment => !(treatment.assessmentDto && treatment.assessmentDto.questions && treatment.assessmentDto.questions.length));
        case this.rowType.message:
          return !row.treatments.every((treatment) => [messageStatus.ready, messageStatus.disabled, messageStatus.sent].includes(treatment.configuration.status));
        default:
          return false;
      }
    },
    showRowTreatmentsColumnTooltipText(row) {
      switch (row.type) {
        case this.rowType.assignment:
          return "Set up your assignment by creating " + (row.treatments.length > 1 ? "treatments" : "a treatment") + ".";
        case this.rowType.message:
          return "Set up your message container by creating " + (row.treatments.length > 1 ? "messages" : "a message") + ".";
        default:
          return "";
      }
    },
    rowPublishedColumnClass(row) {
      switch (row.type) {
        case this.rowType.assignment:
          return row.published ? "label-treatment-complete" : "label-treatment-incomplete";
        case this.rowType.message:
          return !(row.published || row.sent) || this.hasIncompleteTreatments(row) || row.error ? "label-treatment-incomplete" : "label-treatment-complete";
        default:
          return "";
      }
    },
    rowPublishedColumnText(row) {
      switch (row.type) {
        case this.rowType.assignment:
        case this.rowType.message:
          if (row.published) {
            return "Published";
          } else if (row.sent) {
            return "Sent";
          } else if (row.error) {
            return "Error";
          } else {
            return "Unpublished";
          }
        default:
          return "";
      }
    },
    showTreatmentRowTooltip(row, treatment) {
      switch (row.type) {
        case this.rowType.assignment:
          if (treatment.assessmentDto.integration && !treatment.assessmentDto.integrationUrlValid) {
            return true;
          }

          return !(treatment.assessmentDto && treatment.assessmentDto.questions.length);
        case this.rowType.message:
          return ![messageStatus.ready, messageStatus.disabled, messageStatus.sent].includes(treatment.configuration.status);
        default:
          return false;
      }
    },
    treatmentRowTooltipText(row, treatment) {
      switch (row.type) {
        case this.rowType.assignment:
          if (treatment.assessmentDto.integration && !treatment.assessmentDto.integrationUrlValid) {
            return "Error rendering content. Please check the URL.";
          }

          return "Please add content to this treatment.";
        case this.rowType.message:
          return "Please create a message for this treatment.";
        default:
          return "";
      }
    },
    treatmentRowClass(row, treatment) {
      return this.showTreatmentRowTooltip(row, treatment) ? "label-treatment-incomplete" : "label-treatment-complete";
    },
    async handleEditTreatment(row, treatment) {
      switch (row.type) {
        case this.rowType.assignment:
          this.goToBuilder(treatment.conditionId, row.assignmentId);
          break;
        case this.rowType.message:
          this.handleMessageAction(row.id, treatment.id);
          break;
        default:
          break;
      }
    },
    editTreatmentIcon(row, treatment) {
      switch (row.type) {
        case this.rowType.assignment:
          return "mdi-pencil";
        case this.rowType.message:
          return ![messageStatus.queued, messageStatus.processing, messageStatus.sent, messageStatus.deleted].includes(treatment.configuration.status) ? "mdi-pencil" : "mdi-eye";
        default:
          break;
      }
    },
    editTreatmentText(row, treatment) {
      switch (row.type) {
        case this.rowType.assignment:
          return "Edit";
        case this.rowType.message:
          return ![messageStatus.queued, messageStatus.processing, messageStatus.sent, messageStatus.deleted].includes(treatment.configuration.status) ? "Edit" : "View";
        default:
          break;
      }
    },
    isIntegrationAssignment(row, treatment) {
      switch (row.type) {
        case this.rowType.assignment:
          return treatment.assessmentDto.integration;
        case this.rowType.message:
        default:
          return false;
      }
    },
    isMessage(row) {
      switch (row.type) {
        case this.rowType.assignment:
          return false;
        case this.rowType.message:
          return true;
        default:
          return false;
      }
    },
    async handleAddMessage(version, exposure) {
      await this.saveEditMode({
        initialPage: "MessageContainer",
        callerPage: {
          name: "ExperimentSummary",
          tab: "components",
          exposureSet: this.tab
        }
      });
      this.$router.push({
        name: "MessageContainer",
        params: {
          experimentId: this.experimentId,
          exposureId: exposure.exposureId,
          version: version,
          mode: "NEW"
        }
      });
    },
    showMoveAction(row) {
      if (this.exposures.length <= 1) {
        return false;
      }

      switch (row.type) {
        case this.rowType.assignment:
          return this.exposures.length > 1;
        case this.rowType.message:
          return ![messageStatus.queued, messageStatus.processing, messageStatus.sent, messageStatus.deleted].includes(row.configuration.status)
            && row.treatments.every(treatment => treatment.status !== messageStatus.sent);
        default:
          return false;
      }
    },
    showSendAction(row) {
      switch (row.type) {
        case this.rowType.assignment:
          return false;
        case this.rowType.message:
          return row.published === messageStatus.ready;
        default:
          return false;
      }
    },
    async handleEditMessageContainer(messageContainerId, exposureId) {
      await this.saveEditMode({
        initialPage: "MessageContainer",
        callerPage: {
          name: "ExperimentSummary",
          tab: "components",
          exposureSet: this.exposureSetIndex
        }
      });
      this.$router.push({
        name: "MessageContainer",
        params: {
          experimentId: this.experimentId,
          exposureId: exposureId,
          version: null,
          mode: "EDIT",
          containerId: messageContainerId
        }
      });
    },
    async handleSendMessageContainer(messageContainer) {
      try {
        await this.sendMessageContainer(
          [
            this.experimentId,
            messageContainer.exposureId,
            messageContainer.id
          ]
        );
      } catch (error) {
        console.error("handleSend | catch", { error });
      }
    },
    async handleDeleteMessageContainer(messageContainer) {
      const reallyDelete = await this.$swal({
        icon: "question",
        text: `Are you sure you want to delete the message container "${messageContainer.title}"?`,
        showCancelButton: true,
        confirmButtonText: "Yes, delete it",
        cancelButtonText: "No, cancel",
      });
      if (reallyDelete?.isConfirmed) {
        try {
          await this.deleteMessageContainer(
            [
              this.experimentId,
              messageContainer.exposureId,
              messageContainer.id
            ]
          );
        } catch (error) {
          console.error("handleDeleteMessageContainer | catch", { error });
        }
      }
    },
    async handleMoveMessageContainer(targetExposureId, messageContainer) {
      try {
        await this.moveMessageContainer([
          this.experimentId,
          messageContainer.exposureId,
          messageContainer.id,
          {
            ...messageContainer,
            exposureId: targetExposureId
          }
        ]);
      } catch (error) {
        console.error("handleMoveMessageContainer | catch", { error });
      }
    },
    async handleDuplicateMessageContainer(messageContainer) {
      try {
        await this.duplicateMessageContainer([
          this.experimentId,
          messageContainer.exposureId,
          messageContainer.id
        ]);
      } catch (error) {
        console.error("handleDuplicateMessageContainer | catch", { error });
      }
    },
    async handleMessageAction(messageContainerId, messageId) {
      await this.saveEditMode({
        initialPage: "Message",
        callerPage: {
          name: "ExperimentSummary",
          tab: "components",
          exposureSet: this.exposureSetIndex
        }
      });
      this.$router.push(
        {
          name: "Message",
          params: {
            messageId: messageId,
            containerId: messageContainerId
          }
        }
      )
    }
  },
  async mounted() {
    this.tab = parseInt(this.activeExposureSet);
    this.calculateRows();
    this.loaded = true;
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
  flex-direction: column;
  width: 100%;
  min-height: 100px;
  justify-content: center;
  background-color: #fffcf7 !important;
  border-color: #ffe0b2 !important;
  & .no-components-yet-buttons {
    min-width: fit-content;
    max-width: fit-content;
    min-height: fit-content;
    max-height: fit-content;
  }
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
.section-tab-components-balanced,
.section-tab-components-unbalanced {
  text-transform: none !important;
  opacity: 0.87 !important;
}
.treatment-btn,
.label-treatment-complete,
.section-tab-components-balanced {
  color: black !important;
}
.v-btn--disabled {
  .treatment-btn {
    color: rgba(0, 0, 0, 0.26) !important;
  }
}
.icon-treatment-incomplete,
.label-treatment-incomplete,
.section-tab-components-unbalanced {
  color: #E06766 !important;
}
div.section-components.py-3.px-3 {
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
div.component-buttons {
  max-width: fit-content;
}
</style>
