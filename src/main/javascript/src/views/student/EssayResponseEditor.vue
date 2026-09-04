<template>
  <ResponseRow>
    <v-textarea
      v-if="!readonly"
      v-model="response"
      :rows="10"
      :counter="true"
      aria-label="Your response"
      @update:model-value="onInput"
    >
      <template #counter>
        <div class="counter">
          {{ wordCount }} word{{ wordCount !== 1 ? "s" : "" }}
        </div>
      </template>
    </v-textarea>

    <v-textarea
      v-else
      v-model="studentResponse"
      :rows="10"
      :counter="true"
      aria-label="Your submitted response"
      readonly
    />
  </ResponseRow>
</template>

<script setup>
import {
  ref,
  computed,
  watch
} from "vue";

import Countable from "countable";

import ResponseRow from "@/views/student/ResponseRow.vue";

defineOptions({
  name: "EssayResponseEditor"
});

const props = defineProps({
  modelValue: {
    type: String,
    default: null
  },
  readonly: {
    type: Boolean,
    default: false
  },
  answer: {
    type: Object,
    default: null
  }
});

const emit = defineEmits([
  "update:modelValue"
]);

const response = ref(props.modelValue);
const wordCount = ref(0);

const studentResponse = computed(() => {
  return props.answer?.response || "";
});

const updateWordCount = () => {
  Countable.count(response.value || "", counter => {
    wordCount.value = counter.words;
  });
};

watch(
  () => props.modelValue,
  value => {
    response.value = value;

    if (response.value) {
      updateWordCount();
    } else {
      wordCount.value = 0;
    }
  },
  { immediate: true }
);

const onInput = value => {
  response.value = value;
  emit("update:modelValue", response.value);
  updateWordCount();
};
</script>

<style lang="scss" scoped>
.counter {
  font-size: 16px;
  line-height: 16px;
  font-weight: 400;
}
</style>
