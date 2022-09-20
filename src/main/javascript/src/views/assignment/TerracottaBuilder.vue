<template>
  <div class="terracotta-builder" v-if="experiment && assessment">
    <h1>
      Add your treatment for
      {{ assignment.title }}'s condition: <strong>{{ condition.name }}</strong>
    </h1>
    <v-tabs v-model="tab" class="tabs">
      <v-tab>Treatment</v-tab>
      <v-tab>Settings</v-tab>
    </v-tabs>
    <v-tabs-items v-model="tab">
      <v-tab-item>
        <form
          @submit.prevent="saveAll('AssignmentYourAssignments')"
          class="my-5"
        >
          <v-text-field
            v-model="title"
            :rules="rules"
            label="Treatment name"
            placeholder="e.g. Lorem ipsum"
            autofocus
            outlined
            required
          ></v-text-field>
          <v-textarea
            v-model="html"
            label="Instructions or description (optional)"
            placeholder="e.g. Lorem ipsum"
            outlined
          ></v-textarea>

          <h4 class="mb-3"><strong>Questions</strong></h4>

          <template v-if="questionPages && questionPages.length > 0">
            <template v-for="questionPage in questionPages">
              <div :key="questionPage.key">
                <v-expansion-panels
                  class="v-expansion-panels--outlined"
                  flat
                  accordion
                  :key="questionPage.key"
                >
                  <v-expansion-panel
                    v-for="(question, qIndex) in questionPage.questions"
                    :key="qIndex"
                    class="text-left"
                  >
                    <template v-if="question">
                      <v-expansion-panel-header class="text-left">
                        <h2 class="pa-0">
                          {{ questionPage.questionStartIndex + qIndex + 1 }}
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
                          :question="question"
                        />
                      </v-expansion-panel-content>
                    </template>
                  </v-expansion-panel>
                </v-expansion-panels>
                <page-break v-if="questionPage.pageBreakAfter" />
              </div>
            </template>
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
                class="mb-3 mt-3"
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
          <v-menu offset-y>
            <template v-slot:activator="{ on, attrs }">
              <v-btn
                color="primary"
                elevation="0"
                plain
                v-bind="attrs"
                v-on="on"
                class="mb-3 mt-3"
              >
                Copy Treatment From
                <v-icon>mdi-chevron-down</v-icon>
              </v-btn>
            </template>
            <v-list>
              <template v-for="exposure in exposures">
                <template 
                  v-for="(assignment, index) in getAssignmentsForExposure(
                    exposure
                  )"
                >
                  <v-menu offset-x :key="assignment.assignmentId" v-if="assignment.treatments.length > 1">
                    <template v-slot:activator="{ on, attrs }">
                      <v-list-item :key="index" v-bind="attrs" v-on="on">
                        <v-list-item-title>{{
                          assignment.title
                        }}</v-list-item-title>
                      </v-list-item>
                    </template>
                    <v-list>
                      <template
                        v-for="treatment in assignment.treatments"
                      >
                        <v-list-item :key="treatment.treatmentId">
                          <v-list-item-title
                            >Treatment
                            <v-chip
                              label
                              :color="
                                conditionColorMapping[
                                  conditionForTreatment(
                                    exposure.groupConditionList,
                                    treatment.conditionId
                                  ).conditionName
                                ]
                              "
                              >{{
                                conditionForTreatment(
                                  exposure.groupConditionList,
                                  treatment.conditionId
                                ).conditionName
                              }}</v-chip
                            ></v-list-item-title
                          >
                        </v-list-item>
                      </template>
                    </v-list>
                  </v-menu>
                </template>
              </template>
            </v-list>
          </v-menu>
          <br />
          <pre>{{ assignments }}</pre>
        </form>
      </v-tab-item>
      <v-tab-item class="my-5">
        <treatment-settings />
      </v-tab-item>
    </v-tabs-items>
    <v-btn
      :disabled="contDisabled"
      elevation="0"
      color="primary"
      class="mr-4"
      @click="saveAll('AssignmentYourAssignments')"
    >
      Continue
    </v-btn>
  </div>
</template>

<script>
import { mapActions, mapGetters, mapMutations } from "vuex";
import MultipleChoiceQuestionEditor from "./MultipleChoiceQuestionEditor.vue";
import QuestionEditor from "./QuestionEditor.vue";
import PageBreak from "./PageBreak.vue";
import TreatmentSettings from "./TreatmentSettings.vue";

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
      exposures: "exposures/exposures",
      assignments: "assignment/assignments",
      assessment: "assessment/assessment",
      questions: "assessment/questions",
      answerableQuestions: "assessment/answerableQuestions",
      questionPages: "assessment/questionPages",
      conditionColorMapping: "condition/conditionColorMapping",
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
    title: {
      // two-way computed property
      get() {
        return this.assessment.title;
      },
      set(value) {
        this.setAssessment({ ...this.assessment, title: value });
      },
    },
    html: {
      // two-way computed property
      get() {
        return this.assessment.html;
      },
      set(value) {
        this.setAssessment({ ...this.assessment, html: value });
      },
    },
  },
  data() {
    return {
      rules: [
        (v) => (v && !!v.trim()) || "required",
        (v) =>
          (v || "").length <= 255 || "A maximum of 255 characters is allowed",
      ],
      tab: null,
    };
  },
  methods: {
    ...mapMutations({
      setAssessment: "assessment/setAssessment",
    }),
    ...mapActions({
      fetchAssessment: "assessment/fetchAssessment",
      updateAssessment: "assessment/updateAssessment",
      fetchExposures: "exposures/fetchExposures",
      createQuestion: "assessment/createQuestion",
      updateQuestion: "assessment/updateQuestion",
      deleteQuestion: "assessment/deleteQuestion",
      updateAnswer: "assessment/updateAnswer",
    }),
    getAssignmentsForExposure(exp) {
      return this.assignments
        .filter((a) => a.exposureId === exp.exposureId)
        .sort((a, b) => a.assignmentOrder - b.assignmentOrder);
    },
    conditionForTreatment(groupConditionList, conditionId) {
      return groupConditionList.find((c) => c.conditionId === conditionId);
    },
    async getAssignmentDetails() {
      await this.fetchExposures(this.experiment.experimentId);
      return this.exposures;
    },
    async handleAddQuestion(questionType) {
      // POST QUESTION
      try {
        await this.createQuestion([
          this.experiment.experimentId,
          this.condition_id,
          this.treatment_id,
          this.assessment_id,
          0,
          questionType,
          1, // points
          "",
        ]);
      } catch (error) {
        console.error(error);
      }
    },
    async handleSaveAssessment() {
      // PUT ASSESSMENT TITLE & HTML (description) & SETTINGS
      const response = await this.updateAssessment([
        this.experiment.experimentId,
        this.condition.conditionId,
        this.treatment_id,
        this.assessment_id,
        this.assessment.title,
        this.assessment.html,
        this.assessment.allowStudentViewResponses,
        this.assessment.studentViewResponsesAfter,
        this.assessment.studentViewResponsesBefore,
        this.assessment.allowStudentViewCorrectAnswers,
        this.assessment.studentViewCorrectAnswersAfter,
        this.assessment.studentViewCorrectAnswersBefore,
      ]);
      if (response.status === 400) {
        this.$swal(response.data);
        return false;
      }
      return true;
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
              question.randomizeAnswers,
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
      if (this.answerableQuestions.some((q) => !q.html)) {
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
    this.getAssignmentDetails();
  },
  components: {
    QuestionEditor,
    MultipleChoiceQuestionEditor,
    PageBreak,
    TreatmentSettings,
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
  .tabs {
    border-top: 1px solid map-get($grey, "lighten-2");
    border-bottom: 1px solid map-get($grey, "lighten-2");
  }
}
</style>