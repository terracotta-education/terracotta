<template>
  <form
    class="questions-form my-5"
    @submit.prevent="emit('save-all', 'AssignmentYourAssignments')"
  >
    <div class="treatment-instructions">
      <!-- not a <label>: the editor isn't a form control a label can point at, so it names the
           editor through aria-labelledby instead -->
      <div
        id="treatment-instructions-label"
        class="treatment-instructions__label"
      >
        Instructions or description (optional)
      </div>
      <TipTapEditor
        :key="editorKey"
        :content="editorContent"
        aria-labelledby="treatment-instructions-label"
        editor-type="html"
        @edited="handleEditedHtml"
      />
    </div>

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
import { computed, ref, watch } from "vue";

import TipTapEditor from "@/components/editor/TipTapEditor";
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

// the editor rebuilds itself whenever its content prop changes, so it gets the HTML it starts with,
// not every edit echoed back (that would reset the cursor on each keystroke). An html change that
// didn't come from the editor (copying from another treatment, clearing it) mounts a fresh editor
// with it - by key, since the new value can equal the one the editor started with.
const editorContent = ref(props.html);
const editorKey = ref(0);
let lastEditedHtml = props.html;

watch(
  () => props.html,
  value => {
    if (value !== lastEditedHtml) {
      lastEditedHtml = value;
      editorContent.value = value;
      editorKey.value++;
    }
  }
);

const handleEditedHtml = value => {
  lastEditedHtml = value;

  if (value !== props.html) {
    emit("update:html", value);
  }
};

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

<style lang="scss" scoped>
.treatment-instructions__label {
  display: block;
  margin-bottom: 8px;
  font-size: 16px;
  line-height: 24px;
  color: rgba(0, 0, 0, 0.87);
}
</style>
