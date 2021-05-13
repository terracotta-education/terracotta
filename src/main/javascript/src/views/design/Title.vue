<template>
	<div>
		<h1>Create a title for your experiment</h1>
		<form
			submit.prevent="saveTitle"
			class="my-5"
		>
			<v-text-field
				v-model="title"
				:rules="titleRules"
				label="Experiment title"
				placeholder="e.g. Lorem ipsum"
				autofocus
				outlined
				required
			></v-text-field>
			<v-btn
				:disabled="!title"
				elevation="0"
				color="primary"
				class="mr-4"
				@click="saveTitle"
			>
				Next
			</v-btn>
		</form>
		<v-card
			class="mt-15 pt-5 px-5 mx-auto blue lighten-5 rounded-lg"
			outlined
		>
			<p><strong>Note:</strong> Students will be able to see this title if they click on the Terracotta tool, so do not include any details about your study that you wouldn't want participants to see.</p>
		</v-card>
	</div>
</template>

<script>
export default {
	name: 'DesignTitle',

	data: () => ({
		titleRules: [
			v => !!v || 'Title is required'
		],
		title: ''
	}),
	methods: {
		saveTitle () {
			console.log('SAVE TO API');
			this.$router.push({name:'ExperimentDesignDescription'})
		},
	}
}
</script>