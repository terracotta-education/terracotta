<template>
  <div>
    <template v-if="experiment">
      <h1 class="mb-5">How would you like to distribute your experiment participants?</h1>

      <v-expansion-panels class="v-expansion-panels--icon" flat>

        <v-expansion-panel :class="{'v-expansion-panel--selected':experiment.distributionType==='EVEN'}">
          <v-expansion-panel-header hide-actions><img src="@/assets/even.svg" alt="even distribution"> Even
          </v-expansion-panel-header>
          <v-expansion-panel-content>
            <p>Equally distribute your students across all conditions</p>
            <v-btn @click="saveType('EVEN')" color="primary" elevation="0">Select</v-btn>
          </v-expansion-panel-content>
        </v-expansion-panel>

        <v-expansion-panel :class="{'v-expansion-panel--selected':experiment.distributionType==='CUSTOM'}" :disabled="experiment.exposureType === 'WITHIN'">
          <v-expansion-panel-header hide-actions><img src="@/assets/custom.svg" alt="custom distribution"> Custom
          </v-expansion-panel-header>
          <v-expansion-panel-content>
            <p>Customize the percentage of students who receive each condition</p>
            <v-btn @click="saveType('CUSTOM')" color="primary" elevation="0">Select</v-btn>
          </v-expansion-panel-content>
        </v-expansion-panel>

        <v-expansion-panel :class="{'v-expansion-panel--selected':experiment.distributionType==='MANUAL'}" :disabled="experiment.exposureType === 'WITHIN'">
          <v-expansion-panel-header hide-actions><img src="@/assets/manual.svg" alt="Manual distribution"> Manual
          </v-expansion-panel-header>
          <v-expansion-panel-content>
            <p>Manually select which students receive each condition</p>
            <v-btn @click="saveType('MANUAL')" color="primary" elevation="0">Select</v-btn>
          </v-expansion-panel-content>
        </v-expansion-panel>

      </v-expansion-panels>
    </template>
  </div>
</template>

<script>
import { mapActions } from 'vuex';

export default {
  name: 'ParticipationDistribution',
  props: ['experiment'],
  computed: {},
  methods: {
    ...mapActions({
      reportStep: 'api/reportStep',
      updateExperiment: 'experiment/updateExperiment',
    }),
    saveType(type) {
      const e = this.experiment
      e.distributionType = type

      const experimentId = e.experimentId
      const step = 'distribution_type'

      this.updateExperiment(e)
        .then(async response => {
          if (typeof response?.status !== 'undefined' && response?.status === 200) {
            // report the current step
            await this.reportStep({experimentId, step})
            // forward to correct path after selection
            if (this.experiment.distributionType==='EVEN') {
              this.$router.push({name:'ParticipationSummary', params:{experiment: experimentId}})
            } else if(this.experiment.distributionType==='CUSTOM') {
              this.$router.push({name:'ParticipationCustomDistribution', params:{experiment: experimentId}})
            }  else if(this.experiment.distributionType==='MANUAL') {
              this.$router.push({name:'ParticipationManualDistribution', params:{experiment: experimentId}})
            } else {
              this.$swal('Select a distribution type')
            }
          } else if (response?.message) {
            this.$swal(`Error: ${response.message}`)
          } else {
            this.$swal('There was an error saving your experiment.')
          }
        })
        .catch(response => {
          console.log('updateExperiment | catch', {response})
        })
    },
    saveExit() {
      this.$router.push({name:'Home', params:{experiment: this.experiment.experimentId}})
    }
  }
}
</script>
