<template>
  <form
    class="questions-form my-5"
    @submit.prevent="emit('save-all', 'AssignmentYourAssignments')"
  >
    <v-textarea
      :model-value="html"
      label="Instructions or description (optional)"
      placeholder="e.g. Lorem ipsum"
      variant="outlined"
      @update:model-value="emit('update:html', $event)"
    />

    <div
      v-if="showQuestionHeader"
      class="d-flex align-center mb-3 justify-space-between"
    >
      <h4 class="pa-0">
        <strong>Questions</strong>
      </h4>

      <v-btn
        :disabled="!canClearAll"
        color="primary"
        elevation="0"
        class="saveButton"
        variant="text"
        @click="emit('clear-questions')"
      >
        Clear All
      </v-btn>
    </div>

    <QuestionPageList
      v-if="showQuestionList"
      :question-pages="questionPages"
      :expanded-question-panel="expandedQuestionPanel"
      :text-only="textOnly"
      @question-order-change="emit('question-order-change', $event)"
      @edited-question="emit('edited-question', $event)"
      @update-expanded-question-page-panel="emit('update-expanded-question-page-panel', $event)"
      @panel-ref="emit('panel-ref', $event)"
    />

    <template v-if="showEmptyQuestionMessage">
      <h4 class="pa-0">
        <strong>Questions</strong>
      </h4>

      <p class="add-questions-to-continue">
        Add questions to continue
      </p>
    </template>

    <ExternalIntegrationEditor
      v-if="treatmentOptionSelected && isIntegrationType"
      :assessment="assessment"
      :question="questions[0]"
      @integration-updated="emit('integration-updated', $event)"
      @url-validation-in-progress="emit('url-validation-in-progress', $event)"
    />

    <template v-if="!treatmentOptionSelected">
      <h4>Select a treatment mode</h4>
      <p>
        Use the Terracotta Builder to create assignments with multiple choice,
        short answer, or file upload questions. Use the External Integration
        option to use a Qualtrics survey or a custom web activity for this
        treatment.
      </p>
    </template>

    <TreatmentModeSelector
      :treatment-option-selected="treatmentOptionSelected"
      :is-integration-type="isIntegrationType"
      :assignments-available-to-copy="assignmentsAvailableToCopy"
      :integration-clients="integrationClients"
      :display-back-to-treatment-mode-selection="displayBackToTreatmentModeSelection"
      @add-terracotta-builder="emit('add-terracotta-builder')"
      @add-integration="emit('add-integration', $event)"
      @add-question="emit('add-question', $event)"
      @duplicate="emit('duplicate', $event)"
      @back-to-treatment-mode-selection="emit('back-to-treatment-mode-selection')"
    />

    <br />
  </form>
</template>

<script setup>
import { computed } from "vue";

import ExternalIntegrationEditor from "@/views/integrations/ExternalIntegrationEditor.vue";

import QuestionPageList from "./QuestionPageList.vue";
import TreatmentModeSelector from "./TreatmentModeSelector.vue";

defineOptions({
  name: "TreatmentEditorTab"
});

const props = defineProps({
  html: {
    type: String,
    default: ""
  },
  assessment: {
    type: Object,
    required: true
  },
  questions: {
    type: Array,
    default: () => []
  },
  questionPages: {
    type: Array,
    default: () => []
  },
  treatmentOptionSelected: {
    type: Boolean,
    default: false
  },
  isIntegrationType: {
    type: Boolean,
    default: false
  },
  canClearAll: {
    type: Boolean,
    default: false
  },
  expandedQuestionPanel: {
    type: Array,
    required: true
  },
  assignmentsAvailableToCopy: {
    type: Array,
    default: () => []
  },
  integrationClients: {
    type: Array,
    default: () => []
  },
  displayBackToTreatmentModeSelection: {
    type: Boolean,
    default: false
  },
  textOnly: {
    type: Function,
    required: true
  }
});

const emit = defineEmits([
  "update:html",
  "save-all",
  "clear-questions",
  "question-order-change",
  "edited-question",
  "update-expanded-question-page-panel",
  "panel-ref",
  "integration-updated",
  "url-validation-in-progress",
  "add-terracotta-builder",
  "add-integration",
  "add-question",
  "duplicate",
  "back-to-treatment-mode-selection"
]);

const hasQuestionPages = computed(() => {
  return props.questionPages?.length > 0;
});

const showQuestionHeader = computed(() => {
  return props.treatmentOptionSelected &&
    !props.isIntegrationType &&
    hasQuestionPages.value;
});

const showQuestionList = computed(() => {
  return props.treatmentOptionSelected &&
    !props.isIntegrationType &&
    hasQuestionPages.value;
});

const showEmptyQuestionMessage = computed(() => {
  return props.treatmentOptionSelected &&
    !props.isIntegrationType &&
    !hasQuestionPages.value;
});
</script>
