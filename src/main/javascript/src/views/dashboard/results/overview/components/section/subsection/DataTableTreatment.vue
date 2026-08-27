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
      <tr v-if="!isMobile">
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

      <tr
        v-else
        class="mobile-row"
      >
        <td class="mobile-card-container">
          <div class="mobile-card">
            <div class="mobile-field">
              <span class="mobile-label">Treatment</span>
              <span class="mobile-value">
                {{ title(item) }}
                <v-chip
                  v-if="shouldShowConditionChip(item)"
                  :color="conditionColorForTreatment(item)"
                  density="compact"
                >
                  {{ conditionNameForTreatment(item) }}
                </v-chip>
              </span>
            </div>
            <div class="mobile-field">
              <span class="mobile-label">Number of submissions</span>
              <span class="mobile-value">{{ item.submissionCount }}</span>
            </div>
            <div class="mobile-field">
              <span class="mobile-label">Submissions per participant</span>
              <span class="mobile-value">{{ rate(item) }}</span>
            </div>
            <div class="mobile-field">
              <span class="mobile-label">Average grade</span>
              <span class="mobile-value">
                <span v-if="item.averageGrade >= 0.0">
                  {{ grade(item) }}%
                </span>
                <span v-else>
                  &#8212;
                  <tool-tip
                    content="This component includes items that must be graded manually. Data will appear when those items have been graded."
                    activatorType="icon"
                    activatorContent="mdi-information-outline"
                    aria-label="Average grade explanation tooltip"
                  />
                </span>
              </span>
            </div>
            <div class="mobile-field">
              <span class="mobile-label">Standard deviation</span>
              <span class="mobile-value">{{ sd(item) }}%</span>
            </div>
          </div>
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
  },
  isMobile: {
    type: Boolean,
    default: false
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

// mobile only: same stacked label:value card pattern as DataTable.vue's own
// mobile row - the desktop <tr> above renders all 5 columns side by side
// with no wrapping, which collided into unreadable overlapping text once
// this table stopped getting a fixed desktop width from its headers prop
// (also inherited from DataTable.vue's isMobile-aware header sizing).
.mobile-row {
  background: transparent !important;

  > td.mobile-card-container {
    padding: 12px 20px;
  }
}

.mobile-card {
  border: thin solid rgba(0, 0, 0, 0.2);
  border-radius: 10px;
  padding: 4px 16px;
  background: white;
}

.mobile-field {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  padding: 10px 0;
  border-bottom: thin solid rgba(0, 0, 0, 0.08);

  &:last-child {
    border-bottom: none;
  }
}

.mobile-label {
  flex: 0 0 auto;
  font-weight: 500;
  text-align: left;
}

.mobile-value {
  text-align: left;
}
</style>
