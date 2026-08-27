<template>
  <div id="terracotta-main" tabindex="-1">
    <div v-if="!loaded" class="spinner-container-assignment">
      <Spinner height="50px" width="50px" />
    </div>

    <v-container v-if="loaded && experiment" class="px-0" fluid>
      <v-row>
        <v-col cols="12">
          <ExposureTabs
            v-model="tab"
            :exposures="exposures"
            :rows="rows"
            :balanced="balanced"
            :single-condition-experiment="singleConditionExperiment"
          />

          <v-window v-model="tab">
            <v-window-item
              v-for="(exposure, eidx) in exposures"
              :key="exposure.exposureId"
              :value="eidx"
              class="section-components py-3 px-3"
            >
              <div class="components-header d-flex justify-space-between align-center">
                <h3>Components</h3>

                <div
                  v-if="loaded && exposureRows.length"
                  class="component-buttons d-flex justify-space-between"
                >
                  <div v-if="isMessagingEnabled" class="mb-5 mr-5">
                    <AddMessageDialog
                      :has-existing="true"
                      :is-single-condition-experiment="singleConditionExperiment"
                      @add="handleAddMessage($event, exposure)"
                    />
                  </div>

                  <div>
                    <AddAssignmentDialog
                      :has-existing="true"
                      :is-single-condition-experiment="singleConditionExperiment"
                      @multiple="handleAssignmentMultipleVersions(exposure)"
                      @single="handleAssignmentSingleVersion(exposure)"
                    />
                  </div>
                </div>
              </div>

              <template v-if="loaded">
                <v-card
                  v-if="!rows[eidx]?.length"
                  class="no-assignments-yet d-flex flex-column rounded-lg mb-5 d-inline-block"
                  variant="outlined"
                >
                  <div class="no-assignments-yet-container">
                    <h4>You don't have any components yet</h4>

                    <div class="no-components-yet-buttons d-flex flex-row justify-space-between mx-auto">
                      <AddAssignmentDialog
                        :has-existing="false"
                        :is-single-condition-experiment="singleConditionExperiment"
                        @multiple="handleAssignmentMultipleVersions(exposure)"
                        @single="handleAssignmentSingleVersion(exposure)"
                      />

                      <AddMessageDialog
                        v-if="isMessagingEnabled"
                        :is-single-condition-experiment="singleConditionExperiment"
                        :has-existing="false"
                        class="ml-3"
                        @add="handleAddMessage($event, exposure)"
                      />
                    </div>
                  </div>
                </v-card>

                <ComponentTable
                  v-if="rows[eidx]?.length"
                  :key="componentTableKey"
                  :rows="rows[eidx]"
                  :exposure="exposure"
                  :conditions="conditions"
                  :condition-color-mapping="conditionColorMapping"
                  :single-condition-experiment="singleConditionExperiment"
                  :display-treatment-menu="displayTreatmentMenu"
                  :can-delete-assignment="canDeleteAssignment"
                  :exposure-count="exposures.length"
                  :alert-statuses="alertStatuses"
                  @save-order="saveOrder($event, rows[eidx], exposure)"
                  @move="handleMoveComponent"
                  @edit="handleEditComponent(exposure.exposureId, $event)"
                  @duplicate="handleDuplicateComponent(exposure.exposureId, $event)"
                  @delete="handleDeleteComponent(exposure.exposureId, $event)"
                  @publish="handlePublishComponent(exposure.exposureId, $event)"
                  @unpublish="handleUnpublishComponent(exposure.exposureId, $event)"
                  @edit-treatment="handleEditTreatment"
                  @preview-treatment="handleTreatmentPreview"
                />
              </template>

              <ExposureDesignCard
                v-if="!singleConditionExperiment"
                :exposure="exposure"
                :condition-color-mapping="conditionColorMapping"
              />
            </v-window-item>
          </v-window>
        </v-col>
      </v-row>
    </v-container>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, createApp, toRaw } from "vue";
import { useRouter } from "vue-router";
import { useDisplay } from "vuetify";
import Swal from "sweetalert2";

import { message as messageStatus } from "@/helpers/messaging/status.js";
import {
  statusAlert,
  createStatusAlert,
  showSkipLink
} from "@/helpers/ui-utils.js";

import AddAssignmentDialog from "@/components/dialog/AddAssignmentDialog.vue";
import AddMessageDialog from "@/views/messaging/components/dialog/AddMessageDialog.vue";
import MoveAssignmentDialog from "@/components/dialog/MoveAssignmentDialog.vue";
import Spinner from "@/components/Spinner.vue";
import ExposureTabs from "@/components/experiment-assignments/ExposureTabs.vue";
import ComponentTable from "@/components/experiment-assignments/ComponentTable.vue";
import ExposureDesignCard from "@/components/experiment-assignments/ExposureDesignCard.vue";

import vuetify from "@/plugins/vuetify";

import { experiment as experimentModule } from "@/store/experiment.module";
import { exposures as exposuresModule } from "@/store/exposures.module";
import { assignment as assignmentModule } from "@/store/assignment.module";
import { condition as conditionModule } from "@/store/condition.module";
import { api as apiModule } from "@/store/api.module";
import { configuration as configurationModule } from "@/store/configuration.module";
import { alert as alertModule } from "@/store/alert.module";
import { navigation as navigationModule } from "@/store/navigation.module";
import { container as messagingContainerModule } from "@/store/messaging/container.module";

const props = defineProps({
  experiment: {
    type: Object,
    required: true
  },
  balanced: {
    type: Boolean,
    default: false
  },
  activeExposureSet: {
    type: Number,
    default: 0
  }
});

const router = useRouter();
const { name: displayName } = useDisplay();

const experimentStore = experimentModule();
const exposuresStore = exposuresModule();
const assignmentStore = assignmentModule();
const conditionStore = conditionModule();
const apiStore = apiModule();
const configurationStore = configurationModule();
const alertStore = alertModule();
const navigationStore = navigationModule();
const messagingContainerStore = messagingContainerModule();

const tab = ref(0);
const loaded = ref(false);
const componentTableKey = ref(0);

const rowType = {
  assignment: "assignment",
  message: "message"
};

const conditions = computed(() => experimentStore.conditions || []);
const exposures = computed(() => exposuresStore.exposures || []);
const assignments = computed(() => assignmentStore.assignments || []);
const conditionColorMapping = computed(() => conditionStore.conditionColorMapping || {});
const userId = computed(() => apiStore.userId);
const allMessageContainers = computed(() => messagingContainerStore.messageContainers || []);
const configurations = computed(() => configurationStore.configurations || configurationStore.get || {});
const alertStatuses = computed(() => alertStore.statuses || {});

const experimentId = computed(() => Number(props.experiment.experimentId));
const canDeleteAssignment = computed(() => !props.experiment.started);
const singleConditionExperiment = computed(() => conditions.value.length === 1);
const defaultCondition = computed(() => conditions.value.find(condition => condition.defaultCondition));
const exposureRows = computed(() => rows.value[tab.value] || []);
const isMessagingEnabled = computed(() => configurations.value?.messagingEnabled || false);
const displayTreatmentMenu = computed(() => ["xs", "sm", "md"].includes(displayName.value));

// a plain computed instead of deep-watched refs recalculated imperatively: Vue's reactivity
// tracks exactly the fields read below, so it only recomputes when one of those fields
// actually changes instead of deep-diffing the entire assignments/message-container graphs
const rows = computed(() => {
  const messageContainers = allMessageContainers.value || [];
  const messageRows = isMessagingEnabled.value
    ? messageContainers.map(messageContainer => ({
        ...messageContainer,
        assignmentId: messageContainer.id,
        assignmentOrder: messageContainer.configuration.order,
        dueDate: null,
        published: messageContainer.configuration.status === messageStatus.published,
        sent: messageContainer.configuration.status === messageStatus.sent,
        error: messageContainer.configuration.status === messageStatus.error,
        title: messageContainer.configuration.title,
        treatments: messageContainer.messages
          .map(message => ({
            ...message,
            status: message.configuration.status,
            treatmentId: message.id,
            conditionId: message.conditionId,
            assessmentDto: {
              integrationPreviewUrl: "",
              integration: false,
              integrationUrlValid: false,
              integrationIframeInfoUrl: "",
              questions: []
            }
          }))
          .sort((a, b) => a.conditionId - b.conditionId),
        type: rowType.message
      }))
    : [];

  const assignmentsList = assignments.value || [];
  const assignmentRows = assignmentsList.map(assignment => ({
    ...assignment,
    type: rowType.assignment
  }));

  return exposures.value.map(exposure => {
    return [
      ...assignmentRows.filter(row => row.exposureId === exposure.exposureId),
      ...messageRows.filter(row => row.exposureId === exposure.exposureId)
    ].sort((a, b) => a.assignmentOrder - b.assignmentOrder);
  });
});

const saveOrder = async (event, exposureRows, exposure) => {
  const moved = exposureRows.splice(event.oldDraggableIndex, 1)[0];
  exposureRows.splice(event.newDraggableIndex, 0, moved);

  const updated = exposureRows.map((row, index) => ({
    ...row,
    assignmentOrder: index + 1
  }));

  await Promise.allSettled([
    assignmentStore.saveAssignmentOrder([
      experimentId.value,
      exposure.exposureId,
      updated.filter(row => row.type === rowType.assignment)
    ]),
    messagingContainerStore.updateAll([
      experimentId.value,
      exposure.exposureId,
      updated
        .filter(row => row.type === rowType.message)
        .map(row => ({
          ...row,
          configuration: {
            ...row.configuration,
            order: row.assignmentOrder
          }
        }))
    ])
  ]);

  componentTableKey.value++;

  createStatusAlert(
    statusAlert(alertStatuses.value.success, "Component order saved")
  );
};

const handleCreateAssignment = async (exposureId, conditionIds) => {
  await navigationStore.saveEditMode({
    initialPage: "AssignmentCreateAssignment",
    callerPage: {
      name: "ExperimentSummary",
      tab: "components",
      exposureSet: tab.value
    }
  });

  router.push({
    name: "AssignmentCreateAssignment",
    params: { exposureId },
    query: { conditionIds: JSON.stringify(conditionIds) }
  });
};

const handleAssignmentMultipleVersions = exposure => {
  return handleCreateAssignment(
    exposure.exposureId,
    exposure.groupConditionList.map(item => item.conditionId)
  );
};

const handleAssignmentSingleVersion = exposure => {
  return handleCreateAssignment(
    exposure.exposureId,
    [defaultCondition.value.conditionId]
  );
};

const handleAssignmentStartedAlert = async assignmentId => {
  const assignment = assignments.value.find(item => item.assignmentId === assignmentId);

  if (!assignment?.started) {
    return true;
  }

  const result = await Swal.fire({
    icon: "warning",
    text: "You are currently collecting assignment submissions, and at least one student has submitted the assignment. Making changes could compromise the integrity of your experiment.",
    showCancelButton: true,
    confirmButtonText: "OK",
    cancelButtonText: "Cancel",
    cancelButtonColor: "#515961",
    reverseButtons: true
  });

  return result.isConfirmed;
};

const handleAssignmentEdit = async (assignment, exposureId) => {
  const reallyEdit = await handleAssignmentStartedAlert(assignment.assignmentId);

  if (!reallyEdit) {
    return;
  }

  await assignmentStore.setCurrentAssignment(assignment);
  await navigationStore.saveEditMode({
    initialPage: "AssignmentEditor",
    callerPage: {
      name: "ExperimentSummary",
      tab: "components",
      exposureSet: tab.value
    }
  });

  router.push({
    name: "AssignmentEditor",
    params: {
      assignmentId: assignment.assignmentId,
      exposureId
    }
  });
};

const handleMoveAssignment = async (targetExposureId, assignment) => {
  try {
    const response = await assignmentStore.moveAssignment([
      experimentId.value,
      assignment.exposureId,
      assignment.assignmentId,
      {
        ...assignment,
        assignmentId: null,
        exposureId: targetExposureId
      }
    ]);

    if (response?.status === 201) {
      await assignmentStore.fetchAssignmentsByExposure([
        experimentId.value,
        targetExposureId,
        true
      ]);

      createStatusAlert(
        statusAlert(alertStatuses.value.success, "Assignment moved successfully")
      );
    }
  } catch (error) {
    console.error("handleMoveAssignment | catch", error);
    createStatusAlert(
      statusAlert(alertStatuses.value.error, "There was a problem moving the assignment")
    );
  }
};

const handleDeleteAssignment = async (exposureId, assignment) => {
  const result = await Swal.fire({
    icon: "question",
    text: `Are you sure you want to delete the assignment "${assignment.title}"?`,
    showCancelButton: true,
    confirmButtonText: "Yes, delete it",
    cancelButtonText: "No, cancel",
    cancelButtonColor: "#515961"
  });

  if (!result?.isConfirmed) {
    return;
  }

  try {
    await assignmentStore.deleteAssignment([
      experimentId.value,
      exposureId,
      assignment.assignmentId
    ]);

    createStatusAlert(
      statusAlert(alertStatuses.value.success, "Assignment deleted successfully")
    );
  } catch (error) {
    console.error("handleDeleteAssignment | catch", error);
    createStatusAlert(
      statusAlert(alertStatuses.value.error, "There was a problem deleting the assignment")
    );
  }
};

const handleDuplicateAssignment = async (exposureId, assignment) => {
  try {
    const response = await assignmentStore.duplicateAssignment([
      experimentId.value,
      exposureId,
      assignment.assignmentId
    ]);

    if (response?.status === 201) {
      await assignmentStore.fetchAssignmentsByExposure([
        experimentId.value,
        exposureId,
        true
      ]);

      createStatusAlert(
        statusAlert(alertStatuses.value.success, "Assignment duplicated successfully")
      );
    }
  } catch (error) {
    console.error("handleDuplicateAssignment | catch", error);
    createStatusAlert(
      statusAlert(alertStatuses.value.error, "There was a problem duplicating the assignment")
    );
  }
};

const goToBuilder = async (treatment, assignmentId, exposureId) => {
  const reallyEdit = await handleAssignmentStartedAlert(assignmentId);

  if (!reallyEdit) {
    return;
  }

  await navigationStore.saveEditMode({
    initialPage: "TerracottaBuilder",
    callerPage: {
      name: "ExperimentSummary",
      tab: "components",
      exposureSet: tab.value
    }
  });

  // The row's treatment (and its assessment) already exist - fetched from the
  // backend along with the assignment - so we navigate straight to the builder
  // with those existing IDs rather than creating a new treatment/assessment.
  router.push({
    name: "TerracottaBuilder",
    params: {
      experimentId: experimentId.value,
      exposureId,
      assignmentId,
      conditionId: treatment.conditionId,
      treatmentId: treatment.treatmentId,
      assessmentId: treatment.assessmentDto.assessmentId
    },
    state: {
      current_assignment: toRaw(assignments.value.find(item => item.assignmentId === assignmentId))
    }
  });
};

const handleEditTreatment = ({ row, treatment }) => {
  if (row.type === rowType.assignment) {
    return goToBuilder(treatment, row.assignmentId, row.exposureId);
  }

  return handleMessageAction(row.id, treatment.id);
};

const handleTreatmentPreview = treatment => {
  window.open(
    `/preview/experiments/${experimentId.value}/conditions/${treatment.conditionId}/treatments/${treatment.treatmentId}?ownerId=${userId.value}`,
    "_blank"
  );
};

const handleMoveComponent = async row => {
  const availableExposures = exposures.value.filter(exposure => exposure.exposureId !== row.exposureId);
  const selectedExposure = await handleDisplayMoveAssignmentDialog(availableExposures, row.title);

  if (!selectedExposure || selectedExposure.isDismissed) {
    return;
  }

  if (row.type === rowType.assignment) {
    await handleMoveAssignment(selectedExposure.value.exposureId, row);
    return;
  }

  await handleMoveMessageContainer(selectedExposure.value.exposureId, row);
};

const handleEditComponent = async (exposureId, row) => {
  if (row.type === rowType.assignment) {
    await handleAssignmentEdit(row, exposureId);
    return;
  }

  await handleEditMessageContainer(row.id, exposureId);
};

const handleDuplicateComponent = async (exposureId, row) => {
  if (row.type === rowType.assignment) {
    await handleDuplicateAssignment(exposureId, row);
    return;
  }

  await handleDuplicateMessageContainer(row);
};

const handleDeleteComponent = async (exposureId, row) => {
  if (row.type === rowType.assignment) {
    await handleDeleteAssignment(exposureId, row);
    return;
  }

  await handleDeleteMessageContainer(row);
};

const handlePublishComponent = async (exposureId, row) => {
  if (row.type !== rowType.message) {
    return;
  }

  await messagingContainerStore.update([
    experimentId.value,
    exposureId,
    row.id,
    {
      ...row,
      configuration: {
        ...row.configuration,
        status: messageStatus.published
      }
    }
  ]);

  createStatusAlert(
    statusAlert(alertStatuses.value.success, "Message published successfully")
  );
};

const handleUnpublishComponent = async (exposureId, row) => {
  if (row.type !== rowType.message) {
    return;
  }

  await messagingContainerStore.update([
    experimentId.value,
    exposureId,
    row.id,
    {
      ...row,
      configuration: {
        ...row.configuration,
        status: messageStatus.unpublished
      }
    }
  ]);

  createStatusAlert(
    statusAlert(alertStatuses.value.success, "Message unpublished successfully")
  );
};

const handleAddMessage = async (version, exposure) => {
  await navigationStore.saveEditMode({
    initialPage: "MessageContainer",
    callerPage: {
      name: "ExperimentSummary",
      tab: "components",
      exposureSet: tab.value
    }
  });

  router.push({
    name: "MessageContainer",
    params: { experimentId: experimentId.value },
    query: {
      exposureId: exposure.exposureId,
      version,
      mode: "NEW"
    }
  });
};

const handleEditMessageContainer = async (messageContainerId, exposureId) => {
  await navigationStore.saveEditMode({
    initialPage: "MessageContainer",
    callerPage: {
      name: "ExperimentSummary",
      tab: "components",
      exposureSet: tab.value
    }
  });

  router.push({
    name: "MessageContainer",
    params: { experimentId: experimentId.value },
    query: {
      exposureId,
      mode: "EDIT",
      containerId: messageContainerId
    }
  });
};

const handleDeleteMessageContainer = async messageContainer => {
  const result = await Swal.fire({
    icon: "question",
    text: `Are you sure you want to delete the message container "${messageContainer.title}"?`,
    showCancelButton: true,
    confirmButtonText: "Yes, delete it",
    cancelButtonText: "No, cancel"
  });

  if (!result?.isConfirmed) {
    return;
  }

  try {
    await messagingContainerStore.deleteContainer([
      experimentId.value,
      messageContainer.exposureId,
      messageContainer.id
    ]);

    createStatusAlert(
      statusAlert(alertStatuses.value.success, "Message container deleted successfully")
    );
  } catch (error) {
    console.error("handleDeleteMessageContainer | catch", error);
    createStatusAlert(
      statusAlert(alertStatuses.value.error, "There was a problem deleting the message container")
    );
  }
};

const handleMoveMessageContainer = async (targetExposureId, messageContainer) => {
  try {
    await messagingContainerStore.move([
      experimentId.value,
      messageContainer.exposureId,
      messageContainer.id,
      {
        ...messageContainer,
        exposureId: targetExposureId
      }
    ]);

    createStatusAlert(
      statusAlert(alertStatuses.value.success, "Message container moved successfully")
    );
  } catch (error) {
    console.error("handleMoveMessageContainer | catch", error);
    createStatusAlert(
      statusAlert(alertStatuses.value.error, "There was a problem moving the message container")
    );
  }
};

const handleDuplicateMessageContainer = async messageContainer => {
  try {
    await messagingContainerStore.duplicate([
      experimentId.value,
      messageContainer.exposureId,
      messageContainer.id
    ]);

    createStatusAlert(
      statusAlert(alertStatuses.value.success, "Message container duplicated successfully")
    );
  } catch (error) {
    console.error("handleDuplicateMessageContainer | catch", error);
    createStatusAlert(
      statusAlert(alertStatuses.value.error, "There was a problem duplicating the message container")
    );
  }
};

const handleMessageAction = async (messageContainerId, messageId) => {
  await navigationStore.saveEditMode({
    initialPage: "Message",
    callerPage: {
      name: "ExperimentSummary",
      tab: "components",
      exposureSet: tab.value
    }
  });

  router.push({
    name: "Message",
    query: {
      messageId,
      containerId: messageContainerId
    }
  });
};

const handleDisplayMoveAssignmentDialog = availableExposures => {
  let dialogApp = null;

  return Swal.fire({
    html: '<div id="dialog-move-assignment"></div>',
    showCancelButton: true,
    confirmButtonText: "Move",
    cancelButtonText: "Cancel",
    reverseButtons: true,
    allowOutsideClick: false,
    allowEscapeKey: false,
    focusConfirm: false,
    customClass: {
      confirmButton: "response-option-confirm",
      popup: "move-assignment-popup"
    },
    preConfirm: () => {
      const exposureOption = Swal.getPopup().querySelector("input#exposure-option-selected");

      if (exposureOption?.value) {
        return {
          exposureId: exposureOption.value
        };
      }

      Swal.showValidationMessage("Please select an exposure to move the assignment to.");
      return false;
    },
    didOpen: () => {
      const mountTarget = document.getElementById("dialog-move-assignment");
      dialogApp = createApp(MoveAssignmentDialog, {
        exposures: availableExposures
      });
      dialogApp.use(vuetify);
      dialogApp.mount(mountTarget);

      const exposureSetSelect = Swal.getHtmlContainer().querySelector("#move-radio-group");
      exposureSetSelect?.focus();
    },
    willClose: () => {
      dialogApp?.unmount();
    }
  });
};

onMounted(async () => {
  showSkipLink(true);
  tab.value = Number(props.activeExposureSet);
  await nextTick();
  loaded.value = true;
});
</script>

<style lang="scss">
.v-tabs {
  height: auto;

  .v-tab {
    padding: 16px 16px;
  }
}

// Vuetify 2's expanded-item slot auto-wrapped the consumer's <td> in
// tr.v-data-table__expanded__content. Vuetify 3 renamed the slot to expanded-row and
// stopped auto-wrapping - the consumer (ComponentTable.vue) now provides its own <tr>
// (class v-data-table__tr--expanded, matching Vuetify 3's naming for the equivalent
// internal case) - without which this selector had nothing valid to match at all (the
// migrated template omitted the <tr> entirely until this fix, producing invalid markup:
// a <td> with no <tr> parent).
.v-data-table
  > .v-table__wrapper
  tbody
  tr.v-data-table__tr--expanded {
  box-shadow: none;

  // desktop only: ComponentTable.vue's mobile-card view puts a real 32px
  // white "spacer" border-bottom on each expanded row (see its own scoped
  // styles) to space component cards apart - this !important unconditionally
  // zeroed that back out. Kept for desktop, where a thin 1px divider
  // (ComponentTable.vue's own unlayered border-bottom rule) is what's wanted
  // instead of Vuetify's own default border/shadow.
  &:not(.expanded-row--mobile) {
    border-bottom: 0 !important;
  }

  > td {
    // matches the nested treatment table's own background (map.get($grey, "lightest"),
    // set in ComponentTable.vue) - a mismatched shade here was visible as a lighter
    // strip wherever this td has padding the nested table doesn't fill (e.g. the last
    // row's added bottom padding for the rounded card corner)
    background-color: map.get($grey, "lightest") !important;

    .v-table__wrapper {
      border: none !important;
      border-radius: 0;
    }
  }
}

.v-tooltip > .v-overlay__content {
  max-width: 400px;
  opacity: 1 !important;
  background-color: rgba(55, 61, 63, 1) !important;
  color: #fff !important;

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

  > h4 {
    width: fit-content;
    margin: 0 auto;
  }
}

.section-tab-set {
  color: black;
  opacity: 0.74;
}

.label-treatment-complete {
  padding-right: 10px;
}

.treatment-btn,
.label-treatment-complete,
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

.section-tab-components-unbalanced {
  color: map.get($red, "base") !important;
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
    display: flex;
    align-items: center;
    justify-content: center;
    border: thin solid rgba(0, 0, 0, 0.12) !important;
    border-radius: 8px !important;
  }
}

div.no-assignments-yet.px-5.py-5.mx-3.mb-5,
div.data-table-assignments.mx-3.mb-5.mt-3,
div.data-table-design.px-5.py-5.rounded-lg.mx-3.mb-5.d-inline-block {
  margin-left: 0 !important;
}

div.data-table-design.px-5.py-5.rounded-lg.mx-3.mb-5.d-inline-block {
  border: thin solid rgb(224, 224, 224) !important;
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

td.treatments-table-container .v-table__wrapper table {
  padding: 0 !important;
}

.v-locale--is-ltr .v-data-table > .v-table__wrapper > table > tbody > tr > th,
.v-locale--is-ltr .v-data-table > .v-table__wrapper > table > tfoot > tr > th,
.v-locale--is-ltr .v-data-table > .v-table__wrapper > table > thead > tr > th,
div.data-table-assignments > .v-table__wrapper > table > tbody > tr > td:not(.treatments-table-container) {
  padding: 4px !important;
}

div.data-table-assignments > .v-table__wrapper > table > tbody > tr > td:not(.treatments-table-container),
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
span.v-chip.v-chip--label > .v-chip__content {
  min-height: fit-content !important;
  height: unset !important;
  max-width: 400px !important;
}

a.integration-preview-link {
  color: rgba(0, 0, 0, 0.87) !important;
  text-decoration: none;
  font-size: 1rem;
}

.component-icon {
  color: rgba(0, 0, 0, 0.54) !important;
}

div.components-header {
  /* the heading and the Add Message/Add Assignment buttons have nowhere to
     go at narrow widths without this - the buttons row's max-width:
     fit-content held its own preferred size regardless of available space,
     pushing it off-screen to the right instead of dropping below. */
  flex-wrap: wrap;
  row-gap: 16px;
}

div.component-buttons {
  max-width: fit-content;
  /* lets Add Message/Add Assignment stack instead of overflowing off-screen
     once the two buttons together no longer fit even on their own row. */
  flex-wrap: wrap;
  row-gap: 16px;
}

.swal2-styled {
  &.swal2-cancel {
    background-color: map.get($swal, "cancel") !important;
  }
}
</style>
