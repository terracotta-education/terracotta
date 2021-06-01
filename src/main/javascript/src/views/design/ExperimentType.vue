<template>
	<div>
		<template v-if="experiment && experiment.conditions">
			<h1 class="mb-5">
				<span>You have defined <strong>{{ numConditions }} conditions</strong></span><br><br>
				<span>How do you want students to be exposed to these different conditions?</span>
			</h1>

			<v-expansion-panels flat>

				<v-expansion-panel :class="{'v-expansion-panel--selected':experiment.exposureType==='BETWEEN'}">
					<v-expansion-panel-header hide-actions><img src="@/assets/all_conditions.svg" alt="all conditions"> All conditions</v-expansion-panel-header>
					<v-expansion-panel-content>
						<p>All students are exposed to every condition, in different orders. This way you can compare how the different conditions affected each individual student. This is called a within-subject design.</p>
						<v-btn @click="saveType('BETWEEN')" color="primary" elevation="0">Select</v-btn>
					</v-expansion-panel-content>
				</v-expansion-panel>

				<v-expansion-panel :class="{'v-expansion-panel--selected':experiment.exposureType==='WITHIN'}">
					<v-expansion-panel-header hide-actions><img src="@/assets/one_condition.svg" alt="only one condition"> Only one condition</v-expansion-panel-header>
					<v-expansion-panel-content>
						<p>Each student is only exposed to one condition, so that you can compare how the different conditions affected different students. This is called a between-subjects design.</p>
						<v-btn @click="saveType('WITHIN')" color="primary" elevation="0">Select</v-btn>
					</v-expansion-panel-content>
				</v-expansion-panel>

			</v-expansion-panels>
		</template>
		<template v-else>
			<v-alert
				prominent
				type="error"
			>
				<v-row align="center">
					<v-col class="grow">
						No conditions found
					</v-col>
				</v-row>
			</v-alert>
		</template>
	</div>
</template>

<script>
import { mapActions } from "vuex";

export default {
	name: 'ExperimentType',
	props: ['experiment'],
	computed: {
		numConditions() {
			return this.experiment.conditions.length
		}
	},
	methods: {
		...mapActions({
			updateExperiment: 'experiment/updateExperiment',
		}),
		saveType(type) {
			const _this = this
			const e = _this.experiment
			e.exposureType = type

			this.updateExperiment(e)
					.then(response => {
						if (response.status === 200) {
							if (this.experiment.exposureType==='WITHIN') {
								this.$router.push({name:'ExperimentDesignDefaultCondition', params:{experiment: this.experiment.experiment_id}})
							} else if(this.experiment.exposureType==='BETWEEN') {
								this.$router.push({name:'ExperimentDesignSummary', params:{experiment: this.experiment.experiment_id}})
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

<style lang="scss">
	@import '~@/styles/variables';

	.v-expansion-panel {
		padding: 30px;
		border: 2px solid map-get($grey, 'lighten-2');
		border-radius: 10px;
		margin-bottom: 10px;


		&-header {
			padding: 0 !important;
			min-height: 1em !important;

			> img {
				max-width: 20px;
				margin-right: 10px;
			}
		}
		&-content {
			font-size: 14px;
			color: rgba(0,0,0,0.6);

			.v-expansion-panel-content__wrap {
				padding: 10px 0 0 0 !important;
			}
		}
		&--active {
			border-color: map-get($light-blue, 'base');
			text-align:center;

			.v-expansion-panel-header{
				text-align: center;
				flex-direction: column;
				font-weight: bold;
				transition: all 250ms ease-in-out;

				img {
					display: block;
					opacity: 0.6;
					max-width: 40px;
					margin: 0 auto 20px;
				}
			}
		}
	}
</style>