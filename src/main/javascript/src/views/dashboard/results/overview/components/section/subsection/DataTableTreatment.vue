<template>
<td
    :colspan="headers.length"
    class="treatment-row px-0"
>
  <v-data-table
    :headers="headers"
    :items="treatments"
    :items-per-page="-1"
    class="bg-grey-lighten-5"
    item-value="id"
    show-expand
  >
    <template
      v-slot:item="{ item, isExpanded, toggleExpand }"
    >
      <tr>
        <td>
          <span
            class="treatment-title"
          >
            {{ title(item) }}
            <v-chip
              v-if="shouldShowConditionChip(item)"
              :color="conditionColorForTreatment(item)"
              density="compact"
            >
              {{ conditionNameForTreatment(item) }}
            </v-chip>
          </span>
        </td>
        <td
          class="data-column text-center"
        >
          {{ item.submissionCount }}
        </td>
        <td
          class="data-column text-center"
        >
          {{ rate(item) }}
        </td>
        <td
          class="data-column text-center"
        >
          <span
            v-if="item.averageGrade >= 0.0"
          >
            {{ grade(item) }}%
          </span>
          <span
            v-else
          >
            &#8212;
            <tool-tip
              content="This component includes items that must be graded manually. Data will appear when those items have been graded."
              activatorType="icon"
              activatorContent="mdi-information-outline"
              aria-label="Average grade explanation tooltip"
            />
          </span>
        </td>
        <td
          class="data-column text-center"
        >
          {{ sd(item) }}%
        </td>
        <td
          class="text-start"
        >
          <v-btn
            :class="{'v-data-table__expand-icon--active': isExpanded(item)}"
            @click="toggleExpand(item)"
            class="v-data-table__expand-icon"
            variant="text"
            icon="mdi-chevron-down"
          />
        </td>
      </tr>
    </template>
    <template #bottom></template>
  </v-data-table>
</td>
</template>

<script setup>
import { computed } from "vue";

import ToolTip from "@/components/ToolTip.vue";
import { assignment as useAssignmentStore } from "@/store/assignment.module";
import { condition as useConditionStore } from "@/store/condition.module";
import { experiment as useExperimentStore } from "@/store/experiment.module";
import { exposures as useExposuresStore } from "@/store/exposures.module";
import { round, percent } from "@/helpers/dashboard/utils.js";

defineOptions({
  name: "DataTableTreatment"
});

const props = defineProps({
  headers: {
    type: Array,
    required: true
  },
  item: {
    type: Object,
    required: true
  }
});

const treatments = computed(() => props.item?.treatments?.rows || []);

const assignments = computed(() => useAssignmentStore().assignments);
const conditionColorMapping = computed(() => useConditionStore().conditionColorMapping);
const conditions = computed(() => useExperimentStore().conditions);
const exposures = computed(() => useExposuresStore().exposures);

const singleConditionExperiment = computed(() => conditions.value.length === 1);

const findAssignmentById = assignmentId =>
  assignments.value.find(a => a.assignmentId === assignmentId);

const findExposureForAssignment = assignment => {
  if (!assignment) return null;
  return exposures.value.find(e => e.exposureId === assignment.exposureId);
};

const findTreatmentsForAssignmentId = assignmentId =>
  findAssignmentById(assignmentId)?.treatments || [];

const conditionForTreatment = (groupConditionList, conditionId) =>
  groupConditionList?.find(c => c.conditionId === conditionId);

const conditionNameForTreatment = treatment => {
  const assignment = findAssignmentById(treatment.assignmentId);
  const exposure = findExposureForAssignment(assignment);
  const condition = conditionForTreatment(exposure?.groupConditionList, treatment.conditionId);
  return condition?.conditionName || "";
};

const conditionColorForTreatment = treatment =>
  conditionColorMapping.value[conditionNameForTreatment(treatment)];

const shouldShowConditionChip = treatment =>
  !singleConditionExperiment.value &&
  findTreatmentsForAssignmentId(treatment.assignmentId).length === conditions.value.length;

const title = item => item.title || "Treatment";
const rate = item => round(item.submissionRate);
const grade = item => percent(item.averageGrade);
const sd = item => percent(item.standardDeviation);
</script>

<style lang="scss" scoped>
td.treatment-row {
  > .v-data-table {
    margin: 0 auto;

    // bg-grey-lighten-5 (a Vuetify color utility class) generates no CSS in this
    // project's build - see ExperimentType.vue's .card-warning for the full
    // explanation. Unlike ComponentTable.vue's identical-looking treatment-row (whose
    // wrapper already forces its own background), nothing else covers this one.
    background-color: map.get($grey, "lightest");

    & .v-table__wrapper {
      border: none;
      border-bottom: thin solid #e0e0e0;
      border-radius: 0;
      & table {
        width: unset;
        padding-left: 0 !important;
        padding-right: 0 !important;
        > thead {
          visibility: collapse;
        }
        > tbody {
          > tr {
            > td:first-child {
              padding-left: 20px !important;
            }
            & .treatment-title {
              & .v-chip {
                text-wrap: pretty !important;
              }
            }
          }
        }
        & tr {
          background-color: transparent !important;
        }
        & tr:hover {
          background-color: #eee !important;
        }
        & .v-data-table__expand-icon {
          visibility: hidden;
        }
      }
    }
  }
}
</style>
