<template>
	<main class="experiment-steps">
		<aside class="experiment-steps__sidebar">
			<steps :current-section="currentSection" :current-step="currentStep" />
		</aside>
		<nav>
			<router-link
				v-if="$routerHistory.hasPrevious()"
				:to="{ path: $routerHistory.previous().path }">
				<v-icon>mdi-chevron-left</v-icon> Back
			</router-link>
		</nav>
		<article class="experiment-steps__body">
			<v-container>
				<v-row justify="center">
					<v-col md="6">
						<router-view :key="$route.fullPath"></router-view>
					</v-col>
				</v-row>
			</v-container>
		</article>
	</main>
</template>

<script>
	import Steps from '../components/Steps'

	export default {
		name: 'ExperimentSteps',

		data: () => ({}),

		computed: {
			currentSection() {
				return this.$router.currentRoute.meta.currentSection
			},
			currentStep() {
				return this.$router.currentRoute.meta.currentStep
			}
		},

		components: {
			Steps
		}
	}
</script>

<style lang="scss" scoped>
	@import '~vuetify/src/styles/main.sass';
	@import '~@/styles/variables';

	.experiment-steps {
		display: grid;
		min-height: 100%;
		grid-template-rows: auto 1fr;
		grid-template-columns: auto 1fr;
		grid-template-areas:
			"aside nav"
			"aside article";

		> nav {
			grid-area: nav;
			padding: 30px;

			a {
				text-decoration: none;

				* {
					vertical-align: sub;
					@extend .blue--text;
				}
			}
		}
		> aside {
			grid-area: aside;
		}
		> article {
			grid-area: article;
			padding: 0;
		}

		&__sidebar {
			background: $gray-lightest;
			padding: 30px 45px;
		}
		&__body {
		}
	}
</style>