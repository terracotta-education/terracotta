<template>
  <div v-if="experiment && assignment && submissions">
    <h1 class="mb-6">{{ assignment.title }}</h1>
    <template>
      <div v-for="(selectedTreatment, index) in selectedAssignmentTreatments" :key="selectedTreatment.treatmentId" class="mt-6">
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
                <template v-for="(participant, pidx) in getParticipantWithSubmission(participants, selectedTreatment)">
                  <template v-if="participant.submission">
                    <tr :key="pidx">
                      <td>
                        <router-link
                          :to="{
                            name: 'StudentSubmissionGrading',
                            params: {
                              exposure_id: exposure_id,
                              assignment_id: assignment_id,
                              assessment_id: participant.submission.assessmentId,
                              condition_id: participant.submission.conditionId,
                              treatment_id: participant.submission.treatmentId,
                              participant_id: participant.participantId,
                            },
                          }"
                        >
                          {{ participant.user.displayName }}
                        </router-link>
                      </td>
                      <td>
                        <span v-if="participant.submission.totalAlteredGrade !== null">{{ participant.submission.totalAlteredGrade }}</span>
                        <span v-else>{{ participant.submission.alteredCalculatedGrade }}</span>
                      </td>
                    </tr>
                  </template>
                </template>
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
    };
  },
  methods: {
    ...mapActions({
      fetchParticipants: "participants/fetchParticipants",
      fetchAssignment: "assignment/fetchAssignment",
      updateSubmission: "submissions/updateSubmission",
      reportStep: "api/reportStep",
    }),
    getParticipantWithSubmission(participants, treatment) {
      return participants.map(p => {
        const subs = treatment.assessmentDto.submissions;
        const psubs = subs.filter(s => s.participantId === p.participantId);
        const latest = this.getLatestSubmissionFromSet(psubs);
        return {
          ...p,
          submission: latest
        }
      });
    },
    getLatestSubmissionFromSet(subs) {
      if (!subs.length) {
        return null;
      }
      if (subs.length < 2) {
        return subs[0];
      }
      return [...subs].sort((a, b) => a.dateSubmitted - b.dateSubmitted).reverse()[0];
    },
    getParticipantName(participantId) {
      return this.participants?.filter(
        (participant) => participant.participantId === participantId
      )[0]?.user.displayName;
    },
    async saveExit() {
      try {
        this.$router.push({
          name: this.$router.currentRoute.meta.previousStep,
        });
      } catch (error) {
        this.$swal("There was a problem saving assignment scores.");
      }
    },
  },
  async mounted() {
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
        console.log(submission.submissionId)
        submissions[submission.submissionId] = clone(submission);
      }
    }
    this.submissions = submissions;
  },
};
</script>
