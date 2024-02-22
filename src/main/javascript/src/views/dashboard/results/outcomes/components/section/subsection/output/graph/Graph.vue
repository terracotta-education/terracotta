<template>
  <Chart
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
  props: [
    "displayOutput",
    "type"
  ],
  components: {
    Chart
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
      return this.type || "condition"; // default to "condition" data
    },
    displayChartData() {
      return this.displayOutput || false;
    },
    graphData() {
      var dataset = [];
      if (this.getType === "exposure") {
        dataset = this.orderByTitleAsc(this.exposures);
      } else if (this.getType === "condition") {
        dataset = this.orderByTitleAsc(this.conditions);
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
