<template>
  <div v-if="experiment && assignment">
    <h1 class="mb-6">{{ assignment.title }}</h1>
    <form @submit.prevent="saveExit">
      <v-simple-table class="mb-9 v-data-table--light-header">
        <template v-slot:default>
          <thead>
            <tr>
              <th class="text-left">Student Name</th>
              <th class="text-left" style="width:250px;">Score (out of TOTAL)</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>Firstname Lastname</td>
              <td>
                <v-text-field
                  type="number"
                  placeholder="---"
                  style="max-width: 50px;"
                  required
                ></v-text-field>
              </td>
            </tr>
          </tbody>
        </template>
      </v-simple-table>
    </form>
  </div>
</template>

<script>
import {mapActions, mapGetters} from 'vuex'

export default {
  name: 'AssignmentScores',
  computed: {
    ...mapGetters({
      experiment: 'experiment/experiment',
      assignment: 'assignment/assignment'
    }),
    assignment_id() {
      return parseInt(this.$route.params.assignment_id)
    },
    exposure_id() {
      return parseInt(this.$route.params.exposure_id)
    },
    experiment_id() {
      return parseInt(this.$route.params.experiment_id)
    }
  },
  data() {
    return {

    }
  },
  methods: {
    ...mapActions({
      fetchParticipants: 'participants/fetchParticipants',
      fetchAssignment: 'assignment/fetchAssignment'
    }),

    async saveExit() {

    }
  },
  async created() {
    await this.fetchAssignment([this.experiment_id, this.exposure_id, this.assignment_id, true])
    await this.fetchParticipants(this.experiment_id)
  }
}
</script>