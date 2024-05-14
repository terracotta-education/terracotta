<template>
  <div>
    <template
      v-if="hasConditions"
    >
      <h1
        class="mb-5"
      >
        <span>
          You have defined <strong>{{ numConditions }} condition{{ hasConditions ? "s" : "" }}</strong>
        </span>
        <br><br>
        <span>How do you want students to be exposed to these different conditions?</span>
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
          :disabled="hasSelectedExposureType && experiment.exposureType !== panel.type"
          :class="{'panel-not-selected': exposureType !== panel.type, 'panel-selected': exposureType === panel.type}"
          @click.stop=""
        >
          <v-expansion-panel-header
            hide-actions
          >
            <img
              :src="panel.img.src"
              :alt="panel.img.alt"
            >
            <strong>{{ panel.header }}</strong>
          </v-expansion-panel-header>
          <v-expansion-panel-content>
            <p>{{ panel.body }}</p>
            <v-btn
              v-if="!hasSelectedExposureType || exposureType === panel.type"
              @click="saveType(panel.type)"
              color="primary"
              elevation="0"
            >
              Select
            </v-btn>
          </v-expansion-panel-content>
        </v-expansion-panel>
      </v-expansion-panels>
      <v-card
        v-if="!hasSelectedExposureType"
        class="pt-5 px-5 mx-auto blue lighten-5 rounded-lg card-warning"
        outlined
      >
        <p>
          Please note that you will not be able to switch between “All conditions” and “Only one condition” after you click SELECT.
        </p>
        <p>
          Additionally, once you click SELECT to leave this screen, you will be able to add, but not delete conditions,
          so please use the back button now to double-check that you have included what you need. To change your decisions beyond this point,
          you will need to create a new experiment.
        </p>
      </v-card>
      <v-card
        v-if="hasSelectedExposureType"
        class="pt-5 px-5 mx-auto blue lighten-5 rounded-lg card-warning"
        outlined
      >
        <p>
          Please note that you are not able to switch between “All conditions” and “Only one condition” as you have previously selected
          a type. To change the experiment type, please create a new experiment.
        </p>
      </v-card>
    </template>
    <template
      v-else
    >
      <v-alert
        type="error"
        prominent
      >
        <v-row
          align="center"
        >
          <v-col
            class="grow"
          >
            No conditions found
          </v-col>
        </v-row>
      </v-alert>
    </template>
  </div>
</template>

<script>
import { mapActions, mapGetters } from "vuex";

export default {
  name: "ExperimentType",
  props: ["experiment"],
  data() {
    return {
      initialExposureType: null,
      expanded: [0, 1],
      panels: [
        {
          type: "WITHIN",
          img: {
            src: require("@/assets/all_conditions.svg"),
            alt: "all conditions"
          },
          header: "All conditions",
          body: "All students are exposed to every condition, in different orders. This way you can compare how the different conditions affected each individual student. This is called a within-subject design."
        },
        {
          type: "BETWEEN",
          img: {
            src: require("@/assets/one_condition.svg"),
            alt: "only one condition"
          },
          header: "Only one condition",
          body: "Each student is only exposed to one condition, so that you can compare how the different conditions affected different students. This is called a between-subjects design."
        }
      ]
    }
  },
  computed: {
    ...mapGetters({
      editMode: "navigation/editMode"
    }),
    exposureType() {
      return this.experiment.exposureType;
    },
    conditions() {
      return this.experiment.conditions;
    },
    numConditions() {
      return this.conditions?.length || 0;
    },
    hasConditions() {
      return this.numConditions > 0;
    },
    getExposureTypes() {
      return ["WITHIN", "BETWEEN"];
    },
    getSaveExitPage() {
      return this.editMode?.callerPage?.name || "Home";
    },
    hasSelectedExposureType() {
      return this.initialExposureType && this.initialExposureType !== "NOSET";
    }
  },
  methods: {
    ...mapActions({
      reportStep: "api/reportStep",
      updateExperiment: "experiment/updateExperiment"
    }),
    async saveType(type) {
      this.initialExposureType = type;
      const e = this.experiment;
      e.exposureType = type;

      const experimentId = e.experimentId;
      const step = "exposure_type";

      await this.updateExperiment(e)
        .then(
          async response => {
            if (typeof response?.status !== "undefined" && response?.status === 200) {
              if (!this.editMode) {
                // report the current step
                await this.reportStep({experimentId, step})
              }
              if (this.getExposureTypes.includes(this.experiment.exposureType)) {
                this.$router.push({
                  name: "ExperimentDesignDefaultCondition",
                  params:{
                    experiment: experimentId
                  }
                });
              } else {
                this.$swal("Select an experiment type");
              }
            } else if (response?.message) {
              this.$swal(`Error: ${response.message}`)
            } else {
              this.$swal("There was an error saving your experiment.");
            }
          }
        )
        .catch(
          response => {
            console.error("updateExperiment | catch", {response});
            this.$swal("There was an error saving the experiment.");
          }
        )
    },
    async saveExit() {
      this.$router.push({
        name: this.getSaveExitPage,
        params: {
          experiment: this.experiment.experimentId
        }
      });
    }
  },
  async mounted() {
    this.initialExposureType = this.experiment?.exposureType;
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
