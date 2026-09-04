<template>
  <div
    v-if="experiment && assessment"
    class="terracotta-builder"
  >
    <BuilderHeader
      :assignment-title="assignmentTitle"
      :condition-name="conditionName"
      :condition-color="conditionColor"
      :has-single-treatment="hasSingleTreatment"
    />

    <v-tabs
      v-model="tab"
      class="tabs"
    >
      <v-tab value="treatment">
        Treatment
      </v-tab>
      <v-tab value="settings">
        Settings
      </v-tab>
    </v-tabs>

    <v-window v-model="tab">
      <v-window-item value="treatment">
        <TreatmentEditorTab
          v-model:html="html"
          :assessment="assessment"
          :questions="questions"
          :question-pages="questionPages"
          :treatment-option-selected="treatmentOptionSelected"
          :is-integration-type="isIntegrationType"
          :can-clear-all="canClearAll"
          :expanded-question-panel="expandedQuestionPanel"
          :assignments-available-to-copy="assignmentsAvailableToCopy"
          :integration-clients="integrationClients"
          :display-back-to-treatment-mode-selection="displayBackToTreatmentModeSelection"
          :text-only="textOnly"
          @save-all="saveAll"
          @clear-questions="handleClearQuestions"
          @question-order-change="handleQuestionOrderChange"
          @edited-question="addEditedQuestion"
          @update-expanded-question-page-panel="expandedQuestionPagePanel = $event"
          @panel-ref="setQuestionPanelRef"
          @integration-updated="handleIntegrationUpdate"
          @url-validation-in-progress="handleUrlValidationInProgress"
          @add-terracotta-builder="handleAddTerracottaBuilder"
          @add-integration="handleAddIntegration"
          @add-question="handleAddQuestion"
          @duplicate="duplicate"
          @back-to-treatment-mode-selection="handleBackToTreatmentModeSelection"
        />
      </v-window-item>

      <v-window-item
        value="settings"
        class="my-5"
      >
        <TreatmentSettings />
      </v-window-item>
    </v-window>
  </div>
</template>

<script setup>
import {
  ref,
  computed,
  watch,
  nextTick,
  onMounted,
  onBeforeUnmount,
  getCurrentInstance,
  createVNode,
  render
} from "vue";

import { useRoute, useRouter } from "vue-router";
import Swal from "sweetalert2";

import { assessmentService } from "@/services";
import {
  shrinkContainer,
  widenContainer,
  deleteAttributesFromObservedElement,
  deleteAttributesFromElement,
  addAttributesToElement,
  createStatusAlert,
  statusAlert
} from "@/helpers/ui-utils.js";

import omitDeep from "@/helpers/deep-omit";

import BuilderHeader from "./components/BuilderHeader.vue";
import CopyFromDialog from "@/components/dialog/CopyFromDialog.vue";
import RegradeAssignmentDialog from "@/components/RegradeAssignmentDialog.vue";
import TreatmentEditorTab from "./components/TreatmentEditorTab.vue";
import TreatmentSettings from "@/views/assignment/TreatmentSettings.vue";

import { assessment as assessmentModule } from "@/store/assessment.module";
import { assignment as assignmentModule } from "@/store/assignment.module";
import { exposures as exposuresModule } from "@/store/exposures.module";
import { condition as conditionModule } from "@/store/condition.module";
import { submission as submissionModule } from "@/store/submission.module";
import { alert as alertModule } from "@/store/alert.module";
import { treatment as treatmentModule } from "@/store/treatment.module";

defineOptions({
  name: "TerracottaBuilder"
});

const props = defineProps({
  experiment: {
    type: Object,
    required: true
  }
});

const route = useRoute();
const router = useRouter();

const assessmentStore = assessmentModule();
const assignmentStore = assignmentModule();
const exposuresStore = exposuresModule();
const conditionStore = conditionModule();
const submissionStore = submissionModule();
const alertStore = alertModule();
const treatmentStore = treatmentModule();

const appContext = getCurrentInstance()?.appContext;

const assignmentsAvailableToCopy = ref([]);
const tab = ref("treatment");
const expandedQuestionPagePanel = ref(null);
const expandedQuestionPanel = ref([]);
const questionPanelRefs = ref({});
const regradeDetails = ref({
  regradeOption: "NA",
  editedMCQuestionIds: []
});
const treatmentOptionSelected = ref(false);
const integrationQuestionValidation = ref(null);
const urlValidationInProgress = ref(false);

const assignment = computed(() => assignmentStore.assignment);
const exposures = computed(() => exposuresStore.exposures || []);
const assignments = computed(() => assignmentStore.assignments || []);
const assessment = computed(() => assessmentStore.assessment);
const questions = computed(() => assessmentStore.questions || []);
const answerableQuestions = computed(() => assessmentStore.answerableQuestions || []);
const questionPages = computed(() => assessmentStore.questionPages || []);
const conditionColorMapping = computed(() => conditionStore.conditionColorMapping || {});
const submissions = computed(() => submissionStore.submissions || []);
const alertStatuses = computed(() => alertStore.statuses || {});

const currentAssignment = computed(() => {
  return history.state?.current_assignment || {};
});

const assignmentCount = computed(() => assignments.value.length);

const assignmentId = computed(() => currentAssignment.value.assignmentId);
const assignmentTitle = computed(() => currentAssignment.value.title || "");
const treatmentId = computed(() => Number.parseInt(route.params.treatmentId, 10));
const assessmentId = computed(() => Number.parseInt(route.params.assessmentId, 10));
const conditionId = computed(() => Number.parseInt(route.params.conditionId, 10));

const condition = computed(() => {
  return props.experiment.conditions.find(
    item => Number(item.conditionId) === Number(conditionId.value)
  );
});

const conditionName = computed(() => condition.value?.name || "");
const conditionColor = computed(() => conditionColorMapping.value[conditionName.value]);

const canClearAll = computed(() => {
  return assessment.value?.questions?.length > 0 && !assessment.value?.started;
});

const hasSingleTreatment = computed(() => {
  return currentAssignment.value?.treatments?.length === 1;
});

const studentCount = computed(() => {
  return new Set(
    submissions.value.map(submission => submission.participantId)
  ).size;
});

const submissionCount = computed(() => submissions.value?.length ?? 0);
const hasSubmissions = computed(() => submissionCount.value > 0);
const hasEditedQuestions = computed(() => regradeDetails.value.editedMCQuestionIds.length > 0);
const displayRegradeAssignmentDialog = computed(() => hasSubmissions.value && hasEditedQuestions.value);

const isIntegrationType = computed(() => {
  return questions.value.some(question => question.questionType === "INTEGRATION");
});

const integrationClients = computed(() => assessment.value?.integrationClients || []);

const displayBackToTreatmentModeSelection = computed(() => {
  return !currentAssignment.value?.started;
});

const html = computed({
  get() {
    return assessment.value?.html || "";
  },
  set(value) {
    assessmentStore.setAssessment({
      ...assessment.value,
      html: value
    });
  }
});

watch(expandedQuestionPagePanel, async pageIndex => {
  for (let index = 0; index < questionPages.value.length; index++) {
    if (index !== pageIndex) {
      expandedQuestionPanel.value[index] = null;
    }
  }

  await nextTick();
  window.setTimeout(() => {
    deleteAttributesFromElement(".v-expansion-panel", ["aria-expanded"]);
    addAttributesToElement(".editor .ProseMirror", [
      {
        name: "aria-label",
        value: "question editor content"
      }
    ]);
  }, 1000);
});

watch(
  expandedQuestionPanel,
  panelIndexes => {
    if (!treatmentOptionSelected.value || isIntegrationType.value) {
      return;
    }

    const pageIndex = expandedQuestionPagePanel.value;
    const panelIndex = panelIndexes?.[pageIndex];

    if (pageIndex == null || panelIndex == null) {
      return;
    }

    const panel = questionPanelRefs.value[buildExpandedQuestionPanelId(pageIndex, panelIndex)];

    if (!panel?.$el && !panel) {
      return;
    }

    window.setTimeout(() => {
      const element = panel.$el || panel;
      element.scrollIntoView({
        behavior: "smooth",
        block: "start"
      });
    }, 500);
  },
  { deep: true }
);

watch(assignmentCount, () => {
  findAssignmentsAvailableToCopy();
});

watch(questions, async () => {
  await nextTick();

  window.setTimeout(() => {
    deleteAttributesFromElement(".v-expansion-panel", ["aria-expanded"]);
    addAttributesToElement(".editor .ProseMirror", [
      {
        name: "aria-label",
        value: "question editor content"
      }
    ]);
  }, 1000);
});

const setQuestionPanelRef = ({ element, pageIndex, questionIndex }) => {
  questionPanelRefs.value[buildExpandedQuestionPanelId(pageIndex, questionIndex)] = element;
};

const getGroupConditionListForAssignment = item => {
  const exposure = exposures.value.find(exposure => exposure.exposureId === item.exposureId);

  return exposure?.groupConditionList || [];
};

const conditionForTreatment = (groupConditionList, currentConditionId) => {
  return groupConditionList.find(item => item.conditionId === currentConditionId);
};

const getAssignmentDetails = async () => {
  await exposuresStore.fetchExposures(props.experiment.experimentId);

  return exposures.value;
};

const handleAddTerracottaBuilder = () => {
  treatmentOptionSelected.value = true;
};

const handleBackToTreatmentModeSelection = async () => {
  const result = await Swal.fire({
    title: "Are you sure you want to go back?",
    html: "If you go back to treatment mode selection, you will <b>lose <u>all</u> progress</b> you've made here.",
    showCancelButton: true,
    confirmButtonText: "YES, GO BACK",
    cancelButtonText: "CANCEL",
    reverseButtons: true,
    allowOutsideClick: () => !Swal.isLoading()
  });

  if (!result.isConfirmed) {
    return;
  }

  await handleClearQuestions();
  treatmentOptionSelected.value = false;
};

const handleIntegrationUpdate = question => {
  if (question.points === null) {
    question.points = 0;
  }

  assessmentStore.setAssessment({
    ...assessment.value,
    allowStudentViewResponses: question.feedbackEnabled
  });

  integrationQuestionValidation.value = {
    launchUrl: question.launchUrlValidated,
    points: question.pointsValidated
  };
};

const handleAddIntegration = async integrationName => {
  const integrationClientId = integrationClients.value.find(
    integrationClient => integrationClient.name === integrationName
  )?.id;

  treatmentOptionSelected.value = true;

  await handleAddQuestion("INTEGRATION", integrationClientId);
  widenContainer();
};

const handleAddQuestion = async (questionType, integrationClientId = null) => {
  try {
    const response = await assessmentStore.createQuestion([
      props.experiment.experimentId,
      conditionId.value,
      treatmentId.value,
      assessmentId.value,
      questions.value.length,
      questionType,
      1,
      "",
      integrationClientId
    ]);

    if (!response?.data?.questionId) {
      throw new Error("Failed to create question");
    }

    if (questionType === "MC") {
      for (let index = 0; index <= 1; index++) {
        await handleAddMCOption(questions.value[questions.value.length - 1]);
      }
    }

    const questionPageIndex = questionPages.value.length > 0 ? questionPages.value.length - 1 : 0;
    const lastQuestionPage = questionPages.value[questionPageIndex];
    const questionIndex = lastQuestionPage?.questions?.length > 0
      ? lastQuestionPage.questions.length - 1
      : 0;

    expandQuestionPanel(questionPageIndex, questionIndex);

    createStatusAlert(
      statusAlert(
        alertStatuses.value.success,
        "Question added successfully."
      )
    );
  } catch (error) {
    console.error(error);

    createStatusAlert(
      statusAlert(
        alertStatuses.value.error,
        "An error occurred while adding the question. Please try again."
      )
    );
  }
};

const handleAddMCOption = async question => {
  try {
    await assessmentStore.createAnswer([
      props.experiment.experimentId,
      conditionId.value,
      treatmentId.value,
      assessmentId.value,
      question.questionId,
      "",
      false,
      0
    ]);
  } catch (error) {
    console.error(error);

    createStatusAlert(
      statusAlert(
        alertStatuses.value.error,
        "An error occurred while adding the multiple choice option. Please try again."
      )
    );
  }
};

const handleQuestionOrderChange = async event => {
  if (event.removed) {
    return;
  }

  const list = questions.value.map(question => ({ ...question }));
  const draggableEvent = event.added || event.moved;

  if (!draggableEvent) {
    return;
  }

  const { element, newIndex } = draggableEvent;
  const oldIndex = list.findIndex(question => question.questionId === element.questionId);
  const movedItem = list.splice(oldIndex, 1)[0];

  list.splice(newIndex, 0, movedItem);

  const orderedQuestions = list.map((question, index) => ({
    ...question,
    questionOrder: index
  }));

  await handleSaveQuestions(orderedQuestions);

  createStatusAlert(
    statusAlert(
      alertStatuses.value.success,
      "Question order updated successfully."
    )
  );
};

const handleClearQuestions = async () => {
  if (questions.value.length === 0) {
    return true;
  }

  try {
    const response = await assessmentStore.deleteQuestions([
      props.experiment.experimentId,
      condition.value.conditionId,
      treatmentId.value,
      assessmentId.value,
      assessment.value.questions
    ]);

    if (response?.status === 400) {
      await Swal.fire(response.data);

      createStatusAlert(
        statusAlert(
          alertStatuses.value.error,
          "An error occurred while clearing questions. Please try again."
        )
      );

      return false;
    }

    createStatusAlert(
      statusAlert(
        alertStatuses.value.success,
        "Questions cleared successfully."
      )
    );

    return true;
  } catch (error) {
    console.error("handleClearQuestions | catch", { error });

    createStatusAlert(
      statusAlert(
        alertStatuses.value.error,
        "An error occurred while clearing questions. Please try again."
      )
    );

    return false;
  }
};

const handleSaveAssessment = async () => {
  const response = await assessmentStore.updateAssessment([
    props.experiment.experimentId,
    condition.value.conditionId,
    treatmentId.value,
    assessmentId.value,
    assessment.value.html,
    assessment.value.allowStudentViewResponses,
    assessment.value.studentViewResponsesAfter,
    assessment.value.studentViewResponsesBefore,
    assessment.value.allowStudentViewCorrectAnswers,
    assessment.value.studentViewCorrectAnswersAfter,
    assessment.value.studentViewCorrectAnswersBefore,
    assessment.value.numOfSubmissions,
    assessment.value.multipleSubmissionScoringScheme,
    assessment.value.hoursBetweenSubmissions,
    assessment.value.cumulativeScoringInitialPercentage
  ]);

  if (response?.status === 400) {
    await Swal.fire(response.data);

    createStatusAlert(
      statusAlert(
        alertStatuses.value.error,
        "An error occurred while saving the assessment. Please try again."
      )
    );

    return false;
  }

  return true;
};

const handleSaveQuestions = async questionsToSave => {
  const questionsWithOrder = questionsToSave.map((question, index) => {
    const updatedQuestion = {
      ...question,
      questionOrder: index
    };

    assessmentStore.updateQuestions(updatedQuestion);

    return updatedQuestion;
  });

  // one batched request for all questions instead of one PUT per question
  return assessmentStore.updateQuestionsBatch([
    props.experiment.experimentId,
    conditionId.value,
    treatmentId.value,
    assessmentId.value,
    questionsWithOrder
  ]);
};

const handleSaveAnswers = async () => {
  // one batched request per question (covering all of its answers) instead of one PUT per answer
  const answerRequests = questions.value
    .filter(question => (question.answers || []).length)
    .map(question => {
      const answersWithOrder = question.answers.map((answer, answerIndex) => {
        const updatedAnswer = {
          ...answer,
          answerOrder: answerIndex
        };

        assessmentStore.updateAnswers(updatedAnswer);

        return updatedAnswer;
      });

      return assessmentStore.updateAnswersBatch([
        props.experiment.experimentId,
        conditionId.value,
        treatmentId.value,
        assessmentId.value,
        question.questionId,
        answersWithOrder
      ]);
    });

  return Promise.all(answerRequests);
};

const saveAll = async routeName => {
  if (
    questions.value.length &&
    questions.value[0].questionType !== "INTEGRATION" &&
    answerableQuestions.value.some(question => !question.html)
  ) {
    await Swal.fire("Please fill or delete empty questions.");
    return false;
  }

  if (questions.value.length && questions.value[0].questionType === "INTEGRATION") {
    const invalid = !integrationQuestionValidation.value?.launchUrl ||
      !integrationQuestionValidation.value?.points;

    if (invalid) {
      await Swal.fire("Please complete all fields.");
      return false;
    }
  }

  if (displayRegradeAssignmentDialog.value) {
    const regradeOption = await handleDisplayRegradeAssignmentDialog();

    if (regradeOption.isDismissed) {
      return false;
    }

    regradeDetails.value.regradeOption = regradeOption.value.regradeOption;
  }

  const savedAssessment = await handleSaveAssessment();

  if (!savedAssessment) {
    return false;
  }

  await handleSaveQuestions(questions.value);
  await handleSaveAnswers();
  await handleRegradeQuestions();

  createStatusAlert(
    statusAlert(alertStatuses.value.success, "Assignment saved successfully.")
  );

  router.push({
    name: routeName,
    params: {
      experimentId: props.experiment.experimentId
    }
  });

  return true;
};

const handleRegradeQuestions = async () => {
  if (!regradeDetails.value.editedMCQuestionIds.length) {
    return;
  }

  await assessmentStore.regradeQuestions([
    props.experiment.experimentId,
    conditionId.value,
    treatmentId.value,
    assessmentId.value,
    regradeDetails.value
  ]);
};

const duplicate = async fromAssignment => {
  let availableTreatments = fromAssignment.treatments
    .filter(treatment => treatment.treatmentId !== treatmentId.value && !treatment.assessmentDto.integration)
    .map(treatment => {
      const conditionMatch = conditionForTreatment(
        getGroupConditionListForAssignment(fromAssignment),
        treatment.conditionId
      );

      return {
        treatmentId: treatment.treatmentId,
        conditionName: conditionMatch?.conditionName,
        conditionColor: conditionColorMapping.value[conditionMatch?.conditionName]
      };
    });

  const selectedTreatment = await handleDisplayCopyFromDialog(availableTreatments);

  if (!selectedTreatment || selectedTreatment.isDismissed) {
    return;
  }

  const fromTreatment = assignmentsAvailableToCopy.value
    .map(assignmentAvailableToCopy => {
      return assignmentAvailableToCopy.treatments.find(
        treatment => treatment.treatmentId === Number.parseInt(selectedTreatment.value.treatmentId, 10)
      );
    })
    .filter(treatment => treatment !== undefined);

  if (fromTreatment.length === 0) {
    await Swal.fire("Selected assignment does not have any treatments to copy from.");
    return;
  }

  const { assessmentDto, conditionId: fromConditionId } = fromTreatment[0];
  const { treatmentId: fromTreatmentId, assessmentId: fromAssessmentId } = assessmentDto;
  let sourceAssessment;

  try {
    sourceAssessment = await assessmentService.fetchAssessment(
      props.experiment.experimentId,
      fromConditionId,
      fromTreatmentId,
      fromAssessmentId
    );
  } catch (error) {
    console.error("duplicate.fetchAssessment | catch", { error });
    return;
  }

  const copy = omitDeep({ ...sourceAssessment.data }, [
    "answerId",
    "questionId",
    "assessmentId"
  ]);

  try {
    await treatmentStore.updateTreatment([
      props.experiment.experimentId,
      conditionId.value,
      treatmentId.value,
      {
        treatmentId: treatmentId.value,
        conditionId: conditionId.value,
        assignmentId: assignmentId.value,
        assessmentDto: {
          ...copy,
          treatmentId: treatmentId.value,
          assessmentId: assessmentId.value
        },
        assignmentDto: {
          ...assignment.value
        }
      }
    ]);

    treatmentOptionSelected.value = true;

    return await assessmentStore.fetchAssessment([
      props.experiment.experimentId,
      conditionId.value,
      treatmentId.value,
      assessmentId.value
    ]);
  } catch (error) {
    console.error("duplicate.updateTreatment | catch", { error });
    treatmentOptionSelected.value = false;
  }
};

const saveExit = async () => {
  const startTime = Date.now();

  while (urlValidationInProgress.value) {
    await new Promise(resolve => window.setTimeout(resolve, 100));

    if (Date.now() - startTime > 5000) {
      console.log("URL validation timeout");
      urlValidationInProgress.value = false;
    }
  }

  return saveAll("ExperimentSummary");
};

const textOnly = htmlString => {
  const parser = new DOMParser();
  const documentValue = parser.parseFromString(htmlString, "text/html");

  return Array.from(documentValue.body.children)
    .map(child => child.innerText)
    .join(" ");
};

const expandQuestionPanel = (questionPageIndex, questionPanelIndex) => {
  expandedQuestionPagePanel.value = questionPageIndex;
  expandedQuestionPanel.value = [];
  expandedQuestionPanel.value[questionPageIndex] = questionPanelIndex;
};

const buildExpandedQuestionPanelId = (questionPageIndex, questionPanelIndex) => {
  return `question-panel-${questionPageIndex}_${questionPanelIndex}`;
};

const hasTreatmentsNotCurrent = treatments => {
  return treatments.some(
    treatment => treatment.treatmentId !== treatmentId.value && !treatment.assessmentDto.integration
  );
};

const findAssignmentsAvailableToCopy = () => {
  assignmentsAvailableToCopy.value = assignments.value.filter(item => {
    return hasTreatmentsNotCurrent(item.treatments || []);
  });
};

const mountDialogComponent = (selector, component, props = {}) => {
  const target = document.querySelector(selector);

  if (!target) {
    return null;
  }

  const vnode = createVNode(component, props);
  vnode.appContext = appContext;
  render(vnode, target);

  return {
    unmount: () => render(null, target)
  };
};

const handleDisplayRegradeAssignmentDialog = () => {
  let dialogApp = null;

  return Swal.fire({
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
      const regradeOption = Swal.getPopup().querySelector("input#regrade-option-selected");

      if (regradeOption?.value) {
        return {
          regradeOption: regradeOption.value
        };
      }

      Swal.showValidationMessage("Please select a regrade option");
      return false;
    },
    willOpen: () => {
      dialogApp = mountDialogComponent(
        "#dialog-regrade-assignment",
        RegradeAssignmentDialog,
        {
          assignmentName: assignmentTitle.value,
          conditionName: conditionName.value,
          studentCount: studentCount.value,
          editedQuestionCount: regradeDetails.value.editedMCQuestionIds.length
        }
      );
    },
    didDestroy: () => {
      dialogApp?.unmount();
    }
  });
};

const addEditedQuestion = questionId => {
  if (regradeDetails.value.editedMCQuestionIds.includes(questionId)) {
    return;
  }

  regradeDetails.value.editedMCQuestionIds.push(questionId);
};

const handleUrlValidationInProgress = value => {
  urlValidationInProgress.value = value;
};

const handleDisplayCopyFromDialog = availableTreatments => {
  let dialogApp = null;

  return Swal.fire({
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
      const treatmentOption = Swal.getPopup().querySelector("input#treatment-option-selected");

      if (treatmentOption?.value) {
        return {
          treatmentId: treatmentOption.value
        };
      }

      Swal.showValidationMessage("Please select a treatment to copy the content from.");
      return false;
    },
    willOpen: () => {
      dialogApp = mountDialogComponent(
        "#dialog-copy-from",
        CopyFromDialog,
        {
          treatments: availableTreatments,
          assignmentName: assignmentTitle.value
        }
      );
    },
    didOpen: () => {
      const treatmentOptionSelect = Swal.getHtmlContainer().querySelector("#copy-radio-group");

      if (treatmentOptionSelect) {
        treatmentOptionSelect.focus();
      }
    },
    didDestroy: () => {
      dialogApp?.unmount();
    }
  });
};

onMounted(async () => {
  await Promise.all([
    assessmentStore.fetchAssessment([
      props.experiment.experimentId,
      conditionId.value,
      treatmentId.value,
      assessmentId.value
    ]),
    submissionStore.fetchSubmissions([
      props.experiment.experimentId,
      conditionId.value,
      treatmentId.value,
      assessmentId.value
    ]),
    getAssignmentDetails()
  ]);
  findAssignmentsAvailableToCopy();
  treatmentOptionSelected.value = questions.value.length > 0;

  widenContainer();
  deleteAttributesFromObservedElement(
    ".terracotta-builder",
    "question-page",
    ".v-expansion-panel",
    ["aria-expanded"]
  );
});

onBeforeUnmount(() => {
  shrinkContainer();
});

defineExpose({
  saveExit
});
</script>

<style scoped lang="scss">
v-expansion-panels {
  &,
  & > div {
    width: 100%;
  }
}

.terracotta-builder {
  & h4:not(.label-treatment):not(.label-condition-name) {
    font-weight: bold !important;
  }

  :deep(.v-expansion-panel-title--active) {
    border-bottom: 2px solid map.get($grey, "lighter");
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

  .tabs {
    border-top: 1px solid map.get($grey, "lighter");
    border-bottom: 1px solid map.get($grey, "lighter");
  }

  // excludes MultipleChoiceQuestionEditor.vue's correct-answer checkmark and
  // delete-option buttons, and ExternalIntegrationEditor.vue's preview/copy-url
  // buttons, which all set their own deliberate colors (green when checked,
  // grey for delete, white on their own dynamic background colors) - this
  // rule's blue !important otherwise wins over those regardless of
  // specificity, since it targets .v-btn__content (a more specific
  // descendant) rather than the outer .v-btn those rules target.
  :deep(.v-btn:not(.v-btn--active):not(.v-btn--loading):not(:focus):not(:hover):not(.correct):not(.delete_option):not(.preview-btn):not(.copy-url-btn) .v-btn__content) {
    color: map.get($blue, "base") !important;
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
          :deep(.v-btn__content) {
            text-decoration: underline;
          }
        }
      }

      & .add-treatment-type {
        color: map.get($blue, "primary") !important;
        opacity: 1 !important;

        :deep(.v-btn__content) {
          color: map.get($blue, "primary") !important;
          opacity: 1 !important;
        }
      }
    }
  }

  .add-questions-to-continue {
    opacity: 0.7 !important;
  }
}
</style>
