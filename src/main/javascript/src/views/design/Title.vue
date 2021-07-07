<template>
  <div>
    <h1>Create a title for your experiment</h1>
    <form
      @submit.prevent="saveTitle('ExperimentDesignDescription')"
      class="my-5"
      v-if="experiment"
    >
      <v-text-field
        v-model="experiment.title"
        :rules="rules"
        label="Experiment title"
        placeholder="e.g. Lorem ipsum"
        autofocus
        outlined
        required
      ></v-text-field>
      <v-btn
        :disabled="!experiment.title || !experiment.title.trim()"
        elevation="0"
        color="primary"
        class="mr-4"
        type="submit"
      >
        Next
      </v-btn>
    </form>
    <v-card
      class="mt-15 pt-5 px-5 mx-auto blue lighten-5 rounded-lg"
      outlined
    >
      <p><strong>Note:</strong> Students will be able to see this title if they click on the Terracotta tool, so do not
        include any details about your study that you wouldn't want participants to see.</p>
    </v-card>
  </div>
</template>

<script>
import {mapActions} from "vuex"

export default {
  name: 'DesignTitle',
  props: ['experiment'],
  data: () => ({
    rules: [
      v => v && !!v.trim() || 'Title is required',
      v => (v || '').length <= 255 || 'A maximum of 255 characters is allowed'
    ],
  }),
  methods: {
    ...mapActions({
      updateExperiment: 'experiment/updateExperiment',
    }),
    saveTitle(path) {
      const _this = this
      const e = _this.experiment

      this.updateExperiment(e)
        .then(response => {
          if (typeof response?.status !== "undefined" && response?.status === 200) {
            this.$router.push({name: path, params:{experiment: this.experiment.experiment_id}})
          } else if (response?.message) {
            alert(`Error: ${response.message}`)
          } else {
            alert('There was an error saving your experiment.')
          }
        })
        .catch(response => {
          console.log("updateExperiment | catch", {response})
          alert('There was an error saving your experiment.')
        })
    },
    saveExit() {
        this.saveTitle('Home')
				// console.log('Hello World2!')
			}

  }
}
</script>