<template>
  <div v-if="consent">
    <h1>Add your study's <strong>informed consent</strong> file.</h1>

    <file-drop-zone
      class="my-5"
      :existing="consent.file"
      @update="onFileChange"></file-drop-zone>

    <v-btn
      :disabled="!consent.file || !consent.file.name || !consent.file.name.length>0"
      class="mt-3 mb-6"
      color="primary"
      elevation="0"
      @click="saveConsent">Next</v-btn>

    <!-- TODO - update with file url when it arrives -->
    <p>You can
      <a
        :href="require('@/assets/files/Terracotta_ICS_template.docx')"
        download="Terracotta_ICS_template.docx">download an informed consent template here.</a>
    </p>
  </div>
</template>

<script>
import FileDropZone from '@/components/FileDropZone';
import {mapActions, mapGetters} from 'vuex';

export default {
  name: 'ParticipationTypeConsentFile',
  props: ['experiment'],
  computed: {
    ...mapGetters({
      consent: 'consent/consent'
    })
  },
  methods: {
    ...mapActions({
      setConsentFile: 'consent/setConsentFile',
      createConsent: 'consent/createConsent',
    }),
    onFileChange(newFile) {
      if (newFile && newFile.name) {
        this.setConsentFile(newFile)
      }
    },
    saveConsent() {
      this.createConsent(this.experiment.experimentId).then(
        this.$router.push({name: 'ParticipationDistribution', params: {experiment: this.experiment.experimentId}})
      )
    }
  },
  components: {
    FileDropZone
  }
}
</script>