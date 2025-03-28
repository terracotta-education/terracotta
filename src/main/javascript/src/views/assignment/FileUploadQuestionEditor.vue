<template>
    <!-- proxy props and event listeners to question-editor -->
    <question-editor v-bind="$props" v-on="$listeners">
      <p class="ma-0 mb-3">Note: Files must be smaller than 10MB</p>
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
            this.experimentId,
            this.conditionId,
            this.treatmentId,
            this.assessmentId,
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
            this.experimentId,
            this.conditionId,
            this.treatmentId,
            this.assessmentId,
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
