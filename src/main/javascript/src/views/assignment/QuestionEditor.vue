<template>
<div
  v-if="isLoaded"
>
  <tip-tap-editor
    :content="initialContent"
    @edited="handleEditedHtml"
    aria-label="Enter question text"
    editorType="html"
    required
  />
  <v-text-field
    v-model="points"
    :rules="numberRule"
    label="Points"
    type="number"
    step="any"
    class="question-points"
    aria-label="Enter question point value"
    outlined
    required
  >
  </v-text-field>

  <!-- default slot for answer options or other custom content -->
  <slot></slot>

  <v-row>
    <v-col
      cols="auto"
      class="flex-grow-1 py-0"
    >
      <slot
        name="actions"
      ></slot>
    </v-col>
    <v-col
      cols="auto"
      class="text-right py-0"
    >
      <v-menu>
        <template
          v-slot:activator="{ on, attrs }"
        >
          <v-icon
            color="black"
            v-bind="attrs"
            v-on="on"
            :aria-label="`Open actions menu for question ${question.html || question.questionOrder + 1}`"
          >
            mdi-dots-horizontal
          </v-icon>
        </template>
        <v-list
          class="text-left"
        >
          <slot
            name="actions-overflow"
          ></slot>
          <v-list-item
            v-if="isPageBreakAfter"
            @click="removePageBreakAfter(question)"
          >
            <v-list-item-title>
              <v-icon
                class="mr-2"
                aria-label="Remove page break after question"
              >
                mdi-format-page-break
              </v-icon>
              Remove page break after question
            </v-list-item-title>
          </v-list-item>
          <v-list-item
            v-else
            @click="addPageBreakAfter(question)"
          >
            <v-list-item-title>
              <v-icon
                class="mr-2"
                aria-label="Add page break after question"
              >
                mdi-format-page-break
              </v-icon>
              Add page break after question
            </v-list-item-title>
          </v-list-item>
          <v-list-item
            @click="handleDeleteQuestion(question)"
          >
            <v-list-item-title>
              <v-icon
                class="mr-2"
                aria-label="Delete question"
              >
                mdi-delete-outline
              </v-icon>
              Delete question
            </v-list-item-title>
          </v-list-item>
        </v-list>
      </v-menu>
    </v-col>
  </v-row>
</div>
</template>

<script>
import { mapActions, mapGetters, mapMutations } from "vuex";
import { createStatusAlert, statusAlert } from "@/helpers/ui-utils.js";
import TipTapEditor from "@/components/editor/TipTapEditor";

export default {
  name: "QuestionEditor",
  components: {
    TipTapEditor
  },
  props: {
    question: {
      type: Object,
      required: true
    },
    isMC: {
      type: Boolean,
      default: false
    }
  },
  data: () => ({
    isLoaded: false,
    editor: null,
    initialContent: null,
    rules: [
      (v) => (v && !!v.trim()) || "required",
      (v) => (v || "").length <= 255 || "A maximum of 255 characters is allowed",
    ],
    numberRule: [
      (v) => (v && !isNaN(v)) || "required",
      (v) => (!isNaN(parseFloat(v)) && v >= 0) || "The point value cannot be negative",
    ]
  }),
  computed: {
    ...mapGetters({
      questions: "assessment/questions",
      alertStatuses: "alert/statuses"
    }),
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
    isPageBreakAfter() {
      const questionIndex = this.question.questionOrder;

      if (questionIndex + 1 < this.questions.length) {
        return this.questions[questionIndex + 1].questionType === "PAGE_BREAK";
      }

      return false;
    },
    isMultipleChoice() {
      return this.isMC;
    },
    html: {
      get() {
        return this.question?.html || "";
      },
      set(value) {
        if (this.isMultipleChoice) {
          this.$emit("edited");
        }

        this.updateQuestions({
          ...this.question,
          html: value
        });
      },
    },
    points: {
      get() {
        return this.question.points;
      },
      set(value) {
        if (this.isMultipleChoice) {
          this.$emit("edited");
        }

        this.updateQuestions({
          ...this.question,
          points: value
        });
      }
    }
  },
  methods: {
    ...mapMutations({
      updateQuestions: "assessment/updateQuestions",
    }),
    ...mapActions({
      createQuestionAtIndex: "assessment/createQuestionAtIndex",
      deleteQuestion: "assessment/deleteQuestion",
      updateQuestion: "assessment/updateQuestion",
    }),
    async handleDeleteQuestion(question) {
      // DELETE QUESTION
      const reallyDelete = await this.$swal({
        icon: "question",
        text: `Are you sure you want to delete the question?`,
        showCancelButton: true,
        confirmButtonText: "Yes, delete it",
        cancelButtonText: "No, cancel",
      });
      if (reallyDelete?.isConfirmed) {
        try {
          const response = await this.deleteQuestion([
            this.experimentId,
            this.conditionId,
            this.treatmentId,
            this.assessmentId,
            question.questionId,
          ]);

          createStatusAlert(
            statusAlert(
              this.alertStatuses.success,
              "Question deleted successfully."
            )
          );

          return response;
        } catch (error) {
          console.error("handleDeleteQuestion | catch", { error });
          this.$swal("there was a problem deleting the question");
          createStatusAlert(
            statusAlert(
              this.alertStatuses.error,
              "An error occurred while deleting the question. Please try again."
            )
          );
        }
      }
    },
    async addPageBreakAfter(question) {
      try {
        const questionIndex = this.questions.findIndex(
          (que) => que.questionId === question.questionId
        );
        await this.createQuestionAtIndex({
          payload: [
            this.experimentId,
            this.conditionId,
            this.treatmentId,
            this.assessmentId,
            questionIndex + 1,
            "PAGE_BREAK",
            0,
            "",
          ],
          // Put the PAGE_BREAK just after this question
          questionIndex: questionIndex + 1,
        });

        const list = [...this.questions.map(q => ({...q}))];
        list.forEach((q, idx) => {
          q.questionOrder = idx;
        });

        this.handleSaveQuestions(list);
        createStatusAlert(
          statusAlert(
            this.alertStatuses.success,
            "Page break added successfully."
          )
        );
      } catch (error) {
        console.error("addPageBreakAfter | catch", { error });
        this.$swal("there was a problem adding a page break");
        createStatusAlert(
          statusAlert(
            this.alertStatuses.error,
            "An error occurred while adding the page break. Please try again."
          )
        );
      }
    },
    async removePageBreakAfter(question) {
      try {
        const questionIndex = this.questions.findIndex(
          (que) => que.questionId === question.questionId
        );
        // find the PAGE_BREAK question after this question
        const pageBreakQuestion = this.questions[questionIndex + 1];
        await this.deleteQuestion([
          this.experimentId,
          this.conditionId,
          this.treatmentId,
          this.assessmentId,
          pageBreakQuestion.questionId,
        ]);

        const list = [...this.questions.map(q => ({...q}))];
        list.forEach((q, idx) => {
          q.questionOrder = idx;
        });

        this.handleSaveQuestions(list);
        createStatusAlert(
          statusAlert(
            this.alertStatuses.success,
            "Page break removed successfully."
          )
        );
      } catch (error) {
        console.error("removePageBreakAfter | catch", { error });
        this.$swal("there was a problem removing a page break");
        createStatusAlert(
          statusAlert(
            this.alertStatuses.error,
            "An error occurred while removing the page break. Please try again."
          )
        );
      }
    },
    async handleSaveQuestions(questions) {
      // LOOP AND PUT QUESTIONS
      return Promise.all(
        questions.map(async (question, index) => {
          // save question
          try {
            this.updateQuestions(question);
            const q = await this.updateQuestion([
              this.experimentId,
              this.conditionId,
              this.treatmentId,
              this.assessmentId,
              question.questionId,
              question.html,
              question.points,
              index,
              question.questionType,
              question.randomizeAnswers,
              question.answers
            ]);
            createStatusAlert(
              statusAlert(
                this.alertStatuses.success,
                "Questions saved successfully."
              )
            );

            return Promise.resolve(q);
          } catch (error) {
            createStatusAlert(
              statusAlert(
                this.alertStatuses.error,
                "An error occurred while saving the questions. Please try again."
              )
            );

            return Promise.reject(error);
          }
        })
      );
    },
    handleEditedHtml(html) {
      this.question.html = html;
    }
  },
  mounted() {
    this.initialContent = this.html;
    this.isLoaded = true;
  }
};
</script>

<style scoped>
.question-points {
  max-width: 15%;
}
</style>
