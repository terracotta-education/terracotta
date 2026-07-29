<template>
<div
  class="selection-method-container"
>
  <page-loading
      :display="preparingParticipants"
      message="We are tranferring students from your LMS course. Depending on the roster size, this may take a few moments."
    />
  <v-alert
    v-if="displayConsentFileMissingAlert"
    type="warning"
    elevation="0"
    outlined
    text
  >
    Please complete the participation section in order to continue setting up your experiment.
  </v-alert>
  <h1
    class="mb-3"
  >
    How will study participation be determined?
  </h1>
  <v-expansion-panels
    :value="expanded"
    class="v-expansion-panels--icon"
    multiple
    flat
  >
    <v-expansion-panel
      v-for="(panel, i) in panels"
      :key="i"
      :class="{'panel-not-selected': panel.type !== initialParticipationType, 'panel-selected': panel.type === initialParticipationType}"
      :disabled="hasParticipantTypeSelected && panel.type !== initialParticipationType"
      class="participation-expansion-panel"
    >
      <v-expansion-panel-header
        hide-actions
      >
        <img
          :src="panel.img.src"
          :alt="panel.img.alt"
        />
        <strong>{{ panel.header }}</strong>
      </v-expansion-panel-header>
      <v-expansion-panel-content>
        <p>{{ panel.body }}</p>
        <v-btn
          :loading="loading"
          :disabled="loading"
          @click="setParticipationType(panel.type)"
          color="primary"
          elevation="0"
        >
          Select
        </v-btn>
      </v-expansion-panel-content>
    </v-expansion-panel>
  </v-expansion-panels>
</div>
</template>

<script>
import { mapActions, mapGetters } from "vuex";
import { deleteAttributesFromElement } from "@/helpers/ui-utils.js";
import PageLoading from "@/components/PageLoading.vue"

const POLL_INTERVAL_MS = 5000;

export default {
  name: "ParticipationSelectionMethod",
  components: {
    PageLoading
  },
  props: {
    experiment: {
      type: Object,
      required: true
    }
  },
  data: () => ({
    loading: false,
    expanded: [0, 1, 2],
    initialParticipationType: null,
    preparingParticipants: false,
    statusPollTimer: null
  }),
  computed: {
    ...mapGetters({
      editMode: "navigation/editMode",
      configurations: "configuration/get"
    }),
    panels() {
      return [
        {
          type: "CONSENT",
          img: {
            src: require("@/assets/consent_invite.svg"),
            alt: "invite students"
          },
          header: "Students will be invited to consent",
          body: `Select this option if you would like to create a consent assignment within ${this.lmsTitle}`
        },
        {
          type: "MANUAL",
          img: {
            src: require("@/assets/consent_manual.svg"),
            alt: "manually decide students"
          },
          header: "Teacher will manually decide",
          body: "Select this option if you are working with minors or will be collecting parental consent"
        },
        {
          type: "AUTO",
          img: {
            src: require("@/assets/consent_automatic.svg"),
            alt: "automatically include all students"
          },
          header: "Automatically include all students",
          body: "Select this option if informed consent is not needed to run the study"
        }
      ];
    },
    getSaveExitPage() {
      return this.editMode?.callerPage?.name || "Home";
    },
    participationType() {
      return this.experiment.participationType;
    },
    hasParticipantTypeSelected() {
      return this.initialParticipationType && this.initialParticipationType !== "NOSET";
    },
    lmsTitle() {
      return this.configurations?.lmsTitle || "LMS";
    },
    pollMaxDurationMs() {
      return (this.configurations?.participantStatusPollMaxHours || 2) * 60 * 60 * 1000;
    },
    isConsentType() {
      return this.participationType === "CONSENT";
    },
    displayConsentFileMissingAlert() {
      return this.editMode && this.isConsentType && !this.experiment?.consent;
    }
  },
  methods: {
    ...mapActions({
      reportStep: "api/reportStep",
      getStepStatus: "api/getStepStatus",
      updateExperiment: "experiment/updateExperiment",
    }),
    stopPolling() {
      if (this.statusPollTimer) {
        clearInterval(this.statusPollTimer);
        this.statusPollTimer = null;
      }
    },
    isTerminalPrepareParticipationStatus(status) {
      return status === "COMPLETED" || status === "PROCESSED" || status === "FAILED";
    },
    navigateAfterParticipationTypeSelected(selectedParticipationType, experimentId) {
      switch (selectedParticipationType) {
        case "CONSENT":
          this.$router.push({
            name:"ParticipationTypeConsentOverview",
            params: {
              experiment: experimentId
            }
          });
          break;
        case "MANUAL":
          this.$router.push({
            name:"ParticipationTypeManual",
            params: {
              experiment: experimentId
            }
          });
          break;
        case "AUTO":
          this.$router.push({
            name:"ParticipationTypeAutoConfirm",
            params: {
              experiment: experimentId
            }
          });
          break;
        default:
          this.$swal("Select a participation type");
          break;
      }
    },
    async handleTerminalPrepareParticipationStatus(status, batchId, selectedParticipationType, experimentId) {
      if (status === "FAILED") {
        this.$swal(
          `An error occurred processing the enrollment. Error ID: ${batchId}`
        );

        return;
      }

      this.navigateAfterParticipationTypeSelected(selectedParticipationType, experimentId);
    },
    // refreshParticipants (kicked off server-side by reportStep) can take several minutes for a
    // large course roster, so instead of blocking on that one request, poll its status every 5
    // seconds until it reaches a terminal state - giving up after pollMaxDurationMs (configurable
    // via app.participant.status.poll.max.hours, default 2 hours) rather than polling forever if
    // it never does.
    pollPrepareParticipationStatus(experimentId, batchId, selectedParticipationType) {
      const pollStartedAt = Date.now();

      this.statusPollTimer = setInterval(
        async () => {
          if (Date.now() - pollStartedAt >= this.pollMaxDurationMs) {
            this.stopPolling();
            this.preparingParticipants = false;

            this.$swal(
              "Preparing participants is taking longer than expected. Please try again later."
            );

            return;
          }

          const statusResponse = await this.getStepStatus({experimentId, batchId});
          const status = statusResponse?.data?.status;

          if (!this.isTerminalPrepareParticipationStatus(status)) {
            // still IN_PROGRESS/PENDING, or the poll request itself failed - keep polling either way
            return;
          }

          this.stopPolling();
          this.preparingParticipants = false;

          await this.handleTerminalPrepareParticipationStatus(
            status,
            batchId,
            selectedParticipationType,
            experimentId
          );
        },
        POLL_INTERVAL_MS
      );
    },
    setParticipationType(type) {
      this.initialParticipationType = type;
      const e = this.experiment;
      e.participationType = type;

      const experimentId = e.experimentId;
      const step = "participation_type";

      this.loading = true;
      this.updateExperiment(e)
        .then(
          async response => {
            if (typeof response?.status !== "undefined" && response?.status === 200) {
              this.preparingParticipants = true;
              // report the current step
              const stepResponse = await this.reportStep({experimentId, step});

              if (typeof stepResponse?.status === "undefined" || stepResponse?.status !== 200) {
                this.preparingParticipants = false;

                this.$swal(
                  stepResponse?.message
                    ? `Error: ${stepResponse.message}`
                    : "There was an error preparing participants for this experiment."
                );

                return;
              }

              const batchId = stepResponse?.data?.batchId;
              const initialStatus = stepResponse?.data?.status;

              if (!batchId || !initialStatus) {
                this.preparingParticipants = false;

                this.$swal(
                  "There was an error preparing participants for this experiment."
                );

                return;
              }

              if (this.isTerminalPrepareParticipationStatus(initialStatus)) {
                // the roster wasn't due for a sync, so the backend already finished synchronously -
                // no need to poll for something that's already done
                this.preparingParticipants = false;

                await this.handleTerminalPrepareParticipationStatus(
                  initialStatus,
                  batchId,
                  e.participationType,
                  experimentId
                );

                return;
              }

              this.pollPrepareParticipationStatus(experimentId, batchId, e.participationType);
            } else if (response?.message) {
              this.$swal(`Error: ${response.message}`)
            } else {
              this.$swal("There was an error saving your experiment.")
            }
          }
        )
        .catch(response => {
            console.error("updateExperiment | catch", {response})
            this.$swal("There was an error saving the experiment.")
        })
        .finally(
          () => {
            this.loading = false;
          }
        )
    },
    saveExit() {
      this.$router.push({
        name: this.getSaveExitPage,
        params: {
          experiment: this.experiment.experimentId
        }
      })
    }
  },
  async mounted() {
    this.initialParticipationType = this.participationType;
    deleteAttributesFromElement(".v-expansion-panel", ["aria-expanded"]);
  },
  beforeDestroy() {
    this.stopPolling();
  }
}
</script>

<style lang="scss" scoped>
@import "~@/styles/variables";

.v-expansion-panel {
  margin-bottom: 30px !important;
}
.panel-selected {
  border-color: rgba(3, 169, 244, 1) !important;
}
.panel-not-selected {
  border-color: map-get($grey, "lighter") !important;
}
.participation-expansion-panel:focus-within {
  border-color: rgba(0, 0, 0, .87) !important;
}
.v-expansion-panel-header {
  pointer-events: none;
}
.selection-method-container .panel-not-selected::v-deep {
  color: rgba(0, 0, 0, .80) !important;
}
</style>
