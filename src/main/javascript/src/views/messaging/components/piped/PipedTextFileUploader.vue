<template>
<v-card
  class="file-upload-card px-6 py-6"
>
  <v-card-title>
    <span
      class="font-weight-bold mx-auto"
    >
      Upload merge tags CSV
    </span>
    <v-icon
      @click="close"
      right
    >
      mdi-close
    </v-icon>
  </v-card-title>
  <p>
    Upload a CSV file to personalize your messages using merge tags. Not sure how to format your file? Learn how to structure your CSV before uploading.
  </p>
  <div
    class="file-drop-zone"
  >
    <template
      v-if="!newFile"
    >
      <div
        :class="['drop-zone', dragging ? 'drop-zone--over' : '']"
        @dragenter="dragging = true"
        @dragleave="dragging = false"
      >
        <div
          class="drop-zone__info"
          @drag="onChange"
        >
          <v-btn
            class="mb-3"
            elevation="0"
            color="primary"
          >
            Select CSV
          </v-btn>
          <p>or drag and drop here</p>
        </div>
        <input
          type="file"
          accept=".csv,text/csv"
          @change="onChange"
        >
      </div>
    </template>
    <div
      v-else
      class="drop-zone__uploaded pa-3"
    >
      <div
        class="drop-zone__uploaded-info"
      >
        <h4
          class="drop-zone__title"
        >
          Selected file:
        </h4>
        <v-card
          outlined
        >
          <v-card-text
            class="py-1 px-2"
          >
            <strong>{{ newFile.name }}</strong>
            <div>
              <v-btn
                @click="removeFile"
                class="icon-file-remove"
                elevation="0"
                icon
                tile
              >
                <v-icon>
                  mdi-close
                </v-icon>
              </v-btn>
            </div>
          </v-card-text>
        </v-card>
        <div
          class="btn-upload mx-auto"
        >
          <v-btn
            :disabled="isUploading"
            @click="handleUpload"
            class="my-3"
            elevation="0"
            color="primary"
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

<script>
import { mapGetters, mapActions } from "vuex";

export default {
  props: {
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
  },
  data: () => ({
    newFile: null,
    dragging: false,
    isUploading: false,
  }),
  computed: {
    ...mapGetters({
      pipedText: "messagingMessage/pipedText",
      message: "messagingMessage/message"
    }),
  },
  methods: {
    close() {
      this.$emit("close");
    },
    onChange(e) {
      var files = e.target.files || e.dataTransfer.files;

      if (!files.length) {
        this.dragging = false;
        return;
      }

      this.createFile(files[0]);
    },
    createFile(file) {
      this.newFile = file;
      this.dragging = false;
    },
    removeFile() {
      this.newFile = null;
    },
    ...mapActions({
      upload: "messagingMessage/uploadPipedText",
    }),
    async handleUpload() {
      await this.upload([
        this.experimentId,
        this.exposureId,
        this.containerId,
        this.messageId,
        this.contentId,
        this.newFile
      ]);

      this.isUploading = false;
      return;
    }
  }
}
</script>

<style lang="scss">
@import "~@/styles/variables";

.file-upload-card {
  min-width: 600px;
  max-width: 600px;
  & h2 {
    max-width: fit-content;
  }
  & .send-status {
    min-width: fit-content;
    margin-top: 8px;
    color: #9e9e9e;
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
  border: 2px dashed map-get($grey, "lighten-2");
  border-radius: 9px;

  &:hover {
    border: 2px dashed map-get($blue, "lighten-2");
  }

  &--over {
    background: map-get($grey, "lighten-2");
    border: 2px solid map-get($blue, "lighten-2");
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
    border: 2px dashed map-get($grey, "lighten-2");
    border-radius: 9px;

    * {
      color: black;
    }

    .v-card__text {
      display: flex;
      flex-direction: row;
      align-items: center;
      justify-content: space-between;
    }

    .icon-file-remove,
    .icon-file-view {
      height: 30px;
      width: 30px;
      border: 1px solid map-get($grey, "lighten-2");
      border-radius: 4px;

      i {
        font-size: 16px;
      }
    }

    .icon-file-remove {
      &:hover,
      &:focus {
        background: map-get($red, "base");
        color: white;
      }
    }

    .icon-file-view {
      &:hover,
      &:focus {
        background: map-get($grey, "lighten-2");
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
