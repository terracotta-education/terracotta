<template>
  <div>
    <template
      v-if="!readonly"
    >
      <response-row>
        <v-card
          v-if="isIdle && !file"
          @drop.prevent="onDrop($event)"
          @dragover.prevent="dragover = true"
          @dragenter.prevent="dragover = true"
          @dragleave.prevent="dragover = false"
          :class="{ 'grey lighten-2': dragover }"
          elevation="0"
          width="100%"
          height="100%"
        >
          <v-card-actions
            class="d-flex flex-column btn-upload-card-action"
            align="center"
            justify="center"
            dense
          >
            <v-row
              class="d-flex flex-column"
              align="center"
              justify="center"
              dense
            >
              <v-btn
                :loading="isSelecting"
                @click="handleFileImport"
                color="primary"
                class="upload-button"
                align="center"
                dark
              >
                Upload File
              </v-btn>
            </v-row>
            <input
              @change="onFileChanged"
              ref="uploader"
              class="d-none"
              type="file"
            />
            <v-spacer></v-spacer>
          </v-card-actions>
          <v-card-text
            class="drag-drop-card-text"
          >
            <v-row
              class="d-flex flex-column"
              align="center"
              justify="center"
              dense
            >
              <p
                class="drag-drop-text"
              >
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
              dense
            >
              <h2
                v-if="isUploading"
              >
                Uploading...
              </h2>
              <h2
                v-if="!isUploading"
              >
                Selected file:
              </h2>
              <div
                v-if="isUploading"
              >
                <v-progress-linear
                  v-model="uploadBarProgress"
                  height="5"
                >
                </v-progress-linear>
                <v-tooltip
                  top
                >
                  <template
                    v-slot:activator="{on, attrs}"
                  >
                    <v-btn
                      v-bind="attrs"
                      v-on="on"
                      @click="deleteFile"
                      class="btn-uploaded-file"
                    >
                      <v-icon
                        class="btn-uploaded-file-icon"
                      >
                        mdi-close-outine
                      </v-icon>
                    </v-btn>
                  </template>
                  <span>Cancel upload</span>
                </v-tooltip>
              </div>
              <div
                v-if="!isUploading"
                class="v-btn uploaded-file-row"
                outlined
              >
                {{ file.name }}
                <v-tooltip
                  top
                >
                  <template
                    v-slot:activator="{on, attrs}"
                  >
                    <v-btn
                      v-bind="attrs"
                      v-on="on"
                      @click="deleteFile"
                      class="btn-uploaded-file"
                    >
                      <v-icon
                        class="btn-uploaded-file-icon"
                      >
                        mdi-trash-can-outline
                      </v-icon>
                    </v-btn>
                  </template>
                  <span>Delete file</span>
                </v-tooltip>
              </div>
            </v-row>
          </v-card-text>
        </v-card>
      </response-row>
      <v-row
        v-if="isIdle && !file"
        class="d-flex flex-column"
        align="center"
        justify="center"
        dense
      >
        <p>Uploaded files cannot be larger than 10MB</p>
      </v-row>
    </template>
    <template
      v-if="readonly"
    >
      <response-row>
        <v-card
          class="uploaded-file-card"
        >
          <v-card-text>
            <v-row
              class="d-flex flex-column"
              align="center"
              justify="center"
              dense
            >
              <h2>File submitted:</h2>
              <div
                v-for="fileResponse in fileResponses"
                :key="fileResponse.answerSubmissionId"
                class="v-btn uploaded-file-row"
                outlined
              >
                {{ fileResponse.fileName }}
                <v-tooltip
                  v-if="!isDownloading"
                  top
                >
                  <template
                    v-slot:activator="{on, attrs}"
                  >
                    <v-btn
                      v-bind="attrs"
                      v-on="on"
                      @click="handleFileDownload(fileResponse)"
                      class="btn-uploaded-file"
                      target="_blank"
                    >
                      <v-icon
                        class="btn-uploaded-file-icon"
                      >
                        mdi-file-download-outline
                      </v-icon>
                    </v-btn>
                  </template>
                  <span>Download file</span>
                </v-tooltip>
                <span
                  v-if="isDownloading"
                >
                  <spinner />
                </span>
              </div>
            </v-row>
          </v-card-text>
        </v-card>
      </response-row>
    </template>
  </div>
</template>

<script>
import { mapGetters, mapActions } from "vuex";
import ResponseRow from "./ResponseRow.vue";
import Spinner from "@/components/Spinner";

export default {
  props: {
    value: {
      type: Object
    },
    readonly: {
      type: Boolean,
      default: false
    },
    fileResponses: {
      type: Array
    },
    selectedSubmission: {
      type: Object
    },
    selectedDownloadId: {
      type: Number
    },
    submissionId: {
      type: Number
    },
    questionId: {
      type: Number
    }
  },
  components: {
    ResponseRow,
    Spinner
  },
  data() {
    return {
      response: this.value,
      isSelecting: false,
      selectedFile: null,
      dragover: false,
      uploading: false,
      uploadBarProgress: 10,
      uploaded: false
    };
  },
  watch: {
    value() {
      this.response = this.value;
    }
  },
  computed: {
    ...mapGetters({
      files: "submissions/files"
    }),
    isUploading: function () {
      return (this.uploading && !this.uploaded);
    },
    isIdle: function () {
      return (!this.uploading && !this.uploaded);
    },
    isUploaded: function () {
      return (!this.uploading && this.uploaded);
    },
    isDownloading() {
      return this.selectedDownloadId == this.fileResponses[0].answerSubmissionId;
    },
    file() {
      return this.files.find(file => file.questionId === this.questionId && file.submissionId === this.submissionId);
    }
  },
  methods: {
    ...mapActions({
      addFile: "submissions/addFile",
      clearFile: "submissions/clearFile"
    }),
    onInput() {
      this.emitValueChanged();
    },
    emitValueChanged() {
      this.$emit("input", this.response);
    },
    handleFileImport() {
      this.isSelecting = true;
      // After obtaining the focus when closing the FilePicker, return the button state to normal
      window.addEventListener("focus", () => {
        this.isSelecting = false
      }, {once: true});
      // Trigger click on the FileInput
      this.$refs.uploader.click();
    },
    onDrop(e) {
      this.dragover = false;
      this.uploading = true;
      this.clearFile(
        {
          questionId: this.questionId,
          submissionId: this.submissionId
        }
      );
      if (e.dataTransfer.files.length > 1) {
        this.$store.dispatch("addNotification", {
          message: "Only one file may be uploaded at a time.",
          colour: "error"
        });
      } else
        e.dataTransfer.files.forEach(element => {
            this.addFile({
              file: element,
              name: element.name,
              questionId: this.questionId,
              submissionId: this.submissionId
            });
            this.loadFile(element);
          }
        );
    },
    onFileChanged(e) {
      if (e.target.files.length) {
        this.clearFile(
          {
            questionId: this.questionId,
            submissionId: this.submissionId
          }
        );
        this.uploading = true;
        this.addFile({
          file: e.target.files[0],
          name: e.target.files[0].name,
          questionId: this.questionId,
          submissionId: this.submissionId
        });
        this.loadFile(e.target.files[0]);
      }
    },
    loadFile(file) {
      this.uploadBarProgress = 50;
      if (file.size > 10 * 1024 * 1024) {
        this.clearFile(
          {
            questionId: this.questionId,
            submissionId: this.submissionId
          }
        );
        this.uploading = false;
        this.uploaded = false;
        this.response = null;
        alert("File cannot exceed 10MB");
      } else {
        this.uploadBarProgress = 50;
        this.uploading = false;
        this.uploaded = true;
        this.response = file;
        this.emitValueChanged();
      }
    },
    deleteFile() {
      this.clearFile(
        {
          questionId: this.questionId,
          submissionId: this.submissionId
        }
      );
      this.uploadBarProgress = 0;
      this.uploading = false
      this.uploaded = false
      this.response = null;
      this.emitValueChanged();
    },
    fileMimeType(fileResponse) {
      return fileResponse.mimeType;
    },
    fileName(fileResponse) {
      return fileResponse.fileName;
    },
    handleFileDownload(fileResponse) {
     this.$emit(
        "download-file-response",
        {
          conditionId: this.selectedSubmission.conditionId,
          treatmentId: this.selectedSubmission.treatmentId,
          assessmentId: this.selectedSubmission.assessmentId,
          submissionId: this.selectedSubmission.submissionId,
          questionSubmissionId: fileResponse.questionSubmissionId,
          answerSubmissionId: fileResponse.answerSubmissionId,
          mimeType: fileResponse.mimeType,
          fileName: fileResponse.fileName
        }
      );
    },
  }
};
</script>

<style lang="scss" scoped>
.v-tooltip__content {
  max-width: 400px;
  opacity: 1.0 !important;
  background-color: rgba(55,61,63, 1.0) !important;
  a {
    color: #afdcff;
  }
}
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
  color: rgba(0,0,0,.54) !important;
}
.btn-upload-card-action {
  padding-top: 16px;
}
.drag-drop-card-text {
  padding-bottom: 0;
  line-height: .5rem;
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
