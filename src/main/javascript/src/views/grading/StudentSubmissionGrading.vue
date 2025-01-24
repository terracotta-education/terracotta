<template>
  <div>
    <PageLoading
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
          outlined
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
          outlined
        >
          <v-card-text
            class="p-2"
          >
            <v-row
              class="pb-0"
            >
              <v-col
                class="col-score-title"
              >
                <h3>{{ scoreHeader }}</h3>
              </v-col>
              <v-col
               class="col-score-tooltip px-1"
              >
                <InfoTooltip
                  :header="scoreTooltipHeader"
                  :message="scoreTooltip"
                  :activator="scoreTooltipActivator"
                  :iconStyle="tooltipStyles"
                  :location="`bottom`"
                />
              </v-col>
              <v-col
                class="col-score-toggle"
              >
                <a
                  @click="changeScoreType()"
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
                  {{ currentAttemptCalculatedGrade }}/{{ assessment.maxPoints }}
                </span>
              </v-col>
              <v-col
                v-else
              >
                <v-row
                  v-if="selectedSubmission"
                  class="student-grade"
                >
                  <v-text-field
                    @input="
                      (value) => {
                        selectedSubmission.totalAlteredGrade = parseFloat(value);
                        currentAttempt.overrideGrade.touched = true;
                      }
                    "
                    v-model="selectedSubmission.totalAlteredGrade"
                    style="max-width: 70px;max-height: 50px;"
                    class="input-override-grade"
                    type="number"
                    name="maxPoints"
                    outlined
                  ></v-text-field>
                  <span
                    class="total-points ml-2"
                  >
                    /{{ assessment.maxPoints }}
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
                  class="red--text"
                >
                  <v-icon
                    class="red--text"
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
      outlined
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
      v-if="this.selectedSubmissionId"
    >
      <template>
        <div
          v-for="questionPage in questionPages"
          :key="questionPage.key"
        >
          <!-- Individual Question -->
          <v-card
            v-for="(question, index) in questionPage.questions"
            :key="question.questionId"
            :class="studentResponseCardClasses[question.questionId]"
            class="mt-5 mb-2"
            outlined
          >
            <v-chip
              v-if="!isGradeOverridden && (ungradedEssayQuestions.includes(question) || ungradedFileUploadQuestions.includes(question))"
              class="ungraded-essay-question-chip"
              color="rgba(255, 224, 178, 1)"
            >
              <v-icon
                class="ungraded-essay-question-chip__icon"
              >
                mdi-text-box-check-outline
              </v-icon>
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
                    >
                      <v-text-field
                        :disabled="question.points === 0"
                        @input="
                          (value) => {
                            currentAttempt.questionScoreMap[question.questionId] = value || null;
                            currentAttempt.calculatedGrade.touched = true;
                            updateCalculatedGrade();
                          }
                        "
                        type="number"
                        name="questionPoints"
                        outlined
                        required
                        style="max-width: 70px;max-height: 50px;"
                        v-model="currentAttempt.questionScoreMap[question.questionId]"
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
                    v-for="answer in question.answers"
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
                          outlined
                        >
                          <v-card-title>
                            <v-row>
                              <v-col
                                cols="1"
                              >
                                <!-- Radio Button -->
                                <v-radio-group
                                  :value="studentSubmittedAnswers[question.questionId].find((a) => a === answer.answerId)"
                                >
                                  <v-radio
                                    :value="answer.answerId"
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
                      <v-card outlined>
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
                        dense
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
                          outlined
                        >
                          {{ fileResponse.fileName }}
                          <v-tooltip
                            v-if="fileResponse.answerSubmissionId != downloadId"
                            top
                          >
                            <template
                              v-slot:activator="{on, attrs}"
                            >
                              <v-btn
                                @click="downloadFileResponse(fileResponse)"
                                v-bind="attrs"
                                v-on="on"
                                class="btn-uploaded-file"
                                target="_blank"
                              >
                                <v-icon
                                  class="btn-uploaded-file-icon"
                                >
                                  mdi-file-download-outline
                                </v-icon>
                              </v-btn>
                            </template>
                            <span>
                              Download file
                            </span>
                          </v-tooltip>
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
    </template>
  </div>
</template>

<script>
import { mapActions, mapGetters } from "vuex";
import InfoTooltip from "@/components/InfoTooltip.vue";
import PageLoading from "@/components/PageLoading";
import Spinner from "@/components/Spinner";
import SubmissionSelector from "../assignment/SubmissionSelector";

export default {
  name: "StudentSubmissionGrading",
  components: {
    InfoTooltip,
    PageLoading,
    Spinner,
    SubmissionSelector
  },
  computed: {
    ...mapGetters({
      experiment: "experiment/experiment",
      participants: "participants/participants",
      assessment: "assessment/assessment",
      studentResponse: "submissions/studentResponse",
      questionPages: "assessment/questionPages",
    }),

    assessment_id() {
      return parseInt(this.$route.params.assessment_id);
    },
    condition_id() {
      return parseInt(this.$route.params.condition_id);
    },
    treatment_id() {
      return parseInt(this.$route.params.treatment_id);
    },
    exposure_id() {
      return parseInt(this.$route.params.exposure_id);
    },
    participant_id() {
      return parseInt(this.$route.params.participant_id);
    },
    experiment_id() {
      return parseInt(this.$route.params.experiment_id);
    },
    allSubmissions() {
      return this.assessment.submissions || [];
    },
    studentSubmittedAnswers() {
      const answers = {};
      if (this.assessment && this.assessment.questions) {
        for (const question of this.assessment.questions) {
          if (question.questionType === "MC") {
            answers[question.questionId] = this.studentSubmittedMCAnswers(question.questionId);
          } else if (question.questionType === "ESSAY") {
            answers[question.questionId] = this.studentSubmittedEssayResponse(question.questionId);
          } else if (question.questionType === "FILE") {
            answers[question.questionId] = this.studentSubmittedFileResponse(question.questionId);
          }
        }
      }
      return answers;
    },
    gradableQuestions() {
      return this.assessment && this.assessment.questions ? this.assessment.questions.filter((q) => q.questionType !== "PAGE_BREAK") : [];
    },
    hasEssayOrFileAndNonEssayQuestions() {
      return (
        (this.gradableQuestions.some((q) => q.questionType === "ESSAY") && this.gradableQuestions.some((q) => q.questionType !== "ESSAY")) ||
        (this.gradableQuestions.some((q) => q.questionType === "FILE") && this.gradableQuestions.some((q) => q.questionType !== "FILE"))
      );
    },
    ungradedEssayQuestionIndices() {
      return this.ungradedEssayQuestions.map((q) => this.getQuestionIndex(q));
    },
    ungradedFileQuestionIndices() {
      return this.ungradedFileUploadQuestions.map((q) => this.getQuestionIndex(q));
    },
    studentResponseCardClasses() {
      if (this.isGradeOverridden) {
        return {};
      }
      const result = {};
      for (const question of this.ungradedEssayQuestions) {
        result[question.questionId] = ["ungraded-response"];
      }
      for (const question of this.ungradedFileUploadQuestions) {
        result[question.questionId] = ["ungraded-response"];
      }
      return result;
    },
    ungradedEssayQuestions() {
      const questions = [];
      if (this.assessment && this.assessment.questions) {
        for (const question of this.assessment.questions) {
          if (question.questionType === "ESSAY" && question.points > 0 && this.currentAttempt && (this.currentAttempt.questionScoreMap[question.questionId] === null || isNaN(this.currentAttempt.questionScoreMap[question.questionId]))) {
            questions.push(question);
          }
        }
      }
      return questions;
    },
    ungradedFileUploadQuestions() {
      const questions = [];
      if (this.assessment && this.assessment.questions) {
        for (const question of this.assessment.questions) {
          if (question.questionType === "FILE" && question.points > 0 && this.currentAttempt && (this.currentAttempt.questionScoreMap[question.questionId] === null || isNaN(this.currentAttempt.questionScoreMap[question.questionId]))) {
            questions.push(question);
          }
        }
      }
      return questions;
    },
    selectedSubmission() {
      return this.allSubmissions.find(s => s.submissionId === this.selectedSubmissionId);
    },
    selectedSubmissionPoints() {
      return this.selectedSubmission?.totalAlteredGrade || 0;
    },
    participantSubmissions() {
      // return only this participant's submissions
      return this.allSubmissions.filter(s => s.participantId == this.participant_id);
    },
    manualGradeText() {
      var text = "Please grade ";

      if (this.ungradedEssayQuestions.length > 0) {
        text += "short answer responses (" + this.ungradedEssayQuestionIndices.join(", ") + ")";
      }

      if (this.ungradedFileUploadQuestions.length > 0) {
        if (this.ungradedEssayQuestions.length > 0) {
          text += " and ";
        }

        text += "file submissions (" + this.ungradedFileQuestionIndices.join(", ") + ")";
      }

      text += " manually";
      return text;
    },
    showUngradedText() {
      if (this.isGradeOverridden) {
        return false;
      }

      return this.hasEssayOrFileAndNonEssayQuestions && (this.ungradedEssayQuestionIndices.length > 0 || this.ungradedFileQuestionIndices.length > 0);
    },
    scoreHeader() {
      switch(this.getScoreType) {
        case "calculated":
          return "Calculated Score";
        case "override":
          return "Override Score";
        default:
          return "";
      }
    },
    scoreLink() {
      switch(this.getScoreType) {
        case "calculated":
          return "Override";
        case "override":
          return "Revert";
        default:
          return "";
      }
    },
    scoreTooltipHeader() {
      switch(this.getScoreType) {
        case "calculated":
          return "Calculated score";
        case "override":
          return "Override score";
        default:
          return "";
      }
    },
    scoreTooltip() {
      switch(this.getScoreType) {
        case "calculated":
          return `This score updates based on points students earn on individual items (as input by Canvas for multiple choice questions or by instructors for short answer or file
              upload questions). The instructor can override this score by clicking Override (which can be reversed after the change).`;
        case "override":
          return "You have overridden the calculated score. This score will not change based on changes made to points earned on individual questions. Click Revert to go back to the calculated score.";
        default:
          return "";
      }
    },
    scoreTooltipActivator() {
      return {"type": "icon", "text": "mdi-help-circle-outline"};
    },
    tooltipStyles() {
      return {
        "font-size": "20px",
        "vertical-align": "top"
      }
    },
    pageLoadingContainerStyles() {
      return {
        "z-index": 1000,
        "position": "relative",
        "padding": 0
      }
    },
    pageLoadingSpinnerStyles() {
      return {
        "margin-top": "200px"
      }
    },
    getScoreType() {
      return this.isGradeOverridden ? "override" : "calculated";
    },
    isGradeOverridden() {
      return this.currentAttempt ? (this.currentAttempt.gradeOverridden || false) : false;
    },
    showUnsavedChangeWarning() {
      if (this.currentAttemptTypeChanged) {
        return true;
      }

      switch (this.getScoreType) {
        case "calculated":
          return this.currentAttemptCalculatedGradeTouched;
        case "override":
          return this.currentAttemptOverrideGradeTouched;
        default:
          return false;
      }
    },
    currentAttempt() {
      return this.attempts.find(attempt => attempt.submissionId === this.selectedSubmissionId) ||
        {
          submissionId: null,
          initialScoreType: "calculated",
          typeChanged: false,
          calculatedGrade: {
            grade: 0,
            touched: false
          },
          overrideGrade: {
            grade: 0,
            touched: false
          },
          gradeOverridden: false,
          studentResponse: [],
          questionScoreMap: []
        };
    },
    currentAttemptTypeChanged() {
      return this.currentAttempt?.typeChanged || false;
    },
    currentAttemptQuestionScoreMap() {
      return this.currentAttempt?.questionScoreMap || [];
    },
    currentAttemptCalculatedGrade() {
      return this.currentAttempt?.calculatedGrade?.grade || 0;
    },
    currentAttemptOverrideGrade() {
      return this.currentAttempt?.overrideGrade?.grade || 0;
    },
    currentAttemptCalculatedGradeTouched() {
      return this.currentAttempt?.calculatedGrade?.touched || false;
    },
    currentAttemptOverrideGradeTouched() {
      return this.currentAttempt?.overrideGrade?.touched || false;
    }
  },
  watch: {
    selectedSubmissionId(newValue) {
      if (!newValue) {
        return;
      }

      this.loadSubmissionResponses(newValue);
    }
  },
  data() {
    return {
      maxPoints: 0,
      selectedSubmissionId: null,
      downloadId: null,
      attempts: [], // [{submissionId, initialScoreType, typeChanged, calculatedGrade: {grade, touched}, overrideGrade: {grade, touched}, gradeOverridden, studentResponse, questionScoreMap, loaded}]
      isSaving: false
    };
  },
  methods: {
    ...mapActions({
      fetchAssessment: "assessment/fetchAssessment",
      fetchStudentResponse: "submissions/fetchStudentResponse",
      updateQuestionSubmissions: "submissions/updateQuestionSubmissions",
      updateSubmission: "submissions/updateSubmission",
      updateSubmissions: "submissions/updateSubmissions",
      reportStep: "api/reportStep",
      downloadAnswerFileSubmission: "submissions/downloadAnswerFileSubmission",
    }),
    participantName() {
      return this.participants.filter(
        (participant) => participant.participantId === this.participant_id
      )?.[0].user.displayName;
    },
    findSubmissionById(id) {
      return this.allSubmissions.find(s => s.submissionId === id);
    },
    isSameAssessmentQuestion(questionId) {
      return this.assessment.questions
        ?.map((question) => question.questionId)
        ?.includes(+questionId);
    },
    studentResponseForQuestionId(questionId) {
      const filteredResponse = this.currentAttempt.studentResponse?.filter(
        (resp) => resp.questionId === questionId
      );
      return filteredResponse?.length > 0 ? filteredResponse[0] : { answerSubmissionDtoList: [] };
    },
    studentSubmittedMCAnswers(questionId) {
      return this.studentResponseForQuestionId(
        questionId
      ).answerSubmissionDtoList.map((answer) => answer.answerId);
    },
    studentSubmittedEssayResponse(questionId) {
      const answerSubmissionDtoList = this.studentResponseForQuestionId(
        questionId
      ).answerSubmissionDtoList;
      if (!answerSubmissionDtoList || answerSubmissionDtoList.length === 0) {
        return null;
      } else {
        return answerSubmissionDtoList[0].response;
      }
    },
    studentSubmittedFileResponse(questionId) {
      const answerSubmissionDtoList = this.studentResponseForQuestionId(
          questionId
      ).answerSubmissionDtoList;
      if (!answerSubmissionDtoList || answerSubmissionDtoList.length === 0) {
        return null;
      } else {
        return [{
          "fileName": answerSubmissionDtoList[0].fileName,
          "mimeType": answerSubmissionDtoList[0].mimeType,
          "answerSubmissionId": answerSubmissionDtoList[0].answerSubmissionId,
          "questionSubmissionId": answerSubmissionDtoList[0].questionSubmissionId
        }];
      }
    },
    async downloadFileResponse(fileResponse) {
      this.downloadId = fileResponse.answerSubmissionId;

      try {
        await this.downloadAnswerFileSubmission([
          this.experiment_id,
          this.selectedSubmission.conditionId,
          this.selectedSubmission.treatmentId,
          this.selectedSubmission.assessmentId,
          this.selectedSubmission.submissionId,
          fileResponse.questionSubmissionId,
          fileResponse.answerSubmissionId,
          fileResponse.mimeType,
          fileResponse.fileName
        ]);

        this.downloadId = null;
      } catch (error) {
          console.log("downloadFileResponse | catch", error);
          this.downloadId = null;
      }
    },
    async saveExit() {
      // display wait screen
      this.isSaving = true;

      var submissionsToUpdate = [];

      // update grades for attempts
      for (var submissionAttempt of this.attempts) {
        const submission = this.findSubmissionById(submissionAttempt.submissionId);
        submissionsToUpdate.push(
          {
            submissionId: submission.submissionId,
            alteredCalculatedGrade: submissionAttempt.calculatedGrade.grade,
            totalAlteredGrade: submissionAttempt.overrideGrade.touched ? submission.totalAlteredGrade : submissionAttempt.calculatedGrade.grade,
            gradeOverridden: submissionAttempt.gradeOverridden
          }
        );
      }

      this.attempts.forEach(attempt => {
        attempt.typeChanged = false;
        attempt.calculatedGrade.touched = false;
        attempt.overrideGrade.touched = false;
      });

      try {
        await this.updateSubmissions([
          this.allSubmissions[0].experimentId,
          this.allSubmissions[0].conditionId,
          this.allSubmissions[0].treatmentId,
          this.allSubmissions[0].assessmentId,
          submissionsToUpdate
        ]);
      } catch (error) {
        this.isSaving = false;
        return Promise.reject(error);
      }

      for (var attempt of this.attempts) {
        const updateQuestionSubmissions = attempt.studentResponse.map((response) => {
          return {
            questionSubmissionId: response.questionSubmissionId,
            answerSubmissionDtoList: response.answerSubmissionDtoList,
            alteredGrade: attempt.questionScoreMap[response.questionId] !== null ? +attempt.questionScoreMap[response.questionId] : null,
          };
        });

        try {
          // Update Question Submissions
          await this.updateQuestionSubmissions([
            this.experiment_id,
            this.condition_id,
            this.treatment_id,
            this.assessment_id,
            attempt.submissionId,
            updateQuestionSubmissions
          ]);
          // Post Step to Experiment
          await this.reportStep({
            experimentId: this.experiment_id,
            step: "student_submission",
            parameters: { submissionIds: "" + attempt.submissionId },
          });
        } catch (error) {
          this.isSaving = false;
          return Promise.reject(error);
        }
      }

      this.isSaving = false;
    },
    getQuestionIndex(question) {
      for (const questionPage of this.questionPages) {
        const index = questionPage.questions.findIndex(
          (q) => q.questionId === question.questionId
        );
        if (index >= 0) {
          return questionPage.questionStartIndex + index + 1;
        }
      }
      // shouldn't happen
      return -1;
    },
    async loadSubmissionResponses(submissionId) {
      if (this.currentAttempt.loaded) {
        // already loaded once; skip reloading
        return;
      }

      // get the student response for this attempt
      await this.fetchStudentResponse([
        this.experiment.experimentId,
        this.condition_id,
        this.treatment_id,
        this.assessment_id,
        submissionId,
      ]);

      this.currentAttempt.studentResponse = this.studentResponse;

      // initialize questionScoreMap
      const questionScoreMap = {};

      for (const question of this.gradableQuestions) {
        const questionId = question.questionId;
        const alteredGrade = this.studentResponseForQuestionId(questionId).alteredGrade;
        const calculatedPoints = this.studentResponseForQuestionId(questionId).calculatedPoints;

        if (question.questionType === "ESSAY" || question.questionType === "FILE") {
          // Essay / File questions have to be manually graded. The alteredGrade will be null if it hasn't been manually graded.
          questionScoreMap[questionId] = alteredGrade;
        } else {
          questionScoreMap[questionId] = alteredGrade ? alteredGrade : calculatedPoints;
        }
      }

      this.currentAttempt.questionScoreMap = questionScoreMap;
      this.updateCalculatedGrade();

      // initialize maxPoints
      let sum = 0;
      Object.keys(this.currentAttempt.questionScoreMap)?.map((qId) => {
        this.isSameAssessmentQuestion(qId) ? (sum = sum + this.currentAttempt.questionScoreMap[qId]) : sum;
      });
      this.maxPoints = sum;
      this.currentAttempt.loaded = true;
    },
    updateCalculatedGrade() {
      this.currentAttempt.calculatedGrade.grade = 0;
      Object.values(this.currentAttemptQuestionScoreMap)
        .filter(s => s !== null)
        .filter(s => !isNaN(Number(s)))
        .forEach(s => this.currentAttempt.calculatedGrade.grade += Number(s));
    },
    changeScoreType() {
      switch(this.getScoreType) {
        case "calculated":
          this.currentAttempt.gradeOverridden = true;
          this.currentAttempt.typeChanged = this.currentAttempt.initialScoreType === "calculated";
          break;
        case "override":
          this.currentAttempt.gradeOverridden = false;
          this.currentAttempt.typeChanged = this.currentAttempt.initialScoreType === "override";
          break;
        default:
          break;
      }
    }
  },
  async created() {
    await this.fetchAssessment([
      this.experiment.experimentId,
      this.condition_id,
      this.treatment_id,
      this.assessment_id,
    ]);

    for (var submission of this.participantSubmissions) {
      this.attempts.push(
        {
          submissionId: submission.submissionId,
          initialScoreType: submission.gradeOverridden ? "override" : "calculated",
          typeChanged: false,
          calculatedGrade: {
            grade: 0,
            touched: false
          },
          overrideGrade: {
            grade: 0,
            touched: false
          },
          gradeOverridden: submission.gradeOverridden || false,
          studentResponse: [],
          questionScoreMap: [],
          loaded: false
        }
      )
    }
  },
};
</script>

<style lang="scss" scoped>
.v-tooltip__content {
  max-width: 400px;
  opacity: 1.0 !important;
  background-color: rgba(55,61,63, 1.0) !important;
  a {
    color: #afdcff;
  }
}
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
  border: 1px solid rgb(229, 21, 62);
}
.correct-answer {
  border: 1px solid rgb(56, 173, 182);
}
.student-response {
  color: rgb(229, 21, 62);
  font-family: Roboto;
  font-size: 14px;
  font-weight: 700;
  line-height: 20px;
  letter-spacing: 0.25px;
  text-align: left;
}
.correct-answer-text {
  color: rgba(56, 173, 182, 1);
  font-family: Roboto;
  font-size: 14px;
  font-weight: 700;
  line-height: 20px;
  letter-spacing: 0.25px;
  text-align: left;
}
.ungraded-response {
  border: 1px solid #ffe0b2;
  background-color: rgba(255, 224, 178, 0.1);
}
.ungraded-essay-questions-notice {
  border: 1px solid #ffe0b2;
  background-color: rgba(255, 224, 178, 0.1);
  margin-top: 40px;
  margin-bottom: 40px;
}
.ungraded-essay-questions-notice {
  & .v-card__text {
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
  background: rgba(255, 224, 178, 1);
  border-radius: calc(37px / 2);
}
.ungraded-essay-questions-notice__message {
  align-self: center;
}
.ungraded-essay-question-chip,
.ungraded-essay-question-chip__icon {
  font-family: "Roboto";
  font-style: normal;
  font-weight: 400;
  font-size: 12px;
  line-height: 24px;
  letter-spacing: 0.15px;
}
.ungraded-essay-question-chip {
  position: relative;
  height: 28px;
  left: 18px;
  top: -14px;
}
.ungraded-essay-question-chip__icon {
  margin-right: 10px;
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
    }
    & .col-score-toggle {
      min-width: unset;
      max-width: unset;
      > a {
        font-size: 1.17em;
        float: right;
      }
    }
    > .v-card__text {
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
