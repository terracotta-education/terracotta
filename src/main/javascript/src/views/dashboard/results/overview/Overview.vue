<template>
  <div
    class="container-overview"
  >
    <div
      v-if="!isLoaded"
    >
      <PageLoading
        :display="!isLoaded"
        :message="'Loading results data. Please wait.'"
      />
    </div>
    <v-row
      v-if="isLoaded && (!hasOpenAssignments || hasAssignmentSubmissions)"
      class="alert-assignments my-0 mt-0 py-0"
    >
      <v-card
        class="no-assignments-open pt-5 px-5 mx-auto blue lighten-5 rounded-lg"
        outlined
      >
        <p
          class="pb-0"
        >
          <strong>Note:</strong> {{ alertText }}
        </p>
      </v-card>
    </v-row>
    <v-row
      v-if="isLoaded"
      class="overview-summary mx-auto"
    >
      <Summary />
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

<script>
  import { mapGetters, mapActions } from "vuex";
  import Assignments from "./components/section/Assignments.vue";
  import Conditions from "./components/section/Conditions.vue";
  import PageLoading from "@/components/PageLoading";
  import Summary from "./components/section/summary/Summary.vue";

  export default {
    name: "Overview",
    components: {
      Assignments,
      Conditions,
      PageLoading,
      Summary
    },
    data: () => ({
      isLoaded: false,
    }),
    computed: {
      ...mapGetters({
        experiment: "experiment/experiment",
        overview: "resultsDashboard/overview"
      }),
      experimentId() {
        return this.experiment.experimentId;
      },
      resultsOverview() {
        return this.overview || {} ;
      },
      resultsOverviewAssignments() {
        return this.resultsOverview.assignments || {};
      },
      resultsOverviewConditions() {
        return this.resultsOverview.conditions || {};
      },
      hasOpenAssignments() {
        return this.resultsOverviewAssignments?.rows?.filter(a => a.open).length > 0 || false;
      },
      hasAssignmentSubmissions() {
        return this.resultsOverviewAssignments?.rows?.filter(a => a.submissionCount > 0).length > 0 || false;
      },
      hasAllAssignmentSubmissions() {
        return this.resultsOverviewAssignments?.rows?.filter(a => a.submissionCount === 0).length === 0 || false;
      },
      hasAllConditionSubmissions() {
        return this.resultsOverviewConditions?.rows?.filter(c => c.submissionCount === 0).length === 0 || false;
      },
      alertText() {
        if (!this.hasOpenAssignments) {
          return "These components are not yet open, and are not yet collecting submissions.";
        }
        if (this.hasAssignmentSubmissions) {
          let message = "You are currently collecting component submissions.";
          if (!this.hasAllAssignmentSubmissions || !this.hasAllConditionSubmissions) {
            message += " Some ";
            if (!this.hasAllConditionSubmissions && !this.hasAllAssignmentSubmissions) {
              message += "conditions and components";
            } else if (!this.hasAllConditionSubmissions && this.hasAllAssignmentSubmissions) {
              message += "conditions";
            } else if (this.hasAllConditionSubmissions && !this.hasAllAssignmentSubmissions) {
              message += "components";
            }
            message += " do not yet have submissions.";
          }
          return message;
        }
        return "";
      }
    },
    watch: {
      resultsOverview: {
        handler(newValue) {
          this.isLoaded = newValue != null;
        }
      }
    },
    methods: {
      ...mapActions({
        getOverview: "resultsDashboard/getOverview"
      })
    },
    async mounted() {
      await this.getOverview(this.experimentId);
    }
}
</script>

<style scoped>
div.container-overview {
  min-height: 650px;
  > .row {
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
      width: 98%;
      max-width: 98%;
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
      &.v-sheet--outlined.blue.lighten-5 {
        border-color: rgba(29, 157, 255, 0.6) !important;
      }
    }
    &.row-overview-assignments {
      margin-top: 30px !important;
    }
  }
  & .column-left {
    max-width: 25%;
  }
}
</style>
