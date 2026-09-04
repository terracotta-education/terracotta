<template>
  <div class="file-drop-zone">
    <template v-if="!file">
      <div
        :class="[
          'drop-zone',
          dragging ? 'drop-zone--over' : ''
        ]"
        @dragenter.prevent="dragging = true"
        @dragover.prevent="dragging = true"
        @dragleave.prevent="dragging = false"
        @drop.prevent="onChange"
      >
        <div class="drop-zone__info">
          <v-btn
            aria-label="Upload consent file"
            class="mb-3"
            elevation="0"
            color="primary"
            @click="fileInput?.click()"
          >
            Upload PDF
          </v-btn>

          <p>or drag and drop here</p>
        </div>

        <input
          ref="fileInput"
          type="file"
          accept=".pdf,application/pdf"
          aria-label="Upload consent file"
          @change="onChange"
        />
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
            <strong>Informed Consent File</strong>

            <div>
              <v-btn
                aria-label="View uploaded consent file"
                class="icon-file-view"
                elevation="0"
                icon="mdi-file-eye-outline"
                @click="doDisplayFile"
              />

              <v-btn
                aria-label="Remove uploaded consent file"
                class="icon-file-remove"
                elevation="0"
                icon="mdi-close"
                @click="removeFile"
              />
            </div>
          </v-card-text>
        </v-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import {
  ref,
  watch
} from "vue";

import Swal from "sweetalert2";

defineOptions({
  name: "FileDropZone"
});

const props = defineProps({
  existingFile: {
    type: [File, String, Object],
    default: null
  }
});

const emit = defineEmits([
  "update",
  "newUpload",
  "displayFile"
]);

const fileInput = ref(null);
const file = ref(props.existingFile);
const dragging = ref(false);

watch(
  () => props.existingFile,
  newFile => {
    file.value = newFile;
  }
);

const onChange = event => {
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

const createFile = async newFile => {
  if (!newFile.type.match("application/pdf")) {
    await Swal.fire("Please select a pdf file.");
    dragging.value = false;
    return;
  }

  if (newFile.size > 10 * 1024 * 1024) {
    await Swal.fire("Please check file size is not over 10 MB.");
    dragging.value = false;
    return;
  }

  file.value = newFile;
  dragging.value = false;

  emit("update", newFile);
  emit("newUpload", true);
};

const removeFile = () => {
  file.value = null;

  emit("update", null);
  emit("newUpload", true);
  emit("displayFile", false);
};

const doDisplayFile = () => {
  emit("displayFile", true);
};
</script>

<style lang="scss">
.drop-zone {
  height: 153px;
  position: relative;
  border: 2px dashed map.get($grey, "lighter");
  border-radius: 9px;

  &:hover {
    border: 2px dashed map.get($blue, "light");
  }

  &--over {
    background: map.get($grey, "lighter");
    border: 2px solid map.get($blue, "light");
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
    inset: 0;
    width: 100%;
    height: 100%;
    opacity: 0;
  }
}
</style>
