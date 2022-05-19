<template>
	<div>
		<v-card
			class="mb-8 pt-5 px-5 mx-auto red lighten-5 rounded-lg"
			outlined
		>
			<p><strong>Are you sure you want to include all students in your experiment automatically?</strong></p>
			<p>One of the basic principles of ethical research is showing respect for research participants.  One way of showing this respect is by providing people an opportunity to make decisions for themselves about whether they want to participate in a study.</p>
			<p>Terracotta is designed to make this process easy.  If you want, we can create a short assignment where your students will provide consent to be included in this experiment.</p>
		</v-card>
		<v-btn elevation="0" color="primary" class="mb-4" :to="{name: 'ParticipationDistribution'}">Yes, I want to proceed</v-btn>
		<br>
		<v-btn @click="goToConsentPage" outlined tile class="consentBtn" color="primary" elevation="0">No, I want to create a consent assignment instead</v-btn>
		<br>
	</div>
</template>

<script>
import { mapActions } from "vuex";

export default {
	name: 'ParticipationTypeAutoConfirm',
	props: ['experiment'],
	methods: {
		...mapActions({
			updateExperiment: 'experiment/updateExperiment',
		}),
	goToConsentPage() {
		const e = this.experiment
			e.participationType = 'CONSENT'

			this.updateExperiment(e)
					.then(response => {
						if (response.status === 200) {
							this.$router.push({name:'ParticipationTypeConsentOverview', params:{experiment: this.experiment.experimentId}})
						}
					})	
	},
	saveExit() {
		this.$router.push({name:'Home', params:{experiment: this.experiment.experimentId}})
	}
	}
}
</script>

<style lang="scss" scoped>
	@import '~@/styles/variables';

	.v-card.red {
		border-color: map-get($red, 'lighten-2') !important;
	}
	.consentBtn {
		border: none;
		padding: 0 !important;
	}
</style>
