<template>
  <v-data-table
    class="v-data-table-alt"
    :headers="tableHeaders"
    :items="computedTableData"
    :mobile-breakpoint="mobileBreakpoint"
    :items-per-page="computedTableData.length"
    item-key="title"
    disable-sort
    hide-default-footer
  >
  </v-data-table>
</template>

<script>
import { timeFormat, percent } from "@/helpers/dashboard/utils.js";

export default {
  name: "DataTable",
  props: [
    "tableData",
    "titleHeader",
    "outcomeType"
  ],
  data: () => ({
    mobileBreakpoint: 636,
  }),
  computed: {
    computedTableData() {
      // process the datatable-specific data format
      return this.tableData.map(
        (t) => {
          switch (this.outcomeType) {
            case "TIME_ON_TASK":
              return {
                title: t.title,
                number: t.number,
                standardDeviation: timeFormat(t.standardDeviation),
                mean: timeFormat(t.mean)
              }
            case "AVERAGE_ASSIGNMENT_SCORE":
            case "STANDARD":
            default:
              return {
                title: t.title,
                number: t.number,
                standardDeviation: percent(t.standardDeviation) + "%",
                mean: percent(t.mean) + "%"
              }
          }
        }
      );
    },
    tableHeaders() {
      return [
        {
          text: this.titleHeader,
          value: "title",
          align: "start",
          width: "35%"
        },
        {
          text: "N",
          value: "number",
          align: "center"
        },
        {
          text: "Mean",
          value: "mean",
          align: "center"
        },
        {
          text: "Standard deviation",
          value: "standardDeviation",
          align: "center"

        }
      ]
    }
  }
}
</script>

