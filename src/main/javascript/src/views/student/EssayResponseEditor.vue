<template>
  <ResponseRow>
    <div
      v-if="!readonly"
      class="essay-response"
    >
      <TipTapEditor
        :key="editorKey"
        :content="editorContent"
        :features="ESSAY_FEATURES"
        class="essay-response__editor"
        editor-type="html"
        label="Your response"
        @edited="onEdited"
      />
      <div class="counter">
        {{ wordCount }} word{{ wordCount !== 1 ? "s" : "" }}
      </div>
    </div>

    <TipTapEditor
      v-else
      :key="studentResponse"
      :content="studentResponse"
      :features="ESSAY_FEATURES"
      class="essay-response__editor"
      editor-type="html"
      label="Your submitted response"
      read-only
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

import TipTapEditor from "@/components/editor/TipTapEditor.vue";
import { ESSAY_FEATURES } from "@/components/editor/config/essayFeatures";
import { richTextToPlainText, toRichText } from "@/components/editor/util/richText";
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

const wordCount = ref(0);

// a response saved before essays took rich text is plain text, shown as paragraphs
const studentResponse = computed(() => {
  return toRichText(props.answer?.response);
});

const updateWordCount = value => {
  Countable.count(richTextToPlainText(value), counter => {
    wordCount.value = counter.words;
  });
};

// the editor rebuilds itself whenever its content prop changes, so it gets the HTML it starts with,
// not every edit echoed back (that would reset the cursor on each keystroke). A response that
// didn't come from the editor (another attempt loaded, the form cleared) mounts a fresh editor with
// it - by key, since the new value can equal the one the editor started with.
const editorContent = ref(toRichText(props.modelValue));
const editorKey = ref(0);
let lastEditedResponse = props.modelValue;

watch(
  () => props.modelValue,
  value => {
    if (value !== lastEditedResponse) {
      lastEditedResponse = value;
      editorContent.value = toRichText(value);
      editorKey.value++;
    }

    updateWordCount(value);
  },
  { immediate: true }
);

const onEdited = value => {
  lastEditedResponse = value;
  updateWordCount(value);

  if (value !== props.modelValue) {
    emit("update:modelValue", value);
  }
};
</script>

<style lang="scss" scoped>
.essay-response {
  width: 100%;
}

.essay-response__editor {
  width: 100%;
  // the editor's own bottom margin sat between it and the word count
  margin-bottom: 0 !important;

  // body text, not the heading style of the card title it sits in, where everything looked bold
  :deep(.ProseMirror) {
    font-size: 16px;
    font-weight: 400;
    line-height: 24px;
    letter-spacing: normal;
  }
}

.counter {
  margin-top: 8px;
  text-align: right;
  font-size: 16px;
  line-height: 16px;
  font-weight: 400;
}
</style>
