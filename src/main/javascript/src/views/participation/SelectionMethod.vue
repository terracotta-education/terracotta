<template>
	<div>
		<h1 class="mb-3">How will study participation be determined?</h1>
<!--		<v-btn elevation="0" color="primary" :to="{name: 'ExperimentParticipationSelectionMethod'}">Continue</v-btn>-->

		<v-expansion-panels class="v-expansion-panels--icon" flat>

			<v-expansion-panel :class="{'v-expansion-panel--selected':experiment.participationType==='CONSENT'}">
				<v-expansion-panel-header hide-actions><img src="@/assets/consent_invite.svg" alt="invite students"> <strong>Students will be invited to consent</strong></v-expansion-panel-header>
				<v-expansion-panel-content>
					<p>Select this option if you would like to create a consent assignment within Canvas</p>
					<v-btn @click="setParticipationType('CONSENT')" color="primary" elevation="0">Select</v-btn>
				</v-expansion-panel-content>
			</v-expansion-panel>

			<v-expansion-panel :class="{'v-expansion-panel--selected':experiment.participationType==='MANUAL'}">
				<v-expansion-panel-header hide-actions><img src="@/assets/consent_manual.svg" alt="manually decide students"> <strong>Teacher will manually decide</strong></v-expansion-panel-header>
				<v-expansion-panel-content>
					<p>Select this option if you are working with minors or will be collecting parental consent</p>
					<v-btn @click="setParticipationType('MANUAL')" color="primary" elevation="0">Select</v-btn>
				</v-expansion-panel-content>
			</v-expansion-panel>

			<v-expansion-panel :class="{'v-expansion-panel--selected':experiment.participationType==='AUTO'}">
				<v-expansion-panel-header hide-actions><img src="@/assets/consent_automatic.svg" alt="automatically include all students"> <strong>Automatically include all students</strong></v-expansion-panel-header>
				<v-expansion-panel-content>
					<p>Select this option if informed consent is not needed to run the study</p>
					<v-btn @click="setParticipationType('AUTO')" color="primary" elevation="0">Select</v-btn>
				</v-expansion-panel-content>
			</v-expansion-panel>

		</v-expansion-panels>

	</div>
</template>

<script>
import { mapActions } from "vuex";

export default {
	name: 'ParticipationSelectionMethod',
	props: ['experiment'],
	methods: {
		...mapActions({
			reportStep: 'api/reportStep',
			updateExperiment: 'experiment/updateExperiment',
		}),
		setParticipationType(type) {
			const e = this.experiment
			e.participationType = type

			const experimentId = e.experimentId
			const step = "participation_type"

			this.updateExperiment(e)
					.then(async response => {
            if (typeof response?.status !== "undefined" && response?.status === 200) {
              // report the current step
              await this.reportStep({experimentId, step})

              // route based on participation type selection
              if (e.participationType==='CONSENT') {
                this.$router.push({name:'ParticipationTypeConsentOverview', params:{experiment: experimentId}})
              } else if(e.participationType==='MANUAL') {
                this.$router.push({name:'ParticipationTypeManual', params:{experiment: experimentId}})
              } else if(e.participationType==='AUTO') {
                this.$router.push({name:'ParticipationTypeAutoConfirm', params:{experiment:experimentId}})
              } else {
                this.$swal("Select a participation type")
              }
            } else if (response?.message) {
              this.$swal(`Error: ${response.message}`)
            } else {
              this.$swal('There was an error saving your experiment.')
            }
					})
					.catch(response => {
            console.error("updateExperiment | catch", {response})
            this.$swal('There was an error saving the experiment.')
					})
		},
		saveExit() {
			this.$router.push({name:'Home', params:{experiment: this.experiment.experimentId}})
		}
	}
}
</script>
