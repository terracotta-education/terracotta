<template>
  <div>
    <div
      class="mb-5 pb-2"
    >
      <multiple-attempts-setting
        v-model="multipleAttemptsSettings"
      />
    </div>
    <div>
      <reveal-responses-setting
        v-model="revealResponseSettings"
      />
    </div>
  </div>
</template>

<script>
import { mapGetters, mapMutations } from "vuex";
import MultipleAttemptsSetting from "./MultipleAttemptsSetting.vue";
import RevealResponsesSetting from "./RevealResponsesSetting.vue";

export default {
  components: {
    RevealResponsesSetting,
    MultipleAttemptsSetting,
  },
  computed: {
    ...mapGetters({
      assessment: "assessment/assessment",
      questions: "assessment/questions",
    }),
    integration() {
      return this.questions.length ? this.questions[0].integration : null;
    },
    isIntegration() {
      return this.integration !== null;
    },
    revealResponseSettings: {
      get() {
        return this.assessment
          ? {
              allowStudentViewResponses: this.assessment.allowStudentViewResponses,
              studentViewResponsesAfter: this.assessment.studentViewResponsesAfter,
              studentViewResponsesBefore: this.assessment.studentViewResponsesBefore,
              allowStudentViewCorrectAnswers: this.assessment.allowStudentViewCorrectAnswers,
              studentViewCorrectAnswersAfter: this.assessment.studentViewCorrectAnswersAfter,
              studentViewCorrectAnswersBefore: this.assessment.studentViewCorrectAnswersBefore,
              integration: this.integration
            }
          : null;
      },
      set(value) {
        this.setAssessment({ ...this.assessment, ...value });
      },
    },
    multipleAttemptsSettings: {
      get() {
        return this.assessment
          ? {
              allowMultipleAttempts: this.assessment.allowMultipleAttempts,
              numOfSubmissions: this.assessment.numOfSubmissions,
              hoursBetweenSubmissions: this.assessment.hoursBetweenSubmissions,
              multipleSubmissionScoringScheme: this.assessment.multipleSubmissionScoringScheme,
              cumulativeScoringInitialPercentage: this.assessment.cumulativeScoringInitialPercentage,
            }
          : null;
      },
      set(value) {
        this.setAssessment({ ...this.assessment, ...value });
      },
    },

  },
  methods: {
    ...mapMutations({
      setAssessment: "assessment/setAssessment",
    }),
  },
};
</script>

<style lang="scss" scoped></style>
