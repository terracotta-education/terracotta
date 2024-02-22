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
import DataTable from "./subsection/DataTable"

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
        background-color: #f6fbff;
        -webkit-box-shadow: 12px 0 0 2px #f6fbff, -12px 0 0 2px #f6fbff;
        -moz-box-shadow: 12px 0 0 2px #f6fbff, -12px 0 0 2px #f6fbff;
        box-shadow: 12px 0 0 2px #f6fbff, -12px 0 0 2px #f6fbff;
        border-bottom-left-radius: 10px;
        border-bottom-right-radius: 10px;

        @media screen and (min-width: 640px) {
          -webkit-box-shadow: 22px 0 0 2px #f6fbff, -22px 0 0 2px #f6fbff;
          -moz-box-shadow: 22px 0 0 2px #f6fbff, -22px 0 0 2px #f6fbff;
          box-shadow: 22px 0 0 2px #f6fbff, -22px 0 0 2px #f6fbff;
        }
        @media screen and (min-width: 870px) {
          -webkit-box-shadow: 26px 0 0 2px #f6fbff, -26px 0 0 2px #f6fbff;
          -moz-box-shadow: 26px 0 0 2px #f6fbff, -26px 0 0 2px #f6fbff;
          box-shadow: 26px 0 0 2px #f6fbff, -26px 0 0 2px #f6fbff;
        }
        @media screen and (min-width: 1264px) {
          -webkit-box-shadow: 34px 0 0 2px #f6fbff, -34px 0 0 2px #f6fbff;
          -moz-box-shadow: 34px 0 0 2px #f6fbff, -34px 0 0 2px #f6fbff;
          box-shadow: 34px 0 0 2px #f6fbff, -34px 0 0 2px #f6fbff;
        }
      }
    }
  }
  > div.note-included-data {
    width: fit-content;
    float: left;
    color: #666666;
  }
}
</style>
