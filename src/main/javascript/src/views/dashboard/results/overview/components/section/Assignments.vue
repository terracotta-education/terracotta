<template>
<v-row
  class="container-table"
>
  <h3>Components</h3>
  <data-table
    :tableData="rows"
    :tooltips="tooltips"
    :includeNote="true"
    :hasOverall="true"
    noSubmissionsMessage="No submissions yet"
    titleHeader="Component name"
  />
</v-row>
</template>

<script setup>
import { computed } from "vue";

import DataTable from "./subsection/DataTable.vue";

defineOptions({
  name: "OverviewAssignmentsSection"
});

const props = defineProps({
  assignmentsData: {
    type: Object,
    required: false
  }
});

const tooltips = [
  {
    id: "submissionRate",
    message: "This value is calculated by dividing the total number of component submissions by the total number of consenting participants."
  }
];

const sectionData = computed(() => {
  return props.assignmentsData || {};
});

const rows = computed(() => {
  return sectionData.value.rows || [];
});
</script>

<style scoped>
/* DataTable.vue is a separate (shared) child component, so :deep() is required here to reach
   its rendered rows - anchoring on .container-table (this component's own root) means this only
   affects this Assignments.vue instance, not other sections reusing the same DataTable.vue */
.container-table :deep(div.container-data-table) {
  & .data-table {
    & tbody {
      tr:nth-last-child(2) {
        border-bottom: none !important;

        td {
          border-bottom: none !important;
        }
      }
      /* excludes Vuetify's own "No data available" placeholder row - it's
         also technically tr:last-child when the table is empty, but isn't
         the real "Overall" summary row this highlight is meant for. Without
         this, the deliberate 20px box-shadow overflow below (meant to reach
         the table's own outer rounded border from a genuine last data row)
         overshot past the placeholder row's border instead, since it isn't
         sitting in that same surrounding row context. */
      & tr:last-child:not(.v-data-table-rows-no-data) {
        position: relative;
        background-color: #f6fbff !important;
        border-bottom-left-radius: 10px;
        border-bottom-right-radius: 10px;
        -webkit-box-shadow: 20px 0 0 2px #f6fbff, -20px 0 0 2px #f6fbff;
        -moz-box-shadow: 20px 0 0 2px #f6fbff, -20px 0 0 2px #f6fbff;
        box-shadow: 20px 0 0 2px #f6fbff, -20px 0 0 2px #f6fbff;
        z-index: 0;

        &:hover {
          background-color: #f6fbff !important;
        }

        &::after {
          content: "";
          position: absolute;
          left: 0;
          top: -3px;
          right: 0;
          height: 0;
          margin-left: -10px !important;
          width: Calc(100% + 20px) !important;
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
h3 {
  font-weight: 700;
  padding: 0;
  margin: 0;
}
</style>
