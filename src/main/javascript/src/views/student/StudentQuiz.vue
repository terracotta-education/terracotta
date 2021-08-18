<template>
  <form action="" style="width: 100%;">
    <div class="answerSection mt-5 w-100">

      <!-- Individual Question -->
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
            <v-col cols="9">
              <span v-html="question.html"></span>
            </v-col>
            <v-col>
              <div class="total-points text-right ml-2">{{ question.points }} Point{{ (question.points > 1)? 's' : '' }}</div>
            </v-col>
          </v-row>
        </v-card-title>
        <v-card-text>
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
                    <v-radio-group v-model="selected">
                      <v-radio
                        class="radioButton"
                        readonly
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
  </form>
</template>

<script>
import {mapActions, mapGetters} from 'vuex';

export default {
  name: 'StudentQuiz',
  props: ['assignmentId'],
  data() {
    return {
      selected: true,
      questionScoreMap: {},
      maxPoints: 0,
    };
  },
  computed: {
    ...mapGetters({
      assessment: 'assessment/assessment',
    }),
  },
  methods: {
    ...mapActions({
      fetchAssessment: 'assessment/fetchAssessment'
    }),
  },
  async created() {
    // this.fetchAssessment([
    //   this.experiment.experimentId,
    //   this.condition_id,
    //   this.treatment_id,
    //   this.assessment_id,
    // ]);
    await this.fetchAssessment([
      31,
      44,
      110,
      76,
    ]);
    console.log(this.assessment)
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
