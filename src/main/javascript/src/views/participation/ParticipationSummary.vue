<template>
  <div>
    <h1 class="my-3">
      <span class="green--text font-weight-bold"
        >You've completed section 2.</span
      ><br />
      Here's a summary of your experiment participation.
    </h1>
		<template v-if="experiment">

			<v-expansion-panels flat v-if="this.experiment.participationType">
				<v-expansion-panel class="py-3 mb-3">
					<v-expansion-panel-header><strong>Selection Method</strong></v-expansion-panel-header>
					<v-expansion-panel-content>
					<p>{{ participationType }}</p>
					</v-expansion-panel-content>
				</v-expansion-panel>
			</v-expansion-panels>

			<v-expansion-panels flat v-if="this.consent && this.consent.title">
				<v-expansion-panel class="py-3 mb-3">
					<v-expansion-panel-header><strong>Assignment Title</strong></v-expansion-panel-header>
					<v-expansion-panel-content>
					<p>{{ consent.title }}</p>
					</v-expansion-panel-content>
				</v-expansion-panel>
			</v-expansion-panels>

		</template>

		<v-btn
			elevation="0"
			color="primary"
			class="mt-3"
			@click="nextSection"
			>
				Continue to next section
		</v-btn>
	</div>
</template>

<script>
import { mapGetters } from "vuex";

export default {
  name: "ParticipationSummary",
  props: ["experiment"],
	computed: {
		...mapGetters({
			consent: 'consent/consent'
		}),
		participationType() {
			let type = ''

			switch (this.experiment.participationType) {
				case 'CONSENT':
					type = 'Invited students to consent'
					break
				case 'MANUAL':
					type = 'Manually determined students'
					break
				case 'AUTO':
					type = 'Automatically included all students'
					break
			}

			return type
		}
	},
	methods: {
		nextSection() {
			this.$router.push({name:'AssignmentIntro', params:{experiment: this.experiment.experimentId}})
		}
	},
  beforeRouteEnter(to, from, next) {
    // Updating selection type for custom steps
    to.meta.selectionType = from.meta.selectionType
    next()
  },
};
</script>

<style lang="scss" >
.v-expansion-panel {
	border: 1px solid map-get($grey, 'lighten-2');
}
</style>