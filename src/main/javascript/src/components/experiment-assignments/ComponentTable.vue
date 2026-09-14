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
      :row-props="({ item, index }) => ({
        class: [
          'assignment-row',
          {
            'assignment-row--last': index === rows.length - 1,
            'assignment-row--collapsed': !expandedRows.includes(item.assignmentId)
          }
        ]
      })"
      item-value="assignmentId"
      class="v-data-table-alt v-data-table--sorted data-table-assignments mx-3 mb-5 mt-3"
      hide-default-footer
      show-expand
    >
      <template #item.data-table-expand="{ internalItem, isExpanded, toggleExpand }">
        <v-btn
          :aria-label="`Expand component row ${internalItem.raw.title}`"
          :aria-expanded="isExpanded(internalItem) ? 'true' : 'false'"
          :icon="isExpanded(internalItem) ? 'mdi-chevron-up' : 'mdi-chevron-down'"
          variant="text"
          density="compact"
          @click="toggleExpand(internalItem)"
        />
      </template>

      <template #item.title="{ item: row }">
        <div class="title-cell d-flex align-center">
          <div class="icon-circle" :class="rowIconCircleClass(row)">
            <v-icon>{{ rowIcon(row) }}</v-icon>
          </div>
          <span class="row-title">{{ row.title }}</span>

          <v-tooltip
            v-if="row.treatments.length === 1"
            location="top"
            content-class="tool-tip-content"
          >
            <template #activator="{ props: tooltipProps }">
              <v-chip
                v-bind="tooltipProps"
                color="#616161"
                variant="tonal"
                density="compact"
                class="only-one-version-chip ml-2"
              >
                Only One Version
              </v-chip>
            </template>
            <div class="tool-tip-content-body">
              This component has the same content for all students.
            </div>
          </v-tooltip>
        </div>
      </template>

      <template #expanded-row="{ item: row, columns }">
        <tr :class="['v-data-table__tr--expanded', { 'expanded-row--mobile': isMobile }]">
          <td
            :colspan="columns.length"
            class="treatments-table-container"
          >
            <div
              v-if="!singleConditionExperiment"
              class="treatments-section-label"
            >
              TREATMENTS - {{ completeTreatmentsCountForRow(row) }} of {{ treatmentsTotalForRow(row) }} built
            </div>

            <v-data-table
              :headers="treatmentHeaders"
              :items="treatmentTableItems(row)"
              :items-per-page="-1"
              item-value="treatmentId"
              :class="['treatment-row', 'bg-grey-lighten-5', { 'treatment-row--mobile': isMobile }]"
              hide-default-header
              hide-default-footer
            >
              <template #item.title="{ item }">
                <div
                  v-if="item.isPlaceholder"
                  class="treatment-row-content treatment-add-row d-flex align-center"
                >
                  <div class="treatment-add-main">
                    <div class="treatment-info-group d-flex align-center">
                      <div class="icon-circle" :class="placeholderIconCircleClass(row, item.treatment)">
                        <v-icon>{{ placeholderIcon(row, item.treatment) }}</v-icon>
                      </div>
                      <span class="treatment-add-condition-name mr-2">{{ conditionDisplayName(row, item.condition) }}</span>
                      <button
                        type="button"
                        class="treatment-add-box"
                        :aria-label="`add treatment for ${conditionDisplayName(row, item.condition)}`"
                        @click="handlePlaceholderEdit(row, item)"
                      >
                        <v-icon>mdi-plus</v-icon>
                      </button>
                      <button
                        type="button"
                        class="treatment-add-link ml-2"
                        @click="handlePlaceholderEdit(row, item)"
                      >
                        Click to build treatment
                      </button>
                    </div>

                    <v-tooltip
                      location="top"
                      content-class="tool-tip-content"
                    >
                      <template #activator="{ props: tooltipProps }">
                        <v-chip
                          v-bind="tooltipProps"
                          variant="tonal"
                          color="error"
                          density="compact"
                          class="status-pill treatment-add-status-pill"
                          :style="statusPillOffsetStyle(columnOffsets.status)"
                        >
                          <v-icon start>mdi-alert-circle-outline</v-icon>
                          Needs attention
                        </v-chip>
                      </template>
                      <div class="tool-tip-content-body">
                        There are versions of this component that have not yet been created. Be sure to create all versions before publishing.
                      </div>
                    </v-tooltip>
                  </div>

                  <v-menu location="top start">
                    <template #activator="{ props: menuProps }">
                      <v-btn
                        v-bind="menuProps"
                        :aria-label="`treatment actions for ${conditionDisplayName(row, item.condition)}`"
                        :style="columnOffsetStyle(columnOffsets.actions)"
                        class="treatment-add-actions-btn"
                        icon="mdi-dots-vertical"
                        variant="text"
                        density="compact"
                      />
                    </template>

                    <v-list>
                      <v-list-item @click="handlePlaceholderEdit(row, item)">
                        <v-list-item-title class="d-flex justify-content-center">
                          <v-icon>mdi-pencil</v-icon>
                          <span>Edit</span>
                        </v-list-item-title>
                      </v-list-item>

                      <v-list-item disabled>
                        <v-list-item-title class="d-flex justify-content-center">
                          <v-icon>mdi-eye-outline</v-icon>
                          <span>Preview</span>
                        </v-list-item-title>
                      </v-list-item>
                    </v-list>
                  </v-menu>
                </div>

                <TreatmentRow
                  v-else
                  :row="row"
                  :treatment="item"
                  :exposure="exposure"
                  :actions-offset="columnOffsets.actions"
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
          {{ completeTreatmentsCountForRow(row) }} of {{ treatmentsTotalForRow(row) }}

          <ToolTip
            v-if="hasIncompleteTreatments(row)"
            :content="showRowTreatmentsColumnTooltipText(row)"
            :ref="`tooltip-component-${row.assignmentId}`"
            aria-label="incomplete treatments explanation tooltip"
            icon="mdi-circle"
            alignment="top"
            activator-type="icon"
            activator-class="label-treatment-incomplete treatment-ratio-dot"
          />
        </span>
      </template>

      <template #item.drag="{ item: row, index }">
        <button
          type="button"
          class="dragger"
          :data-drag-handle="row.assignmentId"
          :aria-label="`Reorder ${row.title}. Position ${index + 1} of ${rows.length}. Use the up and down arrow keys to move this row.`"
          @keydown="handleDragKeydown($event, row, index)"
        >
          <v-icon>mdi-drag</v-icon>
        </button>
      </template>

      <template #item.published="{ item: row }">
        <v-tooltip
          v-if="statusTooltipText(row)"
          location="top"
          content-class="tool-tip-content"
        >
          <template #activator="{ props: tooltipProps }">
            <v-chip
              v-bind="tooltipProps"
              variant="tonal"
              :color="statusPillColor(row)"
              density="compact"
              class="status-pill"
            >
              {{ rowPublishedColumnText(row) }}
            </v-chip>
          </template>
          <div class="tool-tip-content-body">
            {{ statusTooltipText(row) }}
          </div>
        </v-tooltip>
        <v-chip
          v-else
          variant="tonal"
          :color="statusPillColor(row)"
          density="compact"
          class="status-pill"
        >
          {{ rowPublishedColumnText(row) }}
        </v-chip>
      </template>

      <template #item.dueDate="{ item: row }">
        <v-tooltip
          v-if="row.dueDate"
          location="top"
          content-class="tool-tip-content"
        >
          <template #activator="{ props: tooltipProps }">
            <span v-bind="tooltipProps">{{ dueDate(row) }}</span>
          </template>
          <div class="tool-tip-content-body">
            A due date has been set for this component in the LMS.
          </div>
        </v-tooltip>
        <template v-else>
          {{ dueDate(row) }}
        </template>
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
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from "vue";
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
  canDeleteAssignment: {
    type: Boolean,
    default: false
  },
  exposureCount: {
    type: Number,
    default: 1
  }
});

const emit = defineEmits([
  "save-order",
  "move",
  "edit",
  "duplicate",
  "delete",
  "publish",
  "unpublish",
  "edit-treatment",
  "preview-treatment",
  "add-treatment"
]);

const tableRoot = ref(null);
const expandedRows = ref([]);
const actionsMenuOpen = ref({});

// there's no real Status/Actions column in the nested one-column treatments table for
// the add-treatment placeholder's pill/menu to sit in, and a fixed CSS split can't
// substitute - the outer table's actual column positions shift with viewport width and
// content in ways that don't scale linearly (confirmed empirically: a split tuned for
// one width was visibly wrong at another). Measuring the real positions and applying
// them directly is the only way this stays correct at every width.
const columnOffsets = ref({ status: null, actions: null });
let columnResizeObserver = null;

// .v-data-table-alt (a shared class this table uses, see _tables.scss) applies
// `transform: scale(0.99)` to fix an unrelated border-rendering issue. That transform
// makes .data-table-assignments the actual containing block for any position:absolute
// descendant (per spec, a transform on an ancestor does that, regardless of a *nearer*
// position:relative ancestor), and scales down whatever "left" value is applied inside
// it - confirmed empirically (a bare test div placed with left:500px rendered ~5px
// short of 500px on screen, matching a ~0.99 factor). Reading the real scale back out
// (rather than hardcoding 0.99) keeps this correct if that shared rule's value ever
// changes for unrelated reasons.
const getAncestorScaleX = element => {
  let el = element;

  while (el) {
    const transform = getComputedStyle(el).transform;
    const match = transform && transform !== "none" ? transform.match(/^matrix\(([^,]+),/) : null;

    if (match) {
      return parseFloat(match[1]) || 1;
    }

    el = el.parentElement;
  }

  return 1;
};

const measureColumnOffsets = () => {
  const table = tableRoot.value?.querySelector(".data-table-assignments");
  const placeholderRow = tableRoot.value?.querySelector(".treatment-add-row");

  if (!table || !placeholderRow) {
    return;
  }

  // on mobile, the outer table collapses into stacked label:value cards (see
  // isMobile/assignmentHeaders below) - there's no side-by-side Status/Actions column
  // for these measurements to mean anything relative to, and forcing this row's pill/
  // menu into position:absolute anyway (with a stale or nonsensical "left") pulled them
  // out of the row's normal flow and piled them on top of each other. Falling back to
  // null (columnOffsetStyle below then renders them in normal static flow) instead of
  // computing a desktop-shaped offset that doesn't apply here.
  if (isMobile.value) {
    columnOffsets.value = { status: null, actions: null };
    tableRoot.value.style.removeProperty("--treatment-indent");
    return;
  }

  // measuring the header cells' own edges doesn't work for Actions: that column is
  // center-aligned (see assignmentHeaders' align: "center"), so the real "..." button
  // sits somewhere in the middle of the column, not at its left edge, and how far in
  // depends on the column's width - which is exactly what changes with viewport width.
  // Measuring an actual rendered row's real content instead sidesteps that entirely.
  const statusPill = table.querySelector("tbody .status-pill:not(.treatment-add-status-pill)");
  const actionsBtn = table.querySelector("tbody .component-actions-btn");

  if (!statusPill || !actionsBtn) {
    return;
  }

  // measuring in real rendered (viewport) coordinates and taking the difference works
  // regardless of how many levels of nested padding/margin sit between the placeholder
  // row and the outer table - no need to separately account for any of it. The scale
  // division compensates for .v-data-table-alt's transform (see above) - it's applied
  // once here rather than at every call site that reads columnOffsets.
  const rowLeft = placeholderRow.getBoundingClientRect().left;
  const scaleX = getAncestorScaleX(placeholderRow);

  columnOffsets.value = {
    status: (statusPill.getBoundingClientRect().left - rowLeft) / scaleX,
    actions: (actionsBtn.getBoundingClientRect().left - rowLeft) / scaleX
  };

  // lines every nested treatment/placeholder row's icon-circle up under the real
  // row-title icon-circle above it. A fixed margin can't do this: the row-title
  // icon's distance from the left edge depends on the outer table's own drag/expand
  // column widths, which (confirmed against a real running page, not just this
  // component in isolation) don't match a simple fixed offset - they shift with
  // real column content in a way a plain "ml-8" utility class can't track. Same
  // scale-corrected screen-space diffing as status/actions above, applied to the
  // .treatment-info-group's CURRENT margin (rather than to a from-scratch left
  // value) so it works the same way regardless of which row's group happens to be
  // measured first.
  const outerIcon = table.querySelector("tbody > tr.assignment-row .icon-circle");
  const treatmentGroup = tableRoot.value.querySelector(".treatment-info-group");

  if (outerIcon && treatmentGroup) {
    const currentMarginLeft = parseFloat(getComputedStyle(treatmentGroup).marginLeft) || 0;
    const groupLeft = treatmentGroup.getBoundingClientRect().left;
    const targetLeft = outerIcon.getBoundingClientRect().left;
    const neededMarginLeft = currentMarginLeft + (targetLeft - groupLeft) / scaleX;

    tableRoot.value.style.setProperty("--treatment-indent", `${neededMarginLeft}px`);
  }
};

// the base CSS for these elements is position: absolute (see .treatment-add-status-pill
// /.treatment-add-actions-btn below) so a measured "left" places them precisely - but
// with no real offset to place them at (mobile, or before the first measurement),
// position: absolute is actively harmful: it pulls the element out of the row's normal
// flow with no "left" to replace that with, collapsing the space it would have taken
// and piling every such element in the row on top of each other. Falling back to
// position: relative (an inline style, so it wins over the class's non-!important
// position: absolute without a specificity fight) keeps it in normal flow instead -
// the actions button is a plain trailing sibling of .treatment-add-main (see the
// template), vertically centered against its whole height by .treatment-add-row's own
// align-items: center, so it needs no extra positioning of its own here. Not plain
// "static": this element has its OWN internal position: absolute children (a v-btn's
// overlay/underlay), which need THIS element to still be a positioned ancestor to
// stay contained within it - "static" isn't a positioned value at all, so those
// children would escape to size themselves against the next positioned ancestor up
// (.treatment-add-row) instead (confirmed via the equivalent, more visible bug this
// caused on the status pill below). "relative" behaves like "static" for this
// element's OWN layout position (no top/left offset given), while still containing
// its children correctly. transform also needs clearing: the class's
// translateY(-50%) is only meaningful paired with position: absolute + top: 50% (the
// desktop vertical-centering trick) - transform isn't gated by position, so left
// alone it kept nudging this element up by half its own height even though top: 50%
// (which it was supposed to offset) has no effect once position is no longer absolute.
const columnOffsetStyle = offset => {
  return offset == null ? { position: "relative", transform: "none" } : { left: `${offset}px` };
};

// same fallback as columnOffsetStyle, but for the status pill specifically: it lives
// INSIDE .treatment-add-main (stacked under the icon/condition name/link, see the
// template), so its fallback also needs the same left indent that content uses
// (--treatment-indent, see measureColumnOffsets' comment) to line up under it, plus
// some breathing room from the line above. position: relative, not "static" - see
// columnOffsetStyle's comment above: this chip has its own internal
// position: absolute .v-chip__underlay (the tonal variant's background layer), which
// needs THIS element to stay a positioned ancestor or it escapes to fill whatever
// positioned ancestor is next up instead (confirmed - without this it rendered as a
// giant pink rectangle the full size of .treatment-add-row, not the compact pill).
const statusPillOffsetStyle = offset => {
  return offset == null
    ? {
      position: "relative",
      transform: "none",
      marginLeft: "var(--treatment-indent, 32px)",
      marginTop: "12px"
    }
    : { left: `${offset}px` };
};

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
    { title: "", sortable: false, key: "data-table-expand" },
    { title: "NAME", align: "start", sortable: false, key: "title" },
    { title: "TREATMENTS", sortable: false, key: "treatments" },
    { title: "DUE", sortable: false, key: "dueDate" },
    { title: "STATUS", sortable: false, key: "published" },
    { title: "", align: "center", sortable: false, key: "actions" }
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
  message: "mdi-message-text-outline",
  assignment: "mdi-wrench-outline",
  integration: "mdi-application-brackets-outline"
};

// there's no persisted signal for "this assignment was deliberately created as
// single-version" (backend AssignmentService.isSingleVersion() computes the exact same
// thing from treatments.size() <= 1, without ever storing intent) - reuses the same
// condition the "Only One Version" chip already uses (row.treatments.length === 1)
// above, so both stay consistent with each other and with the pre-remodel behavior,
// where the Treatments column always showed a self-referential N/N (never compared
// against total conditions), making a single-version row trivially "complete".
const isSingleVersionRow = row => row.treatments.length === 1;

// every condition should have a name - this is a fallback for the case where one
// somehow doesn't, not an expected/normal state
// props.conditions comes straight from experimentStore.conditions, whose items use
// "name" (see Conditions.vue's own orderedCondition.name, editing this exact same
// store data) - NOT "conditionName", which is a different field on the differently-
// shaped exposure.groupConditionList items TreatmentRow.vue's own condition chip
// reads from instead. Every condition should have a name; this is a fallback for the
// case where one somehow doesn't, not an expected/normal state.
//
// a single-version row's lone (real or placeholder) treatment applies to every
// condition alike, so naming the one condition it happens to be tied to is
// misleading - see the identical override in TreatmentRow.vue's own conditionName
const conditionDisplayName = (row, condition) => {
  if (isSingleVersionRow(row)) {
    return "TREATMENT";
  }

  return condition.name || "No condition name";
};

watch(
  () => props.rows,
  async rows => {
    expandedRows.value = rows.map(row => row.assignmentId);
    // row content (e.g. a longer assignment title) can shift the outer table's own
    // column widths, so re-measure whenever the rows themselves change, not just on
    // resize
    await nextTick();
    measureColumnOffsets();
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

// SortableJS only binds pointer/touch events to .dragger, so arrow-key reordering
// needs its own path - dispatching the same "sorted" CustomEvent shape SortableJS's
// onUpdate below already produces (oldDraggableIndex/newDraggableIndex) means
// ExperimentAssignments.vue's saveOrder handler doesn't need to know which input
// method triggered the move. focusAssignmentId is the one addition: saveOrder uses
// it to refocus this same row's handle after componentTableKey's forced remount
// (see that file's comment), which a mouse drag doesn't need since the mouse was
// never keyboard-focused to begin with.
const handleDragKeydown = (event, row, index) => {
  if (event.key !== "ArrowUp" && event.key !== "ArrowDown") {
    return;
  }

  event.preventDefault();

  const newIndex = event.key === "ArrowUp" ? index - 1 : index + 1;

  if (newIndex < 0 || newIndex >= props.rows.length) {
    return;
  }

  tableRoot.value.dispatchEvent(
    new CustomEvent("sorted", {
      detail: {
        oldDraggableIndex: index,
        newDraggableIndex: newIndex,
        focusAssignmentId: row.assignmentId
      },
      bubbles: true
    })
  );
};

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

const rowIconCircleClass = row => {
  if (row.type === rowType.assignment) {
    return "icon-circle-document";
  }

  if (row.type === rowType.message) {
    return "icon-circle-message";
  }

  return "";
};

// the icon/circle an add-treatment placeholder shows. Reflects the existing
// treatment's own type when one exists (just incomplete), falling back to this
// row's default type (matching what a newly-created treatment would get) when the
// condition has no treatment at all - see TreatmentRow.vue's own wrench/code/message
// icon convention, which this mirrors.
const placeholderIcon = (row, treatment) => {
  if (row.type === rowType.assignment) {
    return treatment?.assessmentDto?.integration ? treatmentIcon.integration : treatmentIcon.assignment;
  }

  if (row.type === rowType.message) {
    return treatmentIcon.message;
  }

  return "";
};

const placeholderIconCircleClass = (row, treatment) => {
  if (row.type === rowType.assignment) {
    return treatment?.assessmentDto?.integration ? "icon-circle-code" : "icon-circle-control";
  }

  if (row.type === rowType.message) {
    return "icon-circle-message";
  }

  return "";
};

// the add-treatment placeholder's own edit action: if a treatment already exists
// (just incomplete) this is really an edit, not a create - creating one would leave
// a duplicate behind
const handlePlaceholderEdit = (row, item) => {
  if (item.treatment) {
    emit("edit-treatment", { row, treatment: item.treatment });
    return;
  }

  emit("add-treatment", { row, condition: item.condition });
};

// a treatment that exists but lacks content yet (no questions, invalid integration
// URL, or - for messages - a status that isn't ready/disabled/sent) - the per-treatment
// half of hasIncompleteTreatments below, factored out since the add-treatment
// placeholder needs to ask this about one specific treatment, not a whole row
const isTreatmentIncomplete = (row, treatment) => {
  if (row.type === rowType.assignment) {
    if (treatment.assessmentDto.integration && !treatment.assessmentDto.integrationUrlValid) {
      return true;
    }

    return !(treatment.assessmentDto && treatment.assessmentDto.questions && treatment.assessmentDto.questions.length);
  }

  if (row.type === rowType.message) {
    return ![messageStatus.ready, messageStatus.disabled, messageStatus.sent].includes(treatment.configuration.status);
  }

  return false;
};

// the Treatments column's denominator, and the "TREATMENTS - X of Y built" label's Y -
// a single-version row is always shown against its own treatment count (so "1 of 1"),
// not the experiment's total conditions
const treatmentsTotalForRow = row => {
  return isSingleVersionRow(row) ? row.treatments.length : props.conditions.length;
};

// the Treatments column's numerator, and the "TREATMENTS - X of Y built" label's X -
// a treatment record existing isn't the same as it being done: one that's incomplete
// renders as an add-treatment placeholder rather than counting toward "built"
const completeTreatmentsCountForRow = row => {
  return row.treatments.filter(treatment => !isTreatmentIncomplete(row, treatment)).length;
};

// one entry per relevant condition: the real treatment when it exists and has
// content, or an add-treatment placeholder when the condition has no treatment at
// all OR its treatment exists but is incomplete - a single-version row only ever
// considers the one condition it already has a treatment for (see isSingleVersionRow
// above), never the experiment's other conditions, which it was never meant to cover
const treatmentTableItems = row => {
  const relevantConditions = isSingleVersionRow(row)
    ? props.conditions.filter(condition =>
      row.treatments.some(treatment => treatment.conditionId === condition.conditionId)
    )
    : props.conditions;

  return relevantConditions.map(condition => {
    const treatment = row.treatments.find(item => item.conditionId === condition.conditionId);

    if (!treatment || isTreatmentIncomplete(row, treatment)) {
      return {
        isPlaceholder: true,
        condition,
        treatment: treatment || null,
        treatmentId: treatment ? treatment.treatmentId : `missing-treatment-${row.assignmentId}-${condition.conditionId}`
      };
    }

    return treatment;
  });
};

const dueDate = row => {
  return row.dueDate
    ? dayjs(row.dueDate).format("MMM D, YYYY hh:mma")
    : "";
};

const hasIncompleteTreatments = row => {
  if (!isSingleVersionRow(row) && row.treatments.length < props.conditions.length) {
    return true;
  }

  return row.treatments.some(treatment => isTreatmentIncomplete(row, treatment));
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

const statusPillColor = row => {
  if (row.type === rowType.assignment) {
    return row.published ? "success" : "warning";
  }

  if (row.type === rowType.message) {
    if (row.error) {
      return "error";
    }

    if (row.sent) {
      return "info";
    }

    return row.published ? "success" : "warning";
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

// Error (message-only) has no copy in the design, so that pill falls back to no
// tooltip rather than guessing unreviewed wording for it.
const statusTooltipText = row => {
  if (row.published) {
    return "This component has been published in the LMS.";
  }

  if (row.sent) {
    return "This message has been sent.";
  }

  if (!row.error) {
    if (row.type === rowType.message) {
      return "This message has not yet been published, and will not send until it is. Click the 3 dots to the right to publish.";
    }

    return "This component has not yet been published in the LMS, and cannot be accessed by students.";
  }

  return "";
};

onMounted(() => {
  initSortable();

  columnResizeObserver = new ResizeObserver(measureColumnOffsets);

  if (tableRoot.value) {
    columnResizeObserver.observe(tableRoot.value);
  }
});

onBeforeUnmount(() => {
  columnResizeObserver?.disconnect();
});
</script>

<style lang="scss" scoped>
.row-title {
  font-weight: 600;
}

.icon-circle {
  width: 28px;
  height: 28px;
  min-width: 28px;
  border-radius: 50%;
  // text-align/align-content on a plain inline-block box don't reliably center an
  // icon glyph both ways (align-content in particular has no effect here at all -
  // it only applies to multi-line flex/grid containers) - the icon rendered visibly
  // off-center, down and to the right of the circle's true middle. inline-flex +
  // align-items/justify-content centers it deterministically on both axes,
  // regardless of the icon glyph's own font metrics.
  display: inline-flex;
  align-items: center;
  justify-content: center;
  // inline-flex is still an inline-level box, so vertical-align still controls how
  // the circle ITSELF sits among row siblings (unaffected by the flex properties
  // above, which only govern the icon's position INSIDE the circle) - see below for
  // why middle specifically. inline-block defaults to vertical-align: baseline,
  // which lines its BOTTOM edge up with the surrounding text's baseline rather than
  // centering it - since the row's line-box height varies (an expand caret, an
  // "Only One Version" chip, an expanded vs. collapsed row all change it), that
  // produced a different, inconsistent-looking top/bottom gap around the circle
  // from row to row. vertical-align: middle centers it against the line instead, so
  // the gap stays visually even regardless of what else is in the row.
  vertical-align: middle;
  margin-right: 8px;

  > .v-icon {
    font-size: 16px;
  }

  // solid fill with a white icon (not the pastel-fill/colored-icon treatment used
  // below for treatment-level icon-circles) - these are the top-level assignment/
  // message row icons, and the mockup renders them noticeably more solid; colors
  // pixel-sampled directly from the mockup rather than reusing $blue/$orange (which
  // read too saturated next to the mockup's muted swatch)
  &.icon-circle-document {
    background-color: #6789ab;
    > .v-icon { color: white !important; }
  }

  // darkened from the mockup's pixel-sampled #df9d7a - a white icon on that
  // background was only 2.27:1 (WCAG 1.4.11 requires 3:1 for graphical objects
  // like an icon glyph); #d37747 clears it at 3.23:1 while staying in the same
  // terracotta family.
  &.icon-circle-message {
    background-color: #d37747;
    > .v-icon { color: white !important; }
  }

  // icon color pixel-sampled off the mockup directly rather than reusing $yellow's
  // shared "base" (#ffb300) - that reads much more saturated/vivid next to the
  // mockup's muted olive-gold, the same kind of mismatch already fixed once for the
  // status pills (see the AskUserQuestion decision on that: scope color fixes
  // locally, don't retune shared theme tokens other unrelated UI still relies on).
  // border-color matches background-color exactly (same value, not just a similar
  // shade) so the border doesn't read as a separate, darker ring around the pastel
  // fill - the mockup's circles have no visible edge at all. Icon color darkened
  // from the original #b29a57 sample (2.43:1 against this pastel fill, short of
  // WCAG 1.4.11's 3:1) to #9a8446 (3.23:1).
  &.icon-circle-control {
    border: 1px solid rgba(255, 179, 0, 0.2);
    background-color: rgba(255, 179, 0, 0.2);
    color: #9a8446;
    > .v-icon { color: #9a8446 !important; }
  }

  // used by placeholderIconCircleClass for an incomplete integration treatment -
  // matches TreatmentRow.vue's own icon-circle-code variant (Vue's scoped styles
  // don't share across components, so this needs its own copy here too). Same
  // pixel-sampled-vs-shared-token and border-matches-background reasoning as
  // icon-circle-control above - $light-blue base (#03a9f4) is far more saturated
  // than the mockup's muted dusty blue. Icon color darkened from the original
  // #65a5d3 sample (2.19:1 against this pastel fill) to #3786bf (3.24:1).
  &.icon-circle-code {
    border: 1px solid rgba(3, 169, 244, 0.2);
    background-color: rgba(3, 169, 244, 0.2);
    color: #3786bf;
    > .v-icon { color: #3786bf !important; }
  }
}

.treatment-add-condition-name {
  font-weight: 600;
}

// see measureColumnOffsets()'s comment (in the script section above) and
// TreatmentRow.vue's matching rule - this shared custom property lines this
// placeholder's icon-circle up under the real row-title icon-circle, measured live
// since a fixed margin can't track the outer table's actual column widths.
.treatment-info-group {
  margin-left: var(--treatment-indent, 32px);
}

// small relative to the "X of Y" text next to it (confirmed against a direct
// reference screenshot), colored to match the "Needs attention" pill's own error icon
// rather than the muted red _global.scss applies to .label-treatment-incomplete text
// generally. Two things this rule has to beat, both requiring more than the obvious
// selector:
// 1. This dot's <v-icon> is rendered deep inside ToolTip.vue's own activator slot (not
//    written in this file's own template), so it carries ToolTip.vue's scope
//    attribute, not this file's - a plain scoped selector's last segment silently never
//    matches it at all (confirmed via devtools: the compiled rule required this file's
//    own data-v attribute on .treatment-ratio-dot itself, which the real element
//    doesn't have). :deep() is required to drop that requirement, matching this file's
//    existing :deep() use for the same "child-component-internals" reason elsewhere.
// 2. Even once it matches, _global.scss's .label-treatment-incomplete color rule
//    (.v-data-table-alt.data-table-assignments .label-treatment-incomplete) sits at
//    the exact same specificity (two ancestor classes + one class) - a tie decided by
//    source order, which this dot lost in practice. Chaining .label-treatment-incomplete
//    onto this selector too (the element carries both classes at once) adds a 4th
//    class, so this rule wins outright instead of depending on load order.
.v-data-table-alt.data-table-assignments :deep(.treatment-ratio-dot.label-treatment-incomplete) {
  font-size: 6px !important;
  color: rgb(var(--v-theme-error)) !important;
}

.treatments-section-label {
  // symmetric top/bottom padding - this used to be 10px/4px, which visibly pushed
  // the text off-center within its own row instead of centering it between the
  // divider above and the treatments table below.
  padding: 8px 16px;
  font-size: 15px;
  font-weight: 400;
}

// Home.vue has an unscoped, app-wide `.v-data-table *:not(.v-icon) { color: black
// !important; }` rule (see _global.scss's comment on the equivalent
// .label-treatment-incomplete override for the full explanation). It happens to
// already produce close to the right color here (pure black vs. the mockup's
// pixel-sampled rgba(0,0,0,0.87)) - set explicitly anyway, matching this app's own
// existing high-emphasis-text convention (see .v-card-text in _global.scss), rather
// than relying on an unrelated global rule by coincidence. A single class here (even
// with Vue's scoped-style data-v attribute added) only ties that rule's specificity
// and loses on source order - qualifying with the ancestor .treatments-table-container
// class too is what reliably beats it.
.treatments-table-container .treatments-section-label {
  color: rgba(0, 0, 0, 0.87) !important;
}

// ExperimentAssignments.vue has its own unscoped rule (search that file for
// "tr.v-data-table__tr--expanded") painting the WHOLE treatments-table-container <td>
// grey, to match the nested treatments table's own background - this label sits in
// that same <td> as a sibling of the nested table, so it inherits that grey too. The
// mockup wants it on plain white instead, with its own divider separating it from the
// (still grey) treatments below. That competing rule's selector is unusually long
// (4 class-level segments), so beating it needs real qualification, not just one
// ancestor class - .v-data-table-alt and .data-table-assignments are this table's own
// two identifying classes (see the root <v-data-table> class list above).
.v-data-table-alt.data-table-assignments .treatments-table-container .treatments-section-label {
  background-color: white !important;
  border-bottom: 1px solid rgba(0, 0, 0, 0.2);
}

// deliberately its own class, not .status-pill, even though it wants the exact same
// shape/text-transform - measureColumnOffsets() (see the script section above) selects
// the real Status column's pill via `.status-pill:not(.treatment-add-status-pill)`,
// and this chip sits earlier in the row (inside the NAME column), so sharing that
// class made querySelector grab this one instead of the real status pill, throwing
// the "Needs attention" pill/actions-menu positioning off by hundreds of pixels.
.only-one-version-chip {
  text-transform: none;
}

.status-pill {
  text-transform: none;

  // same Home.vue override as .treatments-section-label above, but per Vuetify's
  // "text-<color>" utility class so each status keeps its own theme color instead of
  // every pill flattening to black. Home.vue's rule (`*:not(.v-icon)`) matches the
  // chip's own inner .v-chip__content span (the visible text) AND its .v-chip__underlay
  // span (the tonal variant's tinted background, painted via `background: currentColor`)
  // directly, not just this outer element - a color set only here would be inherited,
  // and a direct match always beats an inherited value regardless of specificity, so
  // both need their own explicit override too. Neither span is rendered by this file's
  // template (they're VChip's own internals), so Vue's scoped-style data-v attribute
  // never reaches them and a plain nested selector silently never matches - :deep() is
  // required to actually target them (matching this file's existing :deep(...) use for
  // the same child-component-internals reason elsewhere).
  // Published/Unpublished want a softer 3-tone look (pale fill + a slightly darker
  // tinted border + a darker text, all pixel-sampled off the mockup) rather than this
  // app's shared success/warning theme tokens' own flat-tonal look - see the
  // AskUserQuestion decision on this exact fork: scoped to this table only, not a
  // global theme retune, so nothing outside this component is affected. The fill goes
  // on the chip's own background (not the tonal variant's :v-chip__underlay, which
  // paints `background: currentColor` at a reduced opacity and would wash out an exact
  // hex) - :v-chip__underlay is position:absolute, which paints above this in-flow
  // background regardless of source order, so it has to be hidden outright (opacity:
  // 0) rather than left semi-transparent, or it would still tint the fill underneath.
  &.text-success {
    // darkened from the mockup's pixel-sampled #468650 (4.22:1 against the fill below,
    // just short of WCAG's 4.5:1 for normal text) to #3a7040 (5.65:1) - same green
    // family, still reads as the same "softer 3-tone look" described above.
    color: #3a7040 !important;
    background-color: #f3fdf5 !important;
    border: 1px solid #cbf5d7;

    :deep(.v-chip__content) {
      color: #3a7040 !important;
    }

    :deep(.v-chip__underlay) {
      opacity: 0 !important;
    }
  }

  &.text-warning {
    color: #b44b23 !important;
    background-color: #fef7ee !important;
    border: 1px solid #f8d9b0;

    :deep(.v-chip__content) {
      color: #b44b23 !important;
    }

    :deep(.v-chip__underlay) {
      opacity: 0 !important;
    }
  }

  &.text-error,
  &.text-error :deep(.v-chip__content),
  &.text-error :deep(.v-chip__underlay) {
    color: rgb(var(--v-theme-error)) !important;
  }

  &.text-info,
  &.text-info :deep(.v-chip__content),
  &.text-info :deep(.v-chip__underlay) {
    color: rgb(var(--v-theme-info)) !important;
  }
}

// right padding only, matching a real TreatmentRow's own right-side breathing room
// before its actions menu - a left padding here would indent this row's icon further
// than a real TreatmentRow's, breaking their alignment
// this nested table has only one column, so there's no real Status/Actions column for
// the pill/menu to sit in. A fixed CSS split (percentages, padding) can't track the
// outer table's actual column positions - those shift with viewport width and content
// in ways that don't scale linearly, so a value tuned for one width breaks at another
// (confirmed empirically). Instead, ComponentTable.vue measures the outer table's real
// Status/Actions column positions in JS (columnOffsets, re-measured on resize and data
// changes) and positions .treatment-add-status-pill/.treatment-add-actions-btn
// absolutely within this row, which needs the positioning context here.
.treatment-add-row {
  position: relative;
}

// wraps the icon/condition-name/link line and (on mobile, once it falls back to
// normal static flow - see statusPillOffsetStyle's comment) the status pill stacked
// beneath it, as one unit .treatment-add-row's align-items: center can center the
// actions button against - without this wrapper, the button centered against only
// the taller of two SIBLING flex lines it happened to land on when wrapping, which
// put it noticeably off from the row's actual vertical center.
.treatment-add-main {
  min-width: 0;
}

.treatment-add-status-pill,
.treatment-add-actions-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
}

// a <button> (not a bare <v-icon>/<span>) so the row-reorder handle is keyboard-
// focusable and operable (arrow-up/down, see handleDragKeydown) - was previously
// SortableJS-only, with no way to reorder rows without a mouse or touch input at
// all. Reset to plain button chrome so it still reads as just the drag icon.
.dragger {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  padding: 0;
  cursor: grab;
  color: inherit;
}

// just the "+" square - the "Click to build treatment" text is a separate link
// alongside it, not inside the dashed box
.treatment-add-box {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  background: white;
  border: 2px dashed map.get($grey, "lighter");
  border-radius: 8px;
  padding: 0;
  cursor: pointer;
  color: inherit;

  > .v-icon {
    font-size: 18px;
  }

  &:hover {
    border-color: map.get($grey, "darker");
  }
}

// a <button> (not an <a href="#">) since this only ever triggers a JS action and
// never navigates - a real link's semantics/keyboard behavior (activates on Enter,
// not Space, and screen readers announce a destination that doesn't exist) don't
// fit. Home.vue's global `.v-data-table *:not(.v-icon) { color: black !important; }`
// rule (see the .treatments-section-label comment above for the full explanation)
// ties with a single class here (even with Vue's scoped-style data-v attribute) and
// loses on source order - same fix as .treatments-section-label, qualify with the
// ancestor .treatments-table-container class too. The rest of the rule strips the
// browser's default <button> chrome (background/border/font) so it reads visually
// identical to the plain text link it replaced.
.treatments-table-container .treatment-add-link {
  background: none;
  border: none;
  padding: 0;
  font: inherit;
  cursor: pointer;
  color: rgba(0, 0, 0, 0.87) !important;
  text-decoration: none;

  &:hover {
    color: map.get($blue, "primary") !important;
  }
}

.treatment-row {
  // _tables.scss's shared `.v-data-table-alt ... td { .v-data-table { margin-top:
  // -6px; } }` rule (a generic provision for OTHER .v-data-table-alt tables
  // elsewhere with their own nested tables, not written with this one in mind)
  // matches this nested table too, since it's a .v-data-table inside a <td> of
  // this table's own .v-data-table-alt root. Used to be harmless here: the
  // treatments-table-container <td>'s old (buggy) 8px top padding comfortably
  // absorbed the -6px pull-up. Once that padding was correctly zeroed out (see
  // .assignment-row--last's comment elsewhere in this file), nothing was left to
  // absorb it, and this table crept 6px up into the "TREATMENTS - N of N built"
  // label above it - covering that label's own border-bottom entirely, which is
  // what actually made the divider disappear.
  margin-top: 0 !important;

  :deep(.v-table__wrapper) {
    border: none !important;
    border-radius: 0 !important;
    background-color: map.get($grey, "lightest") !important;
    > table {
      padding-top: 0px !important;
    }
  }
}

// mobile only: round this wrapper's own bottom corners, rather than the
// outer <td>'s (see the mobile-only rule on .expanded-row--mobile > td
// below). Confirmed empirically (a standalone, Vuetify-free reproduction)
// that a <td> doesn't reliably render a curved BORDER in this browser even
// when border-radius/overflow:hidden compute correctly - background-color
// clips to the curve fine, but the border edge itself renders as a sharp
// rectangle regardless. A plain div, like this v-table__wrapper, doesn't
// have that limitation. !important: needed to beat the blanket
// .treatment-row rule above (border-radius: 0 !important), which still
// applies here too since this element keeps the plain .treatment-row class
// alongside .treatment-row--mobile.
.treatment-row--mobile {
  :deep(.v-table__wrapper) {
    border-bottom-left-radius: 10px !important;
    border-bottom-right-radius: 10px !important;
    overflow: hidden;
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

// _tables.scss's shared `.v-data-table-alt` rule independently draws its OWN left/right
// borders and rounded corners on every `.v-data-table-alt` table (this one included, since
// it carries that class alongside data-table-assignments) - that's the rounded-card look
// other .v-data-table-alt tables want, but this table's own rules above/below already
// establish a different, plain full-bleed desktop list per the mockup, so the shared
// version needs disabling here specifically, not just left to coexist. Not scoped to
// desktop only: this table's own dedicated mobile rules (the .v-data-table__tr--mobile
// selectors above/below, plus .treatment-row--mobile's nested-wrapper radius) already
// fully cover mobile's card look independently, so disabling the shared version doesn't
// lose anything there - it's redundant with those, not required by them.
:deep(.data-table-assignments > .v-table__wrapper > table > tbody > tr) {
  &:not(.v-data-table__tr--mobile) {
    > td:first-child,
    > td:last-child {
      border-left: none !important;
      border-right: none !important;
    }

    &:first-child > td:first-child,
    &:first-child > td:last-child,
    &:last-child > td:first-child,
    &:last-child > td:last-child {
      border-radius: 0 !important;
    }
  }
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

  // real per-cell borders (table-row-group boxes don't consistently honor
  // border-radius/outline across browsers). Desktop is a plain full-bleed list per
  // the mockup - no left/right border, no rounded corners - only horizontal dividers
  // (see the bold group-boundary rule further down). Mobile still uses a genuine
  // rounded-card look, since each component's stacked fields need their own visual
  // boundary there; that's unrelated to and unaffected by the desktop styling here.
  > tbody {
    > tr {
      &:hover {
        background: unset !important;
      }

      // in mobile view every field is its own full-width stacked block, not
      // a column sharing the row's left/right edge with its siblings, so (unlike
      // desktop) every mobile td needs its own explicit left/right border for the
      // card's sides to read as one continuous line down the stack.
      &.v-data-table__tr--mobile > td {
        border-left: 1px solid rgba(0, 0, 0, 0.2);
        border-right: 1px solid rgba(0, 0, 0, 0.2);
      }

      // desktop: bold top edge under the header, matching the same weight as the
      // group-to-group divider further down (mockup pixel-sampled, see that rule's
      // comment) - no corner radius, this is a plain full-bleed list, not a card.
      // Mobile's own rounded-card top corner comes from the existing
      // .v-data-table__tr--mobile.assignment-row rule further below, which already
      // covers every mobile component row including the first.
      // padding-bottom mirrors padding-top here (not just breathing room against the
      // border above) - td's vertical-align: middle centers content within the cell's
      // own padding box, so a one-sided padding-top with no matching padding-bottom
      // grows the row asymmetrically and visibly shifts the icon-circle/title down
      // off-center from the row's actual middle. Matching it on both sides keeps the
      // extra breathing room without throwing off centering.
      &:first-child:not(.v-data-table__tr--mobile) > td {
        padding-top: 8px !important;
        padding-bottom: 8px !important;
        border-top: 2px solid rgba(0, 0, 0, 0.4);
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
      // this unlayered rule is needed just to win it back. Desktop only - in
      // mobile view this would draw a stray 1px line floating in the middle
      // of the white gap between cards; the gap and rounded corners already
      // tell them apart there.
      //
      // 2px at 0.4 opacity (vs. the nested treatments table's own default ~1px/0.12-opacity
      // row borders) so the group-to-group boundary reads as solidly grey and clearly more
      // pronounced than the divider between treatment sub-rows within one group - pixel-
      // sampled directly off the mockup (a solid rgb(155,155,155) on white, matching ~0.4
      // black opacity), since 0.2 alone rendered too faint to read as a real divider.
      &.v-data-table__tr--expanded:not(.expanded-row--mobile) > td {
        border-bottom: 2px solid rgba(0, 0, 0, 0.4);
      }

      // same divider, but for a COLLAPSED component: the rule above only ever
      // fires on a .v-data-table__tr--expanded <tr>, which doesn't exist at all
      // for a collapsed row (Vuetify simply never renders #expanded-row's content
      // for it) - so with every component collapsed, this row's own <td> fell back
      // to Vuetify's default thin ~0.12-opacity border, and the bold group-to-group
      // divider disappeared entirely. .assignment-row--collapsed is set by
      // row-props above from expandedRows (the actual source of truth for expand
      // state), not a structural/class selector, since "is this row currently
      // collapsed" isn't something a plain CSS selector can express here.
      &.assignment-row--collapsed:not(.v-data-table__tr--mobile) > td {
        border-bottom: 2px solid rgba(0, 0, 0, 0.4);
      }

      // desktop: bold bottom edge closing out the list, same weight as the top edge
      // and the group dividers - no corner radius, matching the plain full-bleed
      // list the mockup wants. Mobile's own card spacing/rounding (padding + the
      // nested wrapper's own border-radius, see .treatment-row--mobile above and
      // .expanded-row--mobile below) is a different mechanism entirely and doesn't
      // need this border at all. Only matters when the last component is collapsed
      // (tbody's actual :last-child <tr> is then really this row) - when it's
      // expanded, its own expanded-row sibling becomes tbody's real last child and
      // already gets an identical border-bottom for free from the group-divider rule
      // above, so this ends up redundant-but-harmless rather than wrong in that case.
      &:last-child:not(.v-data-table__tr--mobile) > td {
        border-bottom: 2px solid rgba(0, 0, 0, 0.4);
      }

      // vertical-centering padding for the LAST assignment row's own content -
      // same reasoning as the :first-child rule above, but deliberately keyed off
      // the .assignment-row--last class (set by row-props from the last index in
      // `rows`, above) instead of :last-child. expandedRows starts with every row
      // expanded (see that ref's initial value), which makes tbody's actual
      // structural :last-child the LAST row's own expanded-row sibling, not the
      // summary row itself - a :last-child selector here landed this padding on
      // .treatments-table-container instead, which visibly pushed "TREATMENTS - N
      // of N built" down with an 8px gap above it that had nothing to do with
      // centering anything.
      &.assignment-row--last:not(.v-data-table__tr--mobile) > td {
        padding-top: 8px !important;
        padding-bottom: 8px !important;
      }

      // mobile only: space every component's card apart (not just relying
      // on the thin 1px divider above) so each one reads as a complete,
      // distinct card - the actual rounded-corner look now comes from the
      // nested v-table__wrapper itself (see .treatment-row--mobile above),
      // since a <td> doesn't reliably render a curved border in this
      // browser. padding (not a thick border) creates the gap here - the
      // outer <td>'s own background needs to be plain white (not the grey
      // ExperimentAssignments.vue forces via !important) for that padding
      // area to actually read as empty space rather than more grey. Written
      // after the :last-child rule above so it also wins (equal
      // specificity, so source order decides) for the table's actual last
      // component too.
      &.expanded-row--mobile > td {
        padding-bottom: 32px;
        background-color: white !important;
      }
    }
  }
}
</style>