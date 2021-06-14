<template>
	<div v-if="consent">
		<h1>Add your study's <strong>informed consent</strong> file.</h1>

		<file-drop-zone class="my-5" :existing="consent.file" @update="onFileChange"></file-drop-zone>

		<v-btn :disabled="!consent.file" class="mt-3 mb-6" color="primary" elevation="0" @click="saveConsent">Next</v-btn>

		<p>You can <router-link :to="{ name: 'ParticipationSummary' }">download an informed consent template here.</router-link></p>
	</div>
</template>

<script>
import FileDropZone from "@/components/FileDropZone";
import { mapActions, mapGetters } from "vuex";

export default {
	name: 'ParticipationTypeConsentFile',
	props: ['experiment'],
	computed: {
		...mapGetters({
			consent: 'consent/consent'
		})
	},
	methods: {
		...mapActions({
			setConsentFile: 'consent/setConsentFile',
			createConsent: 'consent/createConsent',
		}),
		onFileChange (newFile) {
			if (newFile?.length) {
				this.setConsentFile(newFile)
			}
		},
		saveConsent() {
			this.createConsent(this.experiment.experimentId).then(
				this.$router.push({name:'ParticipationSummary', params:{experiment: this.experiment.experimentId}})
			)
		}
	},
	components: {
		FileDropZone
	}
}
</script>