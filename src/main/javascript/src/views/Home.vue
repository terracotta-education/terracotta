<template>
	<v-row class="text-center">
		<v-col cols="12" class="mt-10">
			<v-btn @click="startExperiment">Get Started</v-btn>
		</v-col>
	</v-row>
</template>

<script>
	import { mapActions } from "vuex";

	export default {
		name: 'Home',

		components: {},
		computed: {
		},
		methods: {
			...mapActions({
				createExperiment: 'experiment/createExperiment'
			}),

			startExperiment() {
				const _this = this
				this.createExperiment()
						.then(response => {
							console.log("startExperiment -> createExperiment | then",{response})
							_this.$router.push({name: 'ExperimentDesignIntro', params: { experiment_id: response.experimentId }})
						}).catch(response => {
							// TODO - Error, couldn't create experiment
							console.log("startExperiment -> createExperiment | catch",{response})
						})
			}
		}
	}
</script>
