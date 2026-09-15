<template>
  <v-card
    class="mt-5 mb-2"
    variant="outlined"
  >
    <v-card-title>
      <v-row>
        <v-col cols="1">
          <span>{{ questionNumber }}</span>
        </v-col>
        <v-col cols="8">
          <YoutubeEventCapture
            :experiment-id="experimentId"
            :assessment-id="assessmentId"
            :condition-id="conditionId"
            :question-id="question.questionId"
            :submission-id="submissionId"
            :treatment-id="treatmentId"
          >
            <span v-html="question.html" />
          </YoutubeEventCapture>
        </v-col>
        <v-col>
          <div
            v-if="!readonly"
            class="total-points text-right ml-2"
          >
            {{ question.points }} Point{{ question.points > 1 ? "s" : "" }}
          </div>
          <div
            v-if="readonly"
            class="total-points text-right ml-2"
          >
            {{ getQuestionSubmissionValue(question) }} /
            {{ question.points }} Point{{ question.points > 1 ? "s" : "" }}
          </div>
        </v-col>
      </v-row>
    </v-card-title>

    <v-card-text v-if="questionValues.length > 0">
      <MultipleChoiceResponseEditor
        v-if="question.questionType === 'MC'"
        v-model="questionValue.answerId"
        :answers="getQuestionAnswers(question)"
        :readonly="readonly"
        :show-answers="showAnswers"
      />

      <EssayResponseEditor
        v-else-if="question.questionType === 'ESSAY'"
        v-model="questionValue.response"
        :answer="getEssayResponse(question)"
        :readonly="readonly"
      />

      <FileUploadResponseEditor
        v-else-if="question.questionType === 'FILE'"
        v-model="questionValue.response"
        :selected-submission="selectedSubmission"
        :file-responses="getFileResponses(question)"
        :selected-download-id="selectedDownloadId"
        :readonly="readonly"
        :submission-id="submissionId"
        :question-id="question.questionId"
        @download-file-response="$emit('download-file-response', $event)"
      />
    </v-card-text>
  </v-card>
</template>

<script setup>
import { computed, watch } from "vue";
import EssayResponseEditor from "@/views/student/EssayResponseEditor.vue";
import MultipleChoiceResponseEditor from "@/views/student/MultipleChoiceResponseEditor.vue";
import FileUploadResponseEditor from "@/views/student/FileUploadResponseEditor.vue";
import YoutubeEventCapture from "@/views/student/YoutubeEventCapture.vue";

const props = defineProps({
  question: { type: Object, required: true },
  questionNumber: { type: Number, required: true },
  questionValues: { type: Array, required: true },
  questionSubmissions: { type: Array, default: () => [] },
  readonly: { type: Boolean, default: false },
  showAnswers: { type: Boolean, default: false },
  selectedSubmission: { type: Object, default: null },
  selectedDownloadId: { type: [String, Number], default: null },
  submissionId: { type: [String, Number], default: null },
  experimentId: { type: [String, Number], required: true },
  assessmentId: { type: [String, Number], default: null },
  conditionId: { type: [String, Number], default: null },
  treatmentId: { type: [String, Number], default: null }
});

const emit = defineEmits(["update:question-values", "download-file-response"]);

const questionValue = computed(() => (props.questionValues ?? []).find(({ questionId }) => questionId === props.question.questionId) || {});

// questionValue above falls back to a disposable {} if this question has no entry
// in questionValues yet - the MC/ESSAY/FILE editors below v-model straight into its
// properties, so any answer typed while it's the fallback silently vanishes the
// moment this computed re-evaluates (the {} is never that same object twice). Ensure
// a real, tracked entry exists as soon as this card is asked to render a given
// question, via the same v-model:question-values StudentQuiz.vue already wires up -
// not a direct props.questionValues.push, since props are otherwise read-only here.
watch(
  () => props.question.questionId,
  () => {
    const exists = (props.questionValues ?? []).some(({ questionId }) => questionId === props.question.questionId);

    if (!exists) {
      emit("update:question-values", [
        ...(props.questionValues ?? []),
        { questionId: props.question.questionId, answerId: null, response: null }
      ]);
    }
  },
  { immediate: true }
);

const questionSubmission = question => props.questionSubmissions?.find(({ questionId }) => questionId === question.questionId);

const getQuestionSubmissionValue = question => {
  const value = questionSubmission(question);
  const score = value?.alteredGrade != null ? value.alteredGrade : value?.calculatedPoints;
  return score ?? 0;
};

const getQuestionAnswers = question => {
  if (!props.readonly) return question.answers;
  const submission = questionSubmission(question);
  if (!submission) return [];
  const answers = submission.answerDtoList || [];
  const responses = submission.answerSubmissionDtoList || [];
  return answers.map(answer => ({
    ...answer,
    studentResponse: responses.find(response => response.answerId === answer.answerId)?.answerId || false
  }));
};

const getEssayResponse = question => {
  if (!props.readonly) return null;
  const submission = questionSubmission(question);
  return submission?.answerSubmissionDtoList?.find(answer => answer.questionSubmissionId === submission.questionSubmissionId) || null;
};

const getFileResponses = question => {
  if (!props.readonly) return null;
  return questionSubmission(question)?.answerSubmissionDtoList || null;
};
</script>
