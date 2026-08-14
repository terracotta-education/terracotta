<template>
<div>
  <page-loading
    v-if="isSaving"
    :display="true"
    :message="'Saving submission grades. Please wait.'"
    :containerStyles="pageLoadingContainerStyles"
    :spinnerStyles="pageLoadingSpinnerStyles"
  />
  <v-row
    class="header-row"
  >
    <v-col>
      <span
        class="header-participant-response"
      >
        {{ participantName() }}'s response
      </span>
    </v-col>
    <v-col
      class="col-attempts"
    >
      <v-card
        class="card-header"
        variant="outlined"
      >
        <v-card-text
          class="p-2"
        >
          <v-row>
            <v-col
              pb-0
            >
              <h3>Attempts</h3>
            </v-col>
          </v-row>
          <v-row
            class="mt-0"
          >
            <v-col>
              <submission-selector
                :submissions="participantSubmissions"
                @select="(id) => selectedSubmissionId = id"
              />
            </v-col>
          </v-row>
        </v-card-text>
      </v-card>
    </v-col>
    <v-col
      class="col-score"
    >
      <v-card
        class="card-header"
        variant="outlined"
      >
        <v-card-text
          class="p-2"
        >
          <v-row
            class="d-flex align-center pb-0"
            density="compact"
          >
            <v-col
              class="col-score-title"
            >
              <h3>{{ scoreHeader }}</h3>
            </v-col>
            <v-col
              class="col-score-tooltip px-1"
            >
              <tool-tip
                :header="scoreTooltipHeader"
                :content="scoreTooltip"
                :activatorType="scoreTooltipActivator.type"
                :icon="scoreTooltipActivator.text"
                :iconStyle="tooltipStyles"
                :aria-label="`${scoreTooltipHeader} tooltip`"
                location="bottom"
              />
            </v-col>
            <v-col
              class="col-score-toggle"
            >
              <a
                @click="changeScoreType()"
                tabindex="0"
              >
                {{ scoreLink }}
              </a>
            </v-col>
          </v-row>
          <v-row>
            <v-col
              v-if="getScoreType === 'calculated'"
            >
              <span
                class="total-points"
              >
                {{ currentAttemptCalculatedGrade }}/{{ assessment?.maxPoints }}
              </span>
            </v-col>
            <v-col
              v-else
            >
              <v-row
                v-if="selectedSubmission"
                class="student-grade"
                density="compact"
              >
                <v-text-field
                  v-model="selectedSubmission.totalAlteredGrade"
                  @update:model-value="(value) => {
                    selectedSubmission.totalAlteredGrade = parseFloat(value);
                    currentAttempt.overrideGrade.touched = true;
                  }"
                  style="max-width: 100px;max-height: 50px;"
                  class="input-override-grade"
                  type="number"
                  name="maxPoints"
                  variant="outlined"
                ></v-text-field>
                <span
                  class="total-points ml-2"
                >
                  / {{ assessment?.maxPoints }}
                </span>
              </v-row>
            </v-col>
          </v-row>
          <v-row
            v-if="showUnsavedChangeWarning"
            class="unsaved-warn"
          >
            <v-col>
              <span
                class="text-red"
              >
                <v-icon
                  class="text-red"
                >
                  mdi-alert-circle-outline
                </v-icon>
                Unsaved Changes
              </span>
            </v-col>
          </v-row>
        </v-card-text>
      </v-card>
    </v-col>
  </v-row>

  <v-card
    v-if="showUngradedText"
    class="ungraded-essay-questions-notice"
    variant="outlined"
  >
    <v-card-text>
      <v-row>
        <v-col
          cols="1"
        >
          <v-icon
            class="ungraded-essay-questions-notice__icon"
          >
            mdi-text-box-check-outline
        </v-icon>
        </v-col>
        <v-col
          class="ungraded-essay-questions-notice__message"
        >
          {{ manualGradeText }}
        </v-col>
      </v-row>
    </v-card-text>
  </v-card>

  <template
    v-if="selectedSubmissionId"
  >
    <div
      v-for="questionPage in questionPages"
      :key="questionPage.key"
    >
        <!-- Individual Question -->
        <v-card
          v-for="(question, index) in questionPage.questions"
          :key="question.questionId"
          :class="studentResponseCardClasses[question.questionId]"
          class="mt-5 mb-2 question-card"
          variant="outlined"
        >
          <v-chip
            v-if="!isGradeOverridden && (ungradedEssayQuestions.includes(question) || ungradedFileUploadQuestions.includes(question))"
            class="ungraded-essay-question-chip"
            color="rgba(255, 224, 178, 1)"
            prepend-icon="mdi-text-box-check-outline"
            variant="flat"
          >
            Manual grade needed
          </v-chip>
          <v-card-title
            class="question-section"
          >
            <div
              class="card-details"
            >
              <v-row>
                <v-col
                  cols="1"
                >
                  <span>
                    {{ questionPage.questionStartIndex + index + 1 }}
                  </span>
                </v-col>
                <v-col
                  cols="9">
                  <span
                    v-html="question.html"
                  >
                  </span>
                </v-col>
                <v-col>
                  <v-row
                    class="student-grade individual-score"
                    density="compact"
                  >
                    <v-text-field
                      v-model="currentAttempt.questionScoreMap[question.questionId]"
                      :disabled="question.points === 0"
                      :aria-label="`points input field for question ${question.html}`"
                      @update:model-value="() => {
                        currentAttempt.calculatedGrade.touched = true;
                        updateCalculatedGrade();
                      }"
                      style="max-width: 70px;max-height: 50px;"
                      type="number"
                      name="questionPoints"
                      variant="outlined"
                      required
                    >
                    </v-text-field>
                    <span
                      class="total-points ml-2"
                    >
                      / {{ question.points }} Point{{ question.points > 1 ? 's' : ''}}
                    </span>
                  </v-row>
                </v-col>
              </v-row>
            </div>
            <!-- Answer Section -->
            <div
              class="answer-section mt-5 w-100"
            >
              <template
                v-if="question.questionType === 'MC'"
              >
                <div
                  v-for="(answer, index) in question.answers"
                  :key="answer.answerId"
                  class="w-100"
                >
                  <v-row>
                    <v-col
                      cols="1"
                    >
                      &nbsp;
                    </v-col>
                    <v-col
                      cols="10"
                    >
                      <v-card
                        :class="[
                          'abc',
                          answer.correct ? 'correct-answer' : '',
                          studentSubmittedAnswers[question.questionId].includes(answer.answerId) ? 'wrong-answer' : '',
                        ]"
                        variant="outlined"
                      >
                        <v-card-title>
                          <v-row>
                            <v-col
                              cols="1"
                            >
                              <!-- Radio Button -->
                              <v-radio-group
                                :model-value="studentSubmittedAnswers[question.questionId].find((a) => a === answer.answerId)"
                                :aria-label="`answer group for question ${question.html}`"
                              >
                                <v-radio
                                  :value="answer.answerId"
                                  :aria-label="`answer option ${index + 1} for question ${question.html}`"
                                  class="radio-button"
                                  readonly
                                >
                                </v-radio>
                              </v-radio-group>
                            </v-col>
                            <v-col
                              cols="8"
                            >
                              <!-- Answer Text -->
                              <span
                                v-html="answer.html"
                              >
                              </span>
                            </v-col>
                            <v-col>
                              <!-- Correct / Student Response -->
                              <span
                                v-if="answer.correct"
                                class="correct-answer-text"
                              >
                                Correct Response
                              </span>
                              <span
                                v-else-if="studentSubmittedAnswers[question.questionId].includes(answer.answerId)"
                                class="student-response"
                              >
                                Student Response
                              </span>
                            </v-col>
                          </v-row>
                        </v-card-title>
                      </v-card>
                    </v-col>
                  </v-row>
                </div>
              </template>
              <template
                v-else-if="question.questionType === 'ESSAY'"
              >
                <v-row>
                  <v-col
                    cols="1"
                  >
                    &nbsp;
                  </v-col>
                  <v-col
                    cols="10"
                  >
                    <v-card variant="outlined">
                      <v-card-title>
                        {{ studentSubmittedAnswers[question.questionId] }}
                      </v-card-title>
                    </v-card>
                  </v-col>
                </v-row>
              </template>
              <template
                v-else-if="question.questionType === 'FILE'"
              >
                <v-card
                  class="w-100 h-100"
                >
                  <v-card-text>
                    <v-row
                      class="d-flex flex-column"
                      align="center"
                      justify="center"
                    >
                      <h2>
                        File submitted:
                      </h2>
                      <div
                        v-for="fileResponse in studentSubmittedFileResponse(question.questionId)"
                        :key="fileResponse.answerSubmissionId"
                        class="v-btn uploaded-file-row"
                      >
                        {{ fileResponse.fileName }}
                        <tool-tip
                          v-if="fileResponse.answerSubmissionId != downloadId"
                          @clicked="downloadFileResponse(fileResponse)"
                          activatorType="button"
                          activatorClass="btn-uploaded-file"
                          activatorIconClass="btn-uploaded-file-icon"
                          icon="mdi-file-download-outline"
                          content="Download file"
                        />
                        <span
                          v-if="fileResponse.answerSubmissionId === downloadId"
                        >
                          <Spinner />
                        </span>
                      </div>
                    </v-row>
                  </v-card-text>
                </v-card>
              </template>
            </div>
          </v-card-title>
        </v-card>
      </div>
  </template>
</div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from "vue";
import { useRoute } from "vue-router";

import { experiment as experimentModule } from "@/store/experiment.module";
import { participants as participantsModule } from "@/store/participants.module";
import { assessment as assessmentModule } from "@/store/assessment.module";
import { submission as submissionModule } from "@/store/submission.module";
import { configuration as configurationModule } from "@/store/configuration.module";
import { api as apiModule } from "@/store/api.module";

import PageLoading from "@/components/PageLoading.vue";
import Spinner from "@/components/Spinner.vue";
import SubmissionSelector from "@/views/assignment/SubmissionSelector.vue";
import ToolTip from "@/components/ToolTip.vue";

defineOptions({
  name: "StudentSubmissionGrading"
});

const route = useRoute();

const experimentStore = experimentModule();
const participantsStore = participantsModule();
const assessmentStore = assessmentModule();
const submissionsStore = submissionModule();
const configurationStore = configurationModule();
const apiStore = apiModule();

const maxPoints = ref(0);
const downloadId = ref(null);
const isSaving = ref(false);

const experiment = computed(() => experimentStore.experiment);
const participants = computed(() => participantsStore.participants);
const assessment = computed(() => assessmentStore.assessment);
const studentResponse = computed(() => submissionsStore.studentResponse);
const questionPages = computed(() => assessmentStore.questionPages);
const configurations = computed(() => configurationStore.get);

const assessmentId = computed(() => parseInt(route.params.assessmentId));
const conditionId = computed(() => parseInt(route.params.conditionId));
const treatmentId = computed(() => parseInt(route.params.treatmentId));
const participantId = computed(() => parseInt(route.params.participantId));
const experimentId = computed(() => parseInt(route.params.experimentId));

const allSubmissions = computed(() => assessment.value?.submissions || []);

// ---------------- ATTEMPTS ----------------
const selectedSubmissionId = ref(null);
const attempts = ref([]);

const participantSubmissions = computed(() =>
  allSubmissions.value.filter(s => s.participantId == participantId.value)
);

const currentAttempt = computed(() =>
  attempts.value.find(a => a.submissionId === selectedSubmissionId.value) || {
    submissionId: null,
    initialScoreType: "calculated",
    typeChanged: false,
    calculatedGrade: { grade: 0, touched: false },
    overrideGrade: { grade: 0, touched: false },
    gradeOverridden: false,
    studentResponse: [],
    questionScoreMap: [],
    loaded: false
  }
);

const initAttempts = () => {
  attempts.value = participantSubmissions.value.map(submission => ({
    submissionId: submission.submissionId,
    initialScoreType: submission.gradeOverridden ? "override" : "calculated",
    typeChanged: false,
    calculatedGrade: { grade: 0, touched: false },
    overrideGrade: { grade: 0, touched: false },
    gradeOverridden: submission.gradeOverridden || false,
    studentResponse: [],
    questionScoreMap: [],
    loaded: false
  }));
};

// ---------------- API ----------------
const fetchStudentResponse = payload => submissionsStore.fetchStudentResponse(payload);
const updateSubmissions = payload => submissionsStore.updateSubmissions(payload);
const updateQuestionSubmissions = payload => submissionsStore.updateQuestionSubmissions(payload);
const reportStep = payload => apiStore.reportStep(payload);
const downloadAnswerFileSubmission = payload => submissionsStore.downloadAnswerFileSubmission(payload);
const fetchAssessment = payload => assessmentStore.fetchAssessment(payload);

// ---------------- RESPONSES ----------------
const studentResponseForQuestionId = questionId => {
  const res = currentAttempt.value.studentResponse?.filter(r => r.questionId === questionId);
  return res?.length ? res[0] : { answerSubmissionDtoList: [] };
};

const studentSubmittedMCAnswers = questionId =>
  studentResponseForQuestionId(questionId).answerSubmissionDtoList.map(a => a.answerId);

const studentSubmittedEssayResponse = questionId => {
  const list = studentResponseForQuestionId(questionId).answerSubmissionDtoList;
  return list?.length ? list[0].response : null;
};

const studentSubmittedFileResponse = questionId => {
  const list = studentResponseForQuestionId(questionId).answerSubmissionDtoList;
  if (!list || list.length === 0) return null;
  return [{
    fileName: list[0].fileName,
    mimeType: list[0].mimeType,
    answerSubmissionId: list[0].answerSubmissionId,
    questionSubmissionId: list[0].questionSubmissionId
  }];
};

// ---------------- GRADING ----------------
const getScoreType = computed(() =>
  currentAttempt.value?.gradeOverridden ? "override" : "calculated"
);

const updateCalculatedGrade = () => {
  currentAttempt.value.calculatedGrade.grade = 0;
  Object.values(currentAttempt.value.questionScoreMap || {})
    .filter(v => v !== null)
    .filter(v => !isNaN(Number(v)))
    .forEach(v => { currentAttempt.value.calculatedGrade.grade += Number(v); });
};

const changeScoreType = () => {
  const attempt = currentAttempt.value;
  if (getScoreType.value === "calculated") {
    attempt.gradeOverridden = true;
    attempt.typeChanged = attempt.initialScoreType === "calculated";
  } else {
    attempt.gradeOverridden = false;
    attempt.typeChanged = attempt.initialScoreType === "override";
  }
};

const selectedSubmission = computed(() => {
  return allSubmissions.value.find(
    submission => submission.submissionId === selectedSubmissionId.value
  );
});

const gradableQuestions = computed(() => {
  return assessment.value?.questions
    ? assessment.value.questions.filter(
        question => question.questionType !== "PAGE_BREAK"
      )
    : [];
});

const studentSubmittedAnswers = computed(() => {
  const answers = {};

  if (assessment.value?.questions) {
    for (const question of assessment.value.questions) {
      switch (question.questionType) {
        case "MC":
          answers[question.questionId] =
            studentSubmittedMCAnswers(question.questionId);
          break;

        case "ESSAY":
          answers[question.questionId] =
            studentSubmittedEssayResponse(question.questionId);
          break;

        case "FILE":
          answers[question.questionId] =
            studentSubmittedFileResponse(question.questionId);
          break;

        default:
          answers[question.questionId] = null;
      }
    }
  }

  return answers;
});

const hasEssayOrFileAndNonEssayQuestions = computed(() => {
  return (
    (
      gradableQuestions.value.some(
        question => question.questionType === "ESSAY"
      ) &&
      gradableQuestions.value.some(
        question => question.questionType !== "ESSAY"
      )
    ) ||
    (
      gradableQuestions.value.some(
        question => question.questionType === "FILE"
      ) &&
      gradableQuestions.value.some(
        question => question.questionType !== "FILE"
      )
    )
  );
});

const ungradedEssayQuestions = computed(() => {
  if (!assessment.value?.questions || !currentAttempt.value) {
    return [];
  }

  return assessment.value.questions.filter(question => {
    return (
      question.questionType === "ESSAY" &&
      question.points > 0 &&
      (
        currentAttempt.value.questionScoreMap[
          question.questionId
        ] === null ||
        Number.isNaN(
          Number(
            currentAttempt.value.questionScoreMap[
              question.questionId
            ]
          )
        )
      )
    );
  });
});

const ungradedFileUploadQuestions = computed(() => {
  if (!assessment.value?.questions || !currentAttempt.value) {
    return [];
  }

  return assessment.value.questions.filter(question => {
    return (
      question.questionType === "FILE" &&
      question.points > 0 &&
      (
        currentAttempt.value.questionScoreMap[
          question.questionId
        ] === null ||
        Number.isNaN(
          Number(
            currentAttempt.value.questionScoreMap[
              question.questionId
            ]
          )
        )
      )
    );
  });
});

const getQuestionIndex = question => {
  for (const questionPage of questionPages.value) {
    const index = questionPage.questions.findIndex(
      currentQuestion =>
        currentQuestion.questionId === question.questionId
    );

    if (index >= 0) {
      return questionPage.questionStartIndex + index + 1;
    }
  }

  return -1;
};

const ungradedEssayQuestionIndices = computed(() => {
  return ungradedEssayQuestions.value.map(question =>
    getQuestionIndex(question)
  );
});

const ungradedFileQuestionIndices = computed(() => {
  return ungradedFileUploadQuestions.value.map(question =>
    getQuestionIndex(question)
  );
});

const isGradeOverridden = computed(() => {
  return currentAttempt.value?.gradeOverridden || false;
});

const studentResponseCardClasses = computed(() => {
  if (isGradeOverridden.value) {
    return {};
  }

  const result = {};

  for (const question of ungradedEssayQuestions.value) {
    result[question.questionId] = ["ungraded-response"];
  }

  for (const question of ungradedFileUploadQuestions.value) {
    result[question.questionId] = ["ungraded-response"];
  }

  return result;
});

const manualGradeText = computed(() => {
  let text = "Please grade ";

  if (ungradedEssayQuestions.value.length > 0) {
    text += `short answer responses (${ungradedEssayQuestionIndices.value.join(", ")})`;
  }

  if (ungradedFileUploadQuestions.value.length > 0) {
    if (ungradedEssayQuestions.value.length > 0) {
      text += " and ";
    }

    text += `file submissions (${ungradedFileQuestionIndices.value.join(", ")})`;
  }

  text += " manually";

  return text;
});

const showUngradedText = computed(() => {
  if (isGradeOverridden.value) {
    return false;
  }

  return (
    hasEssayOrFileAndNonEssayQuestions.value &&
    (
      ungradedEssayQuestionIndices.value.length > 0 ||
      ungradedFileQuestionIndices.value.length > 0
    )
  );
});

const scoreHeader = computed(() => {
  switch (getScoreType.value) {
    case "calculated":
      return "Calculated Score";

    case "override":
      return "Override Score";

    default:
      return "";
  }
});

const scoreLink = computed(() => {
  switch (getScoreType.value) {
    case "calculated":
      return "Override";

    case "override":
      return "Revert";

    default:
      return "";
  }
});

const scoreTooltipHeader = computed(() => {
  switch (getScoreType.value) {
    case "calculated":
      return "Calculated score";

    case "override":
      return "Override score";

    default:
      return "";
  }
});

const lmsTitle = computed(() => {
  return configurations.value?.lmsTitle || "LMS";
});

const scoreTooltip = computed(() => {
  switch (getScoreType.value) {
    case "calculated":
      return `This score updates based on points students earn on individual items (as input by ${lmsTitle.value} for multiple choice questions or by instructors for short answer or file upload questions). The instructor can override this score by clicking Override (which can be reversed after the change).`;

    case "override":
      return "You have overridden the calculated score. This score will not change based on changes made to points earned on individual questions. Click Revert to go back to the calculated score.";

    default:
      return "";
  }
});

const scoreTooltipActivator = {
  type: "icon",
  text: "mdi-help-circle-outline"
};

const tooltipStyles = {
  "font-size": "20px",
  "vertical-align": "top"
};

const pageLoadingContainerStyles = {
  "z-index": 1000,
  position: "relative",
  padding: 0
};

const pageLoadingSpinnerStyles = {
  "margin-top": "200px"
};

const currentAttemptTypeChanged = computed(() => {
  return currentAttempt.value?.typeChanged || false;
});

const currentAttemptCalculatedGrade = computed(() => {
  return currentAttempt.value?.calculatedGrade?.grade || 0;
});

const currentAttemptCalculatedGradeTouched = computed(() => {
  return currentAttempt.value?.calculatedGrade?.touched || false;
});

const currentAttemptOverrideGradeTouched = computed(() => {
  return currentAttempt.value?.overrideGrade?.touched || false;
});

const showUnsavedChangeWarning = computed(() => {
  if (currentAttemptTypeChanged.value) {
    return true;
  }

  switch (getScoreType.value) {
    case "calculated":
      return currentAttemptCalculatedGradeTouched.value;

    case "override":
      return currentAttemptOverrideGradeTouched.value;

    default:
      return false;
  }
});

const participantName = () => {
  return participants.value.find(
    participant =>
      participant.participantId === participantId.value
  )?.user?.displayName;
};

const findSubmissionById = id => {
  return allSubmissions.value.find(
    submission => submission.submissionId === id
  );
};

const isSameAssessmentQuestion = questionId => {
  return assessment.value?.questions
    ?.map(question => question.questionId)
    ?.includes(Number(questionId));
};

const downloadFileResponse = async fileResponse => {
  downloadId.value = fileResponse.answerSubmissionId;

  try {
    await downloadAnswerFileSubmission([
      experimentId.value,
      selectedSubmission.value.conditionId,
      selectedSubmission.value.treatmentId,
      selectedSubmission.value.assessmentId,
      selectedSubmission.value.submissionId,
      fileResponse.questionSubmissionId,
      fileResponse.answerSubmissionId,
      fileResponse.mimeType,
      fileResponse.fileName
    ]);
  } catch (error) {
    console.log("downloadFileResponse | catch", error);
  } finally {
    downloadId.value = null;
  }
};

const loadSubmissionResponses = async submissionId => {
  if (currentAttempt.value.loaded) {
    return;
  }

  await fetchStudentResponse([
    experiment.value.experimentId,
    conditionId.value,
    treatmentId.value,
    assessmentId.value,
    submissionId
  ]);

  currentAttempt.value.studentResponse =
    studentResponse.value;

  const questionScoreMap = {};

  for (const question of gradableQuestions.value) {
    const questionId = question.questionId;
    const response = studentResponseForQuestionId(questionId);
    const alteredGrade = response.alteredGrade;
    const calculatedPoints = response.calculatedPoints;

    if (
      question.questionType === "ESSAY" ||
      question.questionType === "FILE"
    ) {
      questionScoreMap[questionId] = alteredGrade;
    } else {
      questionScoreMap[questionId] = alteredGrade
        ? alteredGrade
        : calculatedPoints;
    }
  }

  currentAttempt.value.questionScoreMap =
    questionScoreMap;

  updateCalculatedGrade();

  let sum = 0;

  Object.keys(
    currentAttempt.value.questionScoreMap || {}
  ).forEach(questionId => {
    if (isSameAssessmentQuestion(questionId)) {
      sum += Number(
        currentAttempt.value.questionScoreMap[
          questionId
        ] || 0
      );
    }
  });

  maxPoints.value = sum;
  currentAttempt.value.loaded = true;
};

const saveExit = async () => {
  isSaving.value = true;

  const submissionsToUpdate = attempts.value.map(
    submissionAttempt => {
      const submission = findSubmissionById(
        submissionAttempt.submissionId
      );

      return {
        submissionId: submission.submissionId,
        alteredCalculatedGrade:
          submissionAttempt.calculatedGrade.grade,
        totalAlteredGrade:
          submissionAttempt.overrideGrade.touched
            ? submission.totalAlteredGrade
            : submissionAttempt.calculatedGrade.grade,
        gradeOverridden:
          submissionAttempt.gradeOverridden
      };
    }
  );

  attempts.value.forEach(attempt => {
    attempt.typeChanged = false;
    attempt.calculatedGrade.touched = false;
    attempt.overrideGrade.touched = false;
  });

  try {
    await updateSubmissions([
      allSubmissions.value[0].experimentId,
      allSubmissions.value[0].conditionId,
      allSubmissions.value[0].treatmentId,
      allSubmissions.value[0].assessmentId,
      submissionsToUpdate
    ]);

    for (const attempt of attempts.value) {
      const questionSubmissions =
        attempt.studentResponse.map(response => {
          return {
            questionSubmissionId:
              response.questionSubmissionId,
            answerSubmissionDtoList:
              response.answerSubmissionDtoList,
            alteredGrade:
              attempt.questionScoreMap[
                response.questionId
              ] !== null
                ? Number(
                    attempt.questionScoreMap[
                      response.questionId
                    ]
                  )
                : null
          };
        });

      await updateQuestionSubmissions([
        experimentId.value,
        conditionId.value,
        treatmentId.value,
        assessmentId.value,
        attempt.submissionId,
        questionSubmissions
      ]);

      await reportStep({
        experimentId: experimentId.value,
        step: "student_submission",
        parameters: {
          submissionIds: `${attempt.submissionId}`
        }
      });
    }
  } finally {
    isSaving.value = false;
  }
};

watch(selectedSubmissionId, newValue => {
  if (!newValue) {
    return;
  }

  loadSubmissionResponses(newValue);
});

onMounted(async () => {
  await fetchAssessment([
    experiment.value.experimentId,
    conditionId.value,
    treatmentId.value,
    assessmentId.value
  ]);

  initAttempts();
});

defineExpose({
  saveExit
});
</script>

<style lang="scss" scoped>
.question-section {
  display: flex;
  flex-direction: column;
  align-content: flex-start;
}
.v-input--selection-controls {
  margin-top: 0;
}
.student-grade {
  align-items: center;
  padding-left: 12px;
}
.answer-section {
  min-width: 100%;
}
.total-points {
  line-height: 24px;
  font-size: 18px;
  font-weight: bold;
}
.individual-score {
  margin-left: 1px;
}
.radio-button {
  margin-top: 2px;
}
.card-details {
  min-width: 100%;
}
.wrong-answer {
  border: 1px solid map.get($red, "base");
}
.correct-answer {
  border: 1px solid map.get($green, "base");
}
.student-response {
  color: map.get($red, "base");
  font-family: Roboto;
  font-size: 14px;
  font-weight: 700;
  line-height: 20px;
  letter-spacing: 0.25px;
  text-align: left;
}
.correct-answer-text {
  color: map.get($green, "base");
  font-family: Roboto;
  font-size: 14px;
  font-weight: 700;
  line-height: 20px;
  letter-spacing: 0.25px;
  text-align: left;
}
.ungraded-response {
  border: 1px solid map.get($yellow, "base");
  background-color: rgba(255, 224, 178, 0.1);
}
.ungraded-essay-questions-notice {
  border: 1px solid map.get($yellow, "base");
  background-color: rgba(255, 224, 178, 0.1);
  margin-top: 40px;
  margin-bottom: 40px;
  & .v-card-text {
    color: rgba(0, 0, 0, 0.87);
    font-family: Roboto;
    font-size: 16px;
    font-weight: 400;
    line-height: 24px;
    letter-spacing: 0.15000000596046448px;
    text-align: left;
  }
}
.ungraded-essay-questions-notice__icon {
  display: flex;
  margin-left: auto;
  margin-right: auto;
  height: 37px;
  width: 37px;
  background: map.get($yellow, "base");
  border-radius: calc(37px / 2);
}
.ungraded-essay-questions-notice__message {
  align-self: center;
}
.question-card {
  overflow: visible;
}
.ungraded-essay-question-chip {
  font-family: "Roboto";
  font-style: normal;
  font-weight: 400;
  font-size: 12px;
  line-height: 24px;
  letter-spacing: 0.15px;
  position: relative;
  height: 28px;
  left: 18px;
  top: -14px;
}
.uploaded-file-row {
  min-width: 200px !important;
  min-height: 42px !important;
  padding: 0 4px 0 16px !important;
  cursor: inherit;
  background-color: transparent !important;
  border-radius: 4px;
  border: 1px solid lightgrey;
  justify-content: space-between;
}
.btn-uploaded-file {
  padding: 0 !important;
  margin-left: 20px;
  min-width: fit-content !important;
  max-height: 28px;
  border-color: lightgrey;
  background-color: transparent !important;
}
.btn-uploaded-file-icon {
  color: rgba(0,0,0,.54) !important;
}
.header-row {
  & .col-attempts,
  & .col-score {
    max-width: 300px;
  }
  & .card-header {
    max-width: 300px;
    min-height: 100%;
    max-height: 100%;
    background-color: rgba(29, 157, 255, .04);
    & .col-score-title,
    & .col-score-tooltip,
    & .col-score-toggle {
      min-width: fit-content;
      max-width: fit-content;
    }
    & .col-score-title {
      padding-right: 0;
      h3 {
        padding-bottom: 0px !important;
      }
    }
    & .col-score-toggle {
      min-width: unset;
      max-width: unset;
      > a {
        font-size: 1.17em;
        float: right;
        color: map.get($blue, "base") !important;
      }
    }
    > .v-card-text {
      min-width: 100%;
      max-width: 100%;
    }
  }
  & .header-participant-response {
    font-size: 24px;
  }
  & .select-submissions,
  & .input-override-grade {
    background: white;
  }
}
</style>
