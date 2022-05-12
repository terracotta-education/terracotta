<template>
  <div v-if="experiment && assignment && submissions">
    <h1 class="mb-6">{{ assignment.title }}</h1>
    <template
      v-for="(selectedTreatment, index) in selectedAssignmentTreatments"
    >
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
                    Score (out of
                    {{ selectedTreatment.assessmentDto.maxPoints }})
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
                      {{ getParticipantName(submission.participantId) }}
                    </router-link>
                  </td>
                  <td>
                    <v-text-field
                      type="number"
                      class="ml-10"
                      placeholder="---"
                      style="max-width: 50px;"
                      required
                      :value="
                        submissions[submission.submissionId].totalAlteredGrade
                      "
                      @input="
                        updateTotalAlteredGrade(submission.submissionId, $event)
                      "
                    ></v-text-field>
                  </td>
                </tr>
              </tbody>
            </template>
          </v-simple-table>
        </form>
        <template v-if="index !== selectedAssignmentTreatments.length - 1">
          <hr />
        </template>
      </div>
    </template>
  </div>
</template>

<script>
import { clone } from "@/helpers";
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
      submissions: null,
      updatedSubmissions: {},
    };
  },
  methods: {
    ...mapActions({
      fetchParticipants: "participants/fetchParticipants",
      fetchAssignment: "assignment/fetchAssignment",
      updateSubmission: "submissions/updateSubmission",
      reportStep: "api/reportStep",
    }),
    getParticipantName(participantId) {
      return this.participants?.filter(
        (participant) => participant.participantId === participantId
      )[0]?.user.displayName;
    },
    async saveExit() {
      try {
        for (const submission of Object.values(this.updatedSubmissions)) {
          await this.updateSubmission([
            submission.experimentId,
            submission.conditionId,
            submission.treatmentId,
            submission.assessmentId,
            submission.submissionId,
            submission.alteredCalculatedGrade,
            submission.totalAlteredGrade,
          ]);

          await this.reportStep({
            experimentId: submission.experimentId,
            step: "student_submission",
            parameters: { submissionIds: "" + submission.submissionId },
          });
        }

        this.$router.push({
          name: this.$router.currentRoute.meta.previousStep,
        });
      } catch (error) {
        this.$swal("There was a problem saving assignment scores.");
      }
    },
    updateTotalAlteredGrade(submissionId, value) {
      const submission = this.submissions[submissionId];
      submission.totalAlteredGrade = value;
      // Record that this submission was updated
      this.updatedSubmissions[submissionId] = submission;
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
    const submissions = {};
    for (const treatment of this.assignment.treatments) {
      for (const submission of treatment.assessmentDto.submissions) {
        // Create a clone of each submission that can be mutated
        submissions[submission.submissionId] = clone(submission);
      }
    }
    this.submissions = submissions;
  },
};
</script>
