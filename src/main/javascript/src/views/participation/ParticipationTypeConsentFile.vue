<template>
  <div>
    <h1>Add your study's <strong>informed consent</strong> file.</h1>
    <file-drop-zone
      class="my-5"
      :existingFile="pdfFile"
      @update="onFileChange"
      @newUpload="onNewUpload"
      @displayFile="onDisplayFile"
    ></file-drop-zone>

    <div>
      <v-btn
        :disabled="!consentFileExists || uploading"
        class="mt-3 mb-6"
        color="primary"
        elevation="0"
        @click="saveConsent(nextPage)"
      >
        Next
      </v-btn>
      <Spinner v-if="uploading"></Spinner>
    </div>

    <p>
      You can
      <a
        :href="require('@/assets/files/Terracotta_ICS_template.docx')"
        download="Terracotta_ICS_template.docx"
      >
        download an informed consent template here.
      </a>
    </p>

    <div v-if="displayFile">
      <vue-pdf-embed
        :source="pdfFileDisplay"
      />
    </div>
  </div>
</template>

<script>
import { mapActions, mapGetters } from "vuex";
import FileDropZone from "@/components/FileDropZone";
import Spinner from "@/components/Spinner";
import VuePdfEmbed from 'vue-pdf-embed/dist/vue2-pdf-embed';

export default {
  name: "ParticipationTypeConsentFile",
  props: ["experiment"],
  components: {
    FileDropZone,
    Spinner,
    VuePdfEmbed
  },
  data: () => ({
    pdfFile: null,
    pdfFileDisplay: null,
    uploading: false,
    displayFile: false,
    newUpload: false
  }),
  watch: {
    pdfFileDisplay(pdfFile) {
      this.displayFile = pdfFile != null;
    }
  },
  computed: {
    ...mapGetters({
      consent: "consent/consent",
      editMode: 'navigation/editMode'
    }),
    saveExitPage() {
      return this.editMode?.callerPage?.name || "Home";
    },
    nextPage() {
      // if single condition; go directly to the summary page, as 'EVEN' is default
      return this.singleConditionExperiment ? "ParticipationSummary" : "ParticipationDistribution";
    },
    consentFileExists() {
      return this.pdfFile != null;
    },
    conditions() {
      return this.experiment.conditions;
    },
    singleConditionExperiment() {
      return this.conditions.length === 1;
    }
  },
  methods: {
    ...mapActions({
      createConsent: "consent/createConsent",
      getConsentFile: "consent/getConsentFile"
    }),
    onFileChange(newFile) {
      this.newUpload = true;
      this.pdfFile = newFile;
    },
    onNewUpload(isNewUpload) {
      this.newUpload = isNewUpload;
    },
    onDisplayFile(showFile) {
      if (showFile) {
        this.getPdfForDisplay();
      } else {
        this.pdfFileDisplay = null;
      }
    },
    getPdfForDisplay() {
      if (this.newUpload) {
        new Promise(
          (resolve, reject) => {
            const reader = new FileReader();
            reader.readAsDataURL(this.pdfFile);
            reader.onload = () => resolve(reader.result);
            reader.onerror = error => reject(error);
          }
        ).then((file) => {this.pdfFileDisplay = encodeURI(file)});
      } else {
        this.pdfFileDisplay = this.pdfFile;
      }
    },
    handleConsentFileDownload() {
      this.getConsentFile(this.experiment.experimentId)
        .then((file) => {
          if (!file) {
            return;
          }
          this.pdfFile = "data:application/pdf;base64," + encodeURI(file);
        });
    },
    async saveConsent(path) {
      try {
        this.uploading = true;
        if (this.newUpload) {
          await this.createConsent([this.experiment.experimentId, this.pdfFile, this.consent.title]);
        }
        this.uploading = false;
        this.$router.push({
          name: path,
          params: { experiment: this.experiment.experimentId },
        });
      } catch (error) {
        this.uploading = false;
        this.$swal({
          text: `Error: ${error.message}. Please try again.`,
          icon: "error",
        });
      }
    },
    saveExit() {
      this.saveConsent(this.saveExitPage);
    },
  },
  created() {
    this.handleConsentFileDownload();
  }
};
</script>

<style lang="scss" scoped>
div.vue-pdf-embed {
  width: 98%;
  margin: 20px auto;
  min-height: 300px;
  max-height: 600px;
  overflow-y: scroll;
  box-shadow: 0 3px 1px -2px rgba(0,0,0,.2),0 2px 2px 0 rgba(0,0,0,.14),0 1px 5px 0 rgba(0,0,0,.12);
}

$offset: 187;
$duration: 0.75s;
.spinner {
  animation: rotator $duration linear infinite;
  margin: 0 auto;
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
  0% {
    stroke-dashoffset: $offset;
  }
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