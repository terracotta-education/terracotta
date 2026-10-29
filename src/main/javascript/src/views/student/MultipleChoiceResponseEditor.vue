<template>
  <!-- one group for all the answers, named by the question: a screen reader then reads them as one
       question's options ("2 of 4"), and the arrow keys move the selection between them -->
  <v-radio-group
    :model-value="readonly ? studentSelection : response"
    :disabled="readonly"
    :aria-labelledby="labelledby"
    hide-details
    @update:model-value="emitValueChanged"
  >
    <ResponseRow
      v-for="answer in answers"
      :key="answer.answerId"
      :correct="getColor(answer)"
    >
      <!-- clicking anywhere on an answer's row (its text included) selects it -->
      <div
        :class="{ 'selectable-row': !readonly }"
        class="d-flex justify-space-between align-center w-100"
        @click="selectAnswer(answer)"
      >
        <div class="question-input">
          <v-radio
            :value="answer.answerId"
            :aria-label="plainText(answer.html)"
            class="radioButton"
          />

          <span v-html="sanitizeHtml(answer.html)" />
        </div>

        <template v-if="readonly">
          <span
            v-if="answer.studentResponse"
            :class="getColor(answer) ? 'correct-text' : 'incorrect-text'"
            class="decorator"
          >
            Student Response
          </span>

          <span
            v-if="showAnswers && answer.correct && !answer.studentResponse"
            :class="getColor(answer) ? 'correct-text' : 'incorrect-text'"
            class="decorator"
          >
            Correct Response
          </span>
        </template>
      </div>
    </ResponseRow>
  </v-radio-group>
</template>

<script setup>
import {
  computed,
  ref,
  watch
} from "vue";

import ResponseRow from "@/views/student/ResponseRow.vue";
import { sanitizeHtml } from "@/helpers";

defineOptions({
  name: "MultipleChoiceResponseEditor"
});

const props = defineProps({
  answers: {
    type: Array,
    default: () => []
  },
  modelValue: {
    type: String,
    default: null
  },
  responses: {
    type: Array,
    default: () => []
  },
  readonly: {
    type: Boolean,
    default: false
  },
  showAnswers: {
    type: Boolean,
    default: false
  },
  // id of the question's text, read out as the name of the group of answers
  labelledby: {
    type: String,
    default: null
  }
});

const emit = defineEmits([
  "update:modelValue"
]);

const response = ref(props.modelValue);

watch(
  () => props.modelValue,
  value => {
    response.value = value;
  }
);

// reviewing a submission: the answer the student chose
const studentSelection = computed(() => props.answers.find(answer => answer.studentResponse)?.answerId ?? null);

// an answer's text without its markup, for the radio's spoken name
const plainText = html => new DOMParser().parseFromString(sanitizeHtml(html), "text/html").body.textContent.trim();

const emitValueChanged = value => {
  if (props.readonly) {
    return;
  }

  response.value = value;
  emit("update:modelValue", response.value);
};

const selectAnswer = answer => {
  // a click on the radio itself reaches the row too; don't report the same answer twice
  if (props.readonly || response.value === answer.answerId) {
    return;
  }

  emitValueChanged(answer.answerId);
};

const getColor = answer => {
  if (props.readonly) {
    if (answer.correct) {
      if (
        !props.showAnswers &&
        !answer.studentResponse
      ) {
        return null;
      }

      return true;
    }

    if (
      !answer.correct &&
      answer.studentResponse
    ) {
      return false;
    }
  }

  return null;
};
</script>

<style lang="scss" scoped>
.question-input {
  display: flex;
  flex-direction: row;
  align-items: center;
  height: 68px;
}

.radioButton {
  margin-top: 2px;
}

.selectable-row {
  cursor: pointer;
}

.w-100 {
  width: 100%;
}

.decorator {
  font-weight: 500;
  font-size: 0.9rem;
}

.correct-text {
  color: map.get($green, "base") !important;
}

.incorrect-text {
  color: map.get($red, "base") !important;
}
</style>
