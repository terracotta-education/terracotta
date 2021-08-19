<template>
  <v-container fluid>
    <v-row>
      <v-col>
        <template v-if="!submitted && questionValues.length > 0">
          <form v-on:submit.prevent="handleSubmit" style="width: 100%;">
            <div class="answerSection mt-5 w-100">

              <!-- Question -->
              <v-card
                class="mt-5 mb-2"
                outlined
                v-for="(question, index) in assessment.questions"
                :key="question.questionId"
              >
                <v-card-title>
                  <v-row>
                    <v-col cols="1">
                      <span>{{ index + 1 }}</span>
                    </v-col>
                    <v-col cols="8">
                      <span v-html="question.html"></span>
                    </v-col>
                    <v-col>
                      <div class="total-points text-right ml-2">{{ question.points }} Point{{ (question.points > 1)? 's' : '' }}</div>
                    </v-col>
                  </v-row>
                </v-card-title>
                <!-- Options (Answers) -->
                <v-card-text v-if="questionValues.length>0">
                  <v-row
                    v-for="answer in question.answers"
                    :key="answer.answerId"
                  >
                    <v-col cols="1">
                      &nbsp;
                    </v-col>
                    <v-col cols="10">
                      <v-card outlined>
                        <v-card-title class="py-0">
                          <div class="question-input">
                            <v-radio-group v-model="questionValues.find(({questionId}) => questionId===question.questionId).answerId">
                              <v-radio
                                class="radioButton"
                                :value="answer.answerId"
                              >
                              </v-radio>
                            </v-radio-group>
                            <span v-html="answer.html"></span>
                          </div>
                        </v-card-title>
                      </v-card>
                    </v-col>
                  </v-row>

                </v-card-text>
              </v-card>
            </div>

            <v-btn
              :disabled="!!questionValues.find(({answerId}) => !answerId)"
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
import {mapActions, mapGetters} from 'vuex';

export default {
  name: 'StudentQuiz',
  props: ['experimentId','assignmentId'],
  data() {
    return {
      maxPoints: 0,
      questionValues: [],
      conditionId: null,
      treatmentId: null,
      submissionId: null,
      assessmentId: null,
      submitted: false
    };
  },
  computed: {
    ...mapGetters({
      assessment: 'assessment/assessment',
    }),
  },
  methods: {
    ...mapActions({
      reportStep: 'api/reportStep',
      fetchAssessment: "assessment/fetchAssessment",
      createQuestionSubmission: "submissions/createQuestionSubmission",
    }),
    async handleSubmit() {
      const reallySubmit = await this.$swal({
        icon: 'question',
        text: 'Are you ready to submit your answers?',
        showCancelButton: true,
        confirmButtonText: 'Yes, submit',
        cancelButtonText: 'No, cancel',
      })
      if(reallySubmit.isConfirmed){
        try{
          this.submitQuiz()
        } catch (error) {
          this.$swal({
            text: 'Could not submit',
            icon: 'error'
          })
        }
      }
    },
    async submitQuiz() {
      try {
        const experimentId = this.experimentId
        const step = 'student_submission'
        const parameters = {submissionIds:this.submissionId}
        const questions = this.questionValues.map(q => {
          return {
            questionId: q.questionId,
            answerSubmissionDtoList: [{answerId:q.answerId}]
          }
        })

        // submit questions - updateQuestionSubmission()
        await this.createQuestionSubmission([
          this.experimentId,
          this.conditionId,
          this.treatmentId,
          this.assessmentId,
          this.submissionId,
          questions
        ])

        // submit step
        await this.reportStep({experimentId, step, parameters})

        this.submitted = true
      } catch (e) {
        console.error({e})
      }
    }
  },
  async created() {
    const experimentId = this.experimentId
    const step = 'launch_assignment'

    try {
      const stepResponse = await this.reportStep({experimentId, step})

      if (stepResponse?.status === 200) {
        const data = stepResponse?.data
        this.conditionId = data.conditionId
        this.treatmentId = data.treatmentId
        this.assessmentId = data.assessmentId
        this.submissionId = data.submissionId

        this.fetchAssessment([
          experimentId,
          data.conditionId,
          data.treatmentId,
          data.assessmentId,
        ]);

        this.questionValues = this.assessment.questions.map(q => {
          return {
            questionId: q.questionId,
            answerId: null
          }
        })
      }
    } catch (e) {
      console.error({e})
    }
  }
}
</script>


<style lang="scss" scoped>
.total-points {
  line-height: 24px;
  font-size: 16px;
  font-weight: 400;
}
.question-input {
  display: flex;
  flex-direction: row;
  align-items: center;
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
</style>
