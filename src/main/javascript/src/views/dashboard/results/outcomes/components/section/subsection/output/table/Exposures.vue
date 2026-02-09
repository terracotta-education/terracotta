<template>
<data-table
  :tableData="exposures"
  :outcomeType="outcomeType"
  titleHeader="Exposure"
/>
</template>

<script>
import { mapGetters } from "vuex"
import DataTable from "./components/DataTable.vue"

export default {
  name: "Exposures",
  components: {
    DataTable
  },
  computed: {
    ...mapGetters({
      outcomes: "resultsDashboard/outcomes"
    }),
    exposures() {
      return this.outcomes?.exposures?.rows || [];
    },
    outcomeType() {
      return this.outcomes?.outcomeType || "";
    }
  }
}
</script>
