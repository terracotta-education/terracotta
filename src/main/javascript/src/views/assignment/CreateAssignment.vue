<template>
  <div>
    <h1>Create your assignment</h1>
    <p>This will create an unpublished assignment shell in Canvas and will be the way Terracotta will deliver treatments
      to students.</p>
    <form
      @submit.prevent="saveTitle('AssignmentTreatmentSelect')"
      class="my-5"
      v-if="experiment && exposures"
    >
      <v-text-field
        v-model="title"
        :rules="rules"
        label="Assignment name"
        placeholder="e.g. Lorem ipsum"
        autofocus
        outlined
        required
      ></v-text-field>
      <v-btn
        :disabled="!title || !title.trim()"
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
import {mapActions, mapGetters} from 'vuex'

export default {
  name: 'CreateAssignment',
  props: ['experiment'],
  computed: {
    exposure_id() {
      return parseInt(this.$route.params.exposure_id)
    },
    experiment_id() {
      return parseInt(this.experiment.experimentId)
    },
    ...mapGetters({
      exposures: 'exposures/exposures'
    })
  },
  data: () => ({
    title: "",
    rules: [
      v => v && !!v.trim() || 'Assignment Name is required',
      v => (v || '').length <= 255 || 'A maximum of 255 characters is allowed'
    ],
  }),
  methods: {
    ...mapActions({
      createAssignment: 'assignment/createAssignment'
    }),
    saveTitle(path) {
      this.createAssignment([this.experiment_id, this.exposure_id, this.title, 1])
        .then(response => {
          if (response?.status === 201) {
            this.$router.push({name: path, params:{
              experiment_id: this.experiment_id,
              assignment_id: response.data.assignmentId
            }})
          } else {
            alert("error: ", response)
          }
        })
        .catch(response => {
          console.error("createAssignment | catch", {response})
          alert('There was an error creating the assignment.')
        })
    },
    saveExit() {
      this.saveTitle('Home')
    }
  },
}
</script>