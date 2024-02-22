<template>
  <v-row
    class="row-summary"
  >
    <v-col>
      <Participants
        :participantsData="resultsOverviewParticipants"
      />
    </v-col>
    <v-col>
      <Assignments
        :assignmentCount="resultsOverviewSummaryAssignmentsCount"
      />
    </v-col>
    <v-col>
      <Conditions
        :conditionCount="resultsOverviewSummaryConditionsCount"
      />
    </v-col>
    <v-col>
      <Exposures
        :exposureType="resultsOverviewExposureType"
      />
    </v-col>
  </v-row>
</template>

<script>
  import { mapGetters } from "vuex";
  import Assignments from "./Assignments.vue";
  import Conditions from "./Conditions.vue";
  import Exposures from "./Exposures.vue";
  import Participants from "./Participants.vue";

export default {
  name: "Summary",
  components: {
    Assignments,
    Conditions,
    Exposures,
    Participants
  },
  computed: {
    ...mapGetters({
      overview: "resultsDashboard/overview"
    }),
    resultsOverviewExposures() {
      return {
        exposureType: this.resultsOverviewConditionsExposureType
      }
    },
    resultsOverview() {
      return this.overview || {} ;
    },
    resultsOverviewParticipants() {
      return this.resultsOverview.participants || {};
    },
    resultsOverviewExposureType() {
      return this.resultsOverviewConditions.exposureType;
    },
    resultsOverviewSummaryAssignmentsCount() {
      return this.resultsOverviewParticipants.assignmentCount || 0;
    },
    resultsOverviewConditions() {
      return this.resultsOverview.conditions || {};
    },
    resultsOverviewConditionsRows() {
      return this.resultsOverviewConditions.rows || [];
    },
    resultsOverviewSummaryConditionsCount() {
        return this.resultsOverviewNamedConditions.length || 0;
    },
    resultsOverviewNamedConditions() {
      return this.resultsOverview.conditions.rows.filter(r => r.title !== "Assignments with only one version");
    },
  }
}
</script>

<style scoped>
div.row-summary {
  justify-content: space-between;
  > div.col {
    max-width: 24%;
    border: thin solid #e0e0e0;
    border-radius: 10px;
    > .container-summary {
      display: flex;
      flex-direction: column;
      justify-content: space-evenly;
      align-items: center;
      font-size: 1.05em;
    }
  }
}
</style>
