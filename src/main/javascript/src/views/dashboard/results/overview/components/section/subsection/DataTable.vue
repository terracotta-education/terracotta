<template>
  <v-container
    class="container-data-table"
  >
    <v-data-table
      :headers="tableHeaders"
      :items="tableData"
      :items-per-page="tableData.length"
      :mobile-breakpoint="mobileBreakpoint"
      :show-expand="displayExpand"
      class="data-table v-data-table-alt"
      item-key="title"
      disable-sort
      hide-default-footer
    >
      <template
        v-if="submissionRateTooltip"
        v-slot:header.submissionRate="{ header }"
      >
        {{ header.text }}
        <tool-tip
          :header="submissionRateTooltip.header"
          :content="submissionRateTooltip.message"
          :activatorType="submissionRateTooltip.activator.type"
          :activatorContent="submissionRateTooltip.activator.text"
          :icon="submissionRateTooltip.activator.text"
          :iconStyle="submissionRateTooltip.iconStyle"
          alignment="top"
          aria-label="Submission rate tooltip"
        />
      </template>
      <template
        v-slot:item="{ headers, item, expand, isExpanded }"
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
              label
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
                activatorType="icon"
                activatorContent="mdi-information-outline"
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
              @click="expand(!isExpanded)"
              :class="{'v-data-table__expand-icon--active' : isExpanded}"
              class="v-data-table__expand-icon"
              icon
            >
              <v-icon>mdi-chevron-down</v-icon>
            </v-btn>
          </td>
          <td
            v-if="!hasSubmissions(item)"
            :colspan="headers.length"
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
        v-slot:expanded-item="{ headers, item }"
      >
        <data-table-treatment
          v-if="hasTreatments(item)"
          :headers="headers"
          :item="item"
        />
      </template>
    </v-data-table>
    <v-row
      v-if="displayNote"
      class="note-included-data mt-2 pl-4"
    >
      * Only includes data from consenting students
    </v-row>
  </v-container>
</template>

<script>
import DataTableTreatment from "./DataTableTreatment.vue";
import ToolTip from "@/components/ToolTip.vue";
import { round, percent } from "@/helpers/dashboard/utils.js";

export default {
  name: "DataTable",
  props: [
    "tableData",
    "titleHeader",
    "includeNote",
    "showExpand",
    "hasOverall",
    "noSubmissionsMessage",
    "tooltips" // [{tooltip1}, {tooltip2}, {...}]
  ],
  components: {
    DataTableTreatment,
    ToolTip
  },
  data: () => ({
    mobileBreakpoint: 636,
  }),
  computed: {
    tableHeaders() {
      return [
        {
          text: this.titleHeader,
          value: "title",
          align: "start",
          width: this.titleColumnWidth
        },
        {
          text: "Number of submissions",
          value: "submissionCount",
          align: "center",
          width: this.dataColumnWidth
        },
        {
          text: "Submissions per participant",
          value: "submissionRate",
          align: "center",
          width: this.dataColumnWidth
        },
        {
          text: "Average grade",
          value: "averageGrade",
          align: "center",
          width: this.dataColumnWidth
        },
        {
          text: "Standard deviation",
          value: "standardDeviation",
          align: "center",
          width: this.dataColumnWidth
        },
        {
          text: "",
          value: "data-table-expand",
          width: this.expandColumnWidth
        }
      ]
    },
    submissionRateTooltip() {
      const submissionRate = this.customTooltips.find(tooltip => tooltip.id === "submissionRate");

      if (!submissionRate) {
        return null;
      }

      return {
        id: "submissionRate",
        header: submissionRate.header || "Submissions per participant",
        message: submissionRate.message || "N/A",
        activator: {
          "type": submissionRate.activator?.type || "icon",
          "text": submissionRate.activator?.text || "mdi-help-circle-outline"
        },
        iconStyle: submissionRate.iconStyle || {
          "font-size": "16px",
          "vertical-align": "middle"
        }
      }
    },
    displayNote() {
      return this.includeNote || false;
    },
    displayExpand() {
      return this.showExpand || false;
    },
    titleColumnWidth() {
      return "35%";
    },
    dataColumnWidth() {
      return "15%";
    },
    expandColumnWidth() {
      return this.displayExpand ? "5%" : "0%";
    },
    displayOverall() {
      return this.hasOverall || false;
    },
    noSubmissionsText() {
      return this.noSubmissionsMessage || "N/A";
    },
    customTooltips() {
      return this.tooltips || [];
    }
  },
  methods: {
    treatments(item) {
      return item.treatments ? item.treatments.rows || [] : [];
    },
    hasTreatments(item) {
      return this.treatments(item).length > 0;
    },
    isSingleTreatment(item) {
      return this.treatments(item).length === 1;
    },
    hasSubmissions(item) {
      return item.submissionCount > 0;
    },
    title(item) {
      return item.title || "N/A";
    },
    rate(item) {
      return round(item.submissionRate);
    },
    grade(item) {
      return percent(item.averageGrade);
    },
    sd(item) {
      return percent(item.standardDeviation);
    }
  }
}
</script>
