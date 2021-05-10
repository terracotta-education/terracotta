<template>
	<v-row class="text-center">
		<v-col cols="12" class="mt-10">
			<v-btn @click="createExperiment">Get Started</v-btn>
		</v-col>
	</v-row>
</template>

<script>
	export default {
		name: 'Home',

		components: {},
		methods: {
			createExperiment() {
				// createExperiment -> forward to experiment design step 1
				this.$router.push({name: 'ExperimentDesignIntro', params: { experiment_id: 1 }})
			}
		}
	}
</script>
