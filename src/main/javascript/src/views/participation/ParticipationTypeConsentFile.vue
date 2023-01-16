<template>
  <div v-if="consent">
    <h1>Add your study's <strong>informed consent</strong> file.</h1>
    <file-drop-zone
      class="my-5"
      :existing="consent.file"
      :fileName="consentExist"
      @update="onFileChange"
    ></file-drop-zone>

    <v-btn
      :disabled="!consent.file"
      class="mt-3 mb-6"
      color="primary"
      elevation="0"
      @click="saveConsent('ParticipationDistribution')"
      >Next</v-btn
    >

    <!-- TODO - update with file url when it arrives -->
    <p>
      You can
      <a
        :href="require('@/assets/files/Terracotta_ICS_template.docx')"
        download="Terracotta_ICS_template.docx"
        >download an informed consent template here.</a
      >
    </p>
  </div>
</template>

<script>
import FileDropZone from "@/components/FileDropZone";
import { mapActions, mapGetters } from "vuex";

export default {
  name: "ParticipationTypeConsentFile",
  props: ["experiment"],
  computed: {
    ...mapGetters({
      consent: "consent/consent",
      editMode: 'navigation/editMode'
    }),
    getSaveExitPage() {
      return this.editMode?.callerPage?.name || 'Home';
    },
    consentExist() {
      return this.experiment?.consent?.title || "";
    },
  },
  methods: {
    ...mapActions({
      setConsentFile: "consent/setConsentFile",
      createConsent: "consent/createConsent",
    }),
    onFileChange(newFile) {
      this.setConsentFile(newFile);
    },
    async saveConsent(path) {
      try {
        await this.createConsent(this.experiment.experimentId);
        this.$router.push({
          name: path,
          params: { experiment: this.experiment.experimentId },
        });
      } catch (error) {
        this.$swal({
          text: `Error: ${error.message}. Please try again.`,
          icon: "error",
        });
      }
    },
    saveExit() {
      this.saveConsent(this.getSaveExitPage);
    },
  },
  components: {
    FileDropZone,
  },
};
</script>
