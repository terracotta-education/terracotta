<template>
<div
  class="container-overview"
>
  <div
    v-if="!isLoaded"
  >
    <page-loading
      :display="!isLoaded"
      :message="'Loading results data. Please wait.'"
    />
  </div>
  <v-row
    v-if="isLoaded && (!hasOpenAssignments || hasAssignmentSubmissions)"
    class="alert-assignments my-0 mt-0 py-0"
  >
    <v-card
      class="no-assignments-open pt-5 px-5 mx-auto bg-blue-lighten-5 rounded-lg"
      variant="outlined"
    >
      <p
        class="text"
      >
        <strong>Note:</strong> {{ alertText }}
      </p>
    </v-card>
  </v-row>
  <v-row
    v-if="isLoaded"
    class="overview-summary mx-auto"
  >
    <results-overview-summary />
  </v-row>
  <v-row
    v-if="isLoaded"
    class="row-overview-conditions mb-0"
  >
    <Conditions
        :conditionsData="resultsOverviewConditions"
      />
  </v-row>
  <v-row
    v-if="isLoaded"
    class="row-overview-assignments mb-0"
  >
    <Assignments
      :assignmentsData="resultsOverviewAssignments"
    />
  </v-row>
</div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from "vue";

import Assignments from "./components/section/Assignments.vue";
import Conditions from "./components/section/Conditions.vue";
import PageLoading from "@/components/PageLoading";
import ResultsOverviewSummary from "./components/section/summary/Summary.vue";
import { experiment as useExperimentStore } from "@/store/experiment.module";
import { resultsDashboard as useResultsDashboardStore } from "@/store/dashboard/results.module";

defineOptions({
  name: "ResultsOverview"
});


const isLoaded = ref(false);

const experiment = computed(() => useExperimentStore().experiment);
const overview = computed(() => useResultsDashboardStore().overview);

const experimentId = computed(() => {
  return experiment.value?.experimentId;
});

const resultsOverview = computed(() => {
  return overview.value || {};
});

const resultsOverviewAssignments = computed(() => {
  return resultsOverview.value.assignments || {};
});

const resultsOverviewConditions = computed(() => {
  return resultsOverview.value.conditions || {};
});

const hasOpenAssignments = computed(() => {
  return resultsOverviewAssignments.value?.rows?.filter(a => a.open).length > 0 || false;
});

const hasAssignmentSubmissions = computed(() => {
  return resultsOverviewAssignments.value?.rows?.filter(a => a.submissionCount > 0).length > 0 || false;
});

const hasAllAssignmentSubmissions = computed(() => {
  return resultsOverviewAssignments.value?.rows?.filter(a => a.submissionCount === 0).length === 0 || false;
});

const hasAllConditionSubmissions = computed(() => {
  return resultsOverviewConditions.value?.rows?.filter(c => c.submissionCount === 0).length === 0 || false;
});

const alertText = computed(() => {
  if (!hasOpenAssignments.value) {
    return "These components are not yet open, and are not yet collecting submissions.";
  }

  if (hasAssignmentSubmissions.value) {
    let message = "You are currently collecting component submissions.";

    if (!hasAllAssignmentSubmissions.value || !hasAllConditionSubmissions.value) {
      message += " Some ";

      if (!hasAllConditionSubmissions.value && !hasAllAssignmentSubmissions.value) {
        message += "conditions and components";
      } else if (!hasAllConditionSubmissions.value && hasAllAssignmentSubmissions.value) {
        message += "conditions";
      } else if (hasAllConditionSubmissions.value && !hasAllAssignmentSubmissions.value) {
        message += "components";
      }

      message += " do not yet have submissions.";
    }

    return message;
  }

  return "";
});

watch(resultsOverview, newValue => {
  isLoaded.value = newValue != null;
});

const getOverview = experimentId => {
  return useResultsDashboardStore().getOverview( experimentId);
};

onMounted(async () => {
  await getOverview(experimentId.value);
});
</script>

<style scoped>
div.container-overview {
  min-height: 650px;
  > .v-row {
    justify-content: space-evenly;
    margin: 40px 0;
    padding: 20px;
    & h3 {
      width: fit-content;
      padding-bottom: 8px;
    }
    & .container-table {
      > h3 {
        font-weight: bold;
      }
    }
    &.overview-summary {
      width: 100%;
      max-width: 100%;
      margin-top: 30px !important;
      margin-bottom: 0 !important;
      padding-top: 0 !important;
      padding-bottom: 0 !important;
    }
    &.alert-assignments {
      margin-top: 15px !important;
    }
    & .no-assignments-open {
      width: 100%;
      background-color: #e3f2fd !important;
      border-color: #1d9dff99 !important;
      > .text {
        padding-bottom: 0 !important;
      }
    }
    &.row-overview-assignments {
      margin-top: 0 !important;
    }
  }
  & .column-left {
    max-width: 25%;
  }
}
</style>
