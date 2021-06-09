<template>
	<div>
		<h1>Create a title for your consent assignment</h1>
		<p>This will create an <strong>unpublished consent assignment</strong> in Canvas and will be the way your students will read, review and sign your study’s informed consent. The consent assignment will be a prerequisite for your first study treatment assignments.</p>
		<form
			@submit.prevent="saveTitle"
			class="my-5"
			v-if="experiment && experiment.consent"
		>
			<v-text-field
				v-model="title"
				:rules="requiredText"
				label="Assignment title"
				placeholder="e.g. Lorem ipsum"
				autofocus
				outlined
				required
			></v-text-field>
			<v-btn
				:disabled="!titleProxy"
				elevation="0"
				color="primary"
				class="mr-4"
				type="submit"
			>
				Next
			</v-btn>
		</form>
	</div>
</template>

<script>
import { mapMutations } from "vuex";

export default {
	name: 'ParticipationTypeConsentTitle',
	props: ['experiment'],
	created() {
		this.createConsent()
	},
	computed: {
		title: {
			get () {
				return this.$store.state.experiment.experiment.consent.title
			},
			set (value) {
				this.titleProxy = value
				this.$store.commit('experiment/setConsentTitle', value)
			}
		}
	},
	data: () => ({
		titleProxy: "",
		requiredText: [
			v => !!v || 'Title is required'
		],
	}),
	methods: {
		...mapMutations({
			createConsent: 'experiment/createConsent',
		}),
		saveTitle() {
			this.$router.push({name:'ParticipationTypeConsentFile', params:{experiment: this.experiment.experimentId}})
		}
	},
}
</script>