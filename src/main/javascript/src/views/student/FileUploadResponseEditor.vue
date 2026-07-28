<template>
  <div>
    <template v-if="!readonly">
      <ResponseRow>
        <v-card
          v-if="isIdle && !file"
          :class="{ 'bg-grey-lighten-3': dragover }"
          elevation="0"
          width="100%"
          height="100%"
          @drop.prevent="onDrop"
          @dragover.prevent="dragover = true"
          @dragenter.prevent="dragover = true"
          @dragleave.prevent="dragover = false"
        >
          <v-card-actions
            class="d-flex flex-column btn-upload-card-action"
          >
            <v-row
              class="d-flex flex-column"
              align="center"
              justify="center"
            >
              <v-btn
                :loading="isSelecting"
                color="primary"
                class="upload-button"
                @click="handleFileImport"
              >
                Upload File
              </v-btn>
            </v-row>

            <input
              ref="uploader"
              class="d-none"
              type="file"
              @change="onFileChanged"
            />

            <v-spacer />
          </v-card-actions>

          <v-card-text class="drag-drop-card-text">
            <v-row
              class="d-flex flex-column"
              align="center"
              justify="center"
            >
              <p class="drag-drop-text">
                or drag and drop here
              </p>
            </v-row>
          </v-card-text>
        </v-card>

        <v-card
          v-if="!isIdle || file"
          width="100%"
          height="100%"
        >
          <v-card-text>
            <v-row
              class="d-flex flex-column"
              align="center"
              justify="center"
            >
              <h2 v-if="isUploading">
                Uploading...
              </h2>

              <div v-if="isUploading">
                <v-progress-linear
                  v-model="uploadBarProgress"
                  height="5"
                />

                <ToolTip
                  content="Cancel upload"
                  activator-type="button"
                  activator-class="btn-uploaded-file"
                  activator-icon-class="btn-uploaded-file-icon"
                  icon="mdi-close-outline"
                  alignment="top"
                  @clicked="deleteFile"
                />
              </div>

              <div
                v-if="!isUploading"
                class="v-btn uploaded-file-row"
              >
                {{ file?.name }}

                <ToolTip
                  content="Delete file"
                  activator-type="button"
                  activator-class="btn-uploaded-file"
                  activator-icon-class="btn-uploaded-file-icon"
                  icon="mdi-trash-can-outline"
                  alignment="top"
                  @clicked="deleteFile"
                />
              </div>
            </v-row>
          </v-card-text>
        </v-card>
      </ResponseRow>

      <v-row
        v-if="isIdle && !file"
        class="d-flex flex-column"
        align="center"
        justify="center"
      >
        <p>
          Uploaded files cannot be larger than 10MB
        </p>
      </v-row>
    </template>

    <template v-else>
      <ResponseRow>
        <v-card class="uploaded-file-card">
          <v-card-text>
            <v-row
              class="d-flex flex-column"
              align="center"
              justify="center"
            >
              <h2>
                File submitted:
              </h2>

              <div
                v-for="fileResponse in fileResponses"
                :key="fileResponse.answerSubmissionId"
                class="v-btn uploaded-file-row"
              >
                {{ fileResponse.fileName }}

                <ToolTip
                  v-if="!isDownloading"
                  content="Download file"
                  activator-type="button"
                  activator-class="btn-uploaded-file"
                  activator-icon-class="btn-uploaded-file-icon"
                  icon="mdi-file-download-outline"
                  alignment="top"
                  @clicked="handleFileDownload(fileResponse)"
                />

                <span v-else>
                  <Spinner />
                </span>
              </div>
            </v-row>
          </v-card-text>
        </v-card>
      </ResponseRow>
    </template>
  </div>
</template>

<script setup>
import {
  ref,
  computed,
  watch
} from "vue";

import ResponseRow from "@/views/student/ResponseRow.vue";
import Spinner from "@/components/Spinner.vue";
import ToolTip from "@/components/ToolTip.vue";

import { submission as submissionModule } from "@/store/submission.module";

defineOptions({
  name: "FileResponseEditor"
});

const props = defineProps({
  modelValue: {
    type: Object,
    default: null
  },
  readonly: {
    type: Boolean,
    default: false
  },
  fileResponses: {
    type: Array,
    default: () => []
  },
  selectedSubmission: {
    type: Object,
    default: null
  },
  selectedDownloadId: {
    type: Number,
    default: null
  },
  submissionId: {
    type: Number,
    required: true
  },
  questionId: {
    type: Number,
    required: true
  }
});

const emit = defineEmits([
  "update:modelValue",
  "download-file-response"
]);

const submissionStore = submissionModule();

const response = ref(props.modelValue);
const isSelecting = ref(false);
const dragover = ref(false);
const uploading = ref(false);
const uploadBarProgress = ref(10);
const uploaded = ref(false);
const uploader = ref(null);

watch(
  () => props.modelValue,
  value => {
    response.value = value;
  }
);

const files = computed(() => {
  return submissionStore.files;
});

const isUploading = computed(() => {
  return uploading.value && !uploaded.value;
});

const isIdle = computed(() => {
  return !uploading.value && !uploaded.value;
});

const isDownloading = computed(() => {
  return (
    props.fileResponses?.[0] &&
    props.selectedDownloadId ===
      props.fileResponses[0].answerSubmissionId
  );
});

const file = computed(() => {
  return files.value.find(
    file =>
      file.questionId === props.questionId &&
      file.submissionId === props.submissionId
  );
});

const emitValueChanged = () => {
  emit(
    "update:modelValue",
    response.value
  );
};

const handleFileImport = () => {
  isSelecting.value = true;

  window.addEventListener(
    "focus",
    () => {
      isSelecting.value = false;
    },
    { once: true }
  );

  uploader.value?.click();
};

const onDrop = event => {
  dragover.value = false;
  uploading.value = true;

  submissionStore.clearFile({
    questionId: props.questionId,
    submissionId: props.submissionId
  });

  if (event.dataTransfer.files.length > 1) {
    // preserve existing behavior
    alert(
      "Only one file may be uploaded at a time."
    );
    return;
  }

  Array.from(
    event.dataTransfer.files
  ).forEach(file => {
    submissionStore.addFile({
      file,
      name: file.name,
      questionId: props.questionId,
      submissionId: props.submissionId
    });

    loadFile(file);
  });
};

const onFileChanged = event => {
  const selectedFile =
    event.target.files?.[0];

  if (!selectedFile) {
    return;
  }

  submissionStore.clearFile({
    questionId: props.questionId,
    submissionId: props.submissionId
  });

  uploading.value = true;

  submissionStore.addFile({
    file: selectedFile,
    name: selectedFile.name,
    questionId: props.questionId,
    submissionId: props.submissionId
  });

  loadFile(selectedFile);
};

const loadFile = file => {
  uploadBarProgress.value = 50;

  if (file.size > 10 * 1024 * 1024) {
    submissionStore.clearFile({
      questionId: props.questionId,
      submissionId: props.submissionId
    });

    uploading.value = false;
    uploaded.value = false;
    response.value = null;

    alert("File cannot exceed 10MB");
    return;
  }

  uploadBarProgress.value = 50;
  uploading.value = false;
  uploaded.value = true;
  response.value = file;

  emitValueChanged();
};

const deleteFile = () => {
  submissionStore.clearFile({
    questionId: props.questionId,
    submissionId: props.submissionId
  });

  uploadBarProgress.value = 0;
  uploading.value = false;
  uploaded.value = false;
  response.value = null;

  emitValueChanged();
};

const handleFileDownload = fileResponse => {
  emit(
    "download-file-response",
    {
      conditionId:
        props.selectedSubmission.conditionId,
      treatmentId:
        props.selectedSubmission.treatmentId,
      assessmentId:
        props.selectedSubmission.assessmentId,
      submissionId:
        props.selectedSubmission.submissionId,
      questionSubmissionId:
        fileResponse.questionSubmissionId,
      answerSubmissionId:
        fileResponse.answerSubmissionId,
      mimeType:
        fileResponse.mimeType,
      fileName:
        fileResponse.fileName
    }
  );
};
</script>

<style lang="scss" scoped>
iframe {
  margin: 0 auto;
  min-height: 600px;
  min-width: 600px;
  border: none;
}

.uploaded-file-row {
  min-width: 200px !important;
  min-height: 42px !important;
  padding: 0 4px 0 16px !important;
  cursor: inherit;
  background-color: transparent !important;
  border-radius: 4px;
  border: 1px solid lightgrey;
  justify-content: space-between;
}

.btn-uploaded-file {
  padding: 0 !important;
  margin-left: 20px;
  min-width: fit-content !important;
  max-height: 28px;
  border-color: lightgrey;
  background-color: transparent !important;
}

.btn-uploaded-file-icon {
  color: rgba(0, 0, 0, 0.54) !important;
}

.btn-upload-card-action {
  padding-top: 16px;
}

.drag-drop-card-text {
  padding-bottom: 0;
  line-height: 0.5rem;
}

p.drag-drop-text {
  margin-bottom: 0;
}

.uploaded-file-card {
  width: 100%;
  height: 100%;
  box-shadow: none !important;
}
</style>
