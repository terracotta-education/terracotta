<template>
  <div>
    <ResponseRow
      v-for="answer in answers"
      :key="answer.answerId"
      :correct="getColor(answer)"
    >
      <div class="d-flex justify-space-between align-center w-100">
        <div class="question-input">
          <v-radio-group
            v-if="!readonly"
            v-model="response"
            @update:model-value="emitValueChanged"
            hide-details
          >
            <v-radio
              :value="answer.answerId"
              :aria-label="`select option ${answer.html}`"
              class="radioButton"
            />
          </v-radio-group>

          <v-radio-group
            v-else
            :model-value="answer.studentResponse"
            :disabled="readonly"
            hide-details
          >
            <v-radio
              :value="answer.answerId"
              :aria-label="`option ${answer.html}`"
              class="radioButton"
            />
          </v-radio-group>

          <span v-html="answer.html" />
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
  </div>
</template>

<script setup>
import {
  ref,
  watch
} from "vue";

import ResponseRow from "@/views/student/ResponseRow.vue";

defineOptions({
  name: "MultipleChoiceResponseEditor"
});

const props = defineProps({
  answers: {
    type: Array,
    default: () => []
  },
  modelValue: {
    type: [Number, String, null],
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

const emitValueChanged = value => {
  response.value = value;
  emit("update:modelValue", response.value);
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
