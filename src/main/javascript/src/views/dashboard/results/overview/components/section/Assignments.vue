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
         the real "Overall" summary row this highlight is meant for. */
      & tr:last-child:not(.v-data-table-rows-no-data) {
        position: relative;
        background-color: #f6fbff !important;
        border-bottom-left-radius: 10px;
        border-bottom-right-radius: 10px;
        z-index: 0;

        &:hover {
          background-color: #f6fbff !important;
        }

        /* tr:nth-last-child(2) above suppresses the previous row's own border-bottom, so this
           is the only divider between it and this "Overall" row */
        td {
          border-top: 1px solid lightgrey;
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
