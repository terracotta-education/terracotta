<template>
  <v-container
    v-show="pageLoaded && !loading"
    :class="{
      'preview': preview,
      'h-100': preview
    }"
    fluid
  >
    <StudentQuizRetakeBanner
      v-if="!preview"
      :can-try-again="canTryAgain"
      :cant-try-again-message="cantTryAgainMessage"
      :scoring-scheme="assignmentData?.multipleSubmissionScoringScheme"
      @try-again="handleTryAgain"
    />

    <v-row v-if="!preview && showSubmissionDetails">
      <v-spacer />
      <v-col>
        <StudentQuizSubmissionDetails
          :time-before-submission="timeBeforeSubmission"
          :allowed-attempts="allowedAttempts"
          :date-submitted="selectedSubmissionDateSubmitted"
          :current-score="currentScore"
          :kept-score="keptScore"
        />
      </v-col>
    </v-row>

    <StudentQuizReadonlyBanner
      v-if="!preview && readonly"
      :muted="muted"
      :assignment-data="assignmentData"
      @select-submission="selectedSubmissionId = $event"
    />

    <StudentQuizIntegration
      v-if="isIntegration"
      :assessment="assessment"
      :integration="integration"
      :readonly="readonly"
      :submitted="submitted"
      :selected-submission="selectedSubmission"
      :has-resize-message="hasResizeMessage"
    />

    <v-row
      v-if="!isIntegration && assessment && questionValues.length > 0"
      :class="{ 'preview-treatment': preview }"
    >
      <v-col>
        <template v-if="!submitted">
          <div
            v-if="assessment.html && questionPageIndex === 0"
            v-html="assessment.html"
          />

          <form
            ref="form"
            style="width: 100%;"
            @submit.prevent="handleSubmit"
          >
            <div class="answerSection mt-5 w-100">
              <StudentQuizQuestionCard
                v-for="(question, index) in currentQuestionPage.questions"
                :key="question.questionId"
                v-model:question-values="questionValues"
                :question="question"
                :index="index"
                :question-number="currentQuestionPage.questionStartIndex + index + 1"
                :readonly="readonly"
                :show-answers="showAnswers"
                :selected-submission="selectedSubmission"
                :selected-download-id="selectedDownloadId"
                :submission-id="submissionId"
                :experiment-id="experimentId"
                :assessment-id="assessmentId"
                :condition-id="conditionId"
                :treatment-id="treatmentId"
                :question-submissions="questionSubmissions"
                @download-file-response="downloadFileResponse"
              />
            </div>

            <StudentQuizPagination
              :preview="preview"
              :show-back-button="showBackButton"
              :disable-back-button="disableBackButton"
              :show-next-button="showNextButton"
              :disable-next-button="disableNextButton"
              :show-submit-button="showSubmitButton"
              :disable-submit-button="disableSubmitButton"
              :experiment-id="experimentId"
              :condition-id="conditionId"
              :treatment-id="treatmentId"
              :owner-id="ownerId"
              @back="backPage"
              @next="nextPage"
            />
          </form>
        </template>

        <v-alert
          v-if="submitted"
          type="success"
          variant="outlined"
        >
          Your answers have been submitted.
        </v-alert>
      </v-col>
    </v-row>
  </v-container>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from "vue";
import Swal from "sweetalert2";
import dayjs from "@/plugins/dayjs";

import StudentQuizRetakeBanner from "@/views/student/quiz/components/StudentQuizRetakeBanner.vue";
import StudentQuizSubmissionDetails from "@/views/student/quiz/components/StudentQuizSubmissionDetails.vue";
import StudentQuizReadonlyBanner from "@/views/student/quiz/components/StudentQuizReadonlyBanner.vue";
import StudentQuizIntegration from "@/views/student/quiz/components/StudentQuizIntegration.vue";
import StudentQuizQuestionCard from "@/views/student/quiz/components/StudentQuizQuestionCard.vue";
import StudentQuizPagination from "@/views/student/quiz/components/StudentQuizPagination.vue";

import { api as apiModule } from "@/store/api.module";
import { assessment as assessmentModule } from "@/store/assessment.module";
import { submission as submissionsModule } from "@/store/submission.module";
import { preview as previewModule } from "@/store/preview/preview.module";

const props = defineProps({
  experimentId: { type: String, required: true },
  previewConditionId: { type: String, default: null },
  previewTreatmentId: { type: String, default: null },
  ownerId: { type: String, default: null },
  previewId: { type: String, default: null },
  preview: { type: Boolean, default: false }
});

const emit = defineEmits(["loaded", "integrationsTokenAlert"]);

const apiStore = apiModule();
const assessmentStore = assessmentModule();
const submissionsStore = submissionsModule();
const previewStore = previewModule();

const form = ref(null);
const questionValues = ref([]);
const conditionId = ref(null);
const treatmentId = ref(null);
const submissionId = ref(null);
const assessmentId = ref(null);
const submitted = ref(false);
const questionPageIndex = ref(0);
const assignmentData = ref(null);
const selectedSubmissionId = ref(null);
const readonly = ref(false);
const loading = ref(false);
const pageLoaded = ref(false);
const submissions = ref([]);
const downloadId = ref(null);
const treatment = ref(null);
const hasResizeMessage = ref(false);

const integration = ref({
  launchUrl: null,
  token: {
    countdownInterval: null,
    expirationDate: null,
    expirationDateCheckInterval: null,
    expirationRemaining: null,
    warningPeriod: null,
    alert: {
      date: null,
      display: null,
      type: null,
      types: {
        initial: "initial",
        warning: "warning",
        expired: "expired"
      }
    }
  }
});

const assessment = computed(() => assessmentStore.assessment || {});
const answerableQuestions = computed(() => assessmentStore.answerableQuestions || []);
const questionPages = computed(() => assessmentStore.questionPages || []);
const questionSubmissions = computed(() => submissionsStore.questionSubmissions || []);

const currentQuestionPage = computed(() => questionPages.value[questionPageIndex.value] || { questions: [], questionStartIndex: 0 });
const hasNextQuestionPage = computed(() => questionPageIndex.value < questionPages.value.length - 1);
const allCurrentPageQuestionsAnswered = computed(() => areAllQuestionsAnswered(currentQuestionPage.value.questions));
const allQuestionsAnswered = computed(() => areAllQuestionsAnswered(answerableQuestions.value));

const showNextButton = computed(() => readonly.value ? questionPages.value.length > 1 : hasNextQuestionPage.value);
const disableNextButton = computed(() => readonly.value ? !hasNextQuestionPage.value : !allCurrentPageQuestionsAnswered.value);
const showBackButton = computed(() => (props.preview || readonly.value) && questionPages.value.length > 1);
const hasBackQuestionPage = computed(() => (props.preview || readonly.value) && questionPageIndex.value > 0);
const disableBackButton = computed(() => !hasBackQuestionPage.value);
const showSubmitButton = computed(() => !readonly.value && !hasNextQuestionPage.value);
const disableSubmitButton = computed(() => !allQuestionsAnswered.value);

const canTryAgain = computed(() => readonly.value && Boolean(assignmentData.value?.retakeDetails?.retakeAllowed));
const showSubmissionDetails = computed(() => readonly.value || submitted.value);
const cantTryAgainMessage = computed(() => assignmentData.value?.retakeDetails?.retakeNotAllowedReason);
const allowedAttempts = computed(() => {
  if (!assignmentData.value) return " - ";
  const { numOfSubmissions } = assignmentData.value;
  return numOfSubmissions === null ? 1 : numOfSubmissions === 0 ? "Unlimited" : numOfSubmissions;
});
const selectedSubmission = computed(() => assignmentData.value?.submissions?.find(s => s.submissionId === selectedSubmissionId.value));
const selectedSubmissionDateSubmitted = computed(() => selectedSubmission.value?.dateSubmitted ? dayjs(selectedSubmission.value.dateSubmitted).format("MMMM Do YYYY hh:mm") : "");
const timeBeforeSubmission = computed(() => {
  const time = selectedSubmission.value?.dateSubmitted - selectedSubmission.value?.dateCreated;
  return Number.isNaN(time) ? "" : dayjs.duration(time, "milliseconds").humanize();
});
const currentScore = computed(() => {
  const grade = selectedSubmission.value
    ? (selectedSubmission.value.totalAlteredGrade ?? selectedSubmission.value.alteredCalculatedGrade)
    : assignmentData.value?.retakeDetails?.lastAttemptScore;
  return `${round(grade)} / ${assignmentData.value?.maxPoints}`;
});
const keptScore = computed(() => `${assignmentData.value?.retakeDetails?.keptScore ? round(assignmentData.value.retakeDetails.keptScore) : 0} / ${assignmentData.value?.maxPoints}`);
const muted = computed(() => {
  if (!assignmentData.value) return true;
  const { allowStudentViewResponses, studentViewResponsesAfter, studentViewResponsesBefore } = assignmentData.value;
  if (!allowStudentViewResponses) return true;
  const now = Date.now();
  const isAfter = studentViewResponsesAfter ? dayjs(now).isAfter(studentViewResponsesAfter) : true;
  const isBefore = studentViewResponsesBefore ? dayjs(now).isBefore(studentViewResponsesBefore) : true;
  return !(isAfter && isBefore);
});
const showAnswers = computed(() => {
  if (!assignmentData.value) return false;
  const { allowStudentViewCorrectAnswers, studentViewCorrectAnswersAfter, studentViewCorrectAnswersBefore } = assignmentData.value;
  if (!allowStudentViewCorrectAnswers) return false;
  const now = Date.now();
  const isAfter = studentViewCorrectAnswersAfter ? dayjs(now).isAfter(studentViewCorrectAnswersAfter) : true;
  const isBefore = studentViewCorrectAnswersBefore ? dayjs(now).isBefore(studentViewCorrectAnswersBefore) : true;
  return isAfter && isBefore;
});
const selectedDownloadId = computed(() => downloadId.value);
const isIntegration = computed(() => Boolean(assessment.value.integration));
const integrationTokenExpiration = computed(() => integration.value.token.expirationDate || 0);
const integrationTokenExpirationRemaining = computed({
  get: () => integration.value.token.expirationRemaining,
  set: value => {
    integration.value.token.expirationRemaining = value;
  }
});

watch(selectedSubmissionId, () => {
  if (selectedSubmission.value) {
    const { experimentId, conditionId, assessmentId, treatmentId, submissionId } = selectedSubmission.value;
    getQuestions(experimentId, conditionId, assessmentId, treatmentId, submissionId);
    getAnswers(experimentId, conditionId, assessmentId, treatmentId, submissionId);
  }
});

watch(answerableQuestions, questions => {
  questionValues.value = questions
    .filter(q => q.questionType !== "INTEGRATION")
    .map(q => ({ questionId: q.questionId, answerId: null, response: null }));
});

watch(integrationTokenExpirationRemaining, newValue => {
  if (newValue == null) return;
  if (newValue <= integration.value.token.expirationDateCheckInterval) {
    integration.value.token.alert = {
      ...integration.value.token.alert,
      date: dayjs(integration.value.token.expirationDate).format("MMMM D, YYYY [at] h:mma"),
      type: integration.value.token.alert.types.expired
    };
  } else if (newValue <= integration.value.token.warningPeriod) {
    const totalMinutes = Math.floor(newValue / (60 * 1000));
    const hours = Math.floor(totalMinutes / 60);
    const minutes = Math.max(totalMinutes % 60, 1);
    integration.value.token.alert = {
      ...integration.value.token.alert,
      date: `${hours} hour${hours !== 1 ? "s" : ""} ${minutes} minute${minutes !== 1 ? "s" : ""}`,
      type: integration.value.token.alert.types.warning
    };
  } else {
    integration.value.token.alert = {
      ...integration.value.token.alert,
      date: dayjs(integration.value.token.expirationDate).format("MMMM D, YYYY [at] h:mma"),
      type: integration.value.token.alert.types.initial
    };
  }
  emit("integrationsTokenAlert", integration.value.token.alert);
});

const handleTryAgainIntegration = () => attempt(true);
const handleTryAgain = () => attempt();

const handleSubmit = async () => {
  await Swal.fire({
    target: "#app",
    icon: "question",
    text: "Are you ready to submit your answers?",
    showCancelButton: true,
    confirmButtonText: "Yes, submit",
    cancelButtonText: "No, cancel",
    showLoaderOnConfirm: true,
    preConfirm: async () => {
      try {
        Swal.update({
          text: "Please don't refresh or close your browser window until assignment submission is confirmed.",
          showConfirmButton: false
        });
        return await submitQuiz();
      } catch (error) {
        Swal.fire({ target: "#app", text: `Could not submit: ${error.message}`, icon: "error", footer: errorFooter() });
      }
    },
    allowOutsideClick: () => !Swal.isLoading()
  });
};

const submitQuiz = async () => {
  try {
    selectedSubmissionId.value = submissionId.value;
    const parameters = { submissionIds: submissionId.value };
    if (!submissions.value) {
      await submissionsStore.fetchQuestionSubmissions([props.experimentId, conditionId.value, treatmentId.value, assessmentId.value, submissionId.value]);
      submissions.value = questionSubmissions.value;
    }
    await saveAnswers();
    const { data, status } = await apiStore.reportStep({ experimentId: props.experimentId, step: "student_submission", parameters });
    if (!status || ![200, 201].includes(status)) throw Error(`Error submitting quiz: ${data}`);
    const view = await viewAssignment();
    if (view?.status === 200) {
      assignmentData.value = view.data;
      submitted.value = true;
    }
  } catch (error) {
    submissions.value = null;
    console.error({ error });
    throw error;
  }
};

const saveAnswers = async () => {
  const allQuestionSubmissions = questionValues.value.map(q => {
    const existing = submissions.value.find(qs => qs.questionId === q.questionId);
    const questionSubmissionId = existing?.questionSubmissionId;
    const answerSubmissionId = existing?.answerSubmissionDtoList?.[0]?.answerSubmissionId;
    return {
      questionSubmissionId,
      questionId: q.questionId,
      answerSubmissionDtoList: [{ answerSubmissionId, questionSubmissionId, answerId: q.answerId, response: q.response }]
    };
  });

  const existingQuestionSubmissions = allQuestionSubmissions.filter(qs => !!qs.questionSubmissionId);
  const newQuestionSubmissions = allQuestionSubmissions.filter(qs => !qs.questionSubmissionId);
  const answerSubmissions = existingQuestionSubmissions.map(qs => qs.answerSubmissionDtoList[0]);
  const existingAnswerSubmissions = answerSubmissions.filter(ans => !!ans.answerSubmissionId);
  const newAnswerSubmissions = answerSubmissions.filter(ans => !ans.answerSubmissionId);

  if (newAnswerSubmissions.length > 0) {
    const { data, status } = await submissionsStore.createAnswerSubmissions([props.experimentId, conditionId.value, treatmentId.value, assessmentId.value, submissionId.value, newAnswerSubmissions]);
    if (!status || ![200, 201].includes(status)) throw Error(`Error submitting quiz: ${data}`);
  }

  for (const answerSubmission of existingAnswerSubmissions) {
    const { data, status } = await submissionsStore.updateAnswerSubmission([props.experimentId, conditionId.value, treatmentId.value, assessmentId.value, submissionId.value, answerSubmission.questionSubmissionId, answerSubmission.answerSubmissionId, answerSubmission]);
    if (!status || ![200, 201].includes(status)) throw Error(`Error submitting quiz: ${data}`);
  }

  if (newQuestionSubmissions.length > 0) {
    const { data, status } = await submissionsStore.createQuestionSubmissions([props.experimentId, conditionId.value, treatmentId.value, assessmentId.value, submissionId.value, newQuestionSubmissions]);
    if (!status || ![200, 201].includes(status)) throw Error(`Error submitting quiz: ${data}`);
  }
};

const getAnswers = async (experimentId, conditionIdArg, assessmentIdArg, treatmentIdArg, submissionIdArg) => {
  await submissionsStore.fetchQuestionSubmissions([experimentId, conditionIdArg, treatmentIdArg, assessmentIdArg, submissionIdArg]);
};

const downloadFileResponse = async payload => {
  downloadId.value = payload.answerSubmissionId;
  try {
    await submissionsStore.downloadAnswerFileSubmission([props.experimentId, payload.conditionId, payload.treatmentId, payload.assessmentId, payload.submissionId, payload.questionSubmissionId, payload.answerSubmissionId, payload.mimeType, payload.fileName]);
  } finally {
    downloadId.value = null;
  }
};

const getQuestions = async (experimentId, conditionIdArg, assessmentIdArg, treatmentIdArg, submissionIdArg) => {
  questionValues.value = [];
  await assessmentStore.fetchAssessmentForSubmission([experimentId, conditionIdArg, treatmentIdArg, assessmentIdArg, submissionIdArg]);
};

const areAllQuestionsAnswered = questions => {
  if (readonly.value) return true;
  for (const question of questions) {
    const value = questionValues.value.find(({ questionId }) => questionId === question.questionId);
    if (question.questionType === "MC" && value?.answerId == null) return false;
    if (question.questionType === "ESSAY" && (!value?.response || value.response.trim() === "")) return false;
    if (question.questionType === "FILE" && value?.response == null) return false;
  }
  return true;
};

const nextPage = async () => {
  questionPageIndex.value += 1;
  await nextTick();
  form.value?.scrollIntoView({ behavior: "smooth" });
};
const backPage = async () => {
  questionPageIndex.value -= 1;
  await nextTick();
  form.value?.scrollIntoView({ behavior: "smooth" });
};
const viewAssignment = () => apiStore.reportStep({ experimentId: props.experimentId, step: "view_assignment" });

const attempt = async (preferLmsChecks = false) => {
  questionPageIndex.value = 0;
  readonly.value = false;
  loading.value = true;
  try {
    const stepResponse = await apiStore.reportStep({ experimentId: props.experimentId, step: "launch_assignment", preferLmsChecks });
    if (stepResponse?.status === 200) {
      const data = stepResponse.data;
      conditionId.value = data.conditionId;
      treatmentId.value = data.treatmentId;
      assessmentId.value = data.assessmentId;
      submissionId.value = data.submissionId;
      submissions.value = data.questionSubmissionDtoList;
      // must resolve before checking isIntegration: it reads assessmentStore.assessment,
      // which this call is what populates in the first place
      await getQuestions(data.experimentId, data.conditionId, data.assessmentId, data.treatmentId, data.submissionId);
      if (isIntegration.value) setupIntegration(data);
    } else if (stepResponse?.status === 401 && stepResponse?.data?.toString().includes("Error 150:")) {
      await Swal.fire({ target: "#app", text: "You have no more attempts available", icon: "error", footer: errorFooter() });
    }
  } finally {
    loading.value = false;
  }
};

const setupIntegration = data => {
  integration.value = {
    ...integration.value,
    launchUrl: data.integrationLaunchUrl,
    token: {
      ...integration.value.token,
      expirationDate: data.integrationTokenExpirationDate,
      warningPeriod: data.integrationTokenWarningPeriod,
      expirationDateCheckInterval: data.integrationTokenExpirationCheckInterval
    }
  };
  integrationTokenCountdown();
  integration.value.token.countdownInterval = window.setInterval(integrationTokenCountdown, integration.value.token.expirationDateCheckInterval);
};

const round = n => n % 1 ? n.toFixed(2) : n;
const errorFooter = () => `<div class="text-secondary text-body-medium"><div>Timestamp: ${new Date().toString()}</div><div>Experiment: ${props.experimentId}</div></div>`;

const handleIntegrationsResize = event => {
  if (event.data?.height) {
    hasResizeMessage.value = true;
    const iframe = document.getElementById("integration-iframe");
    if (!iframe || iframe.height === event.data.height) return;
    iframe.height = `${event.data.height}px`;
    window.parent.postMessage({ subject: "lti.frameResize", height: event.data.height + 200 }, "*");
  }
};

const handleIntegrationMessage = event => {
  if (!event?.origin || !integration.value.launchUrl) return;
  const messageOrigin = new URL(event.origin).hostname;
  const expectedOrigin = new URL(integration.value.launchUrl).hostname;
  if (messageOrigin !== expectedOrigin) return;
  if (event.data?.subject === "terracotta_iframe_resize") handleIntegrationsResize(event);
};

const handleIntegrationsScore = async () => {
  emit("integrationsTokenAlert", null);
  if (integration.value.token.countdownInterval) {
    window.clearInterval(integration.value.token.countdownInterval);
    integration.value.token.countdownInterval = null;
  }
  const view = await viewAssignment();
  if (view?.status === 200) {
    assignmentData.value = view.data;
    submitted.value = true;
    selectedSubmissionId.value = assignmentData.value.submissions?.at(-1)?.submissionId;
  }
};

const integrationTokenCountdown = () => {
  if (integrationTokenExpiration.value) {
    integrationTokenExpirationRemaining.value = integrationTokenExpiration.value - Date.now();
  }
};

onMounted(async () => {
  submissionsStore.clearFiles();
  if (props.preview) {
    const treatmentPreview = await previewStore.treatment([props.experimentId, props.previewConditionId, props.previewTreatmentId, props.previewId, props.ownerId]);
    treatment.value = treatmentPreview.treatment;
    assessmentStore.setAssessment(treatment.value.assessmentDto);
    assessmentId.value = treatment.value.assessmentDto.assessmentId;
    treatmentId.value = treatment.value.treatmentId;
    conditionId.value = treatment.value.conditionId;
    submissions.value = [treatmentPreview.submission];
    submissionId.value = treatmentPreview.submission.submissionId;
    readonly.value = false;
    pageLoaded.value = true;
    emit("loaded");
  } else {
    loading.value = true;
    try {
      const stepResponse = await viewAssignment();
      if (stepResponse?.status === 200) {
        assignmentData.value = stepResponse.data;
        const { retakeAllowed, submissionAttemptsCount } = stepResponse.data.retakeDetails;
        if (retakeAllowed && submissionAttemptsCount === 0) await attempt();
        else readonly.value = true;
      }
    } finally {
      loading.value = false;
      pageLoaded.value = true;
      emit("loaded");
    }
  }

  window.addEventListener("message", handleIntegrationMessage, false);
  window.document.addEventListener("integrations_score", handleIntegrationsScore);
  window.document.addEventListener("integrations_reattempt", handleTryAgainIntegration);
});

onBeforeUnmount(() => {
  window.removeEventListener("message", handleIntegrationMessage);
  window.document.removeEventListener("integrations_score", handleIntegrationsScore);
  window.document.removeEventListener("integrations_reattempt", handleTryAgainIntegration);
  if (integration.value.token.countdownInterval) {
    window.clearInterval(integration.value.token.countdownInterval);
  }
});
</script>

<style lang="scss" scoped>
.total-points {
  line-height: 24px;
  font-size: 16px;
  font-weight: 400;
}
.preview {
  background-color: rgba(253, 245, 242, 1) !important;
  & .preview-treatment {
    margin: 0 auto;
    max-width: 75%;
  }
}
</style>
