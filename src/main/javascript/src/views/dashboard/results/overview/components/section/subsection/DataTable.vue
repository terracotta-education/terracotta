<template>
  <v-container
    class="container-data-table"
  >
    <v-data-table
      :headers="tableHeaders"
      :items="tableData"
      :items-per-page="-1"
      :show-expand="displayExpand"
      :class="['data-table', isMobile ? 'data-table--mobile' : 'v-data-table-alt']"
      item-value="title"
    >
      <template
        v-if="submissionRateTooltip"
        v-slot:header.submissionRate="{ column }"
      >
        <span class="header-with-tooltip">
          {{ column.title }}
          <tool-tip
            :header="submissionRateTooltip.header"
            :content="submissionRateTooltip.message"
            :icon="submissionRateTooltip.icon"
            size="compact"
            icon-color="primary"
            alignment="top"
            aria-label="Submission rate tooltip"
          />
        </span>
      </template>
      <template
        v-slot:item="{ item, isExpanded, toggleExpand, columns }"
      >
        <tr v-if="!isMobile">
          <td
            class="text-start"
          >
            {{ title(item) }}
            <v-chip
              v-if="item.treatments && item.treatments.rows && item.treatments.rows.length == 1"
              color="lightgrey"
              class="label-one-version"
              density="compact"
            >
              Only One Version
            </v-chip>
          </td>
          <td
            v-if="hasSubmissions(item)"
            class="text-center"
          >
            {{ item.submissionCount }}
          </td>
          <td
            v-if="hasSubmissions(item)"
            class="text-center"
          >
            {{ rate(item) }}
          </td>
          <td
            v-if="hasSubmissions(item)"
            class="text-center"
          >
            <span
              v-if="item.averageGrade >= 0.0"
            >
              {{ grade(item) }}%
            </span>
            <span
              v-else
            >
              &#8212;
              <tool-tip
                content="This component includes items that must be graded manually. Data will appear when those items have been graded."
                icon="mdi-information-outline"
                icon-color="primary"
                aria-label="Average grade explanation tooltip"
              />
            </span>
          </td>
          <td
            v-if="hasSubmissions(item)"
            class="text-center"
          >
            {{ sd(item) }}%
          </td>
          <td
            v-if="hasSubmissions(item)"
            class="text-start"
          >
            <v-btn
              v-if="hasSubmissions(item) && hasTreatments(item) && !isSingleTreatment(item)"
              @click="toggleExpand(item)"
              :class="{'v-data-table__expand-icon--active': isExpanded(item)}"
              class="v-data-table__expand-icon"
              variant="text"
              icon="mdi-chevron-down"
            />
          </td>
          <td
            v-if="!hasSubmissions(item)"
            :colspan="columns.length"
          >
            <span
              class="no-submissions-text"
            >
              {{ noSubmissionsText }}
            </span>
          </td>
        </tr>

        <tr
          v-else
          class="mobile-row"
        >
          <td
            :colspan="columns.length"
            class="mobile-card-container"
          >
            <div class="mobile-card">
              <div class="mobile-field">
                <span class="mobile-label">{{ titleHeader }}</span>
                <span class="mobile-value">
                  {{ title(item) }}
                  <v-chip
                    v-if="item.treatments && item.treatments.rows && item.treatments.rows.length == 1"
                    color="lightgrey"
                    class="label-one-version"
                    density="compact"
                  >
                    Only One Version
                  </v-chip>
                </span>
              </div>

              <template v-if="hasSubmissions(item)">
                <div class="mobile-field">
                  <span class="mobile-label">Number of submissions</span>
                  <span class="mobile-value">{{ item.submissionCount }}</span>
                </div>
                <div class="mobile-field">
                  <span class="mobile-label">
                    Submissions per participant
                    <tool-tip
                      v-if="submissionRateTooltip"
                      :header="submissionRateTooltip.header"
                      :content="submissionRateTooltip.message"
                      :icon="submissionRateTooltip.icon"
                      size="compact"
                      icon-color="primary"
                      alignment="top"
                      aria-label="Submission rate tooltip"
                    />
                  </span>
                  <span class="mobile-value">{{ rate(item) }}</span>
                </div>
                <div class="mobile-field">
                  <span class="mobile-label">Average grade</span>
                  <span class="mobile-value">
                    <span v-if="item.averageGrade >= 0.0">
                      {{ grade(item) }}%
                    </span>
                    <span v-else>
                      &#8212;
                      <tool-tip
                        content="This component includes items that must be graded manually. Data will appear when those items have been graded."
                        icon="mdi-information-outline"
                        icon-color="primary"
                        aria-label="Average grade explanation tooltip"
                      />
                    </span>
                  </span>
                </div>
                <div class="mobile-field">
                  <span class="mobile-label">Standard deviation</span>
                  <span class="mobile-value">{{ sd(item) }}%</span>
                </div>
                <div
                  v-if="hasTreatments(item) && !isSingleTreatment(item)"
                  class="mobile-field"
                >
                  <span class="mobile-label">Treatments</span>
                  <v-btn
                    @click="toggleExpand(item)"
                    :class="{'v-data-table__expand-icon--active': isExpanded(item)}"
                    class="v-data-table__expand-icon"
                    variant="text"
                    icon="mdi-chevron-down"
                  />
                </div>
              </template>

              <div
                v-else
                class="mobile-field mobile-no-submissions"
              >
                <span class="no-submissions-text">{{ noSubmissionsText }}</span>
              </div>
            </div>
          </td>
        </tr>

        <!--
          Vuetify 3's #expanded-row slot is only invoked by its own built-in
          per-row rendering - a monolithic #item override like this one (as
          opposed to per-column #item.<key> slots, e.g. ComponentTable.vue)
          replaces that rendering entirely, so #expanded-row is silently never
          called at all. Rendering the expanded content as an extra sibling
          <tr> here, gated on the same isExpanded(item) the toggle button
          uses, is what #expanded-row was supposed to do.
        -->
        <tr v-if="isExpanded(item) && hasTreatments(item)">
          <data-table-treatment
            :headers="columns"
            :item="item"
          />
        </tr>
      </template>
      <template #bottom></template>
    </v-data-table>
    <v-row
      v-if="displayNote"
      class="note-included-data mt-2 pl-4"
    >
      * Only includes data from consenting students
    </v-row>
  </v-container>
</template>

<script setup>
import { computed } from "vue";
import { useDisplay } from "vuetify";

import DataTableTreatment from "./DataTableTreatment.vue";
import ToolTip from "@/components/ToolTip.vue";
import { round, percent } from "@/helpers/dashboard/utils.js";

defineOptions({
  name: "DataTable"
});

const props = defineProps({
  tableData: {
    type: Array,
    default: () => []
  },
  titleHeader: {
    type: String,
    default: "Title"
  },
  includeNote: {
    type: Boolean,
    default: false
  },
  showExpand: {
    type: Boolean,
    default: false
  },
  hasOverall: {
    type: Boolean,
    default: false
  },
  noSubmissionsMessage: {
    type: String,
    default: "N/A"
  },
  tooltips: {
    type: Array,
    default: () => []
  }
});

const mobileBreakpoint = 636;
const { width } = useDisplay();
// this table's #item slot fully replaces Vuetify's own row rendering (see the
// mobile-row branch below), so its automatic mobile-card transform never
// applies here regardless of :mobile-breakpoint - this mirrors that same
// breakpoint by hand instead.
const isMobile = computed(() => width.value < mobileBreakpoint);

const dataColumnWidth = "15%";
const expandColumnWidth = "5%";
const displayNote = computed(() => props.includeNote || false);
const displayExpand = computed(() => props.showExpand || false);
// the 4 data columns are always 15% each so they match width-wise across tables that do and
// don't show the expand column; the title column absorbs whatever's left so everything still
// sums to 100% (35% + expand's 5% when shown, or 40% when there's no expand column at all)
const titleColumnWidth = computed(() => displayExpand.value ? "35%" : "40%");
const tableHeaders = computed(() => {
  // mobile's #item branch lays every field out by hand in one card instead
  // of per-column cells, so the desktop widths below have nothing to size -
  // worse, Vuetify still builds the underlying <table>'s column layout from
  // them regardless of which row template is active, forcing the table wide
  // enough for all 6 desktop columns and leaving mobile scrolling sideways
  // inside its own wrapper instead of actually collapsing. Same header
  // count/keys (colspan below still needs columns.length), just no width.
  if (isMobile.value) {
    return [
      { title: props.titleHeader, key: "title", align: "start", sortable: false },
      { title: "Number of submissions", key: "submissionCount", align: "center", sortable: false },
      { title: "Submissions per participant", key: "submissionRate", align: "center", sortable: false },
      { title: "Average grade", key: "averageGrade", align: "center", sortable: false },
      { title: "Standard deviation", key: "standardDeviation", align: "center", sortable: false },
      ...(displayExpand.value ? [{ title: "", key: "data-table-expand", sortable: false }] : [])
    ];
  }

  const headers = [
    { title: props.titleHeader, key: "title", align: "start", width: titleColumnWidth.value, sortable: false },
    { title: "Number of submissions", key: "submissionCount", align: "center", width: dataColumnWidth, sortable: false },
    { title: "Submissions per participant", key: "submissionRate", align: "center", width: dataColumnWidth, sortable: false },
    { title: "Average grade", key: "averageGrade", align: "center", width: dataColumnWidth, sortable: false },
    { title: "Standard deviation", key: "standardDeviation", align: "center", width: dataColumnWidth, sortable: false }
  ];

  if (displayExpand.value) {
    headers.push({ title: "", key: "data-table-expand", width: expandColumnWidth, sortable: false });
  }

  return headers;
});
const noSubmissionsText = computed(() => props.noSubmissionsMessage || "N/A");

const customTooltips = computed(() => props.tooltips || []);
const submissionRateTooltip = computed(() => {
  const t = customTooltips.value.find(item => item.id === "submissionRate");
  if (!t) return null;
  return {
    header: t.header || "Submissions per participant",
    message: t.message || "N/A",
    icon: t.activator?.text || "mdi-help-circle-outline"
  };
});

const treatments = item => item.treatments?.rows || [];
const hasTreatments = item => treatments(item).length > 0;
const isSingleTreatment = item => treatments(item).length === 1;
const hasSubmissions = item => item.submissionCount > 0;
const title = item => item.title || "N/A";
const rate = item => round(item.submissionRate);
const grade = item => percent(item.averageGrade);
const sd = item => percent(item.standardDeviation);
</script>

<style lang="scss" scoped>
.header-with-tooltip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
}

// mobile only: each row renders its own independent card (see the
// .mobile-row branch in the #item template) instead of one shared
// desktop-style table body, so this table's own outer border/corner
// treatment (normally .v-data-table-alt, swapped out for this class in
// mobile - see the template) stays out of the way entirely.
:deep(.data-table--mobile) {
  .v-table__wrapper {
    border: none !important;
  }

  thead {
    display: none;
  }

  tbody > tr.mobile-row {
    background: transparent !important;

    > td.mobile-card-container {
      border: none !important;
      padding: 0 0 24px 0;
    }
  }
}

.mobile-card {
  border: thin solid rgba(0, 0, 0, 0.2);
  border-radius: 10px;
  padding: 4px 16px;
  background: white;
}

.mobile-field {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  padding: 10px 0;
  border-bottom: thin solid rgba(0, 0, 0, 0.08);
  text-align: right;

  &:last-child {
    border-bottom: none;
  }
}

.mobile-label {
  flex: 0 0 auto;
  font-weight: 500;
  text-align: left;
}

// right-aligned wrapped text (e.g. a long component title) reads as ragged,
// hard-to-follow lines - left-align it instead, same fix as
// ComponentTable.vue's mobile card values.
.mobile-value {
  text-align: left;
}

.mobile-no-submissions {
  justify-content: flex-start;
  text-align: left;
}
</style>
