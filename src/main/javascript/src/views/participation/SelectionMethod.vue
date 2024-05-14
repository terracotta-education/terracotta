<template>
  <div>
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

export default {
  name: 'ParticipationSelectionMethod',
  props: ['experiment'],
  data() {
    return {
      loading: false,
      expanded: [0, 1, 2],
      initialParticipationType: null,
      panels: [
        {
          type: "CONSENT",
          img: {
            src: require("@/assets/consent_invite.svg"),
            alt: "invite students"
          },
          header: "Students will be invited to consent",
          body: "Select this option if you would like to create a consent assignment within Canvas"
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
      ]
    }
  },
  computed: {
    ...mapGetters({
      editMode: 'navigation/editMode'
    }),
    getSaveExitPage() {
      return this.editMode?.callerPage?.name || 'Home';
    },
    participationType() {
      return this.experiment.participationType;
    },
    hasParticipantTypeSelected() {
      return this.initialParticipationType && this.initialParticipationType !== "NOSET";
    }
  },
  methods: {
    ...mapActions({
      reportStep: 'api/reportStep',
      updateExperiment: 'experiment/updateExperiment',
    }),
    setParticipationType(type) {
      this.initialParticipationType = type;
      const e = this.experiment
      e.participationType = type

      const experimentId = e.experimentId
      const step = "participation_type"

      this.loading = true;
      this.updateExperiment(e)
        .then(
          async response => {
            if (typeof response?.status !== "undefined" && response?.status === 200) {
              // report the current step
              await this.reportStep({experimentId, step});

              // route based on participation type selection
              switch(e.participationType) {
                case "CONSENT":
                  this.$router.push({
                    name:'ParticipationTypeConsentOverview',
                    params: {
                      experiment: experimentId
                    }
                  });
                  break;
                case "MANUAL":
                  this.$router.push({
                    name:'ParticipationTypeManual',
                    params: {
                      experiment: experimentId
                    }
                  });
                  break;
                case "AUTO":
                  this.$router.push({
                    name:'ParticipationTypeAutoConfirm',
                    params: {
                      experiment: experimentId
                    }
                  });
                  break;
                default:
                  this.$swal("Select a participation type");
                  break;
              }
            } else if (response?.message) {
              this.$swal(`Error: ${response.message}`)
            } else {
              this.$swal('There was an error saving your experiment.')
            }
          }
        )
        .catch(response => {
            console.error("updateExperiment | catch", {response})
            this.$swal('There was an error saving the experiment.')
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
  }
}
</script>

<style scoped>
.v-expansion-panel {
  margin-bottom: 30px !important;
}
.panel-selected {
  border-color: rgba(3, 169, 244, 1) !important;
}
.panel-not-selected {
  border-color: #e0e0e0 !important;
}
.v-expansion-panel-header {
  pointer-events: none;
}
</style>
