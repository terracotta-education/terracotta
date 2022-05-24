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
    };
  },
  computed: {
    ...mapGetters({
      assessment: "assessment/assessment",
      answerableQuestions: "assessment/answerableQuestions",
      questionPages: "assessment/questionPages",
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
      fetchAssessment: "assessment/fetchAssessment",
      createQuestionSubmission: "submissions/createQuestionSubmission",
      createNewSubmission: "submissions/createNewSubmission"
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
          this.submitQuiz();
        } catch (error) {
          this.$swal({
            text: "Could not submit",
            icon: "error",
          });
        }
      }
    },
    async submitQuiz() {
      try {
        const experimentId = this.experimentId;
        const step = "student_submission";
        if (this.submissionId === null) {
         let response = await this.createNewSubmission([
            this.experimentId,
            this.conditionId,
            this.treatmentId,
            this.assessmentId
          ])
          this.submissionId = response.data.submissionId;
        }

        const parameters = { submissionIds: this.submissionId };
        const questions = this.questionValues.map((q) => {
          return {
            questionId: q.questionId,
            answerSubmissionDtoList: [
              { answerId: q.answerId, response: q.response },
            ],
          };
        });

        // submit questions - updateQuestionSubmission()
        await this.createQuestionSubmission([
          this.experimentId,
          this.conditionId,
          this.treatmentId,
          this.assessmentId,
          this.submissionId,
          questions,
        ]);

        // submit step
        await this.reportStep({ experimentId, step, parameters });

        this.submitted = true;
      } catch (e) {
        console.error({ e });
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

        await this.fetchAssessment([
          experimentId,
          data.conditionId,
          data.treatmentId,
          data.assessmentId,
        ]);

        this.questionValues = this.answerableQuestions.map((q) => {
          return {
            questionId: q.questionId,
            answerId: null,
            response: null,
          };
        });
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
