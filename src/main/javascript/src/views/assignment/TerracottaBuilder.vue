<template>
  <div class="terracotta-builder" v-if="experiment && assessment">
    <h1>
      Add your treatment for
      {{ assignment.title }}'s condition: <strong>{{ condition.name }}</strong>
    </h1>
    <form @submit.prevent="saveAll('AssignmentYourAssignments')" class="my-5">
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

      <template v-if="questions && questions.length > 0">
        <v-expansion-panels
          class="v-expansion-panels--outlined mb-6"
          flat
          accordion
        >
          <v-expansion-panel
            v-for="(question, qIndex) in questions"
            :key="qIndex"
            class="text-left"
          >
            <template v-if="question">
              <v-expansion-panel-header class="text-left">
                <h2 class="pa-0">
                  {{ qIndex + 1 }}
                  <span
                    class="pl-3 question-text"
                    v-if="question.html"
                    v-html="textOnly(question.html)"
                  ></span>
                </h2>
              </v-expansion-panel-header>
              <v-expansion-panel-content>
                <component
                  :is="questionTypeComponents[question.questionType]"
                  :value="question"
                  @input="handleQuestionChanged"
                  @delete="handleDeleteQuestion"
                />
              </v-expansion-panel-content>
            </template>
          </v-expansion-panel>
        </v-expansion-panels>
      </template>
      <template v-else>
        <p class="grey--text">Add questions to continue</p>
      </template>

      <v-menu offset-y>
        <template v-slot:activator="{ on, attrs }">
          <v-btn
            color="primary"
            elevation="0"
            plain
            v-bind="attrs"
            v-on="on"
            class="mb-3"
          >
            Add Question
            <v-icon>mdi-chevron-down</v-icon>
          </v-btn>
        </template>
        <v-list>
          <v-list-item @click="handleAddQuestion('ESSAY')">
            <v-list-item-title
              ><v-icon class="mr-1">mdi-text</v-icon> Short
              answer</v-list-item-title
            >
          </v-list-item>
          <v-list-item @click="handleAddQuestion('MC')">
            <v-list-item-title
              ><v-icon class="mr-1">mdi-radiobox-marked</v-icon> Multiple
              choice</v-list-item-title
            >
          </v-list-item>
        </v-list>
      </v-menu>
      <br />
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
import { clone } from "@/helpers";
import { mapActions, mapGetters } from "vuex";
import MultipleChoiceQuestionEditor from "./MultipleChoiceQuestionEditor.vue";
import QuestionEditor from "./QuestionEditor.vue";

export default {
  name: "TerracottaBuilder",
  props: ["experiment"],
  computed: {
    assignment_id() {
      return parseInt(this.$route.params.assignment_id);
    },
    exposure_id() {
      return parseInt(this.$route.params.exposure_id);
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
    condition() {
      return this.experiment.conditions.find(
        (c) => parseInt(c.conditionId) === parseInt(this.condition_id)
      );
    },
    ...mapGetters({
      assignment: "assignment/assignment",
      storeAssessment: "assessment/assessment",
      storeQuestions: "assessment/questions",
    }),
    contDisabled() {
      return (
        !this.questions ||
        this.questions.length < 1 ||
        this.questions.some((q) => q.html.trim() === "<p></p>") ||
        !this.assessment.title ||
        !this.assessment.title.trim() ||
        this.assessment.title.length > 255
      );
    },
    questionTypeComponents() {
      return {
        MC: MultipleChoiceQuestionEditor,
        ESSAY: QuestionEditor,
      };
    },
  },
  data() {
    return {
      rules: [
        (v) => (v && !!v.trim()) || "required",
        (v) =>
          (v || "").length <= 255 || "A maximum of 255 characters is allowed",
      ],
      questions: null,
      assessment: null,
    };
  },
  methods: {
    ...mapActions({
      fetchAssessment: "assessment/fetchAssessment",
      updateAssessment: "assessment/updateAssessment",
      createQuestion: "assessment/createQuestion",
      updateQuestion: "assessment/updateQuestion",
      deleteQuestion: "assessment/deleteQuestion",
      updateAnswer: "assessment/updateAnswer",
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
          "",
        ]);
        this.questions.push(clone(response.data));
      } catch (error) {
        console.error(error);
      }
    },
    handleQuestionChanged(question) {
      const questionIndex = this.questions.findIndex(
        (que) => que.questionId === question.questionId
      );
      if (questionIndex >= 0) {
        this.questions.splice(questionIndex, 1, question);
      }
    },
    async handleDeleteQuestion(question) {
      try {
        await this.deleteQuestion([
          this.experiment.experimentId,
          this.condition.conditionId,
          this.treatment_id,
          this.assessment_id,
          question.questionId,
        ]);
        // splice the question out of questions array
        const questionIndex = this.questions.findIndex(
          (que) => que.questionId === question.questionId
        );
        if (questionIndex >= 0) {
          this.questions.splice(questionIndex, 1);
        }
      } catch (error) {
        console.error("handleDeleteQuestion | catch", { error });
        this.$swal("there was a problem deleting the question");
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
          this.assessment.html,
        ]);
      } catch (error) {
        console.error("handleCreateAssessment | catch", { error });
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
              question.questionType,
            ]);

            return Promise.resolve(q);
          } catch (error) {
            return Promise.reject(error);
          }
        })
      );
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
                answerIndex,
              ]);
              return Promise.resolve(a);
            } catch (error) {
              return Promise.reject(error);
            }
          });
        })
      );
    },
    async saveAll(routeName) {
      if (this.questions.some((q) => !q.html)) {
        this.$swal("Please fill or delete empty questions.");
        return false;
      }

      const savedAssessment = await this.handleSaveAssessment();
      if (savedAssessment) {
        await this.handleSaveQuestions();
        await this.handleSaveAnswers();

        this.$router.push({
          name: routeName,
          params: { exposure_id: this.exposure_id },
        });
      }
    },
    saveExit() {
      this.saveAll("home");
    },
    textOnly(htmlString) {
      const parser = new DOMParser();
      const doc = parser.parseFromString(htmlString, "text/html");
      // Add a space between adjacent children
      return [...doc.body.children].map((c) => c.innerText).join(" ");
    },
  },
  async created() {
    await this.fetchAssessment([
      this.experiment.experimentId,
      this.condition_id,
      this.treatment_id,
      this.assessment_id,
    ]);
    // Clone assessment and questions so we can manipulate their values
    this.assessment = clone(this.storeAssessment);
    this.questions = clone(this.storeQuestions);
  },
  components: {
    QuestionEditor,
    MultipleChoiceQuestionEditor,
  },
};
</script>

<style lang="scss">
.terracotta-builder {
  .v-expansion-panel-header {
    &--active {
      border-bottom: 2px solid map-get($grey, "lighten-2");
    }
    h2 {
      display: inline-block;
      max-height: 1em;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;

      > .question-text {
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
</style>
