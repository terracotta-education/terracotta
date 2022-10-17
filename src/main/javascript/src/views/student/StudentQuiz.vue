<template>
  <v-container fluid v-if="!loading">
    <v-row v-if="readonly">
      <v-col v-if="canTryAgain">
        <v-btn
          @click="handleTryAgain"
          elevation="0"
          color="primary"
          class="mt-4 mb-2"
          type="button"
        >
          Try Again
        </v-btn>
        <p>
          <span v-if="assignmentData.multipleSubmissionScoringScheme === 'HIGHEST'">The highest</span>
          <span v-else-if="assignmentData.multipleSubmissionScoringScheme === 'MOST_RECENT'">The most recent</span>
          <span v-else-if="assignmentData.multipleSubmissionScoringScheme === 'AVERAGE'">The average</span>
          <span v-else-if="assignmentData.multipleSubmissionScoringScheme === 'CUMULATIVE'">A cumulative</span>
           score will be kept
        </p>
      </v-col>
      <v-spacer />
      <v-col v-if="!muted">
        <h2>Submission Details</h2>
        <v-divider />
          <v-list dense flat>
            <v-list-item>
              <v-list-item-content>
                <strong>Time</strong>
              </v-list-item-content>
              <v-list-item-icon>
                <span>{{ timeBeforeSubmission }}</span>
              </v-list-item-icon>
            </v-list-item>
            <v-list-item>
              <v-list-item-content>
                <strong>Submitted</strong>
              </v-list-item-content>
              <v-list-item-icon>
                <span>{{ selectedSubmissionDateSubmitted }}</span>
              </v-list-item-icon>
            </v-list-item>
            <v-list-item>
              <v-list-item-content>
                <strong>Current Score</strong>
              </v-list-item-content>
              <v-list-item-icon>
                <span>{{ currentScore }}</span>
              </v-list-item-icon>
            </v-list-item>
            <v-list-item>
              <v-list-item-content>
                <strong>Kept Score</strong>
              </v-list-item-content>
              <v-list-item-icon>
                <span>{{ keptScore }}</span>
              </v-list-item-icon>
            </v-list-item>
          </v-list>
        <v-divider />
      </v-col>
    </v-row>
    <v-row v-if="cantTryAgainMessage">
      <v-col>
        <v-card
          class="pt-5 px-5 mx-auto yellow lighten-5 rounded-lg"
          outlined
        >
          <p class="pb-0" v-if="cantTryAgainMessage === 'MAX_NUMBER_ATTEMPTS_REACHED'">
            You have reached the maximum number of attempts for this assignment.
          </p>
          <p class="pb-0" v-if="cantTryAgainMessage === 'WAIT_TIME_NOT_REACHED'">
            Wait time not reached... You must wait a period of time before submitting again.
          </p>
        </v-card>
      </v-col>
    </v-row>
    <v-row v-if="readonly">
      <v-col>
        <v-card
          class="pt-5 px-5 mx-auto yellow lighten-5 rounded-lg"
          outlined
          v-if="muted"
        >
          <h3>Your assignment is muted</h3>
          <p class="pb-0">
            Your instructor has not released the grades yet. 
          </p>
        </v-card>
        <div v-if="!muted && assignmentData && assignmentData.submissions">
          <submission-selector
            :submissions="assignmentData.submissions"
            @select="(id) => selectedSubmissionId = id" />
        </div>
      </v-col>
    </v-row>
    <v-row v-if="assessment && questionValues.length > 0">
      <v-col>
        <template v-if="!submitted">
          <!-- only display assessment instructions on the first page -->
          <div
            v-if="assessment.html && questionPageIndex === 0"
            v-html="assessment.html"
          />
          <form
            v-on:submit.prevent="handleSubmit"
            style="width: 100%;"
            ref="form"
          >
            <div class="answerSection mt-5 w-100">
              <v-card
                class="mt-5 mb-2"
                outlined
                v-for="(question, index) in currentQuestionPage.questions"
                :key="question.questionId"
              >
                <v-card-title>
                  <v-row>
                    <v-col cols="1">
                      <span>{{
                        currentQuestionPage.questionStartIndex + index + 1
                      }}</span>
                    </v-col>
                    <v-col cols="8">
                      <youtube-event-capture
                        :experimentId="experimentId"
                        :assessmentId="assessmentId"
                        :conditionId="conditionId"
                        :questionId="question.questionId"
                        :submissionId="submissionId"
                        :treatmentId="treatmentId"
                      >
                        <span v-html="question.html"></span>
                      </youtube-event-capture>
                    </v-col>
                    <v-col>
                      <div class="total-points text-right ml-2" v-if="!readonly">
                        {{ question.points }} Point{{
                          question.points > 1 ? "s" : ""
                        }}
                      </div>
                      <div class="total-points text-right ml-2" v-if="readonly">
                        {{ getQuestionSubmissionValue(question) }}
                        /
                        {{ question.points }} Point{{
                          question.points > 1 ? "s" : ""
                        }}
                      </div>
                    </v-col>
                  </v-row>
                </v-card-title>
                <!-- Options (Answers) -->
                <v-card-text v-if="questionValues && questionValues.length > 0">
                  <template v-if="question.questionType === 'MC'">
                    <multiple-choice-response-editor
                      :answers="getQuestionAnswers(question)"
                      :readonly="readonly"
                      :showAnswers="showAnswers"
                      v-model="questionValues.find(
                          ({ questionId }) => questionId === question.questionId
                        ).answerId"
                    />
                  </template>
                  <template v-else-if="question.questionType === 'ESSAY'">
                    <essay-response-editor
                      :answer="getEssayResponse(question)"
                      :readonly="readonly"
                      v-model="
                        questionValues.find(
                          ({ questionId }) => questionId === question.questionId
                        ).response
                      "
                    />
                  </template>
                </v-card-text>
              </v-card>
            </div>

            <v-btn
              v-if="hasNextQuestionPage"
              @click.prevent="nextPage"
              :disabled="!allCurrentPageQuestionsAnswered"
              elevation="0"
              color="primary"
              class="mt-4"
              type="button"
            >
              Next
            </v-btn>
            <v-btn
              :disabled="!allQuestionsAnswered"
              elevation="0"
              color="primary"
              class="mt-4"
              type="submit"
              v-if="!readonly && !hasNextQuestionPage"
            >
              Submit
            </v-btn>
          </form>
        </template>
        <template v-else>
          <v-alert type="success">Your answers have been submitted.</v-alert>
        </template>
      </v-col>
    </v-row>
  </v-container>
</template>

<script>
import Vue from 'vue';
import { mapActions, mapGetters } from "vuex";
import EssayResponseEditor from "./EssayResponseEditor.vue";
import MultipleChoiceResponseEditor from "./MultipleChoiceResponseEditor.vue";
import YoutubeEventCapture from "./YoutubeEventCapture.vue";
import moment from 'moment';
import SubmissionSelector from '../assignment/SubmissionSelector.vue';

Vue.filter('formatDate', (value) => {
  if (value) {
    return moment(value).format('MM/DD/YYYY hh:mm')
  }
});

export default {
  name: "StudentQuiz",
  props: ["experimentId", "assignmentId"],
  components: {
    EssayResponseEditor,
    MultipleChoiceResponseEditor,
    YoutubeEventCapture,
    SubmissionSelector,
  },
  data() {
    return {
      maxPoints: 0,
      questionValues: [],
      conditionId: null,
      treatmentId: null,
      submissionId: null,
      assessmentId: null,
      submitted: false,
      questionPageIndex: 0,
      assignmentData: null,
      selectedSubmissionId: null,
      readonly: false,
      answers: [],
      loading: false,
    };
  },
  watch: {
    selectedSubmissionId() {
      if (this.selectedSubmission) {
        const { experimentId, conditionId, assessmentId, treatmentId, submissionId } = this.selectedSubmission;
        this.getQuestions(experimentId, conditionId, assessmentId, treatmentId, submissionId);
        this.getAnswers(experimentId, conditionId, assessmentId, treatmentId, submissionId);
      }
    },
    answerableQuestions(newValue) {
      this.questionValues = newValue.map((q) => {
        return {
          questionId: q.questionId,
          answerId: null,
          response: null,
        };
      });
    }
  },
  computed: {
    ...mapGetters({
      assessment: "assessment/assessment",
      answerableQuestions: "assessment/answerableQuestions",
      questionPages: "assessment/questionPages",
      questionSubmissions: "submissions/questionSubmissions"
    }),
    allCurrentPageQuestionsAnswered() {
      return this.areAllQuestionsAnswered(this.currentQuestionPage.questions);
    },
    allQuestionsAnswered() {
      return this.areAllQuestionsAnswered(this.answerableQuestions);
    },
    currentQuestionPage() {
      return this.questionPages[this.questionPageIndex];
    },
    hasNextQuestionPage() {
      return this.questionPageIndex < this.questionPages.length - 1;
    },
    canTryAgain() {
      return this.assignmentData ? this.assignmentData.retakeDetails.retakeAllowed : false;
    },
    cantTryAgainMessage() {
      return this.assignmentData?.retakeDetails?.retakeNotAllowedReason;
    },
    selectedSubmissionDateSubmitted() {
      return moment(this.selectedSubmission?.dateSubmitted).format('MMMM Do YYYY hh:mm');
    },
    timeBeforeSubmission() {
      const time = this.selectedSubmission?.dateSubmitted - this.selectedSubmission?.dateCreated;
      return isNaN(time) ? '' : moment.duration(time, "milliseconds").humanize();
    },
    currentScore() {
      let grade;
      if (!this.selectedSubmission) {
        grade = '-';
      } else {
        const { totalAlteredGrade, alteredCalculatedGrade } = this.selectedSubmission;
        grade = totalAlteredGrade !== null ? totalAlteredGrade : alteredCalculatedGrade;
      }
      return `${grade} / ${this.assignmentData?.maxPoints}`;
    },
    keptScore() {
      const kept = this.assignmentData?.retakeDetails.keptScore;

      return `${kept ? kept : 0} / ${this.assignmentData?.maxPoints}`;
    },
    muted() {
      if (!this.assignmentData) { return true; }
      const { allowStudentViewResponses, studentViewResponsesAfter, studentViewResponsesBefore } = this.assignmentData;
      if (allowStudentViewResponses) {
        const now = Date.now();
        let isAfter = true;
        let isBefore = true;
        if (studentViewResponsesAfter) {
          isAfter = moment(now).isAfter(studentViewResponsesAfter);
        }
        if (studentViewResponsesBefore) {
          isBefore = moment(now).isBefore(studentViewResponsesBefore);
        }
        if (isAfter && isBefore) {
          return false;
        }
      }
      return true;
    },
    showAnswers() {
      if (!this.assignmentData) { return false; }
      const { allowStudentViewCorrectAnswers, studentViewCorrectAnswersAfter, studentViewCorrectAnswersBefore } = this.assignmentData;
      if (allowStudentViewCorrectAnswers) {
        const now = Date.now();
        let isAfter = true;
        let isBefore = true;
        if (studentViewCorrectAnswersAfter) {
          isAfter = moment(now).isAfter(studentViewCorrectAnswersAfter);
        }
        if (studentViewCorrectAnswersBefore) {
          isBefore = moment(now).isBefore(studentViewCorrectAnswersBefore);
        }
        if (isAfter && isBefore) {
          return false;
        }
      }
      return true;
    },
    showResponses() {
      return this.assignmentData?.allowStudentViewCorrectAnswers;
    },
    selectedSubmission() {
      return this.assignmentData?.submissions.find(s => s.submissionId === this.selectedSubmissionId);
    },
    selectedSubmissionConditionId() {
      return this.selectedSubmission?.conditionId;
    },
  },
  methods: {
    ...mapActions({
      reportStep: "api/reportStep",
      fetchAssessmentForSubmission: "assessment/fetchAssessmentForSubmission",
      fetchQuestionSubmissions: "submissions/fetchQuestionSubmissions",
      createQuestionSubmissions: "submissions/createQuestionSubmissions",
      createAnswerSubmissions: "submissions/createAnswerSubmissions",
      clearQuestionSubmissions: "submissions/clearQuestionSubmissions",
    }),
    async handleTryAgain() {
      this.attempt();
    },
    async handleSubmit() {
      const reallySubmit = await this.$swal({
        icon: "question",
        text: "Are you ready to submit your answers?",
        showCancelButton: true,
        confirmButtonText: "Yes, submit",
        cancelButtonText: "No, cancel",
      });
      if (reallySubmit.isConfirmed) {
        try {
          await this.submitQuiz();
        } catch (error) {
          this.$swal({
            text: "Could not submit: " + error.message,
            icon: "error",
          });
        }
      }
    },
    async submitQuiz() {
      try {
        const experimentId = this.experimentId;
        const step = "student_submission";
        const parameters = { submissionIds: this.submissionId };
        const allQuestionSubmissions = this.questionValues.map((q) => {
          const existingQuestionSubmission = this.questionSubmissions?.find(
            (qs) => qs.questionId === q.questionId
          );
          const questionSubmissionId =
            existingQuestionSubmission?.questionSubmissionId;
          const questionSubmission = {
            questionSubmissionId,
            questionId: q.questionId,
            answerSubmissionDtoList: [
              {
                answerId: q.answerId,
                response: q.response,
                questionSubmissionId,
              },
            ],
          };
          return questionSubmission;
        });

        // separate question submissions into existing and new by whether they have id
        const existingQuestionSubmissions = allQuestionSubmissions.filter(
          (qs) => !!qs.questionSubmissionId
        );
        const newQuestionSubmissions = allQuestionSubmissions.filter(
          (qs) => !qs.questionSubmissionId
        );

        // call createAnswerSubmission for all existing question submissions
        const answerSubmissions = existingQuestionSubmissions.map(
          (qs) => qs.answerSubmissionDtoList[0]
        );
        if (answerSubmissions.length > 0) {
          const { data, status } = await this.createAnswerSubmissions([
            this.experimentId,
            this.conditionId,
            this.treatmentId,
            this.assessmentId,
            this.submissionId,
            answerSubmissions,
          ]);
          if (status && ![200, 201].includes(status)) {
            throw Error("Error submitting quiz: " + data);
          }
        }

        // call createQuestionSubmissions for all new question submissions
        if (newQuestionSubmissions.length > 0) {
          const { data, status } = await this.createQuestionSubmissions([
            this.experimentId,
            this.conditionId,
            this.treatmentId,
            this.assessmentId,
            this.submissionId,
            newQuestionSubmissions,
          ]);
          if (status && ![200, 201].includes(status)) {
            throw Error("Error submitting quiz: " + data);
          }
        }

        // submit step
        const { data, status } = await this.reportStep({
          experimentId,
          step,
          parameters,
        });
        if (status && ![200, 201].includes(status)) {
          throw Error("Error submitting quiz: " + data);
        }

        this.submitted = true;
      } catch (e) {
        console.error({ e });
        throw e; // rethrow
      }
    },
    async getAnswers(experimentId, conditionId, assessmentId, treatmentId, submissionId) {
      await this.fetchQuestionSubmissions([
        experimentId,
        conditionId,
        treatmentId,
        assessmentId,
        submissionId,
      ]);
    },
    async getQuestions(experimentId, conditionId, assessmentId, treatmentId, submissionId) {
      this.questionValues = [];
      
      await this.fetchAssessmentForSubmission([
          experimentId,
          conditionId,
          treatmentId,
          assessmentId,
          submissionId,
      ]);
    },
    getQuestionSubmissionValue(question) {
      const value = this.questionSubmissions?.find(({ questionId }) => questionId === question.questionId);
      return value?.calculatedPoints;
    },
    getQuestionAnswers(question) {
      if (!this.readonly) { return question.answers; }
      const questionSubmissionDto = this.questionSubmissions?.find(s => s.questionId === question.questionId);
      if (!questionSubmissionDto) { return []; }

      const answers = questionSubmissionDto.answerDtoList;
      const responses = questionSubmissionDto.answerSubmissionDtoList;
      return answers.map(a => {
        const resp = responses.find(r => r.answerId === a.answerId);
        return {
          ...a,
          studentResponse: resp ? resp.answerId : false
        };
      });
    },
    getEssayResponse(question) {
      if (!this.readonly) { return null; }
      const questionSubmissionDto = this.questionSubmissions?.find(s => s.questionId === question.questionId);
      if (!questionSubmissionDto) { return null; }
      return questionSubmissionDto.answerSubmissionDtoList.find(a => a.questionSubmissionId === questionSubmissionDto.questionSubmissionId);
    },
    areAllQuestionsAnswered(answerableQuestions) {
      for (const question of answerableQuestions) {
        if (question.questionType === "MC") {
          const answer = this.questionValues.find(
            ({ questionId }) => questionId === question.questionId
          ).answerId;
          if (answer === null) {
            return false;
          }
        } else if (question.questionType === "ESSAY") {
          const answer = this.questionValues.find(
            ({ questionId }) => questionId === question.questionId
          ).response;
          if (answer === null || answer.trim() === "") {
            return false;
          }
        } else {
          console.log(
            "Unexpected question type",
            question.questionType,
            question
          );
        }
      }
      return true;
    },
    nextPage() {
      this.questionPageIndex++;
      this.$nextTick(() => {
        this.$refs.form.scrollIntoView({ behavior: "smooth" });
      });
    },
    async attempt() {
      const experimentId = this.experimentId;
      const step = "launch_assignment";
      this.readonly = false;
      this.loading = true;
      try {
        const stepResponse = await this.reportStep({ experimentId, step });

        await this.clearQuestionSubmissions();

        if (stepResponse?.status === 200) {
          const data = stepResponse?.data;
          this.conditionId = data.conditionId;
          this.treatmentId = data.treatmentId;
          this.assessmentId = data.assessmentId;
          this.submissionId = data.submissionId;

          const { experimentId, conditionId, assessmentId, treatmentId, submissionId } = data;
          this.getQuestions(experimentId, conditionId, assessmentId, treatmentId, submissionId);
        }else if(stepResponse?.status == 401) {
          if (stepResponse?.data.toString().includes("Error 150:")) {
            this.$swal({
              text: "You have no more attempts available",
              icon: "error",
            });
          }
        }
        this.loading = false;
      } catch (e) {
        console.error({ e });
      }
    },
  },
  async created() {
    const experimentId = this.experimentId;
    const step = "view_assignment";
    this.loading = true;
    try {
      const stepResponse = await this.reportStep({ experimentId, step });

      if (stepResponse?.status === 200) {
        const { data } = stepResponse;
        this.assignmentData = data;

        const { retakeDetails } = data;
        const { retakeAllowed, submissionAttemptsCount } = retakeDetails;
        // const { retakeAllowed } = retakeDetails;
        if (retakeAllowed && submissionAttemptsCount === 0) {
          this.attempt();
        } else {
          this.readonly = true;
        }

      }else if(stepResponse?.status == 401) {
         if (stepResponse?.data.toString().includes("Error 150:")) {
           this.$swal({
             text: "You have no more attempts available",
             icon: "error",
           });
         }
      }
    } catch (e) {
      console.error({ e });
    }
    this.loading = false;
  },
};
</script>

<style lang="scss" scoped>
.v-application .v-sheet--outlined.yellow.lighten-5 {
  border-color: #FFE0B2 !important;
}
.total-points {
  line-height: 24px;
  font-size: 16px;
  font-weight: 400;
}

.individualScore {
  margin-left: 1px;
}

.cardDetails {
  min-width: 100%;
}
</style>
