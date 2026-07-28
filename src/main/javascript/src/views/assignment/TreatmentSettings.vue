<template>
  <div>
    <div class="mb-5 pb-2">
      <MultipleAttemptsSetting
        v-if="multipleAttemptsSettings"
        v-model="multipleAttemptsSettings"
      />
    </div>

    <div>
      <RevealResponsesSetting
        v-if="revealResponseSettings"
        v-model="revealResponseSettings"
      />
    </div>
  </div>
</template>

<script setup>
import { computed } from "vue";

import MultipleAttemptsSetting from "@/views/assignment/MultipleAttemptsSetting.vue";
import RevealResponsesSetting from "@/views/assignment/RevealResponsesSetting.vue";

import { assessment as assessmentModule } from "@/store/assessment.module";

defineOptions({
  name: "TreatmentSettings"
});

const assessmentStore = assessmentModule();

const assessment = computed(() => {
  return assessmentStore.assessment;
});

const questions = computed(() => {
  return assessmentStore.questions || [];
});

const integration = computed(() => {
  return questions.value.length
    ? questions.value[0].integration
    : null;
});

const revealResponseSettings = computed({
  get() {
    if (!assessment.value) {
      return null;
    }

    return {
      allowStudentViewResponses: assessment.value.allowStudentViewResponses,
      studentViewResponsesAfter: assessment.value.studentViewResponsesAfter,
      studentViewResponsesBefore: assessment.value.studentViewResponsesBefore,
      allowStudentViewCorrectAnswers: assessment.value.allowStudentViewCorrectAnswers,
      studentViewCorrectAnswersAfter: assessment.value.studentViewCorrectAnswersAfter,
      studentViewCorrectAnswersBefore: assessment.value.studentViewCorrectAnswersBefore,
      integration: integration.value
    };
  },

  set(value) {
    assessmentStore.setAssessment({
      ...assessment.value,
      ...value
    });
  }
});

const multipleAttemptsSettings = computed({
  get() {
    if (!assessment.value) {
      return null;
    }

    return {
      allowMultipleAttempts: assessment.value.allowMultipleAttempts,
      numOfSubmissions: assessment.value.numOfSubmissions,
      hoursBetweenSubmissions: assessment.value.hoursBetweenSubmissions,
      multipleSubmissionScoringScheme: assessment.value.multipleSubmissionScoringScheme,
      cumulativeScoringInitialPercentage: assessment.value.cumulativeScoringInitialPercentage
    };
  },

  set(value) {
    assessmentStore.setAssessment({
      ...assessment.value,
      ...value
    });
  }
});
</script>
