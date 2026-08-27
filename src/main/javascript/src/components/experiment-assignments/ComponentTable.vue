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
          density="compact"
          label
        >
          Only One Version
        </v-chip>
      </template>

      <template #expanded-row="{ item: row, columns }">
        <tr :class="['v-data-table__tr--expanded', { 'expanded-row--mobile': isMobile }]">
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
        </tr>
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
import { ref, computed, watch, onMounted, nextTick } from "vue";
import { useDisplay } from "vuetify";
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
const { width } = useDisplay();
// mirrors mobileBreakpoint (not Vuetify's own global mobile breakpoint) so this
// switches in step with the table's own mobile-row transform.
const isMobile = computed(() => width.value < mobileBreakpoint);

// the drag-to-reorder handle has no title, so Vuetify's mobile card layout
// rendered it as its own orphaned label-less row (just a lone dot-grid icon) -
// drop it in mobile view entirely, since a tiny drag handle is a poor touch
// target anyway and SortableJS's `handle: ".dragger"` simply won't find
// anything to bind to once it's gone, so dragging is just unavailable there.
const assignmentHeaders = computed(() => {
  const headers = [
    { title: "", align: "start", sortable: false, key: "drag" },
    { title: "Component Name", align: "start", sortable: false, key: "title" },
    { title: "Treatments", sortable: false, key: "treatments" },
    { title: "Due Date", sortable: false, key: "dueDate" },
    { title: "Status", sortable: false, key: "published" },
    { title: "Actions", align: "center", sortable: false, key: "actions" },
    { title: "", sortable: false, key: "data-table-expand" }
  ];

  return isMobile.value ? headers.filter(header => header.key !== "drag") : headers;
});
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

// Vuetify's mobile card layout right-aligns every cell's value by default
// (fine for short single-line values like a status or date), but that makes
// a wrapped multi-line value - the component title plus the "Only One
// Version" chip - read as ragged, hard-to-follow right-aligned lines. Left-
// align just this table's mobile values so wrapped text reads naturally.
:deep(.data-table-assignments .v-data-table__tr--mobile .v-data-table__td-value) {
  text-align: start;
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

  // 1px-wide drag-handle column - only meaningful for the desktop row-of-
  // columns layout. In mobile mode the drag column is dropped entirely (see
  // assignmentHeaders above) and each row's first <td> is actually
  // "Component Name" instead - excluding mobile rows here keeps this rule
  // from mistakenly shrinking that cell down to the handle's 1px width.
  > thead > tr > th:first-child,
  > tbody > tr:not(.v-data-table__tr--mobile) > td:first-child {
    width: 1px;
    padding-left: 0;
    padding-right: 0;
  }

  // real per-cell borders (not outline+border-radius on the tbody itself) so the
  // rounded card border renders reliably across browsers - table-row-group boxes
  // don't consistently honor border-radius on an outline
  > tbody {
    > tr {
      &:hover {
        background: unset !important;
      }

      > td:first-child {
        border-left: 1px solid rgba(0, 0, 0, 0.2);
      }

      > td:last-child {
        border-right: 1px solid rgba(0, 0, 0, 0.2);
      }

      // in mobile view every field is its own full-width stacked block, not
      // a column sharing the row's left/right edge with its siblings - the
      // two rules above only reach the row's structurally-first/last td
      // (Component Name at the top, the expand chevron at the bottom),
      // leaving Treatments/Due Date/Status/Actions in between with no side
      // border at all. Every mobile td needs its own left/right border for
      // the card's sides to read as one continuous line down the stack.
      &.v-data-table__tr--mobile > td {
        border-left: 1px solid rgba(0, 0, 0, 0.2);
        border-right: 1px solid rgba(0, 0, 0, 0.2);
      }

      &:first-child > td {
        padding-top: 8px !important;
        border-top: 1px solid rgba(0, 0, 0, 0.2);

        &:first-child {
          border-top-left-radius: 10px;
        }
      }

      // desktop only: rounds the top-right corner of the table's very first
      // row. In mobile view that same row is "Component Name" - already
      // rounded (both corners, since it's the only visually full-width
      // element at the top of the stack) by the per-component rule below.
      // Applying this there instead rounded the expand chevron's own small
      // box, since that's the row's structurally-last td in mobile - a
      // stray rounded corner with nothing else around it to make sense of.
      &:first-child:not(.v-data-table__tr--mobile) > td:last-child {
        border-top-right-radius: 10px;
      }

      // in mobile view, one component's row-of-fields (Component Name,
      // Treatments, Due Date, ...) stacks as several full-width label:value
      // lines instead of a single compact row. Vuetify's own CSS zeroes
      // border-bottom on every non-last mobile td, so without this, only the
      // table's literal first <tr> got dividers between its own fields - as
      // an accidental side effect of the border-top rule below applying to
      // ALL of that one row's children, not because it was actually meant to
      // provide inter-field dividers. Every mobile td needs its own explicit
      // top divider so every component's fields separate consistently, not
      // just the first component's.
      &.v-data-table__tr--mobile > td {
        border-top: 1px solid rgba(0, 0, 0, 0.2);
      }

      // component-to-component boundary: the divider above accounts for
      // fields *within* one component, but nothing marked where a NEW
      // component's stack begins - the divider after the *previous*
      // component's expanded content just looked like another routine
      // field-to-field line. Give every mobile row's first field the same
      // rounded-card treatment the table's very first row already had, so
      // each component still reads as its own card the way it does on
      // desktop.
      &.v-data-table__tr--mobile.assignment-row > td:first-child {
        padding-top: 8px !important;
        border-top-left-radius: 10px;
        border-top-right-radius: 10px;
      }

      // divider under each component's row group. Vuetify's own CSS zeroes
      // border-bottom on .v-data-table__tr--expanded from its "overrides" layer, so
      // this unlayered rule is needed just to win it back
      &.v-data-table__tr--expanded > td {
        border-bottom: 1px solid rgba(0, 0, 0, 0.2);
      }

      &:last-child > td {
        padding-bottom: 8px !important;
        border-bottom: 1px solid rgba(0, 0, 0, 0.2);

        &:first-child {
          border-bottom-left-radius: 10px;
        }

        &:last-child {
          border-bottom-right-radius: 10px;
        }
      }

      // mobile only: round every component's bottom corners (not just the
      // table's very last row) so each one reads as a complete, distinct
      // card - mirroring the per-component top-rounding above. <tr> can't
      // take a margin to put real whitespace between cards, so a thick
      // white "spacer" border stands in for one instead of the thin 1px
      // divider above; with both corners now rounded on every card, that
      // divider line isn't needed to tell them apart anymore. Written after
      // the :last-child rule above so it also wins (equal specificity, so
      // source order decides) for the table's actual last component too.
      // overflow: hidden clips the nested treatments <v-data-table>'s own
      // grey background to this radius - without it, that background's
      // square corners painted right over the curve, so the bottom edge
      // looked flat even with the radius correctly set.
      &.expanded-row--mobile > td {
        border-bottom: 32px solid white;
        border-bottom-left-radius: 10px;
        border-bottom-right-radius: 10px;
        overflow: hidden;
      }
    }
  }
}
</style>