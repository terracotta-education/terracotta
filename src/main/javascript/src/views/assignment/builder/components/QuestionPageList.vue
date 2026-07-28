<template>
  <div>
    <div
      v-for="(questionPage, pageIndex) in questionPages"
      :key="questionPage.key"
      class="question-page"
    >
      <v-expansion-panels
        v-model="expandedQuestionPanel[pageIndex]"
        :key="questionPage.key"
        class="v-expansion-panels--outlined"
        variant="accordion"
      >
        <Draggable
          :list="questionPage.questions"
          group="questions"
          handle=".dragger"
          item-key="questionId"
          class="questions-draggable"
          @change="emit('question-order-change', $event)"
        >
          <template #item="{ element: question, index: questionIndex }">
            <v-expansion-panel
              :key="question.questionId"
              :ref="el => setPanelRef(el, pageIndex, questionIndex)"
              :class="panelClasses(questionIndex, questionPage.questions.length)"
              @click="emit('update-expanded-question-page-panel', pageIndex)"
            >
              <v-expansion-panel-title class="text-left">
                <div class="d-flex align-center">
                  <span class="dragger me-2">
                    <v-icon>mdi-drag</v-icon>
                  </span>

                  <h2 class="py-0 my-0">
                    {{ questionPage.questionStartIndex + questionIndex + 1 }}
                    <span
                      v-if="question.html"
                      class="pl-3 question-text"
                      v-html="textOnly(question.html)"
                    />
                  </h2>
                </div>
              </v-expansion-panel-title>

              <v-expansion-panel-text>
                <component
                  :is="questionTypeComponents[question.questionType]"
                  :question="question"
                  @edited="emit('edited-question', question.questionId)"
                />
              </v-expansion-panel-text>
            </v-expansion-panel>
          </template>
        </Draggable>
      </v-expansion-panels>

      <PageBreak v-if="questionPage.pageBreakAfter" />
    </div>
  </div>
</template>

<script setup>
import { computed } from "vue";
import Draggable from "vuedraggable";

import ExternalIntegrationEditor from "@/views/integrations/ExternalIntegrationEditor.vue";
import FileUploadQuestionEditor from "@/views/assignment/FileUploadQuestionEditor.vue";
import MultipleChoiceQuestionEditor from "@/views/assignment/MultipleChoiceQuestionEditor.vue";
import PageBreak from "@/views/assignment/PageBreak.vue";
import QuestionEditor from "@/views/assignment/QuestionEditor.vue";

defineOptions({
  name: "QuestionPageList"
});

const props = defineProps({
  questionPages: {
    type: Array,
    default: () => []
  },
  expandedQuestionPanel: {
    type: Array,
    required: true
  },
  textOnly: {
    type: Function,
    required: true
  }
});

// the parent hands down its own expandedQuestionPanel array by reference and relies on this
// component updating it in place (Vuetify's v-expansion-panels accordion state); referencing it
// through this computed instead of the prop directly avoids writing straight through props
const expandedQuestionPanel = computed(() => props.expandedQuestionPanel);

const emit = defineEmits([
  "question-order-change",
  "edited-question",
  "update-expanded-question-page-panel",
  "panel-ref"
]);

const questionTypeComponents = {
  MC: MultipleChoiceQuestionEditor,
  ESSAY: QuestionEditor,
  FILE: FileUploadQuestionEditor,
  INTEGRATION: ExternalIntegrationEditor
};

const panelClasses = (questionIndex, questionCount) => {
  return [
    questionIndex === 0
      ? "rounded-lg"
      : questionIndex === questionCount - 1
        ? "rounded-lg rounded-t-0"
        : "",
    questionIndex === questionCount - 1
      ? ""
      : "rounded-b-0"
  ];
};

const setPanelRef = (element, pageIndex, questionIndex) => {
  if (!element) {
    return;
  }

  emit("panel-ref", {
    element,
    pageIndex,
    questionIndex
  });
};
</script>

<style scoped lang="scss">
.questions-draggable {
  width: 100%;
}
h2 {
  padding-bottom: 0px !important;
}
</style>
