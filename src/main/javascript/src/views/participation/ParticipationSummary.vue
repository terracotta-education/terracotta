<template>
  <div>
    <h1 class="my-3">
      <span class="green--text font-weight-bold">
        You've completed section 2.
      </span>
      <br />
      Here's a summary of your experiment participation.
    </h1>
    <template v-if="experiment">
      <v-expansion-panels
        v-if="this.experiment.participationType"
        flat
      >
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
        v-if="this.experiment.participationType === 'CONSENT'"
        flat
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
        v-if="this.experiment.participationType === 'CONSENT'"
        flat
      >
        <v-expansion-panel class="py-3 mb-3">
          <v-expansion-panel-header>
            <strong>Informed Consent</strong>
          </v-expansion-panel-header>
          <v-expansion-panel-content>
            <button
              v-if="!downloading && !showFile"
              class='pdfButton'
              @click="doDisplayFile"
            >
              View consent file
            </button>
            <button
              v-if="!downloading && showFile"
              class='pdfButton'
              @click="doHideFile"
            >
              Close preview
            </button>
            <Spinner v-if="downloading"></Spinner>
            <div v-if="showFile">
              <vue-pdf-embed
                :source="pdfFile"
              />
            </div>
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
import Spinner from "@/components/Spinner";
import VuePdfEmbed from 'vue-pdf-embed/dist/vue2-pdf-embed';

export default {
  name: "ParticipationSummary",
  props: ["experiment"],
  components: {
    Spinner,
    VuePdfEmbed
  },
  data: () => ({
    pdfFile: null,
    downloading: false,
    showFile: false
  }),
  watch: {
    pdfFile(pdfFile) {
      this.showFile = pdfFile != null;
    }
  },
  computed: {
    ...mapGetters({
      consent: 'consent/consent',
      editMode: 'navigation/editMode'
    }),
    saveExitPage() {
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
    async doDisplayFile() {
      if (!this.pdfFile) {
        this.downloading = true;
        await this.handleConsentFileDownload();
        this.downloading = false;
      } else {
        this.showFile = true;
      }
    },
    doHideFile() {
      this.showFile = false;
    },
    async handleConsentFileDownload() {
      await this.getConsentFile(this.experiment.experimentId)
        .then((file) => {
          if (!file) {
            return;
          }

          this.pdfFile = "data:application/pdf;base64," + encodeURI(file);
        });
    },
    nextSection() {
      this.$router.push({
        name: this.saveExitPage,
        params: {
          experiment: this.experiment.experimentId
        }
      })
    },
    saveExit() {
      this.nextSection();
    },
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
  button.pdfButton {
    background: none!important;
    border: none;
    padding: 0!important;
    color: #069;
    text-decoration: underline;
    cursor: pointer;
  }
  div.vue-pdf-embed {
    width: 98%;
    margin: 20px auto;
    min-height: 300px;
    max-height: 600px;
    overflow-y: scroll;
    box-shadow: 0 3px 1px -2px rgba(0,0,0,.2),0 2px 2px 0 rgba(0,0,0,.14),0 1px 5px 0 rgba(0,0,0,.12);
  }
}
</style>
