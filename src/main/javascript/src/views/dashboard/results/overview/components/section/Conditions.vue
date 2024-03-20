<template>
  <v-row
    class="container-table"
  >
    <h3>Conditions</h3>
    <DataTable
      :tableData="rows"
      :titleHeader="`Condition Name`"
      :noSubmissionsMessage="`Data will appear here when participants assigned to this condition have completed the assignment.`"
    />
  </v-row>
</template>

<script>
import DataTable from "./subsection/DataTable"

export default {
  name: "Conditions",
  props: [
    "conditionsData"
  ],
  components: {
    DataTable
  },
  computed: {
    sectionData() {
        return this.conditionsData || {};
    },
    rows() {
      return this.sectionData.rows || [];
    }
  }
}
</script>
