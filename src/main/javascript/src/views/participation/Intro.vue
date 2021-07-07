<template>
	<div>
		<h1>In this section, you'll decide how students in your class become participants in your experiment</h1>
		<p>Terracotta can allow students to provide consent to participate, and can also allow you the opportunity to manually indicate who has and hasn't agreed to participant.</p>
		<v-btn elevation="0" color="primary" :to="{name: 'ExperimentParticipationSelectionMethod'}">Continue</v-btn>
	</div>
</template>

<script>

// import {mapActions} from "vuex";

export default {
	name: 'ParticipationIntro',
	methods: {
		saveExit() {
				console.log('Hello World2!')
			}
	}
}
</script>