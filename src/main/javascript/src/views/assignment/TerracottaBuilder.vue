<template>
  <div class="terracotta-builder" v-if="experiment && condition">
    <h1>
      Add your treatment for
      Assignment 1’s condition: <strong>{{condition.name}}</strong>
    </h1>
    <form
      @submit.prevent="saveAll('ExperimentDesignDescription')"
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
      <v-text-field
        v-model="assessment.html"
        label="Instructions or description (optional)"
        placeholder="e.g. Lorem ipsum"
        outlined
      ></v-text-field>

      <h4 class="mb-3"><strong>Multiple Choice Questions</strong></h4>

      <v-expansion-panels class="v-expansion-panels--outlined mb-6" flat accordion>
        <v-expansion-panel
          v-for="(question, qIndex) in questions"
          :key="qIndex"
          class="text-left"
        >
          <v-expansion-panel-header class="text-left">
            <h2 class="pa-0">{{ qIndex + 1 }}</h2>
          </v-expansion-panel-header>
          <v-expansion-panel-content>
            <tiptap-vuetify
              v-model="question.body"
              placeholder="Question"
              class="mb-6 outlined"
              :extensions="extensions"
              :card-props="{ flat: true }"
              :rules="rules"
              required
            />
            <v-text-field
              v-model="question.points"
              label="Points"
              type="number"
              outlined
              required
            ></v-text-field>

            <h4><strong>Options</strong></h4>
            <p class="ma-0 mb-3">Select correct option(s) below</p>

            <ul class="options-list pa-0 mb-6">
              <li
                v-for="(option, oIndex) in question.options"
                :key="oIndex"
                class="mb-3"
              >
                <v-row align="center">
                  <v-col class="py-0" cols="1">
                    <v-btn
                      icon
                      tile
                      class="correct"
                      :class="{'green--text':option.correct}"
                      @click="handleToggleCorrect(qIndex, oIndex)"
                    >
                      <template v-if="!option.correct">
                        <v-icon>mdi-checkbox-marked-circle-outline</v-icon>
                      </template>
                      <template v-else>
                        <v-icon>mdi-checkbox-marked-circle</v-icon>
                      </template>
                    </v-btn>
                  </v-col>
                  <v-col cols="9">
                    <v-text-field
                      v-model="option.option"
                      :label="`Option ${oIndex + 1}`"
                      :rules="rules"
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
                      @click="handleDeleteOption(qIndex, oIndex)"
                    >
                      <v-icon>mdi-delete</v-icon>
                    </v-btn>
                  </v-col>
                </v-row>
              </li>
            </ul>

            <v-btn
              elevation="0"
              color="primary"
              class="mr-4 mb-3 px-0"
              @click="handleAddOption(question)"
              plain
            >
              Add another Option
            </v-btn>

          </v-expansion-panel-content>
        </v-expansion-panel>
      </v-expansion-panels>

      <v-btn
        elevation="0"
        color="primary"
        class="mr-4 mb-3 px-0"
        @click="handleAddMCQuestion"
        plain
      >
        Add another Question
      </v-btn>
      <br>
      <v-btn
        :disabled="!experiment.title || !experiment.title.trim()"
        elevation="0"
        color="primary"
        class="mr-4"
        type="submit"
      >
        Save Treatment
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
    condition() {
      return this.experiment.conditions.find(c => parseInt(c.conditionId) === parseInt(this.$route.params.condition_id))
    },
    assignment_id() {
      return this.$route.params.assignment_id
    },
    treatment_id() {
      return this.$route.params.treatment_id
    },
    assessment_id() {
      return this.$route.params.assessment_id
    },
    questions: {
      get () {
        return this.$store.state.assessment.assessment.questions
      },
      set (question) {
        this.$store.commit('assessment/updateQuestions', question)
      }
    },
    ...mapGetters({
      assessment: 'assessment/assessment'
    })
  },
  data() {
    return {
      rules: [
        v => v && !!v.trim() || 'required',
        v => (v || '').length <= 255 || 'A maximum of 255 characters is allowed'
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
      ]
    }
  },
  methods: {
    ...mapActions({
      fetchAssessment: 'assessment/fetchAssessment',
      updateAssessment: 'assessment/updateAssessment',
      createQuestion: 'assessment/createQuestion',
      createAnswer: 'assessment/createAnswer'
    }),
    async handleAddMCQuestion() {
      try {
        await this.createQuestion([
          this.experiment.experimentId,
          this.condition.conditionId,
          this.treatment_id,
          this.assessment_id,
          0,
          'MC',
          0,
          ''
        ])
      } catch (error) {
        console.error(error)
      }
    },
    async handleAddOption (question) {
      console.log(question)
      try {
        await this.createAnswer([
          this.experiment.experimentId,
          this.condition.conditionId,
          this.treatment_id,
          this.assessment_id,
          question.questionId,
          '',
          false,
          0
        ])
      } catch (error) {
        console.error(error)
      }
    },
    handleToggleCorrect(q, o) {
      this.questions[q].options[o].correct = !this.questions[q].options[o].correct
    },
    handleDeleteOption(q, o) {
      this.questions[q].options.splice(o,1)
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
    async handleSaveQuestions(assessment) {
      if (!assessment) { return false }
      // LOOP AND PUT QUESTIONS
      return Promise.all(
        this.assessment.questions.map(async (question, index) => {
          // save question
          try {
            const q = await this.createQuestion([
              this.experiment.experimentId,
              this.condition.conditionId,
              this.treatment_id,
              assessment.assessmentId,
              index,
              "MC",
              question.points,
              question.body
            ])
            return Promise.resolve(q)
          } catch (error) {
            return Promise.reject(error)
          }
        })
      )
    },
    async handleSaveAnswers(treatment, assessment, questions) {
      console.log({questions})
      if (!treatment || !assessment || !questions) { return false }
      // LOOP AND PUT ANSWERS
      return Promise.all(
        this.assessment.questions.map((question) => {
          question?.options?.map(async (answer, answerIndex) => {
            console.log({answer, answerIndex})
            try {
              const a = await this.createAnswer([
                this.experiment.experimentId,
                this.condition.conditionId,
                this.treatment_id,
                assessment.assessmentId,
                question.questionId,
                answer.body,
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
    async saveAll () {
      const assessment = await this.handleSaveAssessment()
      console.log({assessment})
      if (assessment) {
        console.table({assessment})
        // const questions = await this.handleSaveQuestions()
        // console.table({questions})
        // const answers = await this.handleSaveAnswers(questions)
        // console.log({answers})
      }
    },
    saveExit() {
      this.$router.push({name:'Home', params:{experiment: this.experiment.experiment_id}})
    }
  },
  created() {
    this.fetchAssessment([this.experiment.experimentId, this.condition.conditionId, this.treatment_id, this.assessment_id])
  },
  components: {
    TiptapVuetify
  }
};
</script>

<style lang="scss">
  .terracotta-builder {
    .v-expansion-panel-header--active {
      border-bottom: 2px solid map-get($grey, 'lighten-2');
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