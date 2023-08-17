<template>
  <div>
    <div class="mb-5 pb-2">
      <multiple-attempts-setting v-model="multipleAttemptsSettings" />
    </div>
    <div>
      <reveal-responses-setting v-model="revealResponseSettings" />
    </div>
  </div>
</template>

<script>
import { mapGetters, mapMutations } from "vuex";
import MultipleAttemptsSetting from "./MultipleAttemptsSetting.vue"
import RevealResponsesSetting from "./RevealResponsesSetting.vue";

export default {
  components: {
    RevealResponsesSetting,
    MultipleAttemptsSetting,
  },
  computed: {
    ...mapGetters({
      assignment: "assignment/assignment",
    }),
    revealResponseSettings: {
      // two-way computed property
      get() {
        return this.assignment
          ? {
              allowStudentViewResponses: this.assignment.allowStudentViewResponses,
              studentViewResponsesAfter: this.assignment.studentViewResponsesAfter,
              studentViewResponsesBefore: this.assignment.studentViewResponsesBefore,
              allowStudentViewCorrectAnswers: this.assignment.allowStudentViewCorrectAnswers,
              studentViewCorrectAnswersAfter: this.assignment.studentViewCorrectAnswersAfter,
              studentViewCorrectAnswersBefore: this.assignment.studentViewCorrectAnswersBefore
            }
          : null;
      },
      set(value) {
        this.setAssignment({ ...this.assignment, ...value });
      },
    },
    multipleAttemptsSettings: {
      // two-way computed property
      get() {
        return this.assignment
          ? {
              allowMultipleAttempts: this.assignment.allowMultipleAttempts,
              numOfSubmissions: this.assignment.numOfSubmissions,
              hoursBetweenSubmissions: this.assignment.hoursBetweenSubmissions,
              multipleSubmissionScoringScheme: this.assignment.multipleSubmissionScoringScheme,
              cumulativeScoringInitialPercentage: this.assignment.cumulativeScoringInitialPercentage
            }
          : null;
      },
      set(value) {
        this.setAssignment({ ...this.assignment, ...value });
      },
    },

  },
  methods: {
    ...mapMutations({
      setAssignment: "assignment/setAssignment",
    }),
  },
};
</script>

<style lang="scss" scoped></style>
