<template>
  <v-row
    class="container-table"
  >
    <h3>Conditions</h3>
    <DataTable
      :tableData="rows"
      :titleHeader="`Condition Name`"
      :noSubmissionsMessage="`Data will appear here when participants assigned to this condition have completed the component.`"
      :tooltips="tooltips"
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
    tooltips() {
      return [
        {
          id: "submissionRate",
          message: "This value is calculated by dividing the total number of component submissions for each condition by the total number of consenting participants assigned to those conditions.",
        }
      ]
    },
    sectionData() {
        return this.conditionsData || {};
    },
    rows() {
      return this.sectionData.rows || [];
    }
  }
}
</script>
