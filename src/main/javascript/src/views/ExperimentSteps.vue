<template>
	<v-container>
		<v-row class="text-center">
			<v-col cols="3">
				<steps />
			</v-col>
			<v-col cols="9">
				<router-view></router-view>
			</v-col>
		</v-row>
	</v-container>
</template>

<script>
	import Steps from '../components/Steps'

	export default {
		name: 'ExperimentSteps',

		data: () => ({}),

		components: {
			Steps
		}
	}
</script>
