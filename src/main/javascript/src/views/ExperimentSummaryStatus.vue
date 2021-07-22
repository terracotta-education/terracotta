<template>
  <div class="experiment-summary-status" >
    <template v-if="experiment">
      <h1>Experiment Status</h1>
      <p>Once your experiment is running, you will see status updates below</p>

<!--      WIP -->
<!--      <v-expansion-panels class="v-expansion-panels&#45;&#45;outlined mb-7" flat>-->
<!--        <v-expansion-panel class="py-3">-->
<!--          <v-expansion-panel-header>-->
<!--            <strong>Consent</strong>-->
<!--          </v-expansion-panel-header>-->
<!--          <v-expansion-panel-content>-->
<!--            <v-simple-table class="mb-9 v-data-table&#45;&#45;no-outline v-data-table&#45;&#45;light-header">-->
<!--              <template v-slot:default>-->
<!--                <thead>-->
<!--                <tr>-->
<!--                  <th class="text-left">Assignment Name</th>-->
<!--                  <th class="text-left">Status</th>-->
<!--                  <th class="text-left">Submissions</th>-->
<!--                </tr>-->
<!--                </thead>-->
<!--                <tbody>-->
<!--&lt;!&ndash;                WIP&ndash;&gt;-->
<!--&lt;!&ndash;                <tr&ndash;&gt;-->
<!--&lt;!&ndash;                  v-for="assignment in assignments.filter(a => a.exposureId === exposure.exposureId)"&ndash;&gt;-->
<!--&lt;!&ndash;                  :key="assignment.assignmentId"&ndash;&gt;-->
<!--&lt;!&ndash;                >&ndash;&gt;-->
<!--                <tr>-->
<!--                  <td>Informed Consent</td>-->
<!--                  <td><span class="completion-status">In Progress</span></td>-->
<!--                  <td>0/0</td>-->
<!--                </tr>-->
<!--                </tbody>-->
<!--              </template>-->
<!--            </v-simple-table>-->
<!--          </v-expansion-panel-content>-->
<!--        </v-expansion-panel>-->
<!--      </v-expansion-panels>-->

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
                  <td><span class="completion-status" :class="{'complete': assignmentCompletion.find(a => a.assignmentId === assignment.assignmentId).complete}">
                    {{
                      assignmentCompletion.find(a => a.assignmentId === assignment.assignmentId).complete ?
                        'Complete' : 'In Progress'
                    }}
                  </span></td>
                  <td>
                    {{ assignmentCompletion && assignmentCompletion.find(a => a.assignmentId === assignment.assignmentId).submissionsCompleted || 0 }}
                    /
                    {{ assignmentCompletion && assignmentCompletion.find(a => a.assignmentId === assignment.assignmentId).submissionsExpected || 0 }}
                  </td>
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
    }),
    assignmentCompletion() {
      let arr = []
      for (const assignment of this.assignments) {
        let complete = false
        let counts = {
          submissionsExpectedCount: 0,
          submissionsCompletedCount: 0,
          submissionsInProgressCount: 0
        }

        // add assessment submission counts for each assignment
        if (assignment.treatments?.length > 1) {
          counts = assignment.treatments.reduce((prev, cur)=> {
            return {
              submissionsExpectedCount: prev?.assessmentDto?.submissionsExpected+cur?.assessmentDto?.submissionsExpected || 0,
              submissionsCompletedCount: prev?.assessmentDto?.submissionsCompletedCount+cur?.assessmentDto?.submissionsCompletedCount || 0,
              submissionsInProgressCount: prev?.assessmentDto?.submissionsInProgressCount+cur?.assessmentDto?.submissionsInProgressCount || 0
            }
          })
        } else if (assignment.treatments?.length === 1) {
          counts = {
            submissionsExpectedCount: assignment.treatments[0].assessmentDto?.submissionsExpected || 0,
            submissionsCompletedCount: assignment.treatments[0].assessmentDto?.submissionsCompletedCount || 0,
            submissionsInProgressCount: assignment.treatments[0].assessmentDto?.submissionsInProgressCount || 0
          }
        }

        // check if assignment is complete based on completed vs expected submissions
        if (
          counts.submissionsCompleted >= counts.submissionsExpected &&
          counts.submissionsCompleted > 0
        ) {
          complete = true
        }

        arr.push({
          assignmentId: assignment.assignmentId,
          submissionsCompleted: counts.submissionsCompletedCount,
          submissionsExpected: counts.submissionsExpectedCount,
          submissionsInProgress: counts.submissionsInProgressCount,
          complete: complete
        })
      }
      return arr
    }
  },
  methods: {
    ...mapMutations({
      resetAssignments: 'assignment/resetAssignments',
    }),
    ...mapActions({
      fetchAssignmentsByExposure: 'assignment/fetchAssignmentsByExposure',
      fetchExposures: 'exposures/fetchExposures'
    }),
  },
  async created() {
    // reset assignments to get a clean list
    await this.resetAssignments()
    // update assignments on load
    await this.fetchExposures(this.experiment_id)
    for (const e of this.exposures) {
      // add submissions to assignments request
      const submissions = true
      await this.fetchAssignmentsByExposure([this.experiment_id, e.exposureId, submissions])
    }
  }
}
</script>

<style lang="scss" scoped>
  .completion-status {
    &::before {
      content: "";
      display: inline-block;
      background: #FFE0B2;
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