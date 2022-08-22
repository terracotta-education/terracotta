<template>
  <div>
    <reveal-responses-setting v-model="revealResponseSettings" />
  </div>
</template>

<script>
import { mapGetters, mapMutations } from "vuex";
import RevealResponsesSetting from "./RevealResponsesSetting.vue";
export default {
  components: {
    RevealResponsesSetting,
  },
  computed: {
    ...mapGetters({
      assessment: "assessment/assessment",
    }),
    revealResponseSettings: {
      // two-way computed property
      get() {
        return this.assessment
          ? {
              allowStudentViewResponses: this.assessment
                .allowStudentViewResponses,
              studentViewResponsesAfter: this.assessment
                .studentViewResponsesAfter,
              studentViewResponsesBefore: this.assessment
                .studentViewResponsesBefore,
              allowStudentViewCorrectAnswers: this.assessment
                .allowStudentViewCorrectAnswers,
              studentViewCorrectAnswersAfter: this.assessment
                .studentViewCorrectAnswersAfter,
              studentViewCorrectAnswersBefore: this.assessment
                .studentViewCorrectAnswersBefore,
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
