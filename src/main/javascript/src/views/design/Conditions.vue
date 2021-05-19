<template>
	<div>
		<h1>Name your conditions</h1>
		<p>These will be used to label the different experimental versions of your assignments.</p>
		<form
			@submit.prevent="saveExperiment"
			class="my-5 mb-15"
			v-if="experiment"
		>

			<v-text-field
				v-for="condition in experiment.conditions"
				v-model="condition.name"
				:key="condition.condition_id"
				:rules="requiredText"
				label="Condition name"
				placeholder="e.g. Condition A"
				autofocus
				outlined
				required
			></v-text-field>

			<v-btn
				:disabled="!experiment.conditions.length > 0"
				elevation="0"
				color="primary"
				class="mr-4"
				type="submit"
			>
				Next
			</v-btn>
		</form>


	</div>
</template>

<script>
import {mapActions} from "vuex";

export default {
	name: 'DesignConditions',
	props: ['experiment'],
	data: () => ({
		requiredText: [
			v => !!v || 'Condition is required'
		]
	}),
	methods: {
		...mapActions({
			updateExperiment: 'experiment/updateExperiment',
		}),
		saveExperiment() {
			const _this = this
			const e = _this.experiment

			this.updateExperiment(e)
					.then(response => {
						if (response.status === 200) {
							console.log({response})
							// this.$router.push({name:'ExperimentDesignConditions', params:{experiment: this.experiment.experiment_id}})
						} else {
							alert(response.error)
						}
					})
					.catch(response => {
						console.log("updateExperiment | catch", {response})
					})
		},
	}
}
</script>

<style lang="scss">
</style>