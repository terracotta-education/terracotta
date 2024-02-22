<template>
  <v-row
    class="container-table"
  >
    <h3>Assignments</h3>
    <DataTable
      :tableData="rows"
      :titleHeader="`Assignment name`"
      :includeNote="true"
      :showExpand="true"
      :hasOverall="true"
      :noSubmissionsMessage="`No submissions yet`"
    />
  </v-row>
</template>

<script>
import DataTable from "./subsection/DataTable.vue"

export default {
  name: "Assignments",
  props: [
    "assignmentsData"
  ],
  components: {
    DataTable
  },
  computed: {
    sectionData() {
        return this.assignmentsData || {};
    },
    rows() {
      return this.sectionData.rows || [];
    }
  }
}
</script>

<style scoped>
div.container-data-table {
  & .data-table {
    & tbody {
      & tr:last-child {
        background-color: rgba(204,229,255,0.25);
      }
    }
  }
  > div.note-included-data {
    width: fit-content;
    float: left;
  }
}
</style>
