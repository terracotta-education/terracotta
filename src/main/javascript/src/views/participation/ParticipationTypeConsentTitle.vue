<template>
	<div>
		<h1>Create a title for your consent assignment</h1>
		<p>This will create an <strong>unpublished consent assignment</strong> in Canvas and will be the way your students will read, review and sign your study’s informed consent. The consent assignment will be a prerequisite for your first study treatment assignments.</p>
		<v-btn elevation="0" color="primary" class="mb-4" :to="{name: 'ParticipationTypeAssignmentTitle'}">Next</v-btn>
	</div>
</template>

<script>
export default {
	name: 'ParticipationTypeConsentTitle'
}
</script>