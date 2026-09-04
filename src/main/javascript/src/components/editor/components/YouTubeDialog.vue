<template>
  <v-dialog
    v-model="dialog"
    max-width="500px"
  >
    <v-card>
      <v-card-title>
        <span class="text-title-large">
          YouTube Embed
        </span>

        <v-spacer />

        <v-btn
          icon="mdi-close"
          variant="text"
          aria-label="Close"
          @click="close"
        />
      </v-card-title>

      <v-card-text>
        <v-textarea
          v-model="embedCode"
          label="YouTube embed code"
          hint="Paste the YouTube embed code above"
          placeholder="YouTube embed code"
          class="input-embed-code"
          variant="outlined"
        />
      </v-card-text>

      <v-card-actions>
        <v-btn
          variant="text"
          @click="close"
        >
          Close
        </v-btn>

        <v-btn
          :disabled="isDisabled"
          variant="text"
          @click="add"
        >
          Add
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup>
import {
  ref,
  computed,
  watch
} from "vue";

import {
  parseIframeEmbed,
  youtubeParser
} from "../util/YouTubeUtils";

defineOptions({
  name: "YouTubeDialog"
});

const props = defineProps({
  editor: {
    type: Object,
    required: true
  },
  embedCode: {
    type: String,
    default: ""
  }
});

const emit = defineEmits([
  "submit",
  "close"
]);

const dialog = ref(true);
const embedCode = ref(props.embedCode || "");

const iframe = computed(() => {
  return parseIframeEmbed(embedCode.value);
});

const height = computed(() => {
  const parsedHeight = Number.parseInt(
    iframe.value?.height,
    10
  );

  return Number.isNaN(parsedHeight)
    ? 315
    : parsedHeight;
});

const width = computed(() => {
  const parsedWidth = Number.parseInt(
    iframe.value?.width,
    10
  );

  return Number.isNaN(parsedWidth)
    ? 560
    : parsedWidth;
});

const youtubeId = computed(() => {
  const url = iframe.value
    ? iframe.value.src
    : embedCode.value;

  return url
    ? youtubeParser(url) || null
    : null;
});

const isDisabled = computed(() => {
  return !embedCode.value;
});

watch(
  () => props.embedCode,
  value => {
    embedCode.value = value || "";
  }
);

watch(dialog, isOpen => {
  if (!isOpen) {
    emit("close");
  }
});

const add = () => {
  const src = youtubeId.value
    ? `https://youtu.be/${youtubeId.value}`
    : embedCode.value;

  emit("submit", {
    src,
    height: height.value,
    width: width.value
  });

  dialog.value = false;
};

const close = () => {
  emit("close");
  dialog.value = false;
};
</script>

<style scoped>
.input-embed-code :deep(.v-field-label) {
  left: 0 !important;
  right: auto !important;
}
</style>
