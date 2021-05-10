<template>
	<v-container>
		<v-row class="text-center">
			<v-col cols="12">
				<p>Experiment {{$route.params.experiment_id}} Summary</p>
			</v-col>
		</v-row>
	</v-container>
</template>

<script>

	export default {
		name: 'ExperimentSummary',

		data: () => ({}),
	}
</script>
