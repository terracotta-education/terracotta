<template>
  <div>
    <template v-if="!readonly">
      <response-row>
        <v-card v-if="isIdle"
                @drop.prevent="onDrop($event)"
                @dragover.prevent="dragover = true"
                @dragenter.prevent="dragover = true"
                @dragleave.prevent="dragover = false"
                :class="{ 'grey lighten-2': dragover }"
                elevation="0"
                width="100%"
                height="100%"
        >
          <v-card-actions class="d-flex flex-column btn-upload-card-action" dense align="center" justify="center">
            <v-row class="d-flex flex-column" dense align="center" justify="center">
              <v-btn color="primary" dark class="upload-button" align="center" :loading="isSelecting" @click="handleFileImport">
                Upload File
              </v-btn>
            </v-row>
            <input
                ref="uploader"
                class="d-none"
                type="file"
                @change="onFileChanged"
            >
            <v-spacer></v-spacer>
          </v-card-actions>
          <v-card-text class="drag-drop-card-text">
            <v-row class="d-flex flex-column" dense align="center" justify="center">
              <p class="drag-drop-text">or drag and drop here</p>
            </v-row>
          </v-card-text>
        </v-card>
        <v-card v-if="!isIdle" width="100%" height="100%">
          <v-card-text>
            <v-row class="d-flex flex-column" dense align="center" justify="center">
              <h2 v-if="isUploading">Uploading...</h2>
              <h2 v-if="!isUploading">Selected file:</h2>
              <div v-if="isUploading">
                <v-progress-linear
                  v-model="uploadBarProgress"
                  height="5"
                >
                </v-progress-linear>
                <v-tooltip top>
                  <template v-slot:activator="{on, attrs}">
                    <v-btn
                      v-bind="attrs"
                      v-on="on"
                      @click="deleteFile"
                      class="btn-uploaded-file"
                    >
                      <v-icon class="btn-uploaded-file-icon">mdi-close-outine</v-icon>
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
                {{this.uploadedFiles[0].name}}
                <v-tooltip top>
                  <template v-slot:activator="{on, attrs}">
                    <v-btn
                      v-bind="attrs"
                      v-on="on"
                      @click="deleteFile"
                      class="btn-uploaded-file"
                    >
                      <v-icon class="btn-uploaded-file-icon">mdi-trash-can-outline</v-icon>
                    </v-btn>
                  </template>
                  <span>Delete file</span>
                </v-tooltip>
              </div>
            </v-row>
          </v-card-text>
        </v-card>
      </response-row>
      <v-row v-if="isIdle" class="d-flex flex-column" dense align="center" justify="center">
        <p>Uploaded files cannot be larger than 10MB</p>
      </v-row>
    </template>
    <template v-if="readonly">
      <response-row>
        <v-card class="uploaded-file-card">
          <v-card-text>
            <v-row class="d-flex flex-column" dense align="center" justify="center">
              <h2>File submitted:</h2>
              <div
                v-for="fileResponse in fileResponses" :key="fileResponse.answerSubmissionId"
                class="v-btn uploaded-file-row"
                outlined
              >
                {{fileResponse.fileName}}
                <v-tooltip
                  v-if="!isDownloading"
                  top
                >
                  <template v-slot:activator="{on, attrs}">
                    <v-btn
                      v-bind="attrs"
                      v-on="on"
                      @click="handleFileDownload(fileResponse)"
                      class="btn-uploaded-file"
                      target="_blank"
                    >
                      <v-icon class="btn-uploaded-file-icon">mdi-file-download-outline</v-icon>
                    </v-btn>
                  </template>
                  <span>Download file</span>
                </v-tooltip>
                <span v-if="isDownloading">
                  <svg class="spinner" width="28px" height="28px" viewBox="0 0 66 66" xmlns="http://www.w3.org/2000/svg">
                    <circle class="path" fill="none" stroke-width="6" stroke-linecap="round" cx="33" cy="33" r="30"></circle>
                  </svg>
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
import ResponseRow from "./ResponseRow.vue";

export default {
  props: [
    "value",
    "readonly",
    "fileResponses",
    "selectedSubmission",
    "selectedDownloadId"
  ],
  components: {ResponseRow},
  data() {
    return {
      response: this.value,
      isSelecting: false,
      selectedFile: null,
      dragover: false,
      uploadedFiles: [],
      uploading: false,
      uploadBarProgress: 10,
      uploaded:false,
    };
  },
  methods: {
    onInput() {
      this.emitValueChanged();
    },
    emitValueChanged() {
      this.$emit("input", this.response);
    },
    handleFileImport() {
      this.isSelecting = true;
      // After obtaining the focus when closing the FilePicker, return the button state to normal
      window.addEventListener('focus', () => {
        this.isSelecting = false
      }, {once: true});
      // Trigger click on the FileInput
      this.$refs.uploader.click();
    },
    onDrop(e) {
      this.dragover = false;
      this.uploading = true;
      if (this.uploadedFiles.length > 0) this.uploadedFiles = [];
      if (e.dataTransfer.files.length > 1) {
        this.$store.dispatch("addNotification", {
          message: "Only one file may be uploaded at a time.",
          colour: "error"
        });
      } else
        e.dataTransfer.files.forEach(element => {
            this.uploadedFiles.push(element)
            this.loadFile(element)
          }
        );
    },
    onFileChanged(e) {
      if (e.target.files.length) {
        this.uploadedFiles = [];
        this.uploading = true;
        this.uploadedFiles.push(e.target.files[0])
        this.loadFile(e.target.files[0])
      }
    },
    loadFile(file) {
      this.uploadBarProgress = 50;
      if (file.size > 10 * 1024 * 1024) {
        this.uploadedFiles = [];
        this.uploading = false
        this.uploaded = false
        this.response = null;
        alert('File cannot exceed 10MB');
      } else {
        this.uploadBarProgress = 50;
        this.uploading = false
        this.uploaded = true
        this.response = file;
        this.emitValueChanged();
      }
    },
    deleteFile() {
      this.uploadedFiles = [];
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
  },
  computed: {
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
    }
  },
  watch: {
    value() {
      this.response = this.value;
    },
  },
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
$offset: 187;
$duration: 0.75s;
.spinner {
  animation: rotator $duration linear infinite;
}
@keyframes rotator {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(270deg); }
}
.path {
  stroke-dasharray: $offset;
  stroke-dashoffset: 0;
  transform-origin: center;
  animation:
    dash $duration ease-in-out infinite,
    colors ($duration*4) ease-in-out infinite;
}
@keyframes colors {
	0% { stroke: lightgrey; }
}
@keyframes dash {
 0% { stroke-dashoffset: $offset; }
 50% {
   stroke-dashoffset: $offset/4;
   transform:rotate(135deg);
 }
 100% {
   stroke-dashoffset: $offset;
   transform:rotate(450deg);
 }
}
</style>
