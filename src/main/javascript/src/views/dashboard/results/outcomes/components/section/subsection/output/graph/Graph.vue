<template>
<chart
  :displayChartData="displayChartData"
  :graphData="graphData"
  :outcomeType="outcomeType"
  :type="getType"
/>
</template>

<script>
import { mapGetters } from "vuex"
import Chart from "./components/Chart.vue";

export default {
  name: "Graph",
  components: {
    Chart
  },
  props: {
    displayOutput: {
      type: Boolean,
      default: false
    },
    type: {
      type: String,
      default: "condition"
    }
  },
  computed: {
    ...mapGetters({
      outcomes: "resultsDashboard/outcomes"
    }),
    conditions() {
      return this.outcomes?.conditions?.rows || [];
    },
    exposures() {
      return this.outcomes?.exposures?.rows || [];
    },
    getType() {
      return this.type; // default to "condition" data
    },
    displayChartData() {
      return this.displayOutput;
    },
    graphData() {
      var dataset;

      switch (this.getType) {
        case "exposure":
          dataset = this.orderByTitleAsc(this.exposures);
          break;
        case "condition":
          dataset = this.orderByTitleAsc(this.conditions);
          break;
        default:
          dataset = [];
      }

      return dataset
        .filter((ds) => ds.title !== "Overall") // filter "Overall" column from graph data
        .map(
          (d) => {
            return {
              title: d.title,
              mean: this.displayChartData ? d.mean : null,
              scores: this.displayChartData ? d.scores?.length ? d.scores : [] : []
            }
          }
        );
    },
    outcomeType() {
      return this.outcomes?.outcomeType || "";
    }
  },
  methods: {
    orderByTitleAsc(values) {
      return values.sort(
        function(a, b) {

          if (a.title.toUpperCase() < b.title.toUpperCase()) {
            return -1;
          }

          if (a.title.toUpperCase() > b.title.toUpperCase()) {
            return 1;
          }

          return 0;
        }
      )
    }
  }
}
</script>
