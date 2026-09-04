<template>
  <QuestionEditor v-bind="$props">
    <p class="ma-0 mb-3">
      Note: Files must be smaller than 10MB
    </p>
  </QuestionEditor>
</template>

<script setup>
import QuestionEditor from "@/views/assignment/QuestionEditor.vue";

defineOptions({
  name: "FileUploadQuestionEditor"
});

defineProps({
  question: {
    type: Object,
    required: true
  }
});
</script>
