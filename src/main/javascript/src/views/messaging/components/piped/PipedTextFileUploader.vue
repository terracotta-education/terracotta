<template>
  <v-card class="file-upload-card px-6 py-6">
    <v-card-title>
      <span class="font-weight-bold mx-auto">
        Upload merge tags CSV
      </span>

      <v-btn
        icon="mdi-close"
        variant="text"
        @click="close"
      />
    </v-card-title>

    <p>
      Upload a CSV file to personalize your messages using merge tags. Not sure
      how to format your file? Learn how to structure your CSV before uploading.
    </p>

    <div class="file-drop-zone">
      <template v-if="!newFile">
        <div
          :class="[
            'drop-zone',
            dragging ? 'drop-zone--over' : ''
          ]"
          @dragenter.prevent="handleDragEnter"
          @dragover.prevent
          @dragleave.prevent="dragging = false"
          @drop.prevent="onChange"
        >
          <div class="drop-zone__info">
            <v-btn
              :disabled="readOnly"
              class="mb-3"
              elevation="0"
              color="primary"
              @click="fileInput?.click()"
            >
              Select CSV
            </v-btn>

            <p>or drag and drop here</p>
          </div>

          <input
            ref="fileInput"
            type="file"
            accept=".csv,text/csv"
            :disabled="readOnly"
            @change="onChange"
          >
        </div>
      </template>

      <div
        v-else
        class="drop-zone__uploaded pa-3"
      >
        <div class="drop-zone__uploaded-info">
          <h4 class="drop-zone__title">
            Selected file:
          </h4>

          <v-card variant="outlined">
            <v-card-text class="py-1 px-2">
              <strong>{{ newFile.name }}</strong>

              <div>
                <v-btn
                  :disabled="readOnly"
                  class="icon-file-remove"
                  elevation="0"
                  icon="mdi-close"
                  variant="text"
                  @click="removeFile"
                />
              </div>
            </v-card-text>
          </v-card>

          <div class="btn-upload mx-auto">
            <v-btn
              :disabled="isUploading || readOnly"
              class="my-3"
              elevation="0"
              color="primary"
              @click="handleUpload"
            >
              Upload CSV
            </v-btn>

            <span
              v-if="isUploading"
              class="send-status mx-auto"
            >
              Uploading...
            </span>
          </div>
        </div>
      </div>
    </div>
  </v-card>
</template>

<script setup>
import {
  ref,
  computed
} from "vue";

import {
  createStatusAlert,
  statusAlert
} from "@/helpers/ui-utils";

import { message as messagingMessageModule } from "@/store/messaging/message.module";
import { alert as alertModule } from "@/store/alert.module";

defineOptions({
  name: "PipedTextFileUploader"
});

const props = defineProps({
  experimentId: {
    type: Number,
    required: true
  },
  exposureId: {
    type: String,
    required: true
  },
  containerId: {
    type: String,
    required: true
  },
  messageId: {
    type: String,
    required: true
  },
  contentId: {
    type: String,
    required: true
  },
  readOnly: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits([
  "close"
]);

const messagingMessageStore = messagingMessageModule();
const alertStore = alertModule();

const fileInput = ref(null);
const newFile = ref(null);
const dragging = ref(false);
const isUploading = ref(false);

const alertStatuses = computed(() => {
  return alertStore.statuses;
});

const close = () => {
  emit("close");
};

const isCsvFile = file => {
  return (
    file?.type === "text/csv" ||
    file?.name?.toLowerCase().endsWith(".csv")
  );
};

const handleDragEnter = () => {
  if (props.readOnly) {
    return;
  }

  dragging.value = true;
};

const onChange = event => {
  // the drop-zone <div> has no native way to block a drop while read-only (unlike
  // the file input's disabled attribute), so guard here too.
  if (props.readOnly) {
    dragging.value = false;
    return;
  }

  // event.target is the overlaid file <input> even for a drop (it's what's actually
  // under the pointer) - its .files is an empty-but-truthy FileList here, since the
  // @drop.prevent above suppresses the browser's native "set input.files from drop"
  // behavior. Check dataTransfer.files (populated for drag-and-drop) by length, not
  // truthiness, before falling back to target.files (populated for the native picker).
  const files = event.dataTransfer?.files?.length
    ? event.dataTransfer.files
    : event.target?.files;

  if (!files?.length) {
    dragging.value = false;
    return;
  }

  createFile(files[0]);

  if (event.target) {
    event.target.value = "";
  }
};

const createFile = file => {
  dragging.value = false;

  if (!isCsvFile(file)) {
    createStatusAlert(
      statusAlert(
        alertStatuses.value.error,
        "Please select a CSV file."
      )
    );

    return;
  }

  newFile.value = file;
};

const removeFile = () => {
  newFile.value = null;

  createStatusAlert(
    statusAlert(
      alertStatuses.value.success,
      "Piped text file removed"
    )
  );
};

const handleUpload = async () => {
  if (!newFile.value || isUploading.value) {
    return;
  }

  isUploading.value = true;

  try {
    const response = await messagingMessageStore.uploadPipedText([
      props.experimentId,
      props.exposureId,
      props.containerId,
      props.messageId,
      props.contentId,
      newFile.value
    ]);

    // uploadPipedText catches its own errors and resolves with null rather than
    // rejecting, so a failed upload has to be detected from the return value here.
    createStatusAlert(
      response
        ? statusAlert(alertStatuses.value.success, "Piped text file uploaded successfully")
        : statusAlert(alertStatuses.value.error, "Piped text file failed to upload. Please try again.")
    );
  } finally {
    isUploading.value = false;
  }
};
</script>

<style lang="scss">
.file-upload-card {
  min-width: 600px;
  max-width: 600px;

  // Vuetify 3's .v-card-title defaults to display: block (Vuetify 2's is flex), so
  // the title's mx-auto - relying on flex auto-margins to center itself and push the
  // close button to the far edge - had no effect without this.
  & > .v-card-title {
    display: flex;
    align-items: center;
  }

  & h2 {
    max-width: fit-content;
  }

  & .send-status {
    min-width: fit-content;
    margin-top: 8px;
    color: map.get($grey, "darker");
    font-size: 0.9em;
  }

  & .btn-upload {
    max-width: fit-content;

    & .v-btn__content {
      color: white;
    }
  }
}

.drop-zone {
  height: 153px;
  position: relative;
  border: 2px dashed map.get($grey, "lighter");
  border-radius: 9px;

  &:hover {
    border: 2px dashed map.get($blue, "lighten-2");
  }

  &--over {
    background: map.get($grey, "lighter");
    border: 2px solid map.get($blue, "lighten-2");
    opacity: 0.8;
  }

  &__info {
    position: absolute;
    top: 50%;
    width: 100%;
    transform: translate(0, -50%);
    text-align: center;

    p {
      margin: 0;
      padding: 0;
    }
  }

  &__uploaded {
    position: relative;
    display: flex;
    flex-direction: column;
    justify-content: center;
    height: 153px;
    border: 2px dashed map.get($grey, "lighter");
    border-radius: 9px;

    * {
      color: black;
    }

    .v-card-text {
      display: flex;
      flex-direction: row;
      align-items: center;
      justify-content: space-between;
    }

    .icon-file-remove,
    .icon-file-view {
      height: 30px;
      width: 30px;
      border: 1px solid map.get($grey, "lighter");
      border-radius: 4px;

      .v-icon {
        font-size: 16px;
      }
    }

    .icon-file-remove {
      &:hover,
      &:focus {
        background: map.get($red, "base");
        color: white;
      }
    }

    .icon-file-view {
      &:hover,
      &:focus {
        background: map.get($grey, "lighter");
        color: white;
      }
    }
  }

  input {
    position: absolute;
    cursor: pointer;
    top: 0;
    right: 0;
    bottom: 0;
    left: 0;
    width: 100%;
    height: 100%;
    opacity: 0;
  }
}
</style>
