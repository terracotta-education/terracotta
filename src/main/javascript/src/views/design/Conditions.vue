<template>
	<div>
		<h1>Name your conditions</h1>
		<p>These will be used to label the different experimental versions of your assignments.</p>
		<form
			@submit.prevent="saveConditions"
			class="my-5 mb-15"
			v-if="experiment"
		>

			<v-container class="pa-0">
				<v-row
					v-for="(condition, i) in experiment.conditions"
					:key="condition.conditionId"
				>
					<template v-if="i < 2">
						<v-col class="py-0">
							<v-text-field
								v-model="condition.name"
								:rules="requiredText"
								label="Condition name"
								placeholder="e.g. Condition A"
								outlined
								required
							></v-text-field>
						</v-col>
					</template>
					<template v-else>
						<v-col class="py-0">
							<v-text-field
								v-model="condition.name"
								:rules="requiredText"
								label="Condition name"
								placeholder="e.g. Condition A"
								outlined
								required
							></v-text-field>
						</v-col>
						<v-col class="py-0" cols="4" sm="2">
							<v-btn
								icon
								outlined
								tile
								class="delete_condition"
							><v-icon>mdi-delete</v-icon></v-btn>
						</v-col>
					</template>
				</v-row>
			</v-container>

			<div>
				<v-btn
					@click="createCondition({name:'',experiment_experiment_id:experiment.experimentId})"
					color="blue"
					class="add_condition px-0 mb-10"
					text
				>Add another condition</v-btn>
			</div>

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
import store from "@/store";

export default {
	name: 'DesignConditions',
	props: ['experiment'],
	data: () => ({
		requiredText: [
			v => !!v || 'Condition name is required'
		]
	}),

	methods: {
		...mapActions({
			createCondition: 'condition/createCondition',
		}),
		// TODO - DELETE Conditions

		// TODO - SAVE Conditions
		saveConditions() {
			const _this = this
			const e = _this.experiment.conditions
			console.log({e})
			// this.updateConditions(e)
			// 		.then(response => {
			// 			if (response.status === 200) {
			// 				console.log({response})
			// 				// this.$router.push({name:'ExperimentDesignConditions', params:{experiment: this.experiment.experiment_id}})
			// 			} else {
			// 				alert(response.error)
			// 			}
			// 		})
			// 		.catch(response => {
			// 			console.log("updateExperiment | catch", {response})
			// 		})
		},
	},

	beforeRouteEnter(to,from,next) {
		if (store.state.experiment.experiment.conditions.length < 2) {
			store.dispatch('condition/createDefaultConditions', to.params.experiment_id).then(() => next())
		} else {
			next()
		}
	},
	beforeRouteUpdate(to,from,next) {
		if (store.state.experiment.experiment.conditions.length < 2) {
			store.dispatch('condition/createDefaultConditions', to.params.experiment_id).then(() => next())
		} else {
			next()
		}
	},
}
</script>

<style lang="scss" scoped>
	.add_condition {
		text-transform: unset !important;
	}
	.delete_condition {
		border-radius: 4px;
		width: 100%;
		height: 56px;
	}
</style>