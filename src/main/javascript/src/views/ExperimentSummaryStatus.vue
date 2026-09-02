<template>
  <div class="experiment-summary-status">
    <page-loading
      v-if="isLoading"
      :display="true"
      message="Please wait while we load your components and outcomes."
    />

    <template v-else-if="experiment">
      <div class="summary-panels">
        <v-expansion-panels
          v-if="experiment.consent"
          class="v-expansion-panels--outlined mb-7"
        >
          <v-expansion-panel
            class="py-3"
            @click="panelExpansion"
          >
            <v-expansion-panel-title>
              <strong>Consent</strong>
            </v-expansion-panel-title>

            <v-expansion-panel-text>
              <v-table
                class="mb-9 v-data-table--no-outline v-data-table--light-header"
                hover
              >
                <thead>
                  <tr>
                    <th class="text-left">
                      Component Name
                    </th>
                    <th class="text-left">
                      Status
                    </th>
                    <th class="text-left">
                      Submissions
                    </th>
                  </tr>
                </thead>

                <tbody>
                  <tr>
                    <td>{{ experiment.consent.title }}</td>
                    <td>
                      <span
                        :class="{ complete: consentComplete }"
                        class="completion-status"
                      >
                        {{ consentComplete ? "Complete" : "In Progress" }}
                      </span>
                    </td>
                    <td>
                      {{ experiment.consent.answeredConsentCount }}/{{ experiment.consent.expectedConsent }}
                    </td>
                  </tr>
                </tbody>
              </v-table>
            </v-expansion-panel-text>
          </v-expansion-panel>
        </v-expansion-panels>

        <v-expansion-panels
          v-for="exposure in exposures"
          :key="exposure.exposureId"
          class="v-expansion-panels--outlined mb-7"
        >
          <v-expansion-panel
            class="py-3"
            @click="panelExpansion"
          >
            <v-expansion-panel-title>
              <strong>{{ exposure.title }}</strong>
            </v-expansion-panel-title>

            <v-expansion-panel-text>
              <h4 class="mb-3">
                <strong>Components</strong>
              </h4>

              <v-table
                class="mb-9 v-data-table--no-outline v-data-table--light-header"
                hover
              >
                <thead>
                  <tr>
                    <th class="text-left">
                      Component Name
                    </th>
                    <th class="text-left">
                      Status
                    </th>
                    <th class="text-left">
                      Submissions
                    </th>
                  </tr>
                </thead>

                <tbody>
                  <tr
                    v-for="assignment in assignmentsForExposure(exposure.exposureId)"
                    :key="assignment.assignmentId"
                  >
                    <td>
                      <a
                        class="link-view-assignment"
                        tabindex="0"
                        @click="handleViewAssignment(exposure.exposureId, assignment.assignmentId)"
                      >
                        {{ assignment.title }}
                      </a>
                    </td>

                    <td>
                      <span
                        class="completion-status"
                        :class="{ complete: getAssignmentCompletion(assignment.assignmentId).complete }"
                      >
                        {{ getAssignmentCompletion(assignment.assignmentId).complete ? "Complete" : "In Progress" }}
                      </span>
                    </td>

                    <td>
                      {{ getAssignmentCompletion(assignment.assignmentId).submissionsCompleted }}
                      /
                      {{ getAssignmentCompletion(assignment.assignmentId).submissionsExpected }}
                    </td>
                  </tr>
                </tbody>
              </v-table>

              <h4 class="mb-3">
                <strong>Outcomes</strong>
              </h4>

              <v-table
                v-if="outcomesForExposure(exposure.exposureId).length"
                class="mb-9 v-data-table--no-outline v-data-table--light-header"
                hover
              >
                <thead>
                  <tr>
                    <th class="text-left">
                      Outcome Name
                    </th>
                    <th class="text-left">
                      Source
                    </th>
                    <th class="text-left">
                      Actions
                    </th>
                  </tr>
                </thead>

                <tbody>
                  <tr
                    v-for="outcome in outcomesForExposure(exposure.exposureId)"
                    :key="outcome.outcomeId"
                  >
                    <td>{{ outcome.title }}</td>
                    <td>{{ outcome.external ? "Gradebook" : "Manual Entry" }}</td>
                    <td>
                      <v-menu>
                        <template #activator="{ props: menuProps }">
                          <v-icon
                            v-bind="menuProps"
                            color="black"
                          >
                            mdi-dots-horizontal
                          </v-icon>
                        </template>

                        <v-list class="text-left">
                          <v-list-item
                            v-if="!outcome.external"
                            :to="{
                              name: 'OutcomeScoring',
                              params: {
                                experimentId: experimentId,
                                exposureId: outcome.exposureId,
                                outcomeId: outcome.outcomeId
                              }
                            }"
                          >
                            <v-list-item-title>Edit</v-list-item-title>
                          </v-list-item>

                          <v-list-item
                            @click="handleDeleteOutcome(exposure.exposureId, outcome.outcomeId)"
                          >
                            <v-list-item-title>
                              Delete outcome
                            </v-list-item-title>
                          </v-list-item>
                        </v-list>
                      </v-menu>
                    </td>
                  </tr>
                </tbody>
              </v-table>

              <v-menu location="bottom">
                <template #activator="{ props: menuProps }">
                  <v-btn
                    v-bind="menuProps"
                    color="primary"
                    variant="outlined"
                  >
                    Add Outcome
                  </v-btn>
                </template>

                <v-list>
                  <v-list-item
                    :to="{
                      name: 'OutcomeGradebook',
                      params: {
                        experimentId: experimentId,
                        exposureId: exposure.exposureId
                      }
                    }"
                  >
                    <v-list-item-title>
                      Select item from gradebook
                    </v-list-item-title>
                  </v-list-item>

                  <v-list-item
                    @click="handleCreateOutcome(exposure.exposureId, false)"
                  >
                    <v-list-item-title>
                      Manually enter scores for each student
                    </v-list-item-title>
                  </v-list-item>
                </v-list>
              </v-menu>
            </v-expansion-panel-text>
          </v-expansion-panel>
        </v-expansion-panels>
      </div>
    </template>

    <template v-else>
      no experiment
    </template>
  </div>
</template>

<script setup>
import {
  computed,
  onMounted,
  ref
} from "vue";

import {
  useRouter
} from "vue-router";

import Swal from "sweetalert2";

import PageLoading from "@/components/PageLoading.vue";

import {
  deleteAttributesFromObservedElement,
  deleteAttributesFromElement
} from "@/helpers/ui-utils.js";

import { assignment as assignmentModule } from "@/store/assignment.module";
import { exposures as exposuresModule } from "@/store/exposures.module";
import { outcome as outcomeModule } from "@/store/outcome.module";
import { navigation as navigationModule } from "@/store/navigation.module";

defineOptions({
  name: "ExperimentSummaryStatus"
});

const isLoading = ref(true);

const props = defineProps({
  experiment: {
    type: Object,
    required: true
  }
});

const router = useRouter();

const assignmentStore = assignmentModule();
const exposuresStore = exposuresModule();
const outcomeStore = outcomeModule();
const navigationStore = navigationModule();

const experimentId = computed(() => {
  return Number.parseInt(props.experiment.experimentId, 10);
});

const assignments = computed(() => {
  return assignmentStore.assignments || [];
});

const exposures = computed(() => {
  return exposuresStore.exposures || [];
});

const experimentOutcomes = computed(() => {
  return outcomeStore.experimentOutcomes || [];
});

const consentComplete = computed(() => {
  return (
    props.experiment.consent.answeredConsentCount >=
      props.experiment.consent.expectedConsent &&
    props.experiment.consent.answeredConsentCount > 0
  );
});

const assignmentCompletion = computed(() => {
  return assignments.value.map(assignment => {
    const counts = getSubmissionCounts(assignment);
    const complete =
      counts.submissionsCompletedCount >= counts.submissionsExpectedCount &&
      counts.submissionsCompletedCount > 0;

    return {
      assignmentId: assignment.assignmentId,
      submissionsCompleted: counts.submissionsCompletedCount,
      submissionsExpected: counts.submissionsExpectedCount,
      submissionsInProgress: counts.submissionsInProgressCount,
      complete
    };
  });
});

const getSubmissionCounts = assignment => {
  if (!assignment.treatments?.length) {
    return {
      submissionsExpectedCount: 0,
      submissionsCompletedCount: 0,
      submissionsInProgressCount: 0
    };
  }

  return assignment.treatments.reduce(
    (counts, treatment) => {
      return {
        submissionsExpectedCount:
          counts.submissionsExpectedCount +
          (treatment.assessmentDto?.submissionsExpected || 0),
        submissionsCompletedCount:
          counts.submissionsCompletedCount +
          (treatment.assessmentDto?.submissionsCompletedCount || 0),
        submissionsInProgressCount:
          counts.submissionsInProgressCount +
          (treatment.assessmentDto?.submissionsInProgressCount || 0)
      };
    },
    {
      submissionsExpectedCount: 0,
      submissionsCompletedCount: 0,
      submissionsInProgressCount: 0
    }
  );
};

const getAssignmentCompletion = assignmentId => {
  return assignmentCompletion.value.find(
    assignment => assignment.assignmentId === assignmentId
  ) || {
    submissionsCompleted: 0,
    submissionsExpected: 0,
    submissionsInProgress: 0,
    complete: false
  };
};

const assignmentsForExposure = exposureId => {
  return assignments.value.filter(
    assignment => assignment.exposureId === exposureId
  );
};

const outcomesForExposure = exposureId => {
  return experimentOutcomes.value.filter(
    outcome => outcome.exposureId === exposureId && outcome.title
  );
};

const exposureIds = computed(() => {
  return [
    ...new Set(
      exposures.value.map(exposure => exposure.exposureId)
    )
  ];
});

const handleCreateOutcome = async (
  exposureId,
  external
) => {
  try {
    const outcome = await outcomeStore.createOutcome([
      experimentId.value,
      exposureId,
      "",
      0,
      external
    ]);

    router.push({
      name: external ? "OutcomeGradebook" : "OutcomeScoring",
      params: {
        experimentId: experimentId.value,
        exposureId,
        outcomeId: outcome.outcomeId
      }
    });
  } catch (error) {
    console.error({ error });
  }
};

const handleDeleteOutcome = async (
  exposureId,
  outcomeId
) => {
  const reallyDelete = await Swal.fire({
    icon: "question",
    text: "Do you really want to delete?",
    showCancelButton: true,
    confirmButtonText: "Yes, delete it",
    cancelButtonText: "No, cancel"
  });

  if (!reallyDelete.isConfirmed) {
    return;
  }

  try {
    await outcomeStore.deleteOutcome([
      experimentId.value,
      exposureId,
      outcomeId
    ]);

    outcomeStore.fetchOutcomesByExposures([
      experimentId.value,
      exposureIds.value
    ]);
  } catch (error) {
    console.error("handleDeleteOutcome | catch", { error });

    Swal.fire({
      text: "Could not delete outcome.",
      icon: "error"
    });
  }
};

const handleViewAssignment = async (
  exposureId,
  assignmentId
) => {
  await navigationStore.saveEditMode({
    initialPage: "ExperimentSummaryStatus",
    callerPage: {
      name: "ExperimentSummary",
      tab: "status"
    }
  });

  router.push({
    name: "AssignmentScores",
    params: {
      experimentId: experimentId.value,
      exposureId,
      assignmentId
    }
  });
};

const panelExpansion = () => {
  window.setTimeout(() => {
    deleteAttributesFromElement(
      ".v-expansion-panel",
      ["aria-expanded"]
    );
  }, 1000);
};

onMounted(async () => {
  await assignmentStore.resetAssignments();
  await exposuresStore.fetchExposures(experimentId.value);

  for (const exposure of exposures.value) {
    await assignmentStore.fetchAssignmentsByExposure([
      experimentId.value,
      exposure.exposureId,
      true
    ]);
  }

  await outcomeStore.fetchOutcomesByExposures([
    experimentId.value,
    exposureIds.value
  ]);

  isLoading.value = false;

  deleteAttributesFromObservedElement(
    ".experiment-summary-status",
    "summary-panels",
    ".v-expansion-panel",
    ["aria-expanded"]
  );

  window.setTimeout(() => {
    deleteAttributesFromElement(
      ".v-expansion-panel",
      ["aria-expanded"]
    );
  }, 1000);
});
</script>

<style lang="scss" scoped>
.completion-status {
  &::before {
    content: "";
    display: inline-block;
    background: #ffe0b2;
    height: 11px;
    width: 11px;
    margin-right: 8px;
    border-radius: 999px;
  }

  &.complete {
    &::before {
      background: #38adb6;
    }
  }
}

a.link-view-assignment {
  text-decoration: underline;
  color: unset !important;
}
</style>
