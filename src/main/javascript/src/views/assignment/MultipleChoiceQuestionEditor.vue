<template>
  <QuestionEditor
    v-bind="$props"
    :is-m-c="true"
    @edited="handleQuestionEdited"
  >
    <h4>
      <strong>Options</strong>
    </h4>

    <p class="ma-0 mb-3">
      Select correct option(s) below
    </p>

    <ul class="options-list pa-0 mb-6">
      <li
        v-for="(answer, answerIndex) in question.answers"
        :key="answer.answerId"
        class="mb-3"
      >
        <v-row
          align="center"
          class="flex-nowrap"
        >
          <v-col class="py-0">
            <v-btn
              :class="{ 'correct-answer': answer.correct }"
              :aria-label="`Mark option ${answer.html || answerIndex + 1} as correct`"
              @click="handleToggleCorrect(answer)"
              variant="flat"
              class="correct"
              icon
            >
              <v-icon>
                {{
                  answer.correct
                    ? "mdi-checkbox-marked-circle"
                    : "mdi-checkbox-marked-circle-outline"
                }}
              </v-icon>
            </v-btn>
          </v-col>

          <v-col class="flex-basis-auto w-100">
            <v-text-field
              :model-value="answer.html"
              :label="`Option ${answerIndex + 1}`"
              :rules="longString"
              :aria-label="`Input text for option ${answerIndex + 1}`"
              variant="outlined"
              hide-details
              required
              @update:model-value="updateAnswerHtml(answer, $event)"
            />
          </v-col>

          <v-col class="py-0">
            <v-btn
              :aria-label="`Delete option ${answer.html || answerIndex + 1}`"
              class="delete_option"
              icon
              @click="handleDeleteAnswer(question, answer)"
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
      <v-col cols="auto">
        <div class="icon-button-spacer" />
      </v-col>

      <v-col cols="auto">
        <v-btn
          elevation="0"
          class="btn-add-option px-0"
          variant="text"
          @click="handleAddAnswer(question)"
        >
          Add Option
        </v-btn>
      </v-col>
    </v-row>

    <template #actions>
      <div class="d-flex align-center">
        <v-switch
          v-model="randomizeAnswers"
          class="randomize-answers-switch"
          label="Randomize options"
        />
      </div>
    </template>
  </QuestionEditor>
</template>

<script setup>
import { computed } from "vue";
import { useRoute } from "vue-router";
import Swal from "sweetalert2";

import QuestionEditor from "./QuestionEditor.vue";

import { assessment as assessmentModule } from "@/store/assessment.module";

defineOptions({
  name: "MultipleChoiceQuestionEditor"
});

const props = defineProps({
  question: {
    type: Object,
    required: true
  }
});

const emit = defineEmits([
  "edited"
]);

const route = useRoute();
const assessmentStore = assessmentModule();

const longString = [
  value => value && !!value.trim() || "required"
];

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

const randomizeAnswers = computed({
  get() {
    return props.question.randomizeAnswers;
  },

  set(value) {
    assessmentStore.updateQuestions({
      ...props.question,
      randomizeAnswers: value
    });

    handleQuestionEdited();
  }
});

const handleAddAnswer = async question => {
  try {
    await assessmentStore.createAnswer([
      experimentId.value,
      conditionId.value,
      treatmentId.value,
      assessmentId.value,
      question.questionId,
      "",
      false,
      0
    ]);

    handleQuestionEdited();
  } catch (error) {
    console.error(error);
  }
};

const handleToggleCorrect = answer => {
  assessmentStore.updateAnswers({
    ...answer,
    correct: !answer.correct
  });

  handleQuestionEdited();
};

const handleDeleteAnswer = async (
  question,
  answer
) => {
  const response = await assessmentStore.deleteAnswer([
    experimentId.value,
    conditionId.value,
    treatmentId.value,
    assessmentId.value,
    question.questionId,
    answer.answerId
  ]);

  if (response?.status !== 200) {
    await Swal.fire(
      "there was a problem deleting the answer"
    );

    return;
  }

  handleQuestionEdited();
};

const updateAnswerHtml = (
  answer,
  value
) => {
  assessmentStore.updateAnswers({
    ...answer,
    html: value
  });

  handleQuestionEdited();
};

const handleQuestionEdited = () => {
  emit("edited");
};
</script>

<style lang="scss" scoped>
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
  width: 36px;
}

.randomize-answers-switch {
  margin-top: 0;
}

.randomize-answers-switch:deep(.v-selection-control) {
  flex-direction: row-reverse;
}

.randomize-answers-switch:deep(.v-selection-control__input) {
  margin-left: 10px;
}

.btn-add-option:deep(.v-btn__content) {
  color: map.get($blue, "base") !important;
  caret-color: map.get($blue, "base") !important;
  opacity: 1 !important;
}

.correct-answer {
  color: map.get($green, "base") !important;
}
</style>
