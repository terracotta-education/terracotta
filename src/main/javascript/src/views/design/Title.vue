<template>
	<div>
		<h1>Create a title for your experiment</h1>
		<form
			@submit.prevent="saveTitle"
			class="my-5"
			v-if="experiment"
		>
			<v-text-field
				v-model="experiment.title"
				:rules="titleRules"
				label="Experiment title"
				placeholder="e.g. Lorem ipsum"
				autofocus
				outlined
				required
			></v-text-field>
			<v-btn
				:disabled="!experiment.title"
				elevation="0"
				color="primary"
				class="mr-4"
				type="submit"
			>
				Next
			</v-btn>
		</form>
		<v-card
			class="mt-15 pt-5 px-5 mx-auto blue lighten-5 rounded-lg"
			outlined
		>
			<p><strong>Note:</strong> Students will be able to see this title if they click on the Terracotta tool, so do not include any details about your study that you wouldn't want participants to see.</p>
		</v-card>
	</div>
</template>

<script>
import { mapActions } from "vuex"

export default {
	name: 'DesignTitle',
	props: ['experiment'],
	data: () => ({
		titleRules: [
			v => !!v || 'Title is required'
		],
	}),
	methods: {
		...mapActions({
			updateExperiment: 'experiment/updateExperiment',
		}),
		saveTitle () {
			const _this = this
			const e = _this.experiment
			console.log({e})
			this.updateExperiment(e)
					.then(this.$router.push({name:'ExperimentDesignDescription', params:{experiment: this.experiment.experiment_id}})
					).catch(response => {
						console.log("updateExperiment | catch", {response})
					})
		},
	}
}
</script>