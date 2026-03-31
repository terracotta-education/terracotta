<template>
<data-table
  :tableData="conditions"
  :outcomeType="outcomeType"
  titleHeader="Condition"
/>
</template>

<script>
import { mapGetters } from "vuex"
import DataTable from "./components/DataTable.vue"

export default {
  name: "Conditions",
  components: {
    DataTable
  },
  computed: {
    ...mapGetters({
      outcomes: "resultsDashboard/outcomes"
    }),
    conditions() {
      return this.outcomes?.conditions?.rows || [];
    },
    outcomeType() {
      return this.outcomes?.outcomeType || "";
    }
  }
}
</script>
