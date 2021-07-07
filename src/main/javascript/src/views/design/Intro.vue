<template>
	<div>
		<h1>Let's set up your experiment</h1>
		<p>In this section, we'll outline the basic design of your experiment.  Once it is created, you can then select who will participate, and add assignments to your study.</p>
		<v-btn elevation="0" color="primary" :to="{name: 'ExperimentDesignTitle'}">Continue</v-btn>
	</div>
</template>

<script>

// import {mapActions} from "vuex";

export default {
	name: 'DesignIntro',
	props: ['experiment'],
	methods: {
		saveExit() {
			this.$router.push({name:'Home', params:{experiment: this.experiment.experiment_id}})
			}
	}
}
</script>