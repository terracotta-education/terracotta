<template>
<div
  v-if="experiment && exposureId"
  class="outcome-gradebook-container"
>
  <div
    v-if="!isLoaded"
  >
    <page-loading
      :display="!isLoaded"
      :message="'Loading gradebook items from your LMS. Please wait.'"
    />
  </div>
  <h1
    class="mb-6"
  >
    Select gradebook item(s)
  </h1>
  <form
    @submit.prevent="saveExit"
  >
    <v-simple-table
      class="mb-9 v-data-table--light-header"
    >
      <template
        v-slot:default
      >
        <thead>
          <tr>
            <th
              style="width:50px;"
            >
            <v-checkbox
              v-model="selectAll"
              :value="selectAll"
              @change="handleSelectAll()"
              on-icon="$checkboxIndeterminate"
              color="primary"
              aria-label="Select all gradebook items"
              ></v-checkbox>
            </th>
            <th
              class="text-left"
            >
              Gradebook Item
            </th>
            <th
              class="text-left"
              style="width:250px;"
            >
              Total Points
            </th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="(op, opIndex) in outcomePotentials"
            :key="opIndex"
          >
            <td>
              <template
                v-if="!outcomes.some(o => o.lmsOutcomeId === op.assignmentId && o.exposureId === exposureId)"
              >
                <v-checkbox
                  v-model="selectedAssignmentIds"
                  :value="op.assignmentId"
                  :aria-label="`Select ${op.name} for outcome creation`"
                ></v-checkbox>
              </template>
              <template
                v-else
              >
                <v-icon>mdi-check</v-icon>
              </template>
            </td>
            <td>{{op.name}}</td>
            <td>{{op.pointsPossible}}</td>
          </tr>
        </tbody>
      </template>
    </v-simple-table>
  </form>
</div>
</template>

<script>
import { mapActions, mapGetters } from "vuex";
import { statusAlert } from "@/helpers/ui-utils.js";
import PageLoading from "@/components/PageLoading";

export default {
  name: "OutcomeGradebook",
  components: {
    PageLoading
  },
  data: () => ({
    selectedAssignmentIds: [],
    selectAll: false,
    isLoaded: false
  }),
  computed: {
    ...mapGetters({
      experiment: "experiment/experiment",
      outcomePotentials: "outcome/outcomePotentials",
      outcomes: "outcome/outcomes",
      alertStatuses: "alert/statuses"
    }),
    exposureId() {
      return parseInt(this.$route.params.exposureId);
    },
    experimentId() {
      return parseInt(this.$route.params.experimentId);
    }
  },
  methods: {
    ...mapActions({
      fetchOutcomePotentials: "outcome/fetchOutcomePotentials",
      fetchOutcomes: "outcome/fetchOutcomes",
      createOutcome: "outcome/createOutcome",
      resetOutcomePotentials: "outcome/resetOutcomePotentials"
    }),
    handleSelectAll() {
      this.selectedAssignmentIds = this.selectAll ? this.outcomePotentials.map((op) => op.assignmentId) : [];
    },
    async saveExit() {
      try {
        await Promise.all(this.selectedAssignmentIds.map(async assignmentId => {
          const op = this.outcomePotentials.find(o=>parseInt(o.assignmentId) === parseInt(assignmentId));
          // payload = experimentId, exposureId, title, max_points, external, lmsType, lmsOutcomeId
          return await this.createOutcome([this.experimentId, this.exposureId, op.name, op.pointsPossible, true, op.type, parseInt(assignmentId)]);
        })).then(() => {
          let params = {};

          if (this.selectedAssignmentIds.length > 0) {
            params = {
              ...statusAlert(
                this.alertStatuses.success,
                "Outcomes created successfully."
              )
            }
          }
          this.$router.push({
            name:"ExperimentSummary",
            params: {
              ...params
            }
          });
        })
      } catch(error) {
        this.$swal({
          text: "An error occurred while creating outcomes. Please try again.",
          icon: "error"
        });
      }
    }
  },
  async created() {
    await this.resetOutcomePotentials();
    await this.fetchOutcomes([this.experimentId, this.exposureId]);
    await this.fetchOutcomePotentials(this.experimentId);
    this.isLoaded = true;
  }
}
</script>