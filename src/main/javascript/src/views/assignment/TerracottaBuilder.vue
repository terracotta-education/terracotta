<template>
<div
  v-if="experiment && assessment"
  class="terracotta-builder"
>
  <div
    class="header-container"
  >
    <h1>
      {{ this.assignmentTitle }}
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
        :color="conditionColor"
        label
      >
        <h4
          class="label-condition-name"
        >
          {{ this.conditionName }}
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
        class="questions-form my-5"
      >
        <v-textarea
          v-model="html"
          label="Instructions or description (optional)"
          placeholder="e.g. Lorem ipsum"
          outlined
        />
        <div
          v-if="treatmentOptionSelected && !isIntegrationType && questionPages && questionPages.length > 0"
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
          v-if="treatmentOptionSelected && !isIntegrationType && questionPages && questionPages.length > 0"
        >
          <template>
            <div
              v-for="(questionPage, qpIndex) in questionPages"
              :key="questionPage.key"
              class="question-page"
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
                  group="questions"
                  @change="(ev) => handleQuestionOrderChange(ev)"
                  handle=".dragger"
                  style="width:100%"
                >
                  <v-expansion-panel
                    v-for="(question, qIndex) in questionPage.questions"
                    @click="expandedQuestionPagePanel = qpIndex"
                    :key="question.questionId"
                    :ref="buildExpandedQuestionPanelId(qpIndex, qIndex)"
                    :class="[
                      qIndex === 0 ? 'rounded-lg' : qIndex === questionPage.questions.length - 1 ? 'rounded-lg rounded-t-0' : '',
                      qIndex === questionPage.questions.length - 1 ? '' : 'rounded-b-0'
                    ]"
                  >
                    <template
                      v-if="question"
                    >
                      <v-expansion-panel-header
                        class="text-left"
                      >
                        <div
                          class="d-flex align-start"
                        >
                          <span
                            class="dragger me-2"
                          >
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
          v-if="treatmentOptionSelected && !isIntegrationType && questionPages && questionPages.length === 0"
        >
        <h4
            class="pa-0"
          >
            <strong>Questions</strong>
          </h4>
          <p
            class="add-questions-to-continue"
          >
            Add questions to continue
          </p>
        </template>
        <template
          v-if="treatmentOptionSelected && isIntegrationType"
        >
          <external-integration-editor
            @integration-updated="handleIntegrationUpdate($event)"
            @url-validation-in-progress="handleUrlValidationInProgress"
            :assessment="assessment"
            :question="questions[0]"
          />
        </template>
        <template
          v-if="!treatmentOptionSelected"
        >
          <h4>Select a treatment mode</h4>
          <p>
            Use the Terracotta Builder to create assignments with multiple choice, short answer, or file upload questions.
            Use the External Integration option to use a Qualtrics survey or a custom web activity for this treatment.
          </p>
        </template>
        <div
          class="bottom-menu"
        >
          <div
            v-if="!treatmentOptionSelected"
            class="treatment-mode-container d-flex flex-row"
          >
            <v-btn
              @click="handleAddTerracottaBuilder"
              elevation="0"
              class="add-treatment-type"
              plain
            >
              Use Terracotta Builder
            </v-btn>
            <v-menu
              close-on-content-click
              close-on-click
              offset-y
              attach
              top
            >
              <template
                v-slot:activator="{ on, attrs }"
              >
                <v-btn
                  v-bind="attrs"
                  v-on="on"
                  elevation="0"
                  class="add-treatment-type"
                  plain
                >
                  Add External Integration <v-icon>mdi-chevron-down</v-icon>
                </v-btn>
              </template>
              <v-list>
                <v-list-item
                  @click="handleAddIntegration(externalIntegrationClients.qualtrics.name)"
                >
                  <v-list-item-title>
                    {{ externalIntegrationClients.qualtrics.name }}
                  </v-list-item-title>
                </v-list-item>
                <v-list-item
                  @click="handleAddIntegration(externalIntegrationClients.custom.name)"
                >
                  <v-list-item-title>
                    {{ externalIntegrationClients.custom.name }}
                  </v-list-item-title>
                </v-list-item>
              </v-list>
            </v-menu>
          </div>
          <div
            v-if="treatmentOptionSelected"
            class="treatment-mode-selected-container d-flex flex-row"
          >
            <div>
              <v-menu
                v-if="!isIntegrationType"
                offset-y
                attach
                top
              >
                <template
                  v-slot:activator="{ on, attrs }"
                >
                  <v-btn
                    v-bind="attrs"
                    v-on="on"
                    elevation="0"
                    class="treatment-type-selected"
                    plain
                  >
                    ADD QUESTION
                    <v-icon>mdi-chevron-down</v-icon>
                  </v-btn>
                </template>
                <v-list>
                  <v-list-item
                    @click="handleAddQuestion('ESSAY')"
                  >
                    <v-list-item-title>
                      <v-icon
                        class="mr-1"
                      >
                        mdi-text
                      </v-icon>
                      Short answer
                    </v-list-item-title>
                  </v-list-item>
                  <v-list-item
                    @click="handleAddQuestion('MC')"
                  >
                    <v-list-item-title>
                      <v-icon
                        class="mr-1"
                      >
                        mdi-radiobox-marked
                      </v-icon>
                      Multiple choice
                    </v-list-item-title>
                  </v-list-item>
                  <v-list-item
                    @click="handleAddQuestion('FILE')"
                  >
                    <v-list-item-title>
                      <v-icon
                        class="mr-1"
                      >
                        mdi-file-upload-outline
                      </v-icon>
                      File submission
                    </v-list-item-title>
                  </v-list-item>
                </v-list>
              </v-menu>
              <v-menu
                v-if="!isIntegrationType && assignmentsAvailableToCopy.length > 0"
                close-on-content-click
                close-on-click
                offset-y
                attach
                top
              >
                <template
                  v-slot:activator="{ on, attrs }"
                >
                  <v-btn
                    v-bind="attrs"
                    v-on="on"
                    elevation="0"
                    class="treatment-type-selected"
                    plain
                  >
                    Copy Content From <v-icon>mdi-chevron-down</v-icon>
                  </v-btn>
                </template>
                <v-list>
                    <template>
                      <v-list-item
                        v-for="(assignment) in assignmentsAvailableToCopy"
                        :key="assignment.assignmentId"
                        :aria-label="`copy content from ${assignment.title}`"
                        @click="duplicate(assignment)"
                      >
                        <v-list-item-title>
                          {{ assignment.title }}
                        </v-list-item-title>
                      </v-list-item>
                    </template>
                </v-list>
              </v-menu>
            </div>
            <v-btn
              v-if="displayBackToTreatmentModeSelection"
              @click="handleBackToTreatmentModeSelection"
              elevation="0"
              class="treatment-type-selected"
              plain
            >
              BACK TO TREATMENT MODE SELECTION
            </v-btn>
          </div>
        </div>
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
import { shrinkContainer, widenContainer, deleteAttributesFromObservedElement, deleteAttributesFromElement, addAttributesToElement, createStatusAlert, statusAlert } from "@/helpers/ui-utils.js";
import draggable from 'vuedraggable';
import CopyFromDialog from "@/components/dialog/CopyFromDialog.vue";
import FileUploadQuestionEditor from "./FileUploadQuestionEditor.vue";
import ExternalIntegrationEditor from "@/views/integrations/ExternalIntegrationEditor.vue";
import MultipleChoiceQuestionEditor from "./MultipleChoiceQuestionEditor.vue";
import omitDeep from '../../helpers/deep-omit';
import PageBreak from "./PageBreak.vue";
import QuestionEditor from "./QuestionEditor.vue";
import RegradeAssignmentDialog from "@/components/RegradeAssignmentDialog.vue"
import TreatmentSettings from "./TreatmentSettings.vue";
import Vue from 'vue';

export default {
  name: "TerracottaBuilder",
  props: {
    experiment: {
      type: Object,
      required: true
    }
  },
  components: {
    CopyFromDialog,
    draggable,
    FileUploadQuestionEditor,
    ExternalIntegrationEditor,
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
      },
      externalIntegrationClients: {
        qualtrics: {
          name: "Qualtrics",
        },
        custom: {
          name: "Custom Web Activity"
        }
      },
      treatmentOptionSelected: false,
      integrationQuestionValidation: null,
      urlValidationInProgress: false
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
      this.$nextTick(() => {
        setTimeout(() => {
          deleteAttributesFromElement(".v-expansion-panel", ["aria-expanded"]);
          addAttributesToElement(".editor .ProseMirror", [
            { name: "aria-label", value: "question editor content" }
          ]);
        }, 1000);
      });
    },
    expandedQuestionPanel: {
      handler(idx) {
        if (!this.treatmentOptionSelected || this.isIntegrationType) {
          return;
        }
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
    },
    questions() {
      this.$nextTick(() => {
        setTimeout(() => {
          deleteAttributesFromElement(".v-expansion-panel", ["aria-expanded"]);
          addAttributesToElement(".editor .ProseMirror", [
            { name: "aria-label", value: "question editor content" }
          ]);
        }, 1000);
      });
    }
  },
  computed: {
    ...mapGetters({
      assignment: "assignment/assignment",
      exposures: "exposures/exposures",
      assignments: "assignment/assignments",
      assessment: "assessment/assessment",
      questions: "assessment/questions",
      answerableQuestions: "assessment/answerableQuestions",
      questionPages: "assessment/questionPages",
      conditionColorMapping: "condition/conditionColorMapping",
      submissions: "submissions/submissions",
      alertStatuses: "alert/statuses"
    }),
    currentAssignment() {
      return JSON.parse(this.$route.params.current_assignment);
    },
    assignmentCount() {
      return this.assignments.length;
    },
    assignmentId() {
      return this.currentAssignment.assignmentId;
    },
    assignmentTitle() {
      return this.currentAssignment.title;
    },
    exposureId() {
      return parseInt(this.$route.params.exposureId);
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
    conditionName() {
      return this.condition.name;
    },
    conditionColor() {
      return this.conditionColorMapping[this.conditionName];
    },
    condition() {
      return this.experiment.conditions.find(
        (c) => parseInt(c.conditionId) === parseInt(this.conditionId)
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
        INTEGRATION: ExternalIntegrationEditor
      };
    },
    html: {
      get() {
        return this.assessment.html;
      },
      set(value) {
        this.setAssessment({ ...this.assessment, html: value });
      },
    },
    isIntegrationType() {
      return this.questions.some((question) => question.questionType === "INTEGRATION");
    },
    integrationClients() {
      return this.assessment.integrationClients || [];
    },
    displayBackToTreatmentModeSelection() {
      return !this.currentAssignment.started;
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
      deleteQuestions: "assessment/deleteQuestions",
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
    handleAddTerracottaBuilder() {
      this.treatmentOptionSelected = true;
    },
    async handleBackToTreatmentModeSelection() {
      const result = await this.$swal({
        title: "Are you sure you want to go back?",
        html: "If you go back to treatment mode selection, you will <b>lose <u>all</u> progress</b> you've made here.",
        showCancelButton: true,
        confirmButtonText: "YES, GO BACK",
        cancelButtonText: "CANCEL",
        reverseButtons: true,
        allowOutsideClick: () => !this.$swal.isLoading(),
      });

      if (!result.isConfirmed) {
        return;
      }

      this.handleClearQuestions();
      this.treatmentOptionSelected = false;
    },
    async handleIntegrationUpdate(question) {
      // updates the integration and points data for the associated question
      if (question.points === null) {
        question.points = 0;
      }

      this.setAssessment({ ...this.assessment, allowStudentViewResponses: question.feedbackEnabled });

      this.integrationQuestionValidation = {
        launchUrl: question.launchUrlValidated,
        points: question.pointsValidated
      }
    },
    async handleAddIntegration(integrationName) {
      let integrationClientId = this.integrationClients.find((integrationClient) => integrationClient.name === integrationName).id;
      this.treatmentOptionSelected = true;
      await this.handleAddQuestion("INTEGRATION", integrationClientId);
      widenContainer();
    },
    async handleAddQuestion(questionType, integrationClientId = null) {
      try {
        await this.createQuestion([
          this.experiment.experimentId,
          this.conditionId,
          this.treatmentId,
          this.assessmentId,
          this.questions.length,
          questionType,
          1, // points
          "",
          integrationClientId
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

        createStatusAlert(
          statusAlert(
            this.alertStatuses.success,
            "Question added successfully."
          )
        );
      } catch (error) {
        console.error(error);
        createStatusAlert(
          statusAlert(
            this.alertStatuses.error,
            "An error occurred while adding the question. Please try again."
          )
        );
      }
    },
    async handleAddMCOption(question) {
      try {
        await this.createAnswer([
          this.experiment.experimentId,
          this.conditionId,
          this.treatmentId,
          this.assessmentId,
          question.questionId,
          "",
          false,
          0,
        ]);
        createStatusAlert(
          statusAlert(
            this.alertStatuses.success,
            "Multiple choice option added successfully."
          )
        );
      } catch (error) {
        console.error(error);
        createStatusAlert(
          statusAlert(
            this.alertStatuses.error,
            "An error occurred while adding the multiple choice option. Please try again."
          )
        );
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

        createStatusAlert(
          statusAlert(
            this.alertStatuses.success,
            "Question order updated successfully."
          )
        );
      }
    },
    async handleClearQuestions() {
      if (this.questions.length === 0) {
        return;
      }

      this.deleteQuestions([
        this.experiment.experimentId,
        this.condition.conditionId,
        this.treatmentId,
        this.assessmentId,
        this.assessment.questions
      ]).then(response => {
        if (response.status === 400) {
          this.$swal(response.data);
          createStatusAlert(
            statusAlert(
              this.alertStatuses.error,
              "An error occurred while clearing questions. Please try again."
            )
          );
          return false;
        }
        createStatusAlert(
          statusAlert(
            this.alertStatuses.success,
            "Questions cleared successfully."
          )
        );
      });

      return true;
    },
    async handleSaveAssessment() {
      const response = await this.updateAssessment([
        this.experiment.experimentId,
        this.condition.conditionId,
        this.treatmentId,
        this.assessmentId,
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
        createStatusAlert(
          statusAlert(
            this.alertStatuses.error,
            "An error occurred while saving the assessment. Please try again."
          )
        );
        return false;
      }
      return true;
    },
    async handleSaveQuestions(questions) {
      return Promise.all(
        questions.map(async (question, index) => {
          try {
            this.updateQuestions(question);
            const q = await this.updateQuestion([
              this.experiment.experimentId,
              this.conditionId,
              this.treatmentId,
              this.assessmentId,
              question.questionId,
              question.html,
              question.points,
              index,
              question.questionType,
              question.randomizeAnswers,
              question.answers,
              question.integration
            ]);
            return Promise.resolve(q);
          } catch (error) {
            return Promise.reject(error);
          }
        })
      );
    },
    async handleSaveAnswers() {
      return Promise.all(
        this.questions.map((question) => {
          question?.answers?.map(async (answer, answerIndex) => {
            try {
              const a = await this.updateAnswer([
                this.experiment.experimentId,
                this.conditionId,
                this.treatmentId,
                this.assessmentId,
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
      if (this.questions.length && this.questions[0].questionType !== "INTEGRATION") {
        if (this.answerableQuestions.some((q) => !q.html)) {
          this.$swal("Please fill or delete empty questions.");
          return false;
        }
      }

      if (this.questions.length && this.questions[0].questionType === "INTEGRATION") {
        let isInvalid = false;

        if (!this.integrationQuestionValidation.launchUrl) {
          // launchUrl invalid
          isInvalid = true;
        }

        if (!this.integrationQuestionValidation.points) {
          // points invalid
          isInvalid = true;
        }

        if (isInvalid) {
          this.$swal("Please complete all fields.");
          return false;
        }
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
          params: {
            exposureId: isNaN(this.exposureId) ? this.$route.params.exposureId : this.exposureId,
            ...statusAlert(
              this.alertStatuses.success,
              "Assignment saved successfully."
            )
          },
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
        this.conditionId,
        this.treatmentId,
        this.assessmentId,
        this.regradeDetails
      ]);
    },
    async duplicate(fromAssignment) {
      let availableTreatments = fromAssignment.treatments
        .filter(
          (treatment) => treatment.treatmentId !== this.treatmentId && !treatment.assessmentDto.integration
        );
      availableTreatments = availableTreatments.map(
        (availableTreatment) => {
          return {
            treatmentId: availableTreatment.treatmentId,
            conditionName: this.conditionForTreatment(this.getGroupConditionListForAssignment(fromAssignment), availableTreatment.conditionId).conditionName,
            conditionColor: this.conditionColorMapping[this.conditionForTreatment(this.getGroupConditionListForAssignment(fromAssignment), availableTreatment.conditionId).conditionName]
          }
        }
      );

      let selectedTreatment = await this.handleDisplayCopyFromDialog(availableTreatments);

      if (!selectedTreatment || selectedTreatment.isDismissed) {
        // no treatment selected or dialog dismissed
        return;
      }

      let fromTreatment = this.assignmentsAvailableToCopy.map(
        (assignmentAvailableToCopy) => {
          let matchingTreatment = assignmentAvailableToCopy.treatments.find(treatment => treatment.treatmentId === parseInt(selectedTreatment.value.treatmentId));

          if (matchingTreatment) {
            return matchingTreatment;
          }
        }
      ).filter(treatment => treatment != null);

      if (fromTreatment.length === 0) {
        this.$swal("Selected assignment does not have any treatments to copy from.");
        return;
      }

      const { assessmentDto, conditionId } = fromTreatment[0];
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
          this.conditionId,
          this.treatmentId,
          {
            treatmentId: this.treatmentId,
            conditionId: this.conditionId,
            assignmentId: this.assignmentId,
            assessmentDto: {
              ...copy,
              treatmentId: this.treatmentId,
              assessmentId: this.assessmentId
            },
            assignmentDto: {
              ...this.assignment
            }
          },
        ]);

        this.treatmentOptionSelected = true;

        return await this.fetchAssessment([
          this.experiment.experimentId,
          this.conditionId,
          this.treatmentId,
          this.assessmentId,
        ]);
      } catch (error) {
        console.error("handleCreateTreatment | catch", { error });
        this.treatmentOptionSelected = false;
      }
    },
    async saveExit() {
      let startTime = Date.now();

      while (this.urlValidationInProgress) {
        await new Promise((resolve) => setTimeout(resolve, 100));

        if (Date.now() - startTime > 5000) {
          // timeout after 5 seconds
          console.log("URL validation timeout");
          this.urlValidationInProgress = false;
        }
      }

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
      return treatments.some((t) => t.treatmentId !== this.treatmentId && !t.assessmentDto.integration);
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
              assignmentName: this.assignmentTitle,
              conditionName: this.conditionName,
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
    },
    handleUrlValidationInProgress(value) {
      this.urlValidationInProgress = value;
    },
    handleDisplayCopyFromDialog(availableTreatments) {
      return this.$swal({
        html: '<div id="dialog-copy-from"></div>',
        showCancelButton: true,
        confirmButtonText: "Copy",
        cancelButtonText: "Cancel",
        reverseButtons: true,
        allowOutsideClick: false,
        allowEscapeKey: false,
        focusConfirm: false,
        customClass: {
          confirmButton: "response-option-confirm",
          popup: "copy-from-popup"
        },
        preConfirm: () => {
          const treatmentOption = this.$swal.getPopup().querySelector("input#treatment-option-selected");

          if (treatmentOption && treatmentOption.value) {
            return { treatmentId: treatmentOption.value };
          }

          // no value selected; prompt user to select one
          this.$swal.showValidationMessage("Please select a treatment to copy the content from.");
        },
        willOpen: () => {
          var CopyFromDialogClass = Vue.extend(CopyFromDialog);
          var copyFromDialog = new CopyFromDialogClass({
            propsData: {
              treatments: availableTreatments,
              assignmentName: this.assignmentTitle
            }
          });
          copyFromDialog.$mount(document.getElementById("dialog-copy-from"));
        },
        didOpen: () => {
          const treatmentOptionSelect = this.$swal.getHtmlContainer().querySelector("#copy-radio-group");

          if (treatmentOptionSelect) {
            treatmentOptionSelect.focus();
          }
        }
      });
    }
  },
  async created() {
    await this.fetchAssessment([
      this.experiment.experimentId,
      this.conditionId,
      this.treatmentId,
      this.assessmentId,
    ]);
    await await this.fetchSubmissions([
      this.experiment.experimentId,
      this.conditionId,
      this.treatmentId,
      this.assessmentId
    ]);
    this.getAssignmentDetails();
    this.findAssignmentsAvailableToCopy();
    this.treatmentOptionSelected = this.questions.length;
  },
  mounted() {
    widenContainer();
    deleteAttributesFromObservedElement(".terracotta-builder", "question-page", ".v-expansion-panel", ["aria-expanded"]);
  },
  beforeUnmount() {
    shrinkContainer();
  }
};
</script>

<style scoped lang="scss">
@import "~@/styles/variables";

v-expansion-panels {
  &, & > div {
    width: 100%;
  }
}
.terracotta-builder {
  & h4:not(.label-treatment):not(.label-condition-name) {
    font-weight: bold !important;
  }
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
  .v-btn--plain:not(.v-btn--active):not(.v-btn--loading):not(:focus):not(:hover) .v-btn__content {
    color: map-get($blue, "base") !important;
    opacity: 1 !important;
  }
  .bottom-menu {
    & .treatment-mode-container,
    & .treatment-mode-selected-container {
      max-width: fit-content;
      & .add-treatment-type,
      & .treatment-type-selected {
        margin-top: 12px;
        margin-bottom: 12px;
        &:hover,
        &:focus {
          & ::v-deep .v-btn__content {
            text-decoration: underline;
          }
        }
      }
      & .add-treatment-type {
        color: map-get($blue, "primary") !important;
        opacity: 1 !important;
        & ::v-deep .v-btn__content {
          color: map-get($blue, "primary") !important;
          opacity: 1 !important;
        }
      }
    }
  }
  .add-questions-to-continue {
    opacity: .7 !important;
  }
}
</style>
