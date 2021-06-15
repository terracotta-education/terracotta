<template>
	<div>
		<v-card
			class="mb-8 pt-5 px-5 mx-auto red lighten-5 rounded-lg"
			outlined
		>
			<p><strong>Are you sure you want to include all students in your experiment automatically?</strong></p>
			<p>One of the basic principles of ethical research is showing respect for research participants.  One way of showing this respect is by providing people an opportunity to make decisions for themselves about whether they want to participate in a study.</p>
			<p>Terracotta is designed to make this process easy.  If you want, we can create as short assignment where your students will provide consent to be included in this experiment.</p>
		</v-card>
		<v-btn elevation="0" color="primary" class="mb-4" @click="setType">Yes, I want to proceed</v-btn>
		<br>
		<router-link class="plain-link" :to="{name: 'ParticipationDistribution'}">No, I want to create a consent assignment instead</router-link>
	</div>
</template>

<script>
import { mapActions } from "vuex";

export default {
	name: 'ParticipationTypeAutoConfirm',
	methods: {
		...mapActions({
			updateExperiment: 'experiment/updateExperiment',
		}),
		setType() {

			// this.$router.push({name:'ExperimentParticipationSummary', params:{experiment: this.experiment.experiment_id}})
		}
	}
}
</script>

<style lang="scss" scoped>
	@import '~@/styles/variables';

	.v-card.red {
		border-color: map-get($red, 'lighten-2') !important;
	}
</style>