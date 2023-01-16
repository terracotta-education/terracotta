<template>
  <div>
    <h1 class="my-3">
      <span class="green--text font-weight-bold"
        >You've completed section 2.</span
      ><br />
      Here's a summary of your experiment participation.
    </h1>
    <template v-if="experiment">
      <v-expansion-panels flat v-if="this.experiment.participationType">
        <v-expansion-panel class="py-3 mb-3">
          <v-expansion-panel-header>
            <strong>Selection Method</strong>
          </v-expansion-panel-header>
          <v-expansion-panel-content>
            <p>{{ participationType }}</p>
          </v-expansion-panel-content>
        </v-expansion-panel>
      </v-expansion-panels>

      <v-expansion-panels
        flat
        v-if="this.experiment.participationType === 'CONSENT'"
      >
        <v-expansion-panel class="py-3 mb-3">
          <v-expansion-panel-header>
            <strong>Assignment Title</strong>
          </v-expansion-panel-header>
          <v-expansion-panel-content>
            <p>{{ this.experiment.consent.title }}</p>
          </v-expansion-panel-content>
        </v-expansion-panel>
      </v-expansion-panels>

      <v-expansion-panels
        flat
        v-if="this.experiment.participationType === 'CONSENT'"
      >
        <v-expansion-panel class="py-3 mb-3">
          <v-expansion-panel-header
            ><strong>Informed Consent</strong></v-expansion-panel-header
          >
          <v-expansion-panel-content>
            <button
              class='pdfButton'
              @click="openPDF"
            >
              Consent File
            </button>
          </v-expansion-panel-content>
        </v-expansion-panel>
      </v-expansion-panels>
    </template>
    <v-btn
      v-if="!this.editMode"
      elevation="0"
      color="primary"
      class="mt-3"
      @click="nextSection"
    >
      Continue to assignments
    </v-btn>
  </div>
</template>

<script>
import { mapGetters, mapActions } from "vuex";

export default {
  name: "ParticipationSummary",
  props: ["experiment"],
  computed: {
    ...mapGetters({
      consent: 'consent/consent',
      editMode: 'navigation/editMode'
    }),
    getSaveExitPage() {
        return this.editMode?.callerPage?.name || 'ExperimentSummary';
    },
    participationType() {
      let type = ''

      switch (this.experiment.participationType) {
        case 'CONSENT':
          type = 'Invited students to consent'
          break
        case 'MANUAL':
          type = 'Manually determined students'
          break
        case 'AUTO':
          type = 'Automatically included all students'
          break
      }

      return type
    }
  },
  methods: {
    ...mapActions({
      getConsentFile: "consent/getConsentFile"
    }),
    openPDF() {
      this.getConsentFile(this.experiment.experimentId)

      // Second Parameter intentionally left blank
      let pdfWindow = window.open('', '', '_blank')
      pdfWindow.opener = null
      pdfWindow.document.write(
        "<iframe width='100%' height='100%' src='data:application/pdf;base64, " +
          encodeURI(this.consent.file) +
          "'></iframe>"
      )
      return false
    },
    nextSection() {
      this.$router.push({
        name: this.getSaveExitPage,
        params: { experiment: this.experiment.experimentId },
      })
    },
    saveExit() {
      this.nextSection();
    },
  },
  created() {
    if (this.experiment.consent?.filePointer) {
      this.getConsentFile(this.experiment.experimentId);
    }
  },
  beforeRouteEnter(to, from, next) {
    // Updating selection type for custom steps
    to.meta.selectionType = from.meta.selectionType
    next()
  },
};
</script>

<style lang="scss">
.v-expansion-panel {
  border: 1px solid map-get($grey, 'lighten-2');

  .pdfButton {
	background: none!important;
	border: none;
	padding: 0!important;
	color: #069;
	text-decoration: underline;
	cursor: pointer;
  }
}
</style>
