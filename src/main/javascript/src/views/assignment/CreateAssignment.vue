<template>
	<div>
		<h1>Create your first assignment</h1>
		<p>This will create an unpublished assignment shell in Canvas and will be the way Terracotta will deliver treatments to students.</p>
		<form
			@submit.prevent="saveTitle"
			class="my-5"
			v-if="experiment"
		>
			<v-text-field
				v-model="title"
				:rules="requiredText"
				label="Assignment name"
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
				type="submit"
			>
				Next
			</v-btn>
		</form>
	</div>
</template>

<script>
export default {
	name: 'CreateAssignment',
	props: ['experiment'],
	data: () => ({
    title: "",
		requiredText: [
			v => !!v || 'Assignment Name is required'
		],
	}),
	methods: {
		saveTitle() {
			// this.$router.push({name:'XXXXX', params:{experiment: this.experiment.experimentId}})
		}
	},
}
</script>