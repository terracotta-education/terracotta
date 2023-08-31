<template>
  <div>
    <h1>Create a title for your experiment</h1>
    <form
      v-if="experiment"
      @submit.prevent="saveTitle('ExperimentDesignDescription')"
      class="my-5"
    >
      <v-text-field
        v-model="experiment.title"
        :rules="rules"
        label="Experiment title"
        placeholder="e.g. Lorem ipsum"
        outlined
        required
      ></v-text-field>
      <v-btn
        v-if="!this.editMode"
        :disabled="!experiment.title || !experiment.title.trim()"
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
import {mapActions, mapGetters} from "vuex"

export default {
  name: 'DesignTitle',
  props: ['experiment'],
  data: () => ({
    rules: [
      v => v && !!v.trim() || 'Title is required',
      v => (v || '').length <= 255 || 'A maximum of 255 characters is allowed'
    ],
  }),
  computed: {
    ...mapGetters({
      editMode: 'navigation/editMode'
    }),
    getSaveExitPage() {
      return this.editMode?.callerPage?.name || 'Home';
    }
  },
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
            this.$router.push({
              name: path,
              params: {
                experiment: this.experiment.experiment_id
              }
            })
          } else if (response?.message) {
            this.$swal(`Error: ${response.message}`)
          } else {
            this.$swal('There was an error saving your experiment.')
          }
        })
        .catch(response => {
          console.log("updateExperiment | catch", {response})
          this.$swal('There was an error saving your experiment.')
        })
    },
    saveExit()  {
      this.saveTitle(this.getSaveExitPage)
    }
  }
}
</script>
