<template>
	<main class="experiment-steps">
		<aside class="experiment-steps__sidebar">
			<steps />
		</aside>
		<article class="experiment-steps__body">
			<router-view></router-view>
		</article>
	</main>
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

<style lang="scss" scoped>
	.experiment-steps {
		display: grid;
		grid-template-columns: 268px 1fr;

		&__sidebar {}
		&__body {}
	}
</style>