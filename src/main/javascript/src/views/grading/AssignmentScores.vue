<template>
  <div
    v-if="experiment && assignment"
  >
    <h1
      class="mb-6"
    >
      {{ assignment.title }}
    </h1>
    <template>
      <div
        v-for="(selectedTreatment, index) in selectedAssignmentTreatments"
        :key="selectedTreatment.treatmentId"
        class="mt-6"
      >
        <h3>
          {{ selectedTreatment.assessmentDto.title }}
        </h3>
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
                    class="text-left"
                  >
                    Student Name
                  </th>
                  <th
                    class="text-left"
                    style="width:250px;"
                  >
                    Score (out of {{ selectedTreatment.assessmentDto.maxPoints }})
                  </th>
                </tr>
              </thead>
              <tbody>
                <template
                  v-for="(participant, pidx) in getParticipantWithSubmission(participants, selectedTreatment)"
                >
                  <template
                    v-if="participant.submission"
                  >
                    <tr
                      :key="pidx"
                    >
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
                        <span>{{ participant.scoreToDisplay }}</span>
                      </td>
                    </tr>
                  </template>
                </template>
              </tbody>
            </template>
          </v-simple-table>
        </form>
        <template
          v-if="index !== selectedAssignmentTreatments.length - 1"
        >
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
      editMode: "navigation/editMode"
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
    getSaveExitPage() {
      return this.editMode?.callerPage?.name || "ExperimentSummaryStatus";
    },
  },
  methods: {
    ...mapActions({
      fetchParticipants: "participants/fetchParticipants",
      fetchAssignment: "assignment/fetchAssignment"
    }),
    getParticipantWithSubmission(participants, treatment) {
      return participants.map(p => {
        const subs = treatment.assessmentDto.submissions;
        const psubs = subs.filter(s => s.participantId === p.participantId);
        const scoreToDisplay = this.calculateScore(psubs, treatment.assessmentDto.multipleSubmissionScoringScheme);
        const latest = this.getLatestSubmissionFromSet(psubs);
        return {
          ...p,
          submission: latest,
          scoreToDisplay: scoreToDisplay
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
      return this.participants?.filter((participant) => participant.participantId === participantId)[0]?.user.displayName;
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
    calculateScore(participantSubmissions, scheme) {
      // scores sorted by date descending
      const scores = participantSubmissions
        .sort((a, b) => a.dateSubmitted - b.dateSubmitted).reverse()
        .map((ps) => ps.gradeOverridden ? ps.totalAlteredGrade : ps.alteredCalculatedGrade);

      if (!scores.length) {
        return "N/A";
      }

      switch(scheme) {
        case "AVERAGE":
          return this.round((scores.reduce((a, b) => a + b, 0)) / scores.length);
        case "HIGHEST":
          return Math.max(...scores);
        case "MOST_RECENT":
        default:
          // latest score is first in array
          return scores[0];
      }
    },
    round(n) {
      return n % 1 ? n.toFixed(2) : n;
    },
    async loadData() {
      await this.fetchAssignment([
        this.experiment_id,
        this.exposure_id,
        this.assignment_id,
        true,
      ]);
      await this.fetchParticipants(this.experiment_id);
    }
  },
  beforeRouteUpdate() {
    this.loadData();
  },
  async mounted() {
    this.loadData();
  },
};
</script>
