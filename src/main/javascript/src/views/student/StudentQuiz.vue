<template>
  <v-container fluid v-if="assessment && questionValues.length > 0">
    <v-row>
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
                      <div class="total-points text-right ml-2">
                        {{ question.points }} Point{{
                          question.points > 1 ? "s" : ""
                        }}
                      </div>
                    </v-col>
                  </v-row>
                </v-card-title>
                <!-- Options (Answers) -->
                <v-card-text v-if="questionValues.length > 0">
                  <template v-if="question.questionType === 'MC'">
                    <multiple-choice-response-editor
                      :answers="question.answers"
                      v-model="
                        questionValues.find(
                          ({ questionId }) => questionId === question.questionId
                        ).answerId
                      "
                    />
                  </template>
                  <template v-else-if="question.questionType === 'ESSAY'">
                    <essay-response-editor
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
              v-else
              :disabled="!allQuestionsAnswered"
              elevation="0"
              color="primary"
              class="mt-4"
              type="submit"
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
import { mapActions, mapGetters } from "vuex";
import EssayResponseEditor from "./EssayResponseEditor.vue";
import MultipleChoiceResponseEditor from "./MultipleChoiceResponseEditor.vue";
import YoutubeEventCapture from "./YoutubeEventCapture.vue";

export default {
  name: "StudentQuiz",
  props: ["experimentId", "assignmentId"],
  components: {
    EssayResponseEditor,
    MultipleChoiceResponseEditor,
    YoutubeEventCapture,
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
      questionSubmissions: null,
    };
  },
  computed: {
    ...mapGetters({
      assessment: "assessment/assessment",
      answerableQuestions: "assessment/answerableQuestions",
      questionPages: "assessment/questionPages",
      // questionSubmissions: "submissions/questionSubmissions",
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
  },
  methods: {
    ...mapActions({
      reportStep: "api/reportStep",
      fetchAssessmentForSubmission: "assessment/fetchAssessmentForSubmission",
      fetchQuestionSubmissions: "submissions/fetchQuestionSubmissions",
      createQuestionSubmissions: "submissions/createQuestionSubmissions",
      createAnswerSubmissions: "submissions/createAnswerSubmissions",
    }),
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
          const existingQuestionSubmission = this.questionSubmissions.find(
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
  },
  async created() {
    const experimentId = this.experimentId;
    const step = "launch_assignment";

    try {
      const stepResponse = await this.reportStep({ experimentId, step });

      if (stepResponse?.status === 200) {
        const data = stepResponse?.data;
        this.conditionId = data.conditionId;
        this.treatmentId = data.treatmentId;
        this.assessmentId = data.assessmentId;
        this.submissionId = data.submissionId;

        await this.fetchAssessmentForSubmission([
          experimentId,
          data.conditionId,
          data.treatmentId,
          data.assessmentId,
          data.submissionId,
        ]);

        this.questionSubmissions = data.questionSubmissionDtoList;

        this.questionValues = this.answerableQuestions.map((q) => {
          return {
            questionId: q.questionId,
            answerId: null,
            response: null,
          };
        });
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
  },
};
</script>

<style lang="scss" scoped>
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
