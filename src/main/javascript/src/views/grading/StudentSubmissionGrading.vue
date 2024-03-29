<template>
  <div>
    <!-- Heading -->
    <v-row>
      <v-col cols="10">
        <h1>{{ participantName() }}'s response</h1>
      </v-col>
      <v-col>
        <v-row class="studentGrade" v-if="selectedSubmission">
          <v-text-field
            type="number"
            name="maxPoints"
            outlined
            style="max-width: 70px;max-height: 50px;"
            v-model="selectedSubmission.totalAlteredGrade"
            @input="
              (value) => {
                selectedSubmission.totalAlteredGrade = parseInt(value);
                touched = true;
              }
            "
          ></v-text-field>
          <span class="totalPoints ml-2">
            / {{ assessment.maxPoints }} Total Score</span
          >
        </v-row>
      </v-col>
    </v-row>
    <v-row no-gutters>
      <v-col :cols="6">
        <div class="d-flex align-center">
          <submission-selector
            :submissions="participantSubmissions"
            @select="(id) => selectedSubmissionId = id" />
          <span :style="{visibility: `${ touched ? 'visible' : 'hidden' }`}"  class="red--text">
            <v-icon class="red--text">mdi-alert-circle-outline</v-icon>
            Unsaved Changes
          </span>
        </div>
      </v-col>
    </v-row>

    <v-card
      v-if="
        hasEssayAndNonEssayQuestions && ungradedEssayQuestionIndices.length > 0
      "
      class="ungraded-essay-questions-notice"
      outlined
    >
      <v-card-text>
        <v-row>
          <v-col cols="1"
            ><v-icon class="ungraded-essay-questions-notice__icon"
              >mdi-text-box-check-outline</v-icon
            ></v-col
          >
          <v-col class="ungraded-essay-questions-notice__message">
            Please grade short answer responses ({{
              ungradedEssayQuestionIndices.join(", ")
            }}) manually
          </v-col>
        </v-row>
      </v-card-text>
    </v-card>

    <template v-if="this.selectedSubmissionId">
      <template>
        <div v-for="questionPage in questionPages" :key="questionPage.key">
          <!-- Individual Question -->
          <v-card
            class="mt-5 mb-2"
            :class="studentResponseCardClasses[question.questionId]"
            outlined
            v-for="(question, index) in questionPage.questions"
            :key="question.questionId"
          >
            <v-chip
              class="ungraded-essay-question-chip"
              color="rgba(255, 224, 178, 1)"
              v-if="ungradedEssayQuestions.includes(question)"
            >
              <v-icon class="ungraded-essay-question-chip__icon"
                >mdi-text-box-check-outline</v-icon
              >
              Manual grade needed</v-chip
            >
            <v-card-title class="questionSection">
              <div class="cardDetails">
                <v-row>
                  <v-col cols="1">
                    <span>{{ questionPage.questionStartIndex + index + 1 }}</span>
                  </v-col>
                  <v-col cols="9">
                    <span v-html="question.html"></span>
                  </v-col>
                  <v-col>
                    <v-row class="studentGrade individualScore">
                      <v-text-field
                        type="number"
                        name="questionPoints"
                        outlined
                        required
                        style="max-width: 70px;max-height: 50px;"
                        v-model="questionScoreMap[question.questionId]"
                        :disabled="question.points === 0"
                        @input="
                          (value) => {
                            questionScoreMap[question.questionId] = value;
                            touched = true;
                          }
                        "
                      ></v-text-field>
                      <span class="totalPoints  ml-2">
                        / {{ question.points }} Point</span
                      >
                    </v-row>
                  </v-col>
                </v-row>
              </div>

              <!-- Answer Section -->
              <div class="answerSection mt-5 w-100">
                <template v-if="question.questionType === 'MC'">
                  <div
                    v-for="answer in question.answers"
                    :key="answer.answerId"
                    class="w-100"
                  >
                    <v-row>
                      <v-col cols="1">
                        &nbsp;
                      </v-col>
                      <v-col cols="10">
                        <v-card
                          :class="[
                            'abc',
                            answer.correct ? 'correctAnswer' : '',
                            studentSubmittedAnswers[question.questionId].includes(
                              answer.answerId
                            )
                              ? 'wrongAnswer'
                              : '',
                          ]"
                          outlined
                        >
                          <v-card-title>
                            <v-row>
                              <v-col cols="1">
                                <!-- Radio Button -->
                                <v-radio-group
                                  :value="
                                    studentSubmittedAnswers[
                                      question.questionId
                                    ].find((a) => a === answer.answerId)
                                  "
                                >
                                  <v-radio
                                    class="radioButton"
                                    :value="answer.answerId"
                                    readonly
                                  >
                                  </v-radio>
                                </v-radio-group>
                              </v-col>
                              <v-col cols="8">
                                <!-- Answer Text -->
                                <span v-html="answer.html"></span>
                              </v-col>
                              <v-col>
                                <!-- Correct / Student Response -->
                                <span
                                  v-if="answer.correct"
                                  class="correctAnswerText"
                                  >Correct Response</span
                                >
                                <span
                                  v-else-if="
                                    studentSubmittedAnswers[
                                      question.questionId
                                    ].includes(answer.answerId)
                                  "
                                  class="studentResponse"
                                  >Student Response</span
                                >
                              </v-col>
                            </v-row>
                          </v-card-title>
                        </v-card>
                      </v-col>
                    </v-row>
                  </div>
                </template>
                <template v-else-if="question.questionType === 'ESSAY'">
                  <v-row>
                    <v-col cols="1">
                      &nbsp;
                    </v-col>
                    <v-col cols="10">
                      <v-card outlined>
                        <v-card-title>
                          {{ studentSubmittedAnswers[question.questionId] }}
                        </v-card-title>
                      </v-card>
                    </v-col>
                  </v-row>
                </template>
                <template v-else-if="question.questionType === 'FILE'">
                  <v-card width="100%" height="100%">
                    <v-card-text>
                      <v-row class="d-flex flex-column" dense align="center" justify="center">
                        <h2>File submitted:</h2>
                        <div
                          v-for="fileResponse in studentSubmittedFileResponse(question.questionId)" :key="fileResponse.answerSubmissionId"
                          class="v-btn uploaded-file-row"
                          outlined
                        >
                          {{fileResponse.fileName}}
                          <v-tooltip
                            v-if="fileResponse.answerSubmissionId != downloadId"
                            top
                          >
                            <template v-slot:activator="{on, attrs}">
                              <v-btn
                                v-bind="attrs"
                                v-on="on"
                                @click="downloadFileResponse(fileResponse)"
                                class="btn-uploaded-file"
                                target="_blank"
                              >
                                <v-icon class="btn-uploaded-file-icon">mdi-file-download-outline</v-icon>
                              </v-btn>
                            </template>
                            <span>Download file</span>
                          </v-tooltip>
                          <span v-if="fileResponse.answerSubmissionId === downloadId">
                            <Spinner></Spinner>
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
import Spinner from "@/components/Spinner";
import SubmissionSelector from '../assignment/SubmissionSelector';

export default {
  name: "StudentSubmissionGrading",
  components: {
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
            answers[question.questionId] = this.studentSubmittedMCAnswers(
              question.questionId
            );
          } else if (question.questionType === "ESSAY") {
            answers[question.questionId] = this.studentSubmittedEssayResponse(
              question.questionId
            );
          } else if (question.questionType === "FILE") {
            answers[question.questionId] = this.studentSubmittedFileResponse(
                question.questionId
            );
          }
        }
      }
      return answers;
    },
    gradableQuestions() {
      return this.assessment && this.assessment.questions
        ? this.assessment.questions.filter(
            (q) => q.questionType !== "PAGE_BREAK"
          )
        : [];
    },
    hasEssayAndNonEssayQuestions() {
      return (
        this.gradableQuestions.some((q) => q.questionType === "ESSAY") &&
        this.gradableQuestions.some((q) => q.questionType !== "ESSAY")
      );
    },
    ungradedEssayQuestionIndices() {
      return this.ungradedEssayQuestions.map((q) => this.getQuestionIndex(q));
    },
    studentResponseCardClasses() {
      const result = {};
      for (const question of this.ungradedEssayQuestions) {
        result[question.questionId] = ["unanswered-essay-response"];
      }
      return result;
    },
    ungradedEssayQuestions() {
      const questions = [];
      if (this.assessment && this.assessment.questions) {
        for (const question of this.assessment.questions) {
          if (
            question.questionType === "ESSAY" &&
            question.points > 0 &&
            this.questionScoreMap[question.questionId] === null
          ) {
            questions.push(question);
          }
        }
      }
      return questions;
    },
    selectedSubmission() {
      return this.allSubmissions.find(s => s.submissionId === this.selectedSubmissionId);
    },
    participantSubmissions() {
      // return only this participant's submissions
      return this.allSubmissions.filter(s => s.participantId == this.participant_id);
    }
  },
  watch: {
    selectedSubmissionId(newValue) {
      this.loadSubmissionResponses(newValue);
    },
    maxPoints(newValue) {
      console.log(newValue);
    }
  },
  data() {
    return {
      questionScoreMap: {},
      updatedSubmissions: {},
      maxPoints: 0,
      selectedSubmissionId: null,
      touched: false,
      downloadId: null
    };
  },
  methods: {
    ...mapActions({
      fetchAssessment: "assessment/fetchAssessment",
      fetchStudentResponse: "submissions/fetchStudentResponse",
      updateQuestionSubmissions: "submissions/updateQuestionSubmissions",
      updateSubmission: "submissions/updateSubmission",
      reportStep: "api/reportStep",
      downloadAnswerFileSubmission: "submissions/downloadAnswerFileSubmission",
    }),
    participantName() {
      return this.participants.filter(
        (participant) => participant.participantId === this.participant_id
      )?.[0].user.displayName;
    },

    isSameAssessmentQuestion(questionId) {
      return this.assessment.questions
        ?.map((question) => question.questionId)
        ?.includes(+questionId);
    },

    studentResponseForQuestionId(questionId) {
      const filteredResponse = this.studentResponse?.filter(
        (resp) => resp.questionId === questionId
      );
      return filteredResponse?.length > 0
        ? filteredResponse[0]
        : { answerSubmissionDtoList: [] };
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
          'fileName':answerSubmissionDtoList[0].fileName,
          'mimeType':answerSubmissionDtoList[0].mimeType,
          'answerSubmissionId':answerSubmissionDtoList[0].answerSubmissionId,
          'questionSubmissionId':answerSubmissionDtoList[0].questionSubmissionId
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
      const updateSubmissions = this.studentResponse.map((response) => {
        return {
          questionSubmissionId: response.questionSubmissionId,
          answerSubmissionDtoList: response.answerSubmissionDtoList,
          alteredGrade:
            this.questionScoreMap[response.questionId] !== null
              ? +this.questionScoreMap[response.questionId]
              : null,
        };
      });

      const submission = this.selectedSubmission;

      try {
        await this.updateSubmission([
          submission.experimentId,
          submission.conditionId,
          submission.treatmentId,
          submission.assessmentId,
          submission.submissionId,
          submission.alteredCalculatedGrade,
          submission.totalAlteredGrade,
        ]);
        // Update Question Submissions
        await this.updateQuestionSubmissions([
          this.experiment_id,
          this.condition_id,
          this.treatment_id,
          this.assessment_id,
          this.selectedSubmissionId,
          updateSubmissions,
        ]);
        // Post Step to Experiment
        await this.reportStep({
          experimentId: this.experiment_id,
          step: "student_submission",
          parameters: { submissionIds: "" + this.selectedSubmissionId },
        });
        this.touched = false;
        /*this.$router.push({
          name: this.$router.currentRoute.meta.previousStep,
        });*/
      } catch (error) {
        return Promise.reject(error);
      }

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
      this.questionScoreMap = {};

      await this.fetchStudentResponse([
        this.experiment.experimentId,
        this.condition_id,
        this.treatment_id,
        this.assessment_id,
        submissionId,
      ]);

      // Initialize questionScoreMap
      const questionScoreMap = {};
      for (const question of this.gradableQuestions) {
        const questionId = question.questionId;
        const alteredGrade = this.studentResponseForQuestionId(questionId)
          .alteredGrade;
        const calculatedPoints = this.studentResponseForQuestionId(questionId)
          .calculatedPoints;

        if (question.questionType === "ESSAY") {
          // Essay questions have to be manually graded. The alteredGrade will be
          // null if it hasn't been manually graded.
          questionScoreMap[questionId] = alteredGrade;
        } else {
          questionScoreMap[questionId] = alteredGrade
            ? alteredGrade
            : calculatedPoints;
        }
      }
      this.questionScoreMap = questionScoreMap;

      // Initialize maxPoints
      let sum = 0;
      Object.keys(this.questionScoreMap)?.map((qId) => {
        this.isSameAssessmentQuestion(qId)
          ? (sum = sum + this.questionScoreMap[qId])
          : sum;
      });
      this.maxPoints = sum;
    }
  },
  async created() {
    this.fetchAssessment([
      this.experiment.experimentId,
      this.condition_id,
      this.treatment_id,
      this.assessment_id,
    ]);
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
.questionSection {
  display: flex;
  flex-direction: column;
  align-content: flex-start;
}
.v-input--selection-controls {
  margin-top: 0;
}
.studentGrade {
  align-items: center;
}

.answerSection {
  min-width: 100%;
}

.totalPoints {
  line-height: 24px;
  font-size: 16px;
  font-weight: 400;
}

.individualScore {
  margin-left: 1px;
}

.radioButton {
  margin-top: 2px;
}

.cardDetails {
  min-width: 100%;
}

.wrongAnswer {
  border: 1px solid rgb(229, 21, 62);
}

.correctAnswer {
  border: 1px solid rgb(56, 173, 182);
}
.studentResponse {
  color: rgb(229, 21, 62);
  font-family: Roboto;
  font-size: 14px;
  font-weight: 700;
  line-height: 20px;
  letter-spacing: 0.25px;
  text-align: left;
}
.correctAnswerText {
  color: rgba(56, 173, 182, 1);
  font-family: Roboto;
  font-size: 14px;
  font-weight: 700;
  line-height: 20px;
  letter-spacing: 0.25px;
  text-align: left;
}
.unanswered-essay-response {
  border: 1px solid #ffe0b2;
  background-color: rgba(255, 224, 178, 0.1);
}
.ungraded-essay-questions-notice {
  border: 1px solid #ffe0b2;
  background-color: rgba(255, 224, 178, 0.1);
  margin-bottom: 40px;
}
.ungraded-essay-questions-notice .v-card__text {
  color: rgba(0, 0, 0, 0.87);
  font-family: Roboto;
  font-size: 16px;
  font-weight: 400;
  line-height: 24px;
  letter-spacing: 0.15000000596046448px;
  text-align: left;
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
</style>
