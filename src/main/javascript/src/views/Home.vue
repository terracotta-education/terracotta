<template>
	<v-row class="text-center">
		<v-col cols="12" class="mt-10">
			<v-btn @click="createExperiment">Get Started</v-btn>
		</v-col>
	</v-row>
</template>

<script>
	import { mapActions,mapGetters } from "vuex";

	export default {
		name: 'Home',

		components: {},
		computed: {
			...mapGetters({
				pageExperiment: 'experiment/pageExperiment'
			}),
		},
		methods: {
			...mapActions({
				createExperiment: 'experiment/createExperiment'
			}),
			startExperiment() {
				this.createExperiment()
						.then(response => {
							console.log("createExperiment | then",{response})
							this.pageExperiment(response.experiment_id)
									.then(response => {
										// forward user to intro after creating experiment
										this.$router.push({name: 'ExperimentDesignIntro', params: { experiment_id: response.experiment_id }})
									}).catch(response => {
										console.log("pageExperiment | catch",{response})
									})
						}).catch(response => {
							// TODO - Error, couldn't create experiment
							console.log("createExperiment | catch",{response})
						})
			}
		}
	}
</script>
