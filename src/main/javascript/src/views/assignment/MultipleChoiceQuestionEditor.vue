<template>
  <!-- proxy props and event listeners to question-editor -->
  <question-editor v-bind="$props" v-on="$listeners">
    <h4><strong>Options</strong></h4>
    <p class="ma-0 mb-3">Select correct option(s) below</p>

    <ul class="options-list pa-0 mb-6">
      <li
        v-for="(answer, aIndex) in question.answers"
        :key="answer.answerId"
        class="mb-3"
      >
        <v-row align="center" class="flex-nowrap">
          <v-col class="py-0">
            <v-btn
              icon
              tile
              class="correct"
              :class="{ 'green--text': answer.correct }"
              @click="handleToggleCorrect(answer)"
            >
              <template v-if="!answer.correct">
                <v-icon>mdi-checkbox-marked-circle-outline</v-icon>
              </template>
              <template v-else>
                <v-icon>mdi-checkbox-marked-circle</v-icon>
              </template>
            </v-btn>
          </v-col>
          <v-col class="flex-basis-auto">
            <v-text-field
              :value="answer.html"
              @input="updateAnswerHtml(answer, $event)"
              :label="`Option ${aIndex + 1}`"
              :rules="longString"
              hide-details
              outlined
              required
            ></v-text-field>
          </v-col>
          <v-col class="py-0">
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
    <v-row align="center" class="flex-nowrap">
      <v-col cols="auto"><div class="icon-button-spacer"></div></v-col>
      <v-col cols="auto">
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
    </v-row>
    <template v-slot:actions>
      <div class="d-flex align-center">
        <v-switch
          v-model="randomizeAnswers"
          class="randomize-answers-switch"
          label="Randomize options"
        />
      </div>
    </template>
  </question-editor>
</template>

<script>
import { mapActions, mapMutations } from "vuex";
import QuestionEditor from "./QuestionEditor.vue";

export default {
  props: ["question"],
  components: { QuestionEditor },
  data() {
    return {
      longString: [(v) => (v && !!v.trim()) || "required"],
    };
  },
  computed: {
    experiment_id() {
      return parseInt(this.$route.params.experiment_id);
    },
    treatment_id() {
      return parseInt(this.$route.params.treatment_id);
    },
    assessment_id() {
      return parseInt(this.$route.params.assessment_id);
    },
    condition_id() {
      return parseInt(this.$route.params.condition_id);
    },
    randomizeAnswers: {
      // two-way computed property
      get() {
        return this.question.randomizeAnswers;
      },
      set(value) {
        this.updateQuestions({ ...this.question, randomizeAnswers: value });
      },
    },
  },
  methods: {
    ...mapMutations({
      updateAnswers: "assessment/updateAnswers",
      updateQuestions: "assessment/updateQuestions",
    }),
    ...mapActions({
      createAnswer: "assessment/createAnswer",
      deleteAnswer: "assessment/deleteAnswer",
    }),
    async handleToggleRandomizeOptions() {
      this.randomizeAnswers = !this.randomizeAnswers;
    },
    async handleAddAnswer(question) {
      // POST ANSWER
      try {
        await this.createAnswer([
          this.experiment_id,
          this.condition_id,
          this.treatment_id,
          this.assessment_id,
          question.questionId,
          "",
          false,
          0,
        ]);
      } catch (error) {
        console.error(error);
      }
    },
    handleToggleCorrect(answer) {
      this.updateAnswers({ ...answer, correct: !answer.correct });
    },
    async handleDeleteAnswer(q, a) {
      // DELETE ANSWER
      try {
        await this.deleteAnswer([
          this.experiment_id,
          this.condition_id,
          this.treatment_id,
          this.assessment_id,
          q.questionId,
          a.answerId,
        ]);
      } catch (error) {
        console.error("handleDeleteAnswer | catch", { error });
        this.$swal("there was a problem deleting the answer");
      }
    },
    updateAnswerHtml(answer, value) {
      this.updateAnswers({ ...answer, html: value });
    },
  },
};
</script>

<style lang="scss" scoped>
@import "~vuetify/src/components/VBtn/_variables.scss";
.options-list {
  list-style: none;
}
.flex-basis-auto {
  flex-basis: auto;
}
.icon-button-spacer {
  // In order to line up with checkbox icon column, size the same as a default button
  width: #{map-get($map: $btn-sizes, $key: "default")}px;
}
.randomize-answers-switch {
  margin-top: 0px;
}
.randomize-answers-switch::v-deep .v-input__slot {
  // Put the label before the switch
  flex-direction: row-reverse;
}
.randomize-answers-switch::v-deep .v-input--selection-controls__input {
  margin-left: 10px;
}
</style>
