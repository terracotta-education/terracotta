<template>
  <div
    v-if="experiment && assessment"
    class="terracotta-builder"
  >
    <div
      class="header-container"
    >
      <h1>
        {{ this.assignment_title }}
      </h1>
      <div
        v-if="!hasSingleTreatment"
      >
        <h4
          class="label-treatment"
        >
          Treatment
        </h4>
        <v-chip
          :color="condition_color"
          label
        >
          <h4
            class="label-condition-name"
          >
            {{ this.condition_name }}
          </h4>
        </v-chip>
      </div>
    </div>
    <v-tabs
      v-model="tab"
      class="tabs"
    >
      <v-tab>Treatment</v-tab>
      <v-tab>Settings</v-tab>
    </v-tabs>
    <v-tabs-items
      v-model="tab"
    >
      <v-tab-item>
        <form
          @submit.prevent="saveAll('AssignmentYourAssignments')"
          class="my-5"
        >
          <v-textarea
            v-model="html"
            label="Instructions or description (optional)"
            placeholder="e.g. Lorem ipsum"
            outlined
          ></v-textarea>
          <div
            class="d-flex align-center mb-3 justify-space-between"
          >
            <h4
              class="pa-0"
            >
              <strong>Questions</strong>
            </h4>
            <v-btn
              @click="handleClearQuestions()"
              :disabled="!canClearAll"
              color="primary"
              elevation="0"
              class="saveButton"
              text
            >
              Clear All
            </v-btn>
          </div>
          <template
            v-if="questionPages && questionPages.length > 0"
          >
            <template>
              <div
                v-for="(questionPage, qpIndex) in questionPages"
                :key="questionPage.key"
              >
                <v-expansion-panels
                  v-model="expandedQuestionPanel[qpIndex]"
                  :key="questionPage.key"
                  class="v-expansion-panels--outlined"
                  flat
                  accordion
                  outlined
                >
                  <draggable
                    :list="questionPage.questions"
                    @change="(ev) => handleQuestionOrderChange(ev)"
                    group="questions"
                    handle=".dragger"
                    style="width:100%"
                  >
                    <v-expansion-panel
                      v-for="(question, qIndex) in questionPage.questions"
                      @click="expandedQuestionPagePanel = qpIndex"
                      :key="qIndex"
                      :ref="buildExpandedQuestionPanelId(qpIndex, qIndex)"
                      :class="[qIndex === 0 ? 'rounded-lg' : qIndex === questionPage.questions.length - 1 ? 'rounded-lg rounded-t-0' : '',
                        qIndex === questionPage.questions.length - 1 ? '' : 'rounded-b-0']"
                    >
                      <template v-if="question">
                        <v-expansion-panel-header class="text-left">
                          <div class="d-flex align-start">
                            <span class="dragger me-2">
                              <v-icon>mdi-drag</v-icon>
                            </span>
                            <h2
                              class="pa-0"
                            >
                              {{ questionPage.questionStartIndex + qIndex + 1 }}
                              <span
                                v-if="question.html"
                                class="pl-3 question-text"
                                v-html="textOnly(question.html)"
                              ></span>
                            </h2>
                          </div>
                        </v-expansion-panel-header>
                        <v-expansion-panel-content>
                          <component
                            @edited="addEditedQuestion(question.questionId)"
                            :is="questionTypeComponents[question.questionType]"
                            :question="question"
                          />
                        </v-expansion-panel-content>
                      </template>
                    </v-expansion-panel>
                  </draggable>
                </v-expansion-panels>
                <page-break
                  v-if="questionPage.pageBreakAfter"
                />
              </div>
            </template>
          </template>
          <template
            v-else
          >
            <p
              class="grey--text"
            >
              Add questions to continue
            </p>
          </template>
          <v-menu
            offset-y
          >
            <template
              v-slot:activator="{ on, attrs }"
            >
              <v-btn
                v-bind="attrs"
                v-on="on"
                color="primary"
                elevation="0"
                class="mb-3 mt-3"
                plain
              >
                Add Question
                <v-icon>mdi-chevron-down</v-icon>
              </v-btn>
            </template>
            <v-list>
              <v-list-item
                @click="handleAddQuestion('ESSAY')"
              >
                <v-list-item-title>
                  <v-icon class="mr-1">mdi-text</v-icon> Short answer
                </v-list-item-title>
              </v-list-item>
              <v-list-item
                @click="handleAddQuestion('MC')"
              >
                <v-list-item-title>
                  <v-icon class="mr-1">mdi-radiobox-marked</v-icon> Multiple choice
                </v-list-item-title>
              </v-list-item>
              <v-list-item
                @click="handleAddQuestion('FILE')"
              >
                <v-list-item-title>
                  <v-icon class="mr-1">mdi-file-upload-outline</v-icon> File upload
                </v-list-item-title>
              </v-list-item>
            </v-list>
          </v-menu>
          <v-menu
            v-if="assignmentsAvailableToCopy.length > 0"
            offset-y
            close-on-click
            close-on-content-click
          >
            <template
              v-slot:activator="{ on, attrs }"
            >
              <v-btn
                v-bind="attrs"
                v-on="on"
                color="primary"
                elevation="0"
                class="mb-3 mt-3"
                plain
              >
                Copy Treatment From <v-icon>mdi-chevron-down</v-icon>
              </v-btn>
            </template>
            <v-list>
                <template
                  v-for="(assignment, index) in assignmentsAvailableToCopy"
                >
                  <v-menu
                    v-if="assignment.treatments.length > 0 && hasTreatmentsNotCurrent(assignment.treatments)"
                    :key="assignment.assignmentId"
                    transition="slide-x-transition"
                    offset-x
                    open-on-hover
                  >
                    <template
                      v-slot:activator="{ on, attrs }"
                    >
                      <v-list-item
                        :key="index"
                        v-bind="attrs"
                        v-on="on"
                      >
                        <v-list-item-title>
                          {{ assignment.title }}
                        </v-list-item-title>
                        <v-list-item-action
                          class="justify-end"
                        >
                          <v-icon>mdi-menu-right</v-icon>
                        </v-list-item-action>
                      </v-list-item>
                    </template>
                    <v-list>
                      <template
                        v-for="treatment in assignment.treatments"
                      >
                        <v-list-item
                          v-if="treatment.treatmentId != treatment_id"
                          :key="treatment.treatmentId"
                          @click="duplicate(treatment)"
                        >
                          <v-list-item-title>
                            Treatment
                            <v-chip
                              v-if="assignment.treatments.length > 1"
                              label
                              :color="conditionColorMapping[conditionForTreatment(getGroupConditionListForAssignment(assignment), treatment.conditionId).conditionName]"
                            >
                              {{ conditionForTreatment(getGroupConditionListForAssignment(assignment), treatment.conditionId).conditionName }}
                            </v-chip>
                          </v-list-item-title>
                        </v-list-item>
                      </template>
                    </v-list>
                  </v-menu>
                </template>
            </v-list>
          </v-menu>
          <br />
        </form>
      </v-tab-item>
      <v-tab-item
        class="my-5"
      >
        <treatment-settings />
      </v-tab-item>
    </v-tabs-items>
  </div>
</template>

<script>
import { mapActions, mapGetters, mapMutations } from "vuex";
import { assessmentService } from '@/services';
import draggable from 'vuedraggable';
import FileUploadQuestionEditor from "./FileUploadQuestionEditor.vue";
import MultipleChoiceQuestionEditor from "./MultipleChoiceQuestionEditor.vue";
import omitDeep from '../../helpers/deep-omit';
import PageBreak from "./PageBreak.vue";
import QuestionEditor from "./QuestionEditor.vue";
import RegradeAssignmentDialog from "@/components/RegradeAssignmentDialog.vue"
import TreatmentSettings from "./TreatmentSettings.vue";
import Vue from 'vue';

export default {
  name: "TerracottaBuilder",
  props: ["experiment"],
  components: {
    draggable,
    FileUploadQuestionEditor,
    MultipleChoiceQuestionEditor,
    PageBreak,
    QuestionEditor,
    TreatmentSettings
  },
  data() {
    return {
      assignmentsAvailableToCopy: [],
      copyMenuShown: false,
      rules: [
        (v) => (v && !!v.trim()) || "required",
        (v) =>
          (v || "").length <= 255 || "A maximum of 255 characters is allowed",
      ],
      tab: null,
      expandedQuestionPagePanel: null,
      expandedQuestionPanel: [],
      regradeDetails: {
        regradeOption: "NA",
        editedMCQuestionIds: []
      }
    };
  },
  watch: {
    expandedQuestionPagePanel(idx) {
      // opening question panel in another question page; close all others
      for (let i = 0; i < this.questionPages.length; i++) {
        if (i !== idx) {
          this.expandedQuestionPanel[i] = null;
        }
      }
    },
    expandedQuestionPanel: {
      handler(idx) {
        // scroll to the newly-opened question panel
        if (idx && idx[this.expandedQuestionPagePanel] != null) {
          setTimeout(() => {
            this.$vuetify.goTo(
              this.$refs[this.buildExpandedQuestionPanelId(this.expandedQuestionPagePanel, idx[this.expandedQuestionPagePanel])][0],
              {offset: 100}
            );
          },
          500);
        }
      },
      deep: true
    },
    assignmentCount() {
      this.findAssignmentsAvailableToCopy();
    }
  },
  computed: {
    currentAssignment() {
      return JSON.parse(this.$route.params.current_assignment);
    },
    assignmentCount() {
      return this.assignments.length;
    },
    assignment_id() {
      return this.currentAssignment.assignmentId;
    },
    assignment_title() {
      return this.currentAssignment.title;
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
    condition_name() {
      return this.condition.name;
    },
    condition_color() {
      return this.conditionColorMapping[this.condition_name];
    },
    condition() {
      return this.experiment.conditions.find(
        (c) => parseInt(c.conditionId) === parseInt(this.condition_id)
      );
    },
    canClearAll() {
      return this.assessment.questions.length > 0 && !this.assessment.started;
    },
    hasSingleTreatment() {
      return this.currentAssignment.treatments.length === 1;
    },
    studentCount() {
      var submissions = this.submissions.map(s => s.participantId);

      return submissions.filter((s, i) => submissions.indexOf(s) === i).length;
    },
    submissionCount() {
      return this.submissions? this.submissions.length : 0;
    },
    hasSubmissions() {
      return this.submissionCount > 0;
    },
    hasEditedQuestions() {
      return this.regradeDetails.editedMCQuestionIds.length > 0;
    },
    displayRegradeAssignmentDialog() {
      return this.hasSubmissions && this.hasEditedQuestions;
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
      submissions: "submissions/submissions"
    }),
    contDisabled() {
      return (
        !this.questions ||
        this.questions.length < 1 ||
        this.questions.some((q) => q.html.trim() === "<p></p>")
      );
    },
    questionTypeComponents() {
      return {
        MC: MultipleChoiceQuestionEditor,
        ESSAY: QuestionEditor,
        FILE: FileUploadQuestionEditor,
      };
    },
    html: {
      // two-way computed property
      get() {
        return this.assessment.html;
      },
      set(value) {
        this.setAssessment({ ...this.assessment, html: value });
      },
    }
  },
  methods: {
    ...mapMutations({
      setAssessment: "assessment/setAssessment",
      updateQuestions: "assessment/updateQuestions",
    }),
    ...mapActions({
      fetchAssessment: "assessment/fetchAssessment",
      updateAssessment: "assessment/updateAssessment",
      fetchExposures: "exposures/fetchExposures",
      createQuestion: "assessment/createQuestion",
      updateQuestion: "assessment/updateQuestion",
      deleteQuestion: "assessment/deleteQuestion",
      updateAnswer: "assessment/updateAnswer",
      updateTreatment: "treatment/updateTreatment",
      fetchSubmissions: "submissions/fetchSubmissions",
      regradeQuestions: "assessment/regradeQuestions",
      createAnswer: "assessment/createAnswer",
    }),
    getAssignmentsForExposure(exp) {
      return this.assignments
        .filter((a) => a.exposureId === exp.exposureId)
        .sort((a, b) => a.assignmentOrder - b.assignmentOrder);
    },
    getGroupConditionListForAssignment(assignment) {
      const exposure = this.exposures.find(e => e.exposureId === assignment.exposureId);
      return exposure.groupConditionList;
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
          this.questions.length,
          questionType,
          1, // points
          "",
        ]);

        // add two default options on MC question creation
        if (questionType === "MC") {
          for (var i = 0; i <= 1; i++) {
            await this.handleAddMCOption(this.questions[this.questions.length - 1]);
          }
        }

        // open the added question panel
        var questionPageIndex = this.questionPages.length > 0 ? this.questionPages.length - 1 : 0;
        var questionIndex = this.questionPages[questionPageIndex] && this.questionPages[questionPageIndex].questions && this.questionPages[questionPageIndex].questions.length > 0 ?
          this.questionPages[questionPageIndex].questions.length - 1 : 0;
        this.expandQuestionPanel(questionPageIndex, questionIndex);
      } catch (error) {
        console.error(error);
      }
    },
    async handleAddMCOption(question) {
      // POST ANSWER
      try {
        await this.createAnswer([
          this.experiment.experimentId,
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
    async handleQuestionOrderChange(event) {
      if (!event.removed) {
        const list = [...this.questions.map(q => ({...q}))];
        const evt = event.added ? event.added : event.moved;
        const { element, newIndex } = evt;
        const oldIndex = list.findIndex((v) => v.questionId === element.questionId);
        const movedItem = list.splice(oldIndex, 1)[0];
        list.splice(newIndex, 0, movedItem);
        list.forEach((q, idx) => {
          q.questionOrder = idx;
        });

        this.handleSaveQuestions(list);
      }
    },
    async handleClearQuestions() {
      this.assessment.questions.forEach((q) => {
        this.deleteQuestion([
          this.experiment.experimentId,
          this.condition.conditionId,
          this.treatment_id,
          this.assessment_id,
          q.questionId
        ]).then(response => {
          if (response.status === 400) {
            this.$swal(response.data);
            return false;
          }
        });
      });
      return true;
    },
    async handleSaveAssessment() {
      // PUT ASSESSMENT TITLE & HTML (description) & SETTINGS
      const response = await this.updateAssessment([
        this.experiment.experimentId,
        this.condition.conditionId,
        this.treatment_id,
        this.assessment_id,
        this.assessment.html,
        this.assessment.allowStudentViewResponses,
        this.assessment.studentViewResponsesAfter,
        this.assessment.studentViewResponsesBefore,
        this.assessment.allowStudentViewCorrectAnswers,
        this.assessment.studentViewCorrectAnswersAfter,
        this.assessment.studentViewCorrectAnswersBefore,
        this.assessment.numOfSubmissions,
        this.assessment.multipleSubmissionScoringScheme,
        this.assessment.hoursBetweenSubmissions,
        this.assessment.cumulativeScoringInitialPercentage
      ]);
      if (response.status === 400) {
        this.$swal(response.data);
        return false;
      }
      return true;
    },
    async handleSaveQuestions(questions) {
      // LOOP AND PUT QUESTIONS
      return Promise.all(
        questions.map(async (question, index) => {
          // save question
          try {
            this.updateQuestions(question);
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
              question.answers
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

      if (this.displayRegradeAssignmentDialog) {
        var regradeOption = await this.handleDisplayRegradeAssignmentDialog();

        if (regradeOption.isDismissed) {
          return false;
        }

        this.regradeDetails.regradeOption = regradeOption.value.regradeOption;
      }

      const savedAssessment = await this.handleSaveAssessment();

      if (savedAssessment) {
        await this.handleSaveQuestions(this.questions);
        await this.handleSaveAnswers();
        this.handleRegradeQuestions();
        this.$router.push({
          name: routeName,
          params: { exposure_id: isNaN(this.exposure_id) ? this.$route.params.exposure_id : this.exposure_id },
        });
      }
    },
    handleRegradeQuestions() {
      if (!this.regradeDetails.editedMCQuestionIds.length) {
        // no edited MC questions; skip
        return;
      }

      this.regradeQuestions([
        this.experiment.experimentId,
        this.condition_id,
        this.treatment_id,
        this.assessment_id,
        this.regradeDetails
      ]);
    },
    async duplicate(treatment) {
      const { assessmentDto, conditionId } = treatment;
      /* eslint-disable-next-line */
      const { treatmentId, assessmentId } = assessmentDto;
      let assessment;
      this.copyMenuShown = false;

      try {
        assessment = await assessmentService.fetchAssessment(this.experiment.experimentId, conditionId, treatmentId, assessmentId)
      } catch (error) {
        console.error("handleCreateTreatment | catch", { error });
        return;
      }

      const copy = omitDeep({
        ...assessment.data
      }, ['answerId', 'questionId', 'assessmentId']);

      try {
        await this.updateTreatment([
          this.experiment.experimentId,
          this.condition_id,
          this.treatment_id,
          {
            treatmentId: this.treatment_id,
            conditionId: this.condition_id,
            assignmentId: this.assignment_id,
            assessmentDto: {
              ...copy,
              treatmentId: this.treatment_id,
              assessmentId: this.assessment_id
            },
            assignmentDto: {
              ...this.assignment
            }
          },
        ]);
        return await this.fetchAssessment([
          this.experiment.experimentId,
          this.condition_id,
          this.treatment_id,
          this.assessment_id,
        ]);
      } catch (error) {
        console.error("handleCreateTreatment | catch", { error });
      }
    },
    async saveExit() {
      return this.saveAll("ExperimentSummary");
    },
    textOnly(htmlString) {
      const parser = new DOMParser();
      const doc = parser.parseFromString(htmlString, "text/html");
      // Add a space between adjacent children
      return [...doc.body.children].map((c) => c.innerText).join(" ");
    },
    expandQuestionPanel(questionPageIndex, questionPanelIndex) {
      this.expandedQuestionPagePanel = questionPageIndex;
      this.expandedQuestionPanel = [];
      this.expandedQuestionPanel[questionPageIndex] = questionPanelIndex;
    },
    buildExpandedQuestionPanelId(expandedQuestionPagePanel, expandedQuestionPanel) {
      return "question-panel-" + expandedQuestionPagePanel + "_" + expandedQuestionPanel;
    },
    hasTreatmentsNotCurrent(treatments) {
      return treatments.some((t) => t.treatmentId !== this.treatment_id);
    },
    findAssignmentsAvailableToCopy() {
      this.assignments.forEach((a) => {
        var hasAvailableTreatment = this.hasTreatmentsNotCurrent(a.treatments);

        if (hasAvailableTreatment) {
          this.assignmentsAvailableToCopy.push(a);
        }
      });
    },
    async handleDisplayRegradeAssignmentDialog() {
      return this.$swal({
        html: '<div id="dialog-regrade-assignment"></div>',
        showCancelButton: true,
        confirmButtonText: "Update",
        cancelButtonText: "Cancel",
        reverseButtons: true,
        allowOutsideClick: false,
        allowEscapeKey: false,
        customClass: {
          confirmButton: "response-option-confirm",
          popup: "regrade-assignment-popup"
        },
        preConfirm: () => {
          const regradeOption = this.$swal.getPopup().querySelector("input#regrade-option-selected");

          if (regradeOption && regradeOption.value) {
            return {regradeOption: regradeOption.value};
          }

          // no value selected; prompt user to select one
          this.$swal.showValidationMessage("Please select a regrade option");
        },
        willOpen: () => {
          var RegradeAssignmentDialogClass = Vue.extend(RegradeAssignmentDialog);
          var regradeAssignmentDialog = new RegradeAssignmentDialogClass({
            propsData: {
              assignmentName: this.assignment_title,
              conditionName: this.condition_name,
              studentCount: this.studentCount,
              editedQuestionCount: this.regradeDetails.editedMCQuestionIds.length
            }
          });
          regradeAssignmentDialog.$mount(document.getElementById("dialog-regrade-assignment"));
        }
      });
    },
    addEditedQuestion(questionId) {
      if (this.regradeDetails.editedMCQuestionIds.includes(questionId)) {
        return;
      }

      this.regradeDetails.editedMCQuestionIds.push(questionId);
      console.log("added edited MC question id: " + questionId);
    }
  },
  async created() {
    await this.fetchAssessment([
      this.experiment.experimentId,
      this.condition_id,
      this.treatment_id,
      this.assessment_id,
    ]);
    await await this.fetchSubmissions([
      this.experiment.experimentId,
      this.condition_id,
      this.treatment_id,
      this.assessment_id
    ]);
    this.getAssignmentDetails();
    this.findAssignmentsAvailableToCopy();
  }
};
</script>

<style lang="scss">
v-expansion-panels {
  &, & > div {
    width: 100%;
  }
}
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
  .header-container {
    width: 100%;
    min-height: fit-content;
    padding-bottom: 10px;
  }
  h4.label-treatment,
  h4.label-condition-name {
    display: inline;
    padding-right: 5px;
    padding-bottom: 0;
  }
}
</style>
