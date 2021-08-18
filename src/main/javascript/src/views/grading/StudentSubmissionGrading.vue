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
                    studentSubmittedAnswers(question.questionId).includes(
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
                        <v-radio-group v-model="selected">
                          <v-radio
                            class="radioButton"
                            :value="
                              answer.correct ||
                                studentSubmittedAnswers(
                                  question.questionId
                                ).includes(answer.answerId)
                            "
                            readonly
                            name="selected"
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
                            studentSubmittedAnswers(
                              question.questionId
                            ).includes(answer.answerId)
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
  },
  data() {
    return {
      selected: true,
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
      return filteredResponse.length > 0
        ? filteredResponse[0]
        : { answerSubmissionDtoList: [] };
    },

    studentSubmittedAnswers(questionId) {
      this.questionScoreMap[questionId] = this.studentResponseForQuestionId(
        questionId
      ).alteredGrade || 0;

      let sum = 0;
      Object.keys(this.questionScoreMap)?.map((qId) => {
        this.isSameAssessmentQuestion(qId)
          ? (sum = sum + this.questionScoreMap[qId])
          : sum;
      });
      this.maxPoints = sum;

      return this.studentResponseForQuestionId(
        questionId
      ).answerSubmissionDtoList?.map((answer) => answer.answerId);
    },

    async saveExit() {
      const updateSubmissions = this.studentResponse.map((response) => {
        console.log(JSON.stringify(response));
        return {
          questionSubmissionId: response.questionSubmissionId,
          answerSubmissionDtoList: response.answerSubmissionDtoList,
          alteredGrade: +this.questionScoreMap[response.questionId],
        };
      });

      try {
        await this.updateQuestionSubmission([
          this.experiment_id,
          this.condition_id,
          this.treatment_id,
          this.assessment_id,
          this.submissions[0].submissionId,
          updateSubmissions,
        ]);
        this.$router.push({
          name: this.$router.currentRoute.meta.previousStep,
        });
      } catch (error) {
        return Promise.reject(error);
      }
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
      this.submissions[0].submissionId,
    ]);
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
