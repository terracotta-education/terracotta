<template>
  <dev>
    <response-row>
      <!--    <v-dialog  :value="dialog" max-width="450px">-->
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
        <v-card-actions>
          <v-btn color="primary" dark class="upload-button" align="center" :loading="isSelecting" @click="handleFileImport">
            Upload File
          </v-btn>
          <input
              ref="uploader"
              class="d-none"
              type="file"
              @change="onFileChanged"
          >
          <v-spacer></v-spacer>
        </v-card-actions>
        <v-card-text>
          <v-row class="d-flex flex-column" dense align="center" justify="center">
            <p>
              or drag and drop here
            </p>
          </v-row>
        </v-card-text>
      </v-card>
      <v-card v-if="!isIdle" width="100%"
              height="100%">
        <v-card-text>
          <v-row class="d-flex flex-column" dense align="center" justify="center">
            <p v-if="isUploading">
              Uploading...
            </p>
            <p v-if="isUploaded">
              Selected Files
            </p>
          </v-row>
          <v-row class="d-flex flex-column" dense align="center" justify="center">
            <p>
              {{this.uploadedFiles[0].name}}
            </p>
          </v-row>
        </v-card-text>
        <v-row>
          <v-col cols="8">
            <v-progress-linear v-if="!isIdle"
                               v-model="uploadBarProgress"
                               height="5"
            >
            </v-progress-linear>
          </v-col>
          <v-col cols="2">
            <v-btn  @click="deleteFile" icon>
              <v-icon id="close-button">mdi-trash-can</v-icon>
            </v-btn>
          </v-col>
        </v-row>
      </v-card>
      <!--    </v-dialog>-->
    </response-row>
    <v-row v-if="isIdle" class="d-flex flex-column" dense align="center" justify="center">
      <p>
        Uploaded files cannot be larger than 10MB
      </p>
    </v-row>
  </dev>
</template>

<script>
import ResponseRow from "./ResponseRow.vue";


export default {
  props: ["value"],
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
      uploaded:false
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
          message: "Only one file can be uploaded at a time..",
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
      this.uploadedFiles = [];
      this.uploading = true;
      this.uploadedFiles.push(e.target.files[0])
      this.loadFile(e.target.files[0])
    },

    loadFile(file) {
      this.uploadBarProgress=50;
      if (file.size > 10* 1024 * 1024) {
        alert('File too big (> 10MB)');
        return;
      }
      this.uploadBarProgress=100;
      this.uploading=false
      this.uploaded=true
      this.response=file;
      this.emitValueChanged();

    },
    deleteFile() {
      this.uploadedFiles = [];
      this.uploadBarProgress=0;
      this.uploading=false
      this.uploaded=false
      this.response=null;
      this.emitValueChanged();
    }
  },
  computed: {
    isUploading: function () {
      return (this.uploading && !this.uploaded)
    },
    isIdle: function () {
      return (!this.uploading && !this.uploaded)
    },
    isUploaded: function () {
      return (!this.uploading && this.uploaded)
    },
  },
  watch: {
    value() {
      this.response = this.value;
    },
  },
};
</script>

<style lang="scss" scoped>
.counter {
  font-size: 16px;
  line-height: 16px;
  font-weight: 400;
}

.div-1 {
  margin-left: 40%;
}

.upload-button {
  margin-left: 35%;
}

</style>
