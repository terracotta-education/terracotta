<template>
  <!-- proxy props and event listeners to question-editor -->
  <question-editor
    v-bind="$props"
    v-on="$listeners"
  >
    <p class="ma-0 mb-3">Note: Files must be smaller than 10MB</p>
  </question-editor>
</template>

<script>
  import QuestionEditor from "./QuestionEditor.vue";

  export default {
    props: {
      question: {
        type: Object,
        required: true,
      }
    },
    components: {
      QuestionEditor
    }
  };
</script>
