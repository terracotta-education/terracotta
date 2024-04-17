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
              <InfoTooltip
                :header=null
                :message="avgGradeTooltip"
                :activator="avgGradeTooltipActivator"
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
        <DataTableTreatment
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
import InfoTooltip from "@/components/InfoTooltip.vue";
import { round, percent } from "@/helpers/dashboard/utils.js";

export default {
  name: "DataTable",
  props: [
    "tableData",
    "titleHeader",
    "includeNote",
    "showExpand",
    "hasOverall",
    "noSubmissionsMessage"
  ],
  components: {
    DataTableTreatment,
    InfoTooltip
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
    avgGradeTooltip() {
      return "This assignment includes items that must be graded manually. Data will appear when those items have been graded.";
    },
    avgGradeTooltipActivator() {
      return {"type": "icon", "text": "mdi-information-outline"};
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