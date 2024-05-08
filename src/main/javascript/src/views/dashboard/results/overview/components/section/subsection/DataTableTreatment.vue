<template>
  <td
      :colspan="headers.length"
      class="treatment-row px-0"
  >
    <v-data-table
      :headers="headers"
      :items="treatments"
      class="grey lighten-5"
      item-key="id"
      hide-default-footer
      disable-sort
      show-expand
    >
      <template
        v-slot:item="{ headers, item, expand, isExpanded }"
      >
        <tr>
          <td>
            <span
              class="treatment-title"
            >
              {{ title(item) }}
              <v-chip
                v-if="!singleConditionExperiment && findTreatmentsForAssignmentId(item.assignmentId).length === conditions.length"
                label
                :color="
                  conditionColorMapping[
                    conditionForTreatment(
                      findExposureForAssignment(findAssignmentById(item.assignmentId)).groupConditionList,
                      item.conditionId
                    ).conditionName
                  ]
                "
              >
                {{
                  conditionForTreatment(
                    findExposureForAssignment(findAssignmentById(item.assignmentId)).groupConditionList,
                    item.conditionId
                  ).conditionName
                }}
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
              <InfoTooltip
                :header=null
                :message="avgGradeTooltip"
                :activator="avgGradeTooltipActivator"
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
              :class="{'v-data-table__expand-icon--active' : isExpanded}"
              @click="expand(!isExpanded)"
              class="v-data-table__expand-icon"
              icon
            >
              <v-icon>mdi-chevron-down</v-icon>
            </v-btn>
          </td>
        </tr>
      </template>
    </v-data-table>
  </td>
</template>

<script>
import { round, percent } from "@/helpers/dashboard/utils.js";
import { mapGetters } from "vuex";
import InfoTooltip from "@/components/InfoTooltip.vue";

export default {
  name: "DataTableTreatment",
  props: [
    "headers",
    "item"
  ],
  components: {
    InfoTooltip
  },
  computed: {
    ...mapGetters({
      assignments: "assignment/assignments",
      conditionColorMapping: "condition/conditionColorMapping",
      conditions: "experiment/conditions",
      exposures: "exposures/exposures"
    }),
    singleConditionExperiment() {
      return this.conditions.length === 1;
    },
    treatments() {
      return this.item.treatments.rows || [];
    },
    avgGradeTooltip() {
      return "This assignment includes items that must be graded manually. Data will appear when those items have been graded.";
    },
    avgGradeTooltipActivator() {
      return {"type": "icon", "text": "mdi-information-outline"};
    }
  },
  methods: {
    title(item) {
      return item.title || "Treatment";
    },
    rate(item) {
      return round(item.submissionRate);
    },
    grade(item) {
      return percent(item.averageGrade);
    },
    sd(item) {
      return percent(item.standardDeviation);
    },
    conditionForTreatment(groupConditionList, conditionId) {
      return groupConditionList.find((c) => c.conditionId === conditionId);
    },
    findAssignmentById(assignmentId) {
      return this.assignments.find(a => a.assignmentId === assignmentId);
    },
    findExposureForAssignment(assignment) {
      return this.exposures.find(e => e.exposureId === assignment.exposureId);
    },
    findTreatmentsForAssignmentId(assignmentId) {
      return this.findAssignmentById(assignmentId).treatments || [];
    }
  }
}
</script>

<style scoped>
td.treatment-row {
  > .v-data-table {
    margin: 0 auto;
    & .v-data-table__wrapper {
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
              & .v-chip__content {
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
