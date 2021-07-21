<template>
  <div class="experiment-summary-status" >
    <template v-if="experiment">
      <h1>Experiment Status</h1>
      <p>Once your experiment is running, you will see status updates below</p>

      <v-expansion-panels class="v-expansion-panels--outlined mb-7" flat>
        <v-expansion-panel class="py-3">
          <v-expansion-panel-header>
            <strong>Consent</strong>
          </v-expansion-panel-header>
          <v-expansion-panel-content>
            <v-simple-table class="mb-9 v-data-table--no-outline v-data-table--light-header">
              <template v-slot:default>
                <thead>
                <tr>
                  <th class="text-left">Assignment Name</th>
                  <th class="text-left">Status</th>
                  <th class="text-left">Submissions</th>
                </tr>
                </thead>
                <tbody>
<!--                WIP-->
<!--                <tr-->
<!--                  v-for="assignment in assignments.filter(a => a.exposureId === exposure.exposureId)"-->
<!--                  :key="assignment.assignmentId"-->
<!--                >-->
                <tr>
                  <td>Informed Consent</td>
                  <td><span class="completion-status complete">Complete</span></td>
                  <td>23/23</td>
                </tr>
                </tbody>
              </template>
            </v-simple-table>
          </v-expansion-panel-content>
        </v-expansion-panel>
      </v-expansion-panels>

      <v-expansion-panels class="v-expansion-panels--outlined mb-7"
                          v-for="(exposure, eIndex) in exposures"
                          :key="eIndex"
                          flat>
        <v-expansion-panel class="py-3">
          <v-expansion-panel-header>
            <strong>{{ exposure.title }}</strong>
          </v-expansion-panel-header>
          <v-expansion-panel-content>
            <h4 class="mb-3"><strong>Assignments</strong></h4>
            <v-simple-table class="mb-9 v-data-table--no-outline v-data-table--light-header">
              <template v-slot:default>
                <thead>
                <tr>
                  <th class="text-left">Assignment Name</th>
                  <th class="text-left">Status</th>
                  <th class="text-left">Submissions</th>
                </tr>
                </thead>
                <tbody>
                <tr
                  v-for="assignment in assignments.filter(a => a.exposureId === exposure.exposureId)"
                  :key="assignment.assignmentId"
                >
                  <td>{{ assignment.title }}</td>
                  <td><span class="completion-status complete">Complete</span></td>
                  <td>23/23</td>
                </tr>
                </tbody>
              </template>
            </v-simple-table>
            <h4 class="mb-3"><strong>Outcomes</strong></h4>
            <v-menu offset-y>
              <template v-slot:activator="{ on, attrs }">
                <v-btn
                  outlined
                  color="primary"
                  v-bind="attrs"
                  v-on="on"
                >
                  Add Outcome
                </v-btn>
              </template>
              <v-list>
                <v-list-item @click="console.log('Select item from gradebook')">
                  <v-list-item-title>Select item from gradebook</v-list-item-title>
                </v-list-item>
                <v-list-item @click="console.log('Manually enter scores for each student')">
                  <v-list-item-title>Manually enter scores for each student</v-list-item-title>
                </v-list-item>
              </v-list>
            </v-menu>
          </v-expansion-panel-content>
        </v-expansion-panel>
      </v-expansion-panels>
    </template>
    <template v-else>
      no experiment
    </template>
  </div>
</template>

<script>
import {mapActions, mapGetters, mapMutations} from "vuex";

export default {
  name: 'ExperimentSummaryStatus',
  props: ['experiment'],
  computed: {
    experiment_id() {
      return parseInt(this.experiment.experimentId)
    },
    ...mapGetters({
      assignments: 'assignment/assignments',
      conditions: 'experiment/conditions',
      exposures: 'exposures/exposures'
    })
  },
  methods: {
    ...mapMutations({
      resetAssignments: 'assignment/resetAssignments',
    }),
    ...mapActions({
      fetchAssignmentsByExposure: 'assignment/fetchAssignmentsByExposure',
      fetchExposures: 'exposures/fetchExposures'
    })
  },
  async created() {
    // reset assignments to get a clean list
    await this.resetAssignments()
    // update assignments on load
    await this.fetchExposures(this.experiment_id)
    for (const e of this.exposures) {
      await this.fetchAssignmentsByExposure([this.experiment_id, e.exposureId])
    }
  }
}
</script>

<style lang="scss" scoped>
  .completion-status {
    &::before {
      content: "";
      display: inline-block;
      background: gray;
      height: 11px;
      width: 11px;
      margin-right: 8px;
      border-radius: 999px;
    }

    &.complete {
      &::before {
        background: #38ADB6;
      }
    }
  }
</style>