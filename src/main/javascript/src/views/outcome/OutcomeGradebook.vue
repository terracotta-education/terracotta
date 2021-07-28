<template>
  <div v-if="experiment && exposure_id && outcome">
    <h1 class="mb-6">Select gradebook item(s)</h1>
    <form @submit.prevent="saveExit">
      <v-simple-table class="mb-9 v-data-table--light-header">
        <template v-slot:default>
          <thead>
            <tr>
              <th width="50"></th>
              <th class="text-left">Gradebook Item</th>
              <th class="text-left" width="250">Total Points</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>
                <v-checkbox
                  v-model="selected"
                  value="25"
                ></v-checkbox>
              </td>
              <td>Quiz 1</td>
              <td>25</td>
            </tr>
            <tr>
              <td>
                <v-checkbox
                  v-model="selected"
                  value="35"
                ></v-checkbox>
              </td>
              <td>Quiz 2</td>
              <td>35</td>
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
  name: 'OutcomeGradebook',
  computed: {
    ...mapGetters({
      experiment: 'experiment/experiment',
      outcome: 'outcome/outcome',
      outcomeScores: 'outcome/outcomeScores',
      participants: 'participants/participants'
    }),
    exposure_id() {
      return parseInt(this.$route.params.exposure_id)
    },
    experiment_id() {
      return parseInt(this.$route.params.experiment_id)
    },
    outcome_id() {
      return parseInt(this.$route.params.outcome_id)
    },
  },
  data() {
    return {
      selected: []
    }
  },
  methods: {
    ...mapActions({
      fetchParticipants: 'participants/fetchParticipants',
      fetchOutcomeById: 'outcome/fetchOutcomeById',
      fetchOutcomeScores: 'outcome/fetchOutcomeScores',
      updateOutcome: 'outcome/updateOutcome',
      updateOutcomeScores: 'outcome/updateOutcomeScores'
    }),
    async saveExit() {}
  },
  async created() {}
}
</script>