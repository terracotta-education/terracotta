<template>
  <div
    ref="tableRoot"
    @sorted="$emit('save-order', $event.detail)"
  >
    <v-data-table
      v-model:expanded="expandedRows"
      :headers="assignmentHeaders"
      :items="rows"
      :sort-by="[{ key: 'assignmentOrder', order: 'asc' }]"
      :mobile-breakpoint="mobileBreakpoint"
      :items-per-page="-1"
      :row-props="() => ({ class: 'assignment-row' })"
      item-value="assignmentId"
      class="v-data-table-alt v-data-table--sorted data-table-assignments mx-3 mb-5 mt-3"
      density="compact"
      hide-default-footer
      show-expand
    >
      <template #item.data-table-expand="{ internalItem, isExpanded, toggleExpand }">
        <v-icon
          :aria-label="`Expand component row ${internalItem.raw.title}`"
          @click="toggleExpand(internalItem)"
        >
          {{ isExpanded(internalItem) ? "mdi-chevron-up" : "mdi-chevron-down" }}
        </v-icon>
      </template>

      <template #item.title="{ item: row }">
        <v-icon>{{ rowIcon(row) }}</v-icon>
        {{ row.title }}

        <v-chip
          v-if="row.treatments.length === 1"
          color="#d3d3d3"
          class="v-chip--only-one"
          variant="flat"
          label
        >
          Only One Version
        </v-chip>
      </template>

      <template #expanded-row="{ item: row, columns }">
        <td
          :colspan="columns.length"
          class="treatments-table-container"
        >
          <v-data-table
            :headers="treatmentHeaders"
            :items="row.treatments"
            :items-per-page="-1"
            item-value="treatmentId"
            class="treatment-row bg-grey-lighten-5"
            hide-default-header
            hide-default-footer
          >
            <template #item.title="{ item: treatment }">
              <TreatmentRow
                :row="row"
                :treatment="treatment"
                :exposure="exposure"
                :conditions="conditions"
                :condition-color-mapping="conditionColorMapping"
                :single-condition-experiment="singleConditionExperiment"
                :display-treatment-menu="displayTreatmentMenu"
                @edit-treatment="$emit('edit-treatment', $event)"
                @preview-treatment="$emit('preview-treatment', $event)"
              />
            </template>
          </v-data-table>
        </td>
      </template>

      <template #item.treatments="{ item: row }">
        <span :class="rowTreatmentsColumnClass(row)">
          {{ row.treatments.length }} / {{ row.treatments.length }}

          <ToolTip
            v-if="hasIncompleteTreatments(row)"
            :content="showRowTreatmentsColumnTooltipText(row)"
            :ref="`tooltip-component-${row.assignmentId}`"
            aria-label="incomplete treatments explanation tooltip"
            icon="mdi-alert-circle-outline"
            alignment="top"
            activator-type="icon"
            activator-class="label-treatment-incomplete"
          />
        </span>
      </template>

      <template #item.drag>
        <span class="dragger">
          <v-icon>mdi-drag</v-icon>
        </span>
      </template>

      <template #item.published="{ item: row }">
        <span :class="rowPublishedColumnClass(row)">
          {{ rowPublishedColumnText(row) }}
        </span>
      </template>

      <template #item.dueDate="{ item: row }">
        {{ dueDate(row) }}
      </template>

      <template #item.actions="{ item: row }">
        <ComponentActionsMenu
          v-model="actionsMenuOpen[row.assignmentId]"
          :row="row"
          :can-delete-assignment="canDeleteAssignment"
          :exposure-count="exposureCount"
          :has-incomplete-treatments="hasIncompleteTreatments"
          @move="$emit('move', $event)"
          @edit="$emit('edit', $event)"
          @duplicate="$emit('duplicate', $event)"
          @delete="$emit('delete', $event)"
          @publish="$emit('publish', $event)"
          @unpublish="$emit('unpublish', $event)"
        />
      </template>
    </v-data-table>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, nextTick } from "vue";
import Sortable from "sortablejs";
import dayjs from "@/plugins/dayjs";

import { message as messageStatus } from "@/helpers/messaging/status.js";
import { deleteAttributesFromElement } from "@/helpers/ui-utils.js";
import ToolTip from "@/components/ToolTip.vue";
import TreatmentRow from "./TreatmentRow.vue";
import ComponentActionsMenu from "./ComponentActionsMenu.vue";

const props = defineProps({
  rows: {
    type: Array,
    required: true
  },
  exposure: {
    type: Object,
    required: true
  },
  conditions: {
    type: Array,
    required: true
  },
  conditionColorMapping: {
    type: Object,
    required: true
  },
  singleConditionExperiment: {
    type: Boolean,
    default: false
  },
  displayTreatmentMenu: {
    type: Boolean,
    default: false
  },
  canDeleteAssignment: {
    type: Boolean,
    default: false
  },
  exposureCount: {
    type: Number,
    default: 1
  }
});

defineEmits([
  "save-order",
  "move",
  "edit",
  "duplicate",
  "delete",
  "publish",
  "unpublish",
  "edit-treatment",
  "preview-treatment"
]);

const tableRoot = ref(null);
const expandedRows = ref([]);
const actionsMenuOpen = ref({});

const mobileBreakpoint = 636;
const assignmentHeaders = [
  { title: "", align: "start", sortable: false, key: "drag" },
  { title: "Component Name", align: "start", sortable: false, key: "title" },
  { title: "Treatments", sortable: false, key: "treatments" },
  { title: "Due Date", sortable: false, key: "dueDate" },
  { title: "Status", sortable: false, key: "published" },
  { title: "Actions", align: "center", sortable: false, key: "actions" },
  { title: "", sortable: false, key: "data-table-expand" }
];
const treatmentHeaders = [
  { title: "Treatment Name", align: "start", sortable: false, key: "title" }
];
const rowType = {
  assignment: "assignment",
  message: "message"
};
const treatmentIcon = {
  file: "mdi-file-outline",
  message: "mdi-message-text-outline"
};

watch(
  () => props.rows,
  rows => {
    expandedRows.value = rows.map(row => row.assignmentId);
  },
  { immediate: true }
);

watch(
  actionsMenuOpen,
  async () => {
    await nextTick();
    deleteAttributesFromElement(".list-item-move", ["tabindex"]);
  },
  { deep: true }
);

const initSortable = async () => {
  await nextTick();

  const tbody = tableRoot.value?.querySelector(".data-table-assignments tbody");

  if (!tbody) {
    return;
  }

  Sortable.create(tbody, {
    animation: 150,
    handle: ".dragger",
    draggable: ".assignment-row",
    onUpdate(event) {
      tableRoot.value.dispatchEvent(
        new CustomEvent("sorted", {
          detail: event,
          bubbles: true
        })
      );
    }
  });
};

const rowIcon = row => {
  if (row.type === rowType.assignment) {
    return treatmentIcon.file;
  }

  if (row.type === rowType.message) {
    return treatmentIcon.message;
  }

  return "";
};

const dueDate = row => {
  return row.dueDate
    ? dayjs(row.dueDate).format("MMM D, YYYY hh:mma")
    : "";
};

const hasIncompleteTreatments = row => {
  if (row.type === rowType.assignment) {
    if (
      row.treatments.some(
        treatment => treatment.assessmentDto.integration && !treatment.assessmentDto.integrationUrlValid
      )
    ) {
      return true;
    }

    return row.treatments.some(
      treatment => !(treatment.assessmentDto && treatment.assessmentDto.questions && treatment.assessmentDto.questions.length)
    );
  }

  if (row.type === rowType.message) {
    return !row.treatments.every(treatment =>
      [messageStatus.ready, messageStatus.disabled, messageStatus.sent].includes(treatment.configuration.status)
    );
  }

  return false;
};

const rowTreatmentsColumnClass = row => {
  return hasIncompleteTreatments(row)
    ? "label-treatment-incomplete"
    : "label-treatment-complete";
};

const showRowTreatmentsColumnTooltipText = row => {
  if (row.type === rowType.assignment) {
    return `Set up your assignment by creating ${row.treatments.length > 1 ? "treatments" : "a treatment"}.`;
  }

  if (row.type === rowType.message) {
    return `Set up your message container by creating ${row.treatments.length > 1 ? "messages" : "a message"}.`;
  }

  return "";
};

const rowPublishedColumnClass = row => {
  if (row.type === rowType.assignment) {
    return row.published
      ? "label-treatment-complete"
      : "label-treatment-incomplete";
  }

  if (row.type === rowType.message) {
    return !(row.published || row.sent) || hasIncompleteTreatments(row) || row.error
      ? "label-treatment-incomplete"
      : "label-treatment-complete";
  }

  return "";
};

const rowPublishedColumnText = row => {
  if (row.published) {
    return "Published";
  }

  if (row.sent) {
    return "Sent";
  }

  if (row.error) {
    return "Error";
  }

  return "Unpublished";
};

onMounted(initSortable);
</script>

<style lang="scss" scoped>
.treatment-row {
  :deep(.v-table__wrapper) {
    border: none !important;
    border-radius: 0 !important;
    background-color: map.get($grey, "lightest") !important;
    > table {
      padding-top: 0px !important;
    }
  }
}

:deep(.data-table-assignments > .v-table__wrapper) {
  border: none !important;
}

// this table already draws its own border via the tbody outline below (rows expand to
// variable heights, unlike .v-data-table-alt's ::before box-shadow which assumes a fixed
// row height) - disable the alt class's border so the two don't double up
:deep(.data-table-assignments > .v-table__wrapper > table > tbody::before) {
  content: none;
}

:deep(.data-table-assignments > .v-table__wrapper > table) {
  > thead > tr > th {
    border-bottom: none !important;
  }

  > thead > tr > th:first-child,
  > tbody > tr > td:first-child {
    width: 1px;
    padding-left: 0;
    padding-right: 0;
  }

  > tbody {
    display: table-row-group;
    outline: 1px solid rgba(0, 0, 0, 0.12);
    outline-offset: -1px;
    border-radius: 10px;

    > tr {
      &:hover {
        background: unset !important;
      }

      &:first-child > td {
        padding-top: 8px !important;
      }

      &:last-child > td {
        padding-bottom: 8px !important;
      }
    }
  }
}
</style>