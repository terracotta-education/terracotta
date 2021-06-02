<template>
	<div>
		<h1 class="mb-3">How will study participation be determined?</h1>
<!--		<v-btn elevation="0" color="primary" :to="{name: 'ExperimentParticipationSelectionMethod'}">Continue</v-btn>-->

		<v-expansion-panels class="v-expansion-panels--icon" flat>

			<v-expansion-panel :class="{'v-expansion-panel--selected':experiment.participation_type===0}">
				<v-expansion-panel-header hide-actions><img src="@/assets/consent_invite.svg" alt="invite students"> <strong>Students will be invited to consent</strong></v-expansion-panel-header>
				<v-expansion-panel-content>
					<p>Select this option if you would like to create a consent assignment within Canvas</p>
					<v-btn @click="participationType(0)" color="primary" elevation="0">Select</v-btn>
				</v-expansion-panel-content>
			</v-expansion-panel>

			<v-expansion-panel :class="{'v-expansion-panel--selected':experiment.participation_type===1}">
				<v-expansion-panel-header hide-actions><img src="@/assets/consent_manual.svg" alt="manually decide students"> <strong>Teacher will manually decide</strong></v-expansion-panel-header>
				<v-expansion-panel-content>
					<p>Select this option if you are working with minors or will be collecting parental consent</p>
					<v-btn @click="participationType(1)" color="primary" elevation="0">Select</v-btn>
				</v-expansion-panel-content>
			</v-expansion-panel>

			<v-expansion-panel :class="{'v-expansion-panel--selected':experiment.participation_type===2}">
				<v-expansion-panel-header hide-actions><img src="@/assets/consent_automatic.svg" alt="automatically include all students"> <strong>Automatically include all students</strong></v-expansion-panel-header>
				<v-expansion-panel-content>
					<p>Select this option if informed consent is not needed to run the study</p>
					<v-btn @click="participationType(2)" color="primary" elevation="0">Select</v-btn>
				</v-expansion-panel-content>
			</v-expansion-panel>

		</v-expansion-panels>

	</div>
</template>

<script>
// import {mapActions} from "vuex";

export default {
	name: 'ParticipationSelectionMethod',
	props: ['experiment'],
	methods: {
		participationType(type) {
			console.log(type)
		}
	}
}
</script>