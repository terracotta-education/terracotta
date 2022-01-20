<template>
	<div>
		<template v-if="experiment && experiment.conditions">
			<h1 class="mb-5">
				<span>You have defined <strong>{{ numConditions }} conditions</strong></span><br><br>
				<span>How do you want students to be exposed to these different conditions?</span>
			</h1>

			<v-expansion-panels class="v-expansion-panels--icon" flat>

				<v-expansion-panel :class="{'v-expansion-panel--selected':experiment.exposureType==='WITHIN'}">
					<v-expansion-panel-header hide-actions><img src="@/assets/all_conditions.svg" alt="all conditions"> All conditions</v-expansion-panel-header>
					<v-expansion-panel-content>
						<p>All students are exposed to every condition, in different orders. This way you can compare how the different conditions affected each individual student. This is called a within-subject design.</p>
						<v-btn @click="saveType('WITHIN')" color="primary" elevation="0">Select</v-btn>
					</v-expansion-panel-content>
				</v-expansion-panel>

				<v-expansion-panel :class="{'v-expansion-panel--selected':experiment.exposureType==='BETWEEN'}">
					<v-expansion-panel-header hide-actions><img src="@/assets/one_condition.svg" alt="only one condition"> Only one condition</v-expansion-panel-header>
					<v-expansion-panel-content>
						<p>Each student is only exposed to one condition, so that you can compare how the different conditions affected different students. This is called a between-subjects design.</p>
						<v-btn @click="saveType('BETWEEN')" color="primary" elevation="0">Select</v-btn>
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
			return this.experiment?.conditions?.length || 0
		}
	},
	methods: {
		...mapActions({
			reportStep: 'api/reportStep',
			updateExperiment: 'experiment/updateExperiment',
		}),
		saveType(type) {
			const e = this.experiment
			e.exposureType = type

			const experimentId = e.experimentId
			const step = "exposure_type"

			this.updateExperiment(e)
					.then(async response => {
            if (typeof response?.status !== "undefined" && response?.status === 200) {
              // report the current step
              await this.reportStep({experimentId, step})
              if (this.experiment.exposureType==='BETWEEN' || this.experiment.exposureType==='WITHIN') {
                this.$router.push({name:'ExperimentDesignDefaultCondition', params:{experiment: experimentId}})
              } else {
                this.$swal("Select an experiment type")
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
