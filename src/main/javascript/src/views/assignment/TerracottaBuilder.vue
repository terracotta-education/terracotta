<template>
  <div v-if="experiment && condition">
    <h1>
      Add your treatment for
      Assignment 1’s condition: <strong>{{condition.name}}</strong>
    </h1>
    <form
      @submit.prevent="saveTreatment('ExperimentDesignDescription')"
      class="my-5"
    >
      <v-text-field
        v-model="treatmentName"
        :rules="rules"
        label="Treatment name"
        placeholder="e.g. Lorem ipsum"
        autofocus
        outlined
        required
      ></v-text-field>
      <v-text-field
        v-model="treatmentDescription"
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

            <v-textarea
              v-model="question.question"
              :rules="rules"
              label="Question"
              autofocus
              outlined
              required
            ></v-textarea>
            <v-text-field
              v-model="question.points"
              label="Points"
              outlined
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
              @click="handleAddOption(qIndex)"
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
        @click="handleAddQuestion"
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
export default {
  name: 'TerracottaBuilder',
  props: ['experiment'],
  computed: {
    condition() {
      return this.experiment.conditions.find(c => parseInt(c.conditionId) === parseInt(this.$route.params.condition_id))
    }
  },
  data() {
    return {
      rules: [
        v => v && !!v.trim() || 'required',
        v => (v || '').length <= 255 || 'A maximum of 255 characters is allowed'
      ],
      treatmentName: '',
      treatmentDescription: '',
      questions: [
        {
          question: '',
          options: [{
            option: '',
            correct: false
          }],
          points: 0
        },
          {
          question: '',
          options: [{
            option: '',
            correct: false
          }],
          points: 0
        }
      ]
    }
  },
  methods: {
    handleAddQuestion() {
      this.questions.push({
        question: '',
        options: [{
          option: '',
          correct: false
        }],
        points: 0
      })
    },
    handleAddOption (q) {
      this.questions[q].options.push({
        option: '',
        correct: false
      })
    },
    handleToggleCorrect(q, o) {
      this.questions[q].options[o].correct = !this.questions[q].options[o].correct
    },
    handleDeleteOption(q, o) {
      this.questions[q].options.splice(o,1)
    },
    saveTreatment() {
      alert('submit')
    },
    saveExit() {
      // this.$router.push({name:'Home', params:{experiment: this.experiment.experiment_id}})
      alert('save & exit')
    }
  }
};
</script>

<style lang="scss">
  .v-expansion-panel-header--active {
    border-bottom: 2px solid map-get($grey, 'lighten-2');
  }
  .options-list {
    list-style: none;
  }
</style>