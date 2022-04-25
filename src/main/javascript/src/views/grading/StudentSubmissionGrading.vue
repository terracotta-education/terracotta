<template>
  <div>
    <!-- Heading -->
    <v-row>
      <v-col cols="10">
        <h1>{{ participantName() }}'s response</h1>
      </v-col>
      <v-col>
        <v-row class="studentGrade">
          <v-text-field
            type="number"
            name="maxPoints"
            outlined
            style="max-width: 70px;max-height: 50px;"
            v-model="maxPoints"
          ></v-text-field>
          <span class="totalPoints ml-2">
            / {{ assessment.maxPoints }} Total Score</span
          >
        </v-row>
      </v-col>
    </v-row>

    <div
      v-if="
        hasEssayAndNonEssayQuestions && ungradedEssayQuestionIndices.length > 0
      "
    >
      Please grade short answer responses ({{
        ungradedEssayQuestionIndices.join(", ")
      }}) manually
    </div>

    <!-- Individual Question -->
    <v-card
      class="mt-5 mb-2"
      outlined
      v-for="(question, index) in assessment.questions"
      :key="question.questionId"
    >
      <v-card-title class="questionSection">
        <div class="cardDetails">
          <v-row>
            <v-col cols="1">
              <span>{{ index + 1 }}</span>
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
                  @input="
                    (value) => {
                      questionScoreMap[question.questionId] = value;
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
                              studentSubmittedAnswers[question.questionId].find(
                                (a) => a === answer.answerId
                              )
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
                          <span v-if="answer.correct" class="correctAnswerText"
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
        </div>
      </v-card-title>
    </v-card>
  </div>
</template>

<script>
import { mapActions, mapGetters } from "vuex";

export default {
  name: "StudentSubmissionGrading",
  computed: {
    ...mapGetters({
      experiment: "experiment/experiment",
      participants: "participants/participants",
      assessment: "assessment/assessment",
      submissions: "submissions/submissions",
      studentResponse: "submissions/studentResponse",
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
    submission_id() {
      return parseInt(this.$route.params.submission_id);
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
          }
        }
      }
      return answers;
    },
    hasEssayAndNonEssayQuestions() {
      return (
        this.assessment &&
        this.assessment.questions &&
        this.assessment.questions.some((q) => q.questionType === "ESSAY") &&
        this.assessment.questions.some((q) => q.questionType !== "ESSAY")
      );
    },
    ungradedEssayQuestionIndices() {
      const questionIndices = [];
      if (this.assessment && this.assessment.questions) {
        for (const question of this.assessment.questions) {
          if (
            question.questionType === "ESSAY" &&
            this.questionScoreMap[question.questionId] === null
          ) {
            questionIndices.push(this.getQuestionIndex(question));
          }
        }
      }
      return questionIndices;
    },
  },
  data() {
    return {
      questionScoreMap: {},
      maxPoints: 0,
    };
  },
  methods: {
    ...mapActions({
      fetchSubmissions: "submissions/fetchSubmissions",
      updateSubmission: "submissions/updateSubmission",
      fetchAssessment: "assessment/fetchAssessment",
      fetchStudentResponse: "submissions/fetchStudentResponse",
      updateQuestionSubmission: "submissions/updateQuestionSubmission",
      reportStep: "api/reportStep",
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

    async saveExit() {
      const updateSubmissions = this.studentResponse.map((response) => {
        return {
          questionSubmissionId: response.questionSubmissionId,
          answerSubmissionDtoList: response.answerSubmissionDtoList,
          alteredGrade: +this.questionScoreMap[response.questionId],
        };
      });

      try {
        // Update Question Submissions
        await this.updateQuestionSubmission([
          this.experiment_id,
          this.condition_id,
          this.treatment_id,
          this.assessment_id,
          this.submission_id,
          updateSubmissions,
        ]);
        // Post Step to Experiment
        await this.reportStep({
          experimentId: this.experiment_id,
          step: "student_submission",
          parameters: { submissionIds: "" + this.submission_id },
        });
        this.$router.push({
          name: this.$router.currentRoute.meta.previousStep,
        });
      } catch (error) {
        return Promise.reject(error);
      }
    },
    getQuestionIndex(question) {
      return (
        this.assessment.questions.findIndex(
          (q) => q.questionId === question.questionId
        ) + 1
      );
    },
  },
  async created() {
    this.questionScoreMap = {};
    this.fetchAssessment([
      this.experiment.experimentId,
      this.condition_id,
      this.treatment_id,
      this.assessment_id,
    ]);
    await this.fetchSubmissions([
      this.experiment.experimentId,
      this.condition_id,
      this.treatment_id,
      this.assessment_id,
    ]);
    await this.fetchStudentResponse([
      this.experiment.experimentId,
      this.condition_id,
      this.treatment_id,
      this.assessment_id,
      this.submission_id,
    ]);

    // Initialize questionScoreMap
    const questionScoreMap = {};
    for (const question of this.assessment.questions) {
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
  },
};
</script>

<style lang="scss" scoped>
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
  background: rgba(229, 21, 62, 0.5);
  border: 1px solid rgb(229, 21, 62);
}

.correctAnswer {
  background: rgba(56, 173, 182, 0.5);
  border: 1px solid rgb(56, 173, 182);
}
.studentResponse {
  color: red;
}
.correctAnswerText {
  color: green;
}
</style>
