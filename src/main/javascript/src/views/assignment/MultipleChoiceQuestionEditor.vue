<template>
<!-- proxy props and event listeners to question-editor -->
<question-editor
  v-bind="$props"
  v-on="$listeners"
  :isMC="true"
  @edited="handleQuestionEdited"
>
  <h4><strong>Options</strong></h4>
  <p
    class="ma-0 mb-3"
  >
    Select correct option(s) below
  </p>

  <ul
    class="options-list pa-0 mb-6"
  >
    <li
      v-for="(answer, aIndex) in question.answers"
      :key="answer.answerId"
      class="mb-3"
    >
      <v-row
        align="center"
        class="flex-nowrap"
      >
        <v-col
          class="py-0"
        >
          <v-btn
            :class="{
              'green--text': answer.correct
            }"
            :aria-label="`Mark option ${answer.html || aIndex + 1} as correct`"
            @click="handleToggleCorrect(answer)"
            class="correct"
            icon
            tile
          >
            <template
              v-if="!answer.correct"
            >
              <v-icon>mdi-checkbox-marked-circle-outline</v-icon>
            </template>
            <template
              v-else
            >
              <v-icon>mdi-checkbox-marked-circle</v-icon>
            </template>
          </v-btn>
        </v-col>
        <v-col class="flex-basis-auto">
          <v-text-field
            :value="answer.html"
            :label="`Option ${aIndex + 1}`"
            :rules="longString"
            :aria-label="`Input text for option ${aIndex + 1}`"
            @input="updateAnswerHtml(answer, $event)"
            hide-details
            outlined
            required
          >
          </v-text-field>
        </v-col>
        <v-col
          class="py-0"
        >
          <v-btn
            :aria-label="`Delete option ${answer.html || aIndex + 1}`"
            @click="handleDeleteAnswer(question, answer)"
            class="delete_option"
            icon
            tile
          >
            <v-icon>mdi-delete</v-icon>
          </v-btn>
        </v-col>
      </v-row>
    </li>
  </ul>
  <v-row
    align="center"
    class="flex-nowrap"
  >
    <v-col
      cols="auto"
    >
      <div
        class="icon-button-spacer"
      ></div>
    </v-col>
    <v-col
      cols="auto"
    >
      <v-btn
        @click="handleAddAnswer(question)"
        elevation="0"
        class="btn-add-option px-0"
        plain
      >
        Add Option
      </v-btn>
    </v-col>
  </v-row>
  <template
    v-slot:actions
  >
    <div
      class="d-flex align-center"
    >
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
  name: "MultipleChoiceQuestionEditor",
  components: {
    QuestionEditor
  },
  props: {
    question: {
      type: Object,
      required: true
    }
  },
  data: () => ({
    longString: [(v) => (v && !!v.trim()) || "required"],
  }),
  computed: {
    experimentId() {
      return parseInt(this.$route.params.experimentId);
    },
    treatmentId() {
      return parseInt(this.$route.params.treatmentId);
    },
    assessmentId() {
      return parseInt(this.$route.params.assessmentId);
    },
    conditionId() {
      return parseInt(this.$route.params.conditionId);
    },
    randomizeAnswers: {
      get() {
        return this.question.randomizeAnswers;
      },
      set(value) {
        this.updateQuestions({
          ...this.question,
          randomizeAnswers: value
        });
        this.handleQuestionEdited();
      },
    },
  },
  methods: {
    ...mapMutations({
      updateAnswers: "assessment/updateAnswers",
      updateQuestions: "assessment/updateQuestions"
    }),
    ...mapActions({
      createAnswer: "assessment/createAnswer",
      deleteAnswer: "assessment/deleteAnswer"
    }),
    async handleToggleRandomizeOptions() {
      this.randomizeAnswers = !this.randomizeAnswers;
    },
    async handleAddAnswer(question) {
      // POST ANSWER
      try {
        await this.createAnswer([
          this.experimentId,
          this.conditionId,
          this.treatmentId,
          this.assessmentId,
          question.questionId,
          "",
          false,
          0,
        ]);
        this.handleQuestionEdited();
      } catch (error) {
        console.error(error);
      }
    },
    handleToggleCorrect(answer) {
      this.updateAnswers({ ...answer, correct: !answer.correct });
      this.handleQuestionEdited();
    },
    async handleDeleteAnswer(q, a) {
      // DELETE ANSWER
      try {
        await this.deleteAnswer([
          this.experimentId,
          this.conditionId,
          this.treatmentId,
          this.assessmentId,
          q.questionId,
          a.answerId,
        ]);
        this.handleQuestionEdited();
      } catch (error) {
        this.$swal("there was a problem deleting the answer");
      }
    },
    updateAnswerHtml(answer, value) {
      this.updateAnswers({
        ...answer,
        html: value
      });
      this.handleQuestionEdited();
    },
    handleQuestionEdited() {
      this.$emit("edited");
    }
  }
};
</script>

<style lang="scss" scoped>
@import "~vuetify/src/components/VBtn/_variables.scss";
@import "~@/styles/variables";

.options-list {
  list-style: none;
  > li {
    max-width: 45%;
  }
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
.btn-add-option::v-deep {
  color: map-get($blue, "base") !important;
  caret-color: map-get($blue, "base") !important;
  & .v-btn__content {
    opacity: 1 !important;
  }
}
</style>
