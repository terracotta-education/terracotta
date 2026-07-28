<template>
  <v-container
    class="container-data-table"
  >
    <v-data-table
      :headers="tableHeaders"
      :items="tableData"
      :items-per-page="-1"
      :show-expand="displayExpand"
      class="data-table v-data-table-alt"
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
        <tr>
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
      </template>
      <template
        v-slot:expanded-row="{ item, columns }"
      >
        <data-table-treatment
          v-if="hasTreatments(item)"
          :headers="columns"
          :item="item"
        />
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

const dataColumnWidth = "15%";
const expandColumnWidth = "5%";
const displayNote = computed(() => props.includeNote || false);
const displayExpand = computed(() => props.showExpand || false);
// the 4 data columns are always 15% each so they match width-wise across tables that do and
// don't show the expand column; the title column absorbs whatever's left so everything still
// sums to 100% (35% + expand's 5% when shown, or 40% when there's no expand column at all)
const titleColumnWidth = computed(() => displayExpand.value ? "35%" : "40%");
const tableHeaders = computed(() => {
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
</style>
