<template>
<v-row
  class="container-table"
>
  <h3>Conditions</h3>
  <data-table
    :tableData="rows"
    :tooltips="tooltips"
    titleHeader="Condition Name"
    noSubmissionsMessage="Data will appear here when participants assigned to this condition have completed the component."
  />
</v-row>
</template>

<script>
import DataTable from "./subsection/DataTable"

export default {
  name: "Conditions",
  components: {
    DataTable
  },
  props: {
    conditionsData: {
      type: Object,
      required: false
    }
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
