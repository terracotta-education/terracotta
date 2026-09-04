<template>
  <div>
    <div
      class="mb-5 pb-2"
    >
      <MultipleAttemptsSetting
        v-model="multipleAttemptsSettings"
      />
    </div>

    <div>
      <RevealResponsesSetting
        v-model="revealResponseSettings"
      />
    </div>
  </div>
</template>

<script setup>
import { computed } from "vue";

import MultipleAttemptsSetting from "@/views/assignment/MultipleAttemptsSetting.vue";
import RevealResponsesSetting from "@/views/assignment/RevealResponsesSetting.vue";

import { assignment as assignmentModule } from "@/store/assignment.module";

defineOptions({
  name: "AssignmentSettings"
});

const assignmentStore = assignmentModule();

const assignment = computed(() => {
  return assignmentStore.assignment;
});

const revealResponseSettings = computed({
  get() {
    if (!assignment.value) {
      return null;
    }

    return {
      allowStudentViewResponses: assignment.value.allowStudentViewResponses,
      studentViewResponsesAfter: assignment.value.studentViewResponsesAfter,
      studentViewResponsesBefore: assignment.value.studentViewResponsesBefore,
      allowStudentViewCorrectAnswers: assignment.value.allowStudentViewCorrectAnswers,
      studentViewCorrectAnswersAfter: assignment.value.studentViewCorrectAnswersAfter,
      studentViewCorrectAnswersBefore: assignment.value.studentViewCorrectAnswersBefore
    };
  },

  set(value) {
    assignmentStore.setAssignment({
      ...assignment.value,
      ...value
    });
  }
});

const multipleAttemptsSettings = computed({
  get() {
    if (!assignment.value) {
      return null;
    }

    return {
      allowMultipleAttempts: assignment.value.allowMultipleAttempts,
      numOfSubmissions: assignment.value.numOfSubmissions,
      hoursBetweenSubmissions: assignment.value.hoursBetweenSubmissions,
      multipleSubmissionScoringScheme: assignment.value.multipleSubmissionScoringScheme,
      cumulativeScoringInitialPercentage: assignment.value.cumulativeScoringInitialPercentage
    };
  },

  set(value) {
    assignmentStore.setAssignment({
      ...assignment.value,
      ...value
    });
  }
});
</script>
