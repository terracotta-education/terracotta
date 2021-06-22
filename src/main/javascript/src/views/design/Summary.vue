<template>
	<div>
		<h1 class="mb-3">
			<span class="green--text font-weight-bold">You've completed section 1.</span><br>
			<span>Here's a summary of your experiment design.</span>
		</h1>

		<template v-if="experiment">
			<v-expansion-panels flat>
				<v-expansion-panel class="py-3 mb-3">
					<v-expansion-panel-header><strong>Title</strong></v-expansion-panel-header>
					<v-expansion-panel-content>
						<p>{{ experiment.title }}</p>
					</v-expansion-panel-content>
				</v-expansion-panel>
			</v-expansion-panels>
			<v-expansion-panels flat>
				<v-expansion-panel class="py-3 mb-3">
					<v-expansion-panel-header><strong>Description</strong></v-expansion-panel-header>
					<v-expansion-panel-content>
						<p>{{ experiment.description }}</p>
					</v-expansion-panel-content>
				</v-expansion-panel>
			</v-expansion-panels>
			<v-expansion-panels flat v-if="experiment.conditions && experiment.conditions.length>0">
				<v-expansion-panel class="py-3 mb-3">
					<v-expansion-panel-header><strong>Conditions</strong></v-expansion-panel-header>
					<v-expansion-panel-content>
						<v-list class="m-0 p-0">
							<v-list-item
								v-for="condition in experiment.conditions"
								:key="condition.conditionId"
								class="mx-0 px-0"
							>
								<v-list-item-content>
									<v-list-item-title v-text="condition.name"></v-list-item-title>
								</v-list-item-content>

								<v-list-item-icon>
									<v-icon v-if="condition.defaultCondition" class="green--text">mdi-check</v-icon>
								</v-list-item-icon>
							</v-list-item>
						</v-list>
					</v-expansion-panel-content>
				</v-expansion-panel>
			</v-expansion-panels>
			<v-expansion-panels flat>
				<v-expansion-panel class="py-3 mb-6">
					<v-expansion-panel-header><strong>Experiment type</strong></v-expansion-panel-header>
					<v-expansion-panel-content>
						<p>{{ exposureType }}</p>
					</v-expansion-panel-content>
				</v-expansion-panel>
			</v-expansion-panels>
		</template>

		<v-btn
			elevation="0"
			color="primary"
			class="mr-4"
			@click="nextSection"
		>
			Continue to next section
		</v-btn>
	</div>
</template>

<script>
// import {mapActions} from "vuex";

export default {
	name: 'DesignSummary',
	props: ['experiment'],
	computed: {
		exposureType() {
			return (this.experiment.exposureType==='BETWEEN')?'One condition':'All conditions'
		}
	},
	methods: {
		nextSection() {
			this.$router.push({name:'ExperimentParticipationIntro', params:{experiment: this.experiment.experimentId}})
		}
	}
}
</script>

<style lang="scss" >
	.v-expansion-panel {
		border: 1px solid map-get($grey, 'lighten-2');
	}
</style>