<template>
  <div v-if="experiment && assignment">
    <h1 class="mb-6">{{ assignment.title }}</h1>
    <template v-for="(selectedTreatment, index) in selectedAssignmentTreatments">
      <div :key="selectedTreatment.treatmentId" class="mt-6">
        <h3>
          {{ selectedTreatment.assessmentDto.title }}
        </h3>
        <form @submit.prevent="saveExit">
          <v-simple-table class="mb-9 v-data-table--light-header">
            <template v-slot:default>
              <thead>
                <tr>
                  <th class="text-left">Student Name</th>
                  <th class="text-left" style="width:250px;">
                    Score (out of {{ selectedTreatment.assessmentDto.maxPoints }})
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="submission in selectedTreatment.assessmentDto
                    .submissions"
                  :key="submission.submissionId"
                >
                  <td>
                    <router-link
                      :to="{
                        name: 'StudentSubmissionGrading',
                        params: {
                          exposure_id: exposure_id,
                          assignment_id: assignment_id,
                          assessment_id: submission.assessmentId,
                          condition_id: submission.conditionId,
                          treatment_id: submission.treatmentId,
                          participant_id: submission.participantId,
                          submission_id: submission.submissionId,
                        },
                      }"
                    >
                      {{
                        getParticipantName(submission.participantId, submission)
                      }}
                    </router-link>
                  </td>
                  <td>
                    <v-text-field
                      type="number"
                      class="ml-10"
                      placeholder="---"
                      style="max-width: 50px;"
                      required
                      v-model="
                        resultValues[submission.submissionId].totalAlteredGrade
                      "
                    ></v-text-field>
                  </td>
                </tr>
              </tbody>
            </template>
          </v-simple-table>
        </form>
        <template v-if="index !== selectedAssignmentTreatments.length-1">
          <hr />
        </template>
      </div>
    </template>
  </div>
</template>

<script>
import { mapActions, mapGetters } from "vuex";

export default {
  name: "AssignmentScores",
  computed: {
    ...mapGetters({
      experiment: "experiment/experiment",
      assignment: "assignment/assignment",
      participants: "participants/participants",
    }),
    assignment_id() {
      return parseInt(this.$route.params.assignment_id);
    },
    exposure_id() {
      return parseInt(this.$route.params.exposure_id);
    },
    experiment_id() {
      return parseInt(this.$route.params.experiment_id);
    },
    selectedAssignmentTreatments() {
      return this.assignment.treatments;
    },
  },
  data() {
    return {
      resultValues: {},
    };
  },
  methods: {
    ...mapActions({
      fetchParticipants: "participants/fetchParticipants",
      fetchAssignment: "assignment/fetchAssignment",
      fetchSubmissions: "submissions/fetchSubmissions",
      updateSubmission: "submissions/updateSubmission",
      reportStep: "api/reportStep",
    }),
    getParticipantName(participantId, submission) {
      this.resultValues[submission.submissionId] = submission;
      return this.participants?.filter(
        (participant) => participant.participantId === participantId
      )[0]?.user.displayName;

    },
    async getSubmissions(experimentId, conditionId, treatmentId, assessmentId) {
      const submissions = await this.fetchSubmissions(
        experimentId,
        conditionId,
        treatmentId,
        assessmentId
      );
      this.resultValues[treatmentId] = submissions;
      return submissions;
    },
    async saveExit() {
      // Update Scores and send it to backend
      Promise.all(
        Object.values(this.resultValues).map(async (value) => {
          try {
            const submission = await this.updateSubmission([
              value.experimentId,
              value.conditionId,
              value.treatmentId,
              value.assessmentId,
              value.submissionId,
              value.alteredCalculatedGrade,
              value.totalAlteredGrade,
            ]);

            // Post Step to Experiment
            await this.reportStep({
              experimentId: value.experimentId,
              step: "student_submission",
              parameters: { submissionIds: "" + value.submissionId },
            });

            return Promise.resolve(submission);
          } catch (error) {
            return Promise.reject(error);
          }
        })
      );
      
      this.$router.push({ name: this.$router.currentRoute.meta.previousStep });
    },
  },
  async created() {
    await this.fetchAssignment([
      this.experiment_id,
      this.exposure_id,
      this.assignment_id,
      true,
    ]);
    await this.fetchParticipants(this.experiment_id);
  },
};
</script>
