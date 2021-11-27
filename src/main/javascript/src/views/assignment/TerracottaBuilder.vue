<template>
  <div class="terracotta-builder" v-if="experiment && assessment">
    <h1>
      Add your treatment for
      {{assignment.title}}'s condition: <strong>{{condition.name}}</strong>
    </h1>
    <form
      @submit.prevent="saveAll('AssignmentYourAssignments')"
      class="my-5"
    >
      <v-text-field
        v-model="assessment.title"
        :rules="rules"
        label="Treatment name"
        placeholder="e.g. Lorem ipsum"
        autofocus
        outlined
        required
      ></v-text-field>
      <v-textarea
        v-model="assessment.html"
        label="Instructions or description (optional)"
        placeholder="e.g. Lorem ipsum"
        outlined
      ></v-textarea>

      <h4 class="mb-3"><strong>Questions</strong></h4>

      <template v-if="questions && questions.length>0">
        <v-expansion-panels class="v-expansion-panels--outlined mb-6" flat accordion>
          <v-expansion-panel
            v-for="(question, qIndex) in questions"
            :key="qIndex"
            class="text-left"
          >
            <template v-if="question">
              <v-expansion-panel-header class="text-left">
                <h2 class="pa-0">
                  {{ qIndex + 1 }}
                  <span class="pl-3" v-if="question.html" v-html="question.html" ></span>
                </h2>
              </v-expansion-panel-header>
              <v-expansion-panel-content>
                <tiptap-vuetify
                  v-model="question.html"
                  placeholder="Question"
                  class="mb-6 outlined"
                  :extensions="extensions"
                  :card-props="{ flat: true }"
                  :rules="rules"
                  required
                />
                <v-text-field
                  v-model="question.points"
                  :rules="numberRule"
                  label="Points"
                  type="number"
                  step="any"
                  outlined
                  required
                ></v-text-field>

                <template v-if="question.questionType === 'MC'">
                  <h4><strong>Options</strong></h4>
                  <p class="ma-0 mb-3">Select correct option(s) below</p>

                  <ul class="options-list pa-0 mb-6">
                    <li
                      v-for="(answer, aIndex) in question.answers"
                      :key="aIndex"
                      class="mb-3"
                    >
                      <v-row align="center">
                        <v-col class="py-0" cols="1">
                          <v-btn
                            icon
                            tile
                            class="correct"
                            :class="{'green--text':answer.correct}"
                            @click="handleToggleCorrect(qIndex, aIndex)"
                          >
                            <template v-if="!answer.correct">
                              <v-icon>mdi-checkbox-marked-circle-outline</v-icon>
                            </template>
                            <template v-else>
                              <v-icon>mdi-checkbox-marked-circle</v-icon>
                            </template>
                          </v-btn>
                        </v-col>
                        <v-col cols="9">
                          <v-text-field
                            v-model="answer.html"
                            :label="`Option ${aIndex + 1}`"
                            :rules="longString"
                            hide-details
                            outlined
                            required
                          ></v-text-field>
                        </v-col>
                        <v-col class="py-0" cols="2">
                          <v-btn
                            icon
                            tile
                            class="delete_option"
                            @click="handleDeleteAnswer(question, answer)"
                          >
                            <v-icon>mdi-delete</v-icon>
                          </v-btn>
                        </v-col>
                      </v-row>
                    </li>
                  </ul>
                </template>

                <v-row>
                  <v-col v-if="question.questionType === 'MC'">
                    <v-btn
                      elevation="0"
                      color="primary"
                      class="px-0"
                      @click="handleAddAnswer(question)"
                      plain
                    >
                      Add Option
                    </v-btn>
                  </v-col>
                  <v-col class="text-right">
                    <v-menu>
                      <template v-slot:activator="{ on, attrs }">
                        <v-icon
                          color="black"
                          v-bind="attrs"
                          v-on="on"
                        >
                          mdi-dots-horizontal
                        </v-icon>
                      </template>
                      <v-list class="text-left">
                        <v-list-item @click="handleDeleteQuestion(question)">
                          <v-list-item-title>Delete Question</v-list-item-title>
                        </v-list-item>
                      </v-list>
                    </v-menu>
                  </v-col>
                </v-row>

              </v-expansion-panel-content>
            </template>
          </v-expansion-panel>
        </v-expansion-panels>
      </template>
      <template v-else>
        <p class="grey--text">Add questions to continue</p>
      </template>

      <div>
        <h4><strong>Add Question</strong></h4>
        <v-btn @click="handleAddQuestion('MC')" color="primary" elevation="0" outlined class="mr-1">Multiple Choice</v-btn>
        <v-btn @click="handleAddQuestion('ESSAY')" color="primary" elevation="0" outlined class="mr-1">Essay</v-btn>
      </div>
      <br>
      <v-btn
        :disabled="contDisabled"
        elevation="0"
        color="primary"
        class="mr-4"
        type="submit"
      >
        Continue
      </v-btn>
    </form>
  </div>
</template>

<script>
import {mapActions, mapGetters} from 'vuex'
import { TiptapVuetify, Heading, Bold, Italic, Strike, Underline, Code, Paragraph, BulletList, OrderedList, ListItem, Link, Blockquote, HardBreak, HorizontalRule, History } from 'tiptap-vuetify'

export default {
  name: 'TerracottaBuilder',
  props: ['experiment'],
  computed: {
    assignment_id() {
      return parseInt(this.$route.params.assignment_id)
    },
    exposure_id() {
      return parseInt(this.$route.params.exposure_id)
    },
    treatment_id() {
      return parseInt(this.$route.params.treatment_id)
    },
    assessment_id() {
      return parseInt(this.$route.params.assessment_id)
    },
    condition_id() {
      return parseInt(this.$route.params.condition_id)
    },
    condition() {
      return this.experiment.conditions.find(c => parseInt(c.conditionId) === parseInt(this.condition_id))
    },
    ...mapGetters({
      assignment: 'assignment/assignment',
      storeAssessment: 'assessment/assessment',
      storeQuestions: 'assessment/questions'
    }),
    contDisabled() {
      return !this.questions || this.questions.length<1 || this.questions.some(q => (q.html.trim() === '<p></p>')) || !this.assessment.title || !this.assessment.title.trim() || this.assessment.title.length>255
    }
  },
  data() {
    return {
      rules: [
        v => v && !!v.trim() || 'required',
        v => (v || '').length <= 255 || 'A maximum of 255 characters is allowed',
      ],
      longString: [
        v => v && !!v.trim() || 'required'
      ],
      numberRule: [
        v => v && !isNaN(v)  || 'required',
        v => (!isNaN(parseFloat(v))) && v >= 0 || 'The point value cannot be negative'
      ],
      extensions: [
        History,
        Blockquote,
        Link,
        Underline,
        Strike,
        Italic,
        ListItem,
        BulletList,
        OrderedList,
        [Heading, {
          options: {
            levels: [1, 2, 3]
          }
        }],
        Bold,
        Code,
        HorizontalRule,
        Paragraph,
        HardBreak
      ],
      questions: null,
      assessment: null,
    }
  },
  methods: {
    ...mapActions({
      fetchAssessment: 'assessment/fetchAssessment',
      updateAssessment: 'assessment/updateAssessment',
      createQuestion: 'assessment/createQuestion',
      updateQuestion: 'assessment/updateQuestion',
      deleteQuestion: 'assessment/deleteQuestion',
      createAnswer: 'assessment/createAnswer',
      updateAnswer: 'assessment/updateAnswer',
      deleteAnswer: 'assessment/deleteAnswer'
    }),
    async handleAddQuestion(questionType) {
      // POST QUESTION
      try {
        const response = await this.createQuestion([
          this.experiment.experimentId,
          this.condition_id,
          this.treatment_id,
          this.assessment_id,
          0,
          questionType,
          0,
          ''
        ]);
        this.questions.push(JSON.parse(JSON.stringify(response.data)));
      } catch (error) {
        console.error(error)
      }
    },
    async handleAddAnswer (question) {
      // POST ANSWER
      try {
        const response = await this.createAnswer([
          this.experiment.experimentId,
          this.condition_id,
          this.treatment_id,
          this.assessment_id,
          question.questionId,
          '',
          false,
          0
        ])
        if (!question.answers) {
          question.answers = [];
        }
        question.answers.push(JSON.parse(JSON.stringify(response.data)));
      } catch (error) {
        console.error(error)
      }
    },
    handleToggleCorrect(q, a) {
      this.questions[q].answers[a].correct = !this.questions[q].answers[a].correct
    },
    async handleDeleteAnswer(q, a) {
      // DELETE ANSWER
      try {
        await this.deleteAnswer([
          this.experiment.experimentId,
          this.condition_id,
          this.treatment_id,
          this.assessment_id,
          q.questionId,
          a.answerId
        ]);
        const answerIndex = q.answers.findIndex(answer => answer.answerId === a.answerId);
        q.answers.splice(answerIndex, 1);
      } catch (error) {
        console.error("handleDeleteAnswer | catch", {error})
        this.$swal('there was a problem deleting the answer')
      }
    },
    async handleDeleteQuestion(question) {
      // DELETE QUESTION
      const reallyDelete = await this.$swal({
        icon: 'question',
        text: `Are you sure you want to delete the question?`,
        showCancelButton: true,
        confirmButtonText: 'Yes, delete it',
        cancelButtonText: 'No, cancel',
      });
      if (reallyDelete?.isConfirmed) {
        try {
          await this.deleteQuestion([
            this.experiment.experimentId,
            this.condition.conditionId,
            this.treatment_id,
            this.assessment_id,
            question.questionId
          ])
          // splice the question out of questions array
          const questionIndex = this.questions.findIndex(que => que.questionId === question.questionId);
          if (questionIndex >= 0) {
            this.questions.splice(questionIndex, 1);
          }
        } catch (error) {
          console.error("handleDeleteQuestion | catch", {error})
          this.$swal('there was a problem deleting the question')
        }
      }
    },
    async handleSaveAssessment() {
      // PUT ASSESSMENT TITLE & HTML (description)
      try {
        return await this.updateAssessment([
          this.experiment.experimentId,
          this.condition.conditionId,
          this.treatment_id,
          this.assessment_id,
          this.assessment.title,
          this.assessment.html
        ])
      } catch (error) {
        console.error("handleCreateAssessment | catch", {error})
      }
    },
    async handleSaveQuestions() {
      // LOOP AND PUT QUESTIONS
      return Promise.all(
        this.questions.map(async (question, index) => {
          // save question
          try {
            const q = await this.updateQuestion([
              this.experiment.experimentId,
              this.condition_id,
              this.treatment_id,
              this.assessment_id,
              question.questionId,
              question.html,
              question.points,
              index,
              question.questionType
            ])

            return Promise.resolve(q)
          } catch (error) {
            return Promise.reject(error)
          }
        })
      )
    },
    async handleSaveAnswers() {
      // LOOP AND PUT ANSWERS
      return Promise.all(
        this.questions.map((question) => {
          question?.answers?.map(async (answer, answerIndex) => {
            try {
              const a = await this.updateAnswer([
                this.experiment.experimentId,
                this.condition_id,
                this.treatment_id,
                this.assessment_id,
                answer.questionId,
                answer.answerId,
                answer.answerType,
                answer.html,
                answer.correct,
                answerIndex
              ])
              return Promise.resolve(a)
            } catch (error) {
              return Promise.reject(error)
            }
          })
        })
      )
    },
    async saveAll (routeName) {
      if (this.questions.some(q => !q.html)) {
        this.$swal('Please fill or delete empty questions.')
        return false
      }

      const savedAssessment = await this.handleSaveAssessment()
      if (savedAssessment) {
        await this.handleSaveQuestions()
        await this.handleSaveAnswers()

        this.$router.push({name:routeName, params: {exposure_id: this.exposure_id}})
      }
    },
    saveExit() {
      this.saveAll('home')
    }
  },
  async created() {
    await this.fetchAssessment([this.experiment.experimentId, this.condition_id, this.treatment_id, this.assessment_id])
    // Clone assessment and questions so we can manipulate their values
    this.assessment = JSON.parse(JSON.stringify(this.storeAssessment));
    this.questions = JSON.parse(JSON.stringify(this.storeQuestions));
  },
  components: {
    TiptapVuetify
  }
};
</script>

<style lang="scss">
  .terracotta-builder {
    .v-expansion-panel-header {
      &--active {
        border-bottom: 2px solid map-get($grey, 'lighten-2');
      }
      h2 {
        display: inline-block;
        max-height: 1em;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;

        > span {
          * {
            display: inline;
            font-size: 16px;
            line-height: 1em;
            margin: 0;
            padding: 0;
            vertical-align: middle;
          }
        }
      }
    }
    .options-list {
      list-style: none;
    }
    .tiptap-vuetify-editor {
      box-shadow: none;
      border-radius: 4px;
      border: 1px solid map-get($grey, 'base');
      overflow: hidden;

      .ProseMirror {
        margin: 20px 5px !important;

        .is-editor-empty::before {
          color: map-get($grey, 'darken-1');
          font-style: normal;
        }
      }
      &__toolbar {
        border-top: 1px solid map-get($grey, 'base');
        border-radius: 0 !important;
      }
      .v-card {
        display: flex;
        flex-direction: column-reverse;
      }
    }
  }
</style>
