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
      tr:nth-last-child(2) {
        border-bottom: none !important;

        td {
          border-bottom: none !important;
        }
      }
      & tr:last-child {
        position: relative;
        background-color: #f6fbff;
        border-bottom-left-radius: 10px;
        border-bottom-right-radius: 10px;

        -webkit-box-shadow: 12px 0 0 2px #f6fbff, -12px 0 0 2px #f6fbff;
        -moz-box-shadow: 12px 0 0 2px #f6fbff, -12px 0 0 2px #f6fbff;
        box-shadow: 12px 0 0 2px #f6fbff, -12px 0 0 2px #f6fbff;

        @media screen and (min-width: 640px) {
          & {
            -webkit-box-shadow: 20px 0 0 2px #f6fbff, -20px 0 0 2px #f6fbff;
            -moz-box-shadow: 20px 0 0 2px #f6fbff, -20px 0 0 2px #f6fbff;
            box-shadow: 20px 0 0 2px #f6fbff, -20px 0 0 2px #f6fbff;
          }

          &::after {
            margin-left: -1.4% !important;
            width: 103% !important;
          }
        }
        @media screen and (min-width: 870px) {
          & {
            -webkit-box-shadow: 26px 0 0 2px #f6fbff, -26px 0 0 2px #f6fbff;
            -moz-box-shadow: 26px 0 0 2px #f6fbff, -26px 0 0 2px #f6fbff;
            box-shadow: 26px 0 0 2px #f6fbff, -26px 0 0 2px #f6fbff;
          }

          &::after {
              margin-left: -1.8% !important;
              width: 103.7% !important;
            }
        }
        @media screen and (min-width: 1264px) {
          & {
            -webkit-box-shadow: 34px 0 0 2px #f6fbff, -34px 0 0 2px #f6fbff;
            -moz-box-shadow: 34px 0 0 2px #f6fbff, -34px 0 0 2px #f6fbff;
            box-shadow: 34px 0 0 2px #f6fbff, -34px 0 0 2px #f6fbff;
          }

          &::after {
              margin-left: -2.2% !important;
              width: 104.4% !important;
            }
        }

        &::after {
          content: "";
          position: absolute;
          left: 0;
          margin-left: -1.3%;
          top: -3px;
          right: 0;
          height: 0;
          width: 102.6%;
          -webkit-box-shadow: 12px 0 0 .5px lightgrey, -12px 0 0 .5px lightgrey;
          -moz-box-shadow: 12px 0 0 .5px lightgrey, -12px 0 0 .5px lightgrey;
          box-shadow: 12px 0 0 .5px lightgrey, -12px 0 0 .5px lightgrey;
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
