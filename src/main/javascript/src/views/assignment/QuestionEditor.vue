<template>
  <div v-if="isLoaded">
    <TipTapEditor
      :content="initialContent"
      aria-label="Enter question"
      editor-type="html"
      required
      @edited="handleEditedHtml"
    />

    <v-text-field
      v-model="points"
      :rules="numberRule"
      label="Points"
      class="question-points"
      type="number"
      step="any"
      aria-label="Enter question point value"
      variant="outlined"
      required
    />

    <slot />

    <v-row>
      <v-col
        cols="auto"
        class="flex-grow-1 py-0"
      >
        <slot name="actions" />
      </v-col>

      <v-col
        cols="auto"
        class="text-right py-0"
      >
        <v-menu>
          <template #activator="{ props: menuProps }">
            <v-icon
              v-bind="menuProps"
              color="black"
              :aria-label="`Open question menu for ${question.html || question.questionOrder + 1}`"
            >
              mdi-dots-horizontal
            </v-icon>
          </template>

          <v-list class="text-left">
            <slot name="actions-overflow" />

            <v-list-item
              v-if="isPageBreakAfter"
              @click="removePageBreakAfter(question)"
            >
              <v-list-item-title>
                <v-icon class="mr-2">
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
                <v-icon class="mr-2">
                  mdi-format-page-break
                </v-icon>
                Add page break after question
              </v-list-item-title>
            </v-list-item>

            <v-list-item @click="handleDeleteQuestion(question)">
              <v-list-item-title>
                <v-icon class="mr-2">
                  mdi-delete
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

<script setup>
import {
  ref,
  computed,
  onMounted
} from "vue";

import { useRoute } from "vue-router";
import Swal from "sweetalert2";

import {
  createStatusAlert,
  statusAlert
} from "@/helpers/ui-utils.js";

import TipTapEditor from "@/components/editor/TipTapEditor";

import { assessment as assessmentModule } from "@/store/assessment.module";
import { alert as alertModule } from "@/store/alert.module";

defineOptions({
  name: "QuestionEditor"
});

const props = defineProps({
  question: {
    type: Object,
    required: true
  },
  isMC: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(["edited"]);

const route = useRoute();

const assessmentStore = assessmentModule();
const alertStore = alertModule();

const isLoaded = ref(false);
const initialContent = ref(null);

const numberRule = [
  value => value !== "" && value !== null && !Number.isNaN(Number(value)) || "required",
  value => Number.parseFloat(value) >= 0 || "The point value cannot be negative"
];

const questions = computed(() => {
  return assessmentStore.questions || [];
});

const alertStatuses = computed(() => {
  return alertStore.statuses;
});

const experimentId = computed(() => {
  return Number.parseInt(route.params.experimentId, 10);
});

const treatmentId = computed(() => {
  return Number.parseInt(route.params.treatmentId, 10);
});

const assessmentId = computed(() => {
  return Number.parseInt(route.params.assessmentId, 10);
});

const conditionId = computed(() => {
  return Number.parseInt(route.params.conditionId, 10);
});

const questionIndex = computed(() => {
  return questions.value.findIndex(
    currentQuestion => currentQuestion.questionId === props.question.questionId
  );
});

const isPageBreakAfter = computed(() => {
  const nextQuestion = questions.value[questionIndex.value + 1];

  return nextQuestion?.questionType === "PAGE_BREAK";
});

const html = computed({
  get() {
    return props.question?.html || "";
  },

  set(value) {
    updateQuestionLocally({
      html: value
    });
  }
});

const points = computed({
  get() {
    return props.question.points;
  },

  set(value) {
    updateQuestionLocally({
      points: value
    });
  }
});

const updateQuestionLocally = changes => {
  if (props.isMC) {
    emit("edited");
  }

  assessmentStore.updateQuestions({
    ...props.question,
    ...changes
  });
};

const renumberQuestions = questionList => {
  return questionList.map((question, index) => ({
    ...question,
    questionOrder: index
  }));
};

const saveQuestions = async questionList => {
  const normalizedQuestions = renumberQuestions(questionList);

  normalizedQuestions.forEach(question => {
    assessmentStore.updateQuestions(question);
  });

  // one batched request for all questions instead of one PUT per question
  await assessmentStore.updateQuestionsBatch([
    experimentId.value,
    conditionId.value,
    treatmentId.value,
    assessmentId.value,
    normalizedQuestions
  ]);
};

const handleDeleteQuestion = async question => {
  const result = await Swal.fire({
    icon: "question",
    text: "Are you sure you want to delete the question?",
    showCancelButton: true,
    confirmButtonText: "Yes, delete it",
    cancelButtonText: "No, cancel"
  });

  if (!result.isConfirmed) {
    return;
  }

  try {
    const response = await assessmentStore.deleteQuestion([
      experimentId.value,
      conditionId.value,
      treatmentId.value,
      assessmentId.value,
      question.questionId
    ]);

    createStatusAlert(
      statusAlert(
        alertStatuses.value.success,
        "Question deleted successfully."
      )
    );

    return response;
  } catch (error) {
    console.error("handleDeleteQuestion | catch", { error });

    await Swal.fire("There was a problem deleting the question.");

    createStatusAlert(
      statusAlert(
        alertStatuses.value.error,
        "An error occurred while deleting the question. Please try again."
      )
    );

    return null;
  }
};

const addPageBreakAfter = async () => {
  try {
    const insertIndex = questionIndex.value + 1;

    await assessmentStore.createQuestionAtIndex({
      payload: [
        experimentId.value,
        conditionId.value,
        treatmentId.value,
        assessmentId.value,
        insertIndex,
        "PAGE_BREAK",
        0,
        ""
      ],
      questionIndex: insertIndex
    });

    await saveQuestions(questions.value);

    createStatusAlert(
      statusAlert(
        alertStatuses.value.success,
        "Page break added successfully."
      )
    );
  } catch (error) {
    console.error("addPageBreakAfter | catch", { error });

    await Swal.fire("There was a problem adding a page break.");

    createStatusAlert(
      statusAlert(
        alertStatuses.value.error,
        "An error occurred while adding the page break. Please try again."
      )
    );
  }
};

const removePageBreakAfter = async question => {
  try {
    const index = questions.value.findIndex(
      currentQuestion => currentQuestion.questionId === question.questionId
    );

    const pageBreakQuestion = questions.value[index + 1];

    if (!pageBreakQuestion || pageBreakQuestion.questionType !== "PAGE_BREAK") {
      return;
    }

    await assessmentStore.deleteQuestion([
      experimentId.value,
      conditionId.value,
      treatmentId.value,
      assessmentId.value,
      pageBreakQuestion.questionId
    ]);

    await saveQuestions(questions.value);

    createStatusAlert(
      statusAlert(
        alertStatuses.value.success,
        "Page break removed successfully."
      )
    );
  } catch (error) {
    console.error("removePageBreakAfter | catch", { error });

    await Swal.fire("There was a problem removing the page break.");

    createStatusAlert(
      statusAlert(
        alertStatuses.value.error,
        "An error occurred while removing the page break. Please try again."
      )
    );
  }
};

const handleEditedHtml = value => {
  html.value = value;
};

onMounted(() => {
  initialContent.value = html.value;
  isLoaded.value = true;
});
</script>

<style scoped>
.question-points {
  max-width: 15%;
}
</style>
