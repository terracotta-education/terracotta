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
        <v-row align="center">
          <v-col class="py-0" cols="1">
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
          <v-col cols="9">
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
    <template v-slot:actions>
      <v-btn
        elevation="0"
        color="primary"
        class="px-0"
        @click="handleAddAnswer(question)"
        plain
      >
        Add Option
      </v-btn>
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
  },
  methods: {
    ...mapMutations({
      updateAnswers: "assessment/updateAnswers",
    }),
    ...mapActions({
      createAnswer: "assessment/createAnswer",
      deleteAnswer: "assessment/deleteAnswer",
    }),
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
.options-list {
  list-style: none;
}
</style>
