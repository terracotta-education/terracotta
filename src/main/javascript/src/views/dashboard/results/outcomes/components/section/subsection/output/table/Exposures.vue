<template>
  <DataTable
    :titleHeader="`Exposure`"
    :tableData="exposures"
    :outcomeType="outcomeType"
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
