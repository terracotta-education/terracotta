<template>
	<div>
		<h1>Describe your experiment</h1>
		<p>Use this space to write down some notes about your research question, why this question is meaningful, and your hypothesis.</p>
		<form
			@submit.prevent="saveExperiment"
			class="my-5 mb-15"
			v-if="experiment"
		>
			<v-textarea
				v-model="experiment.description"
				:rules="requiredText"
				label="Experiment description"
				placeholder="e.g. Lorem ipsum"
				autofocus
				outlined
				required
			></v-textarea>
			<v-btn
				:disabled="!experiment.description"
				elevation="0"
				color="primary"
				class="mr-4"
				type="submit"
			>
				Next
			</v-btn>
		</form>

		<h4 class="mb-3">Examples</h4>
		<v-carousel
			v-model="slide"
			hide-delimiters
			height="auto"
			light
		>
			<v-carousel-item
				v-for="(blurb, i) in blurbs"
				:key="i"
			>
				<v-card
					class="pt-5 px-5 mx-auto"
					outlined
				>
					<v-card-text>
						<p>{{ blurb }}</p>
					</v-card-text>
				</v-card>
			</v-carousel-item>
		</v-carousel>
	</div>
</template>

<script>
import {mapActions} from "vuex";

export default {
	name: 'DesignDescription',
	props: ['experiment'],
	data: () => ({
		requiredText: [
			v => v && !!v.trim() || 'Description is required'
		],
		slide: 0,
		blurbs: [
			"The study looks to test whether exposure to ones' own lexile level (and seeing how it improves over time) might improve students' learning outcomes.",
			"The study explores how learning about the biology of skin color (vs. social construction of race) impacts students’ conception of race. It is important for science teachers to understand the impacts of teaching about skin color biology and how it may impact students' concepts of race."
		]
	}),
	methods: {
		...mapActions({
			updateExperiment: 'experiment/updateExperiment',
		}),
		saveExperiment() {
			const _this = this
			const e = _this.experiment

			this.updateExperiment(e)
					.then(response => {
						if (response.status === 200) {
							this.$router.push({name:'ExperimentDesignConditions', params:{experiment: this.experiment.experiment_id}})
						} else {
							alert("error: ", response.statusText || response.status)
						}
					})
					.catch(response => {
						console.log("updateExperiment | catch", {response})
					})
		},
	}
}
</script>

<style lang="scss">
	@import '~vuetify/src/styles/main.sass';
	@import '~@/styles/variables';

	.v-window.v-carousel {
		overflow: unset !important;

		.v-window__container {
			//overflow: visible;
		}

		.v-window__prev,
		.v-window__next {

			.v-btn--icon.v-size--default {
				background: white;
				height: 40px;
				width: 40px;

				@extend .elevation-4 !optional;

				i.v-icon {
					color: black;
					font-size: 20px !important;
				}
			}
		}
		.v-window__prev {
			left: -32px;
		}
		.v-window__next {
			right: -32px;
		}
	}
</style>