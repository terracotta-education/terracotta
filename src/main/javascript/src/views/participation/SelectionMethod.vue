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
			updateExperiment: 'experiment/updateExperiment',
		}),
		setParticipationType(type) {
			const _this = this
			const e = _this.experiment
			e.participationType = type

			this.updateExperiment(e)
					.then(response => {
						if (response.status === 200) {
							if (this.experiment.participationType==='CONSENT') {
								this.$router.push({name:'ExperimentParticipationConsent', params:{experiment: this.experiment.experiment_id}})
							} else if(this.experiment.participationType==='MANUAL') {
								this.$router.push({name:'ExperimentParticipationManual', params:{experiment: this.experiment.experiment_id}})
							} else if(this.experiment.participationType==='AUTO') {
								this.$router.push({name:'ExperimentParticipationAutoConfirm', params:{experiment: this.experiment.experiment_id}})
							}
						} else {
							alert("error: ", response.statusText || response.status)
						}
					})
					.catch(response => {
						console.log("updateExperiment | catch", {response})
					})
		}
	}
}
</script>