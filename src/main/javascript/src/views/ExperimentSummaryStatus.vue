<template>
  <div class="experiment-summary-status">
    <template
      v-if="experiment"
    >
      <v-expansion-panels
        v-if="experiment.consent"
        class="v-expansion-panels--outlined mb-7"
        flat
      >
        <v-expansion-panel
          class="py-3"
        >
          <v-expansion-panel-header>
            <strong>Consent</strong>
          </v-expansion-panel-header>
          <v-expansion-panel-content>
            <v-simple-table
              class="mb-9 v-data-table--no-outline v-data-table--light-header"
            >
              <template v-slot:default>
                <thead>
                  <tr>
                    <th
                      class="text-left"
                    >
                      Component Name
                    </th>
                    <th
                      class="text-left"
                    >
                      Status
                    </th>
                    <th
                      class="text-left"
                    >
                      Submissions
                    </th>
                  </tr>
                </thead>
                <tbody>
                <tr>
                  <td>{{ experiment.consent.title }}</td>
                  <td>
                    <span
                      class="completion-status"
                      :class="{
                        'complete': experiment.consent.answeredConsentCount >= experiment.consent.expectedConsent && experiment.consent.answeredConsentCount > 0
                      }"
                    >
                      {{ experiment.consent.answeredConsentCount >= experiment.consent.expectedConsent && experiment.consent.answeredConsentCount > 0 ? "Complete" : "In Progress" }}
                    </span>
                  </td>
                  <td>
                    {{ experiment.consent.answeredConsentCount }}/{{ experiment.consent.expectedConsent }}
                  </td>
                </tr>
                </tbody>
              </template>
            </v-simple-table>
          </v-expansion-panel-content>
        </v-expansion-panel>
      </v-expansion-panels>

      <v-expansion-panels
        v-for="(exposure, eIndex) in exposures"
        :key="eIndex"
        class="v-expansion-panels--outlined mb-7"
        flat
      >
        <v-expansion-panel
          class="py-3"
        >
          <v-expansion-panel-header>
            <strong>{{ exposure.title }}</strong>
          </v-expansion-panel-header>
          <v-expansion-panel-content>
            <h4
              class="mb-3"
            >
              <strong>Components</strong>
            </h4>
            <v-simple-table
              class="mb-9 v-data-table--no-outline v-data-table--light-header"
            >
              <template
                v-slot:default
              >
                <thead>
                  <tr>
                    <th
                      class="text-left"
                    >
                      Component Name
                    </th>
                    <th
                      class="text-left"
                    >
                      Status
                    </th>
                    <th
                      class="text-left"
                    >
                      Submissions
                    </th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="assignment in assignments.filter(a => a.exposureId === exposure.exposureId)"
                    :key="assignment.assignmentId"
                  >
                    <td>
                      <a
                        class="link-view-assignment"
                        @click="handleViewAssignment(exposure.exposureId, assignment.assignmentId)"
                      >
                        {{ assignment.title }}
                      </a>
                    </td>
                    <td>
                      <span
                        class="completion-status"
                        :class="{
                          'complete': assignmentCompletion.find(a => a.assignmentId === assignment.assignmentId).complete
                        }"
                      >
                        {{ assignmentCompletion.find(a => a.assignmentId === assignment.assignmentId).complete ? "Complete" : "In Progress" }}
                      </span>
                    </td>
                    <td>
                      {{ assignmentCompletion && assignmentCompletion.find(a => a.assignmentId === assignment.assignmentId).submissionsCompleted || 0 }}
                      /
                      {{ assignmentCompletion && assignmentCompletion.find(a => a.assignmentId === assignment.assignmentId).submissionsExpected || 0 }}
                    </td>
                  </tr>
                </tbody>
              </template>
            </v-simple-table>
            <h4
              class="mb-3"
            >
              <strong>Outcomes</strong>
            </h4>
            <v-simple-table
              v-if="experimentOutcomes && experimentOutcomes.length"
              class="mb-9 v-data-table--no-outline v-data-table--light-header"
            >
              <template
                v-slot:default
              >
                <thead>
                  <tr>
                    <th
                      class="text-left"
                    >
                      Outcome Name
                    </th>
                    <th
                      class="text-left"
                    >
                      Source
                    </th>
                    <th
                      class="text-left"
                    >
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="outcome in experimentOutcomes"
                    :key="outcome.outcomeId"
                  >
                    <template
                      v-if="outcome.exposureId === exposure.exposureId && outcome.title"
                    >
                      <template>
                        <td>{{ outcome.title }}</td>
                      </template>
                      <template
                        v-if="!outcome.external"
                      >
                        <td>Manual Entry</td>
                      </template>
                      <template
                        v-else
                      >
                        <td>Gradebook</td>
                      </template>
                      <td>
                        <v-menu>
                          <template
                            v-slot:activator="{ on, attrs }"
                          >
                            <v-icon
                              v-bind="attrs"
                              v-on="on"
                              color="black"
                            >
                              mdi-dots-horizontal
                            </v-icon>
                          </template>
                          <v-list class="text-left">
                            <v-list-item
                              v-if="!outcome.external"
                              :to="{
                                name:'OutcomeScoring',
                                params: {
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
                              <v-list-item-title>Delete outcome</v-list-item-title>
                            </v-list-item>
                          </v-list>
                        </v-menu>
                      </td>
                    </template>
                  </tr>
                </tbody>
              </template>
            </v-simple-table>
            <v-menu
              offset-y
            >
              <template
                v-slot:activator="{ on, attrs }"
              >
                <v-btn
                  v-bind="attrs"
                  v-on="on"
                  color="primary"
                  outlined
                >
                  Add Outcome
                </v-btn>
              </template>
              <v-list>
                <v-list-item
                  :to="{
                    name:'OutcomeGradebook',
                    params: {
                      exposureId: exposure.exposureId
                    }
                  }"
                >
                  <v-list-item-title>Select item from gradebook</v-list-item-title>
                </v-list-item>
                <v-list-item
                  @click="handleCreateOutcome(exposure.exposureId, false)"
                >
                  <v-list-item-title>Manually enter scores for each student</v-list-item-title>
                </v-list-item>
              </v-list>
            </v-menu>
          </v-expansion-panel-content>
        </v-expansion-panel>
      </v-expansion-panels>
    </template>
    <template
      v-else
    >
      no experiment
    </template>
  </div>
</template>

<script>
import {mapActions, mapGetters, mapMutations} from "vuex";

export default {
  name: "ExperimentSummaryStatus",
  props: {
    experiment: {
      type: Object,
      required: true
    }
  },
  computed: {
    experimentId() {
      return parseInt(this.experiment.experimentId)
    },
    ...mapGetters({
      assignments: "assignment/assignments",
      conditions: "experiment/conditions",
      exposures: "exposures/exposures",
      experimentOutcomes: "outcome/experimentOutcomes",
      editMode: "navigation/editMode"
    }),
    getSaveExitPage() {
      return this.editMode?.callerPage?.name || "Home";
    },
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
      resetAssignments: "assignment/resetAssignments",
    }),
    ...mapActions({
      fetchAssignmentsByExposure: "assignment/fetchAssignmentsByExposure",
      fetchExposures: "exposures/fetchExposures",
      fetchOutcomesByExposures: "outcome/fetchOutcomesByExposures",
      fetchOutcomeScores: "outcome/fetchOutcomeScores",
      createOutcome: "outcome/createOutcome",
      deleteOutcome: "outcome/deleteOutcome",
      saveEditMode: "navigation/saveEditMode"
    }),
    async handleCreateOutcome(exposureId, external) {
      try {
        const outcome = await this.createOutcome([this.experimentId, exposureId, "", 0, external])
        const routeName = (external) ? "OutcomeGradebook" : "OutcomeScoring"
        this.$router.push({
          name:routeName,
          params: {
            exposureId,
            outcomeId: outcome.outcomeId
          }
        })
      } catch(error) {
        console.error({error})
      }
    },
    async handleDeleteOutcome(exposureId, outcomeId) {
      const reallyDelete = await this.$swal({
        icon: "question",
        text: `Do you really want to delete?`,
        showCancelButton: true,
        confirmButtonText: "Yes, delete it",
        cancelButtonText: "No, cancel",
      })
      // if confirmed, delete experiment
      if (reallyDelete.isConfirmed) {
        try {
          await this.deleteOutcome([this.experimentId, exposureId, outcomeId])
          this.fetchOutcomesByExposures([this.experimentId, [...new Set(this.exposures.map(item => item.exposureId))]])
        } catch (error) {
          console.error("handleDeleteOutcome | catch", {error})
          this.$swal({
            text: "Could not delete outcome.",
            icon: "error"
          })
        }
      }
    },
    async handleViewAssignment(exposureId, assignmentId) {
      await this.saveEditMode({
        initialPage: "ExperimentSummaryStatus",
        callerPage: {
          name: "ExperimentSummary",
          tab: "status"
        }
      });
      this.$router.push({
        name: "AssignmentScores",
        params: {
          experimentId: this.experimentId,
          exposureId: exposureId,
          assignmentId: assignmentId
        }
      });
    }
  },
  async created() {
    // reset assignments to get a clean list
    await this.resetAssignments()
    // update assignments on load
    await this.fetchExposures(this.experimentId)
    for (const e of this.exposures) {
      // add submissions to assignments request
      const submissions = true
      await this.fetchAssignmentsByExposure([this.experimentId, e.exposureId, submissions])
    }
    this.fetchOutcomesByExposures([this.experimentId, [...new Set(this.exposures.map(item => item.exposureId))]])
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
a.link-view-assignment {
  text-decoration: underline;
}
</style>
