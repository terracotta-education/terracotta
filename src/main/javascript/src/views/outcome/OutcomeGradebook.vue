<template>
  <div v-if="experiment && exposureId">
    <h1 class="mb-6">Select gradebook item(s)</h1>
    <form @submit.prevent="saveExit">
      <v-simple-table class="mb-9 v-data-table--light-header">
        <template v-slot:default>
          <thead>
            <tr>
              <th style="width:50px;">
              <v-checkbox
                on-icon="$checkboxIndeterminate"
                v-model="selectAll"
                :value="selectAll"
                color="primary"
                @change="handleSelectAll()"
               ></v-checkbox>
              </th>
              <th class="text-left">Gradebook Item</th>
              <th class="text-left" style="width:250px;">Total Points</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="(op, opIndex) in outcomePotentials"
              :key="opIndex"
            >
              <td>
                <template v-if="!outcomes.some(o=>parseInt(o.lmsOutcomeId)===parseInt(op.assignmentId) && o.exposureId === exposureId)">
                  <v-checkbox
                    v-model="selectedAssignmentIds"
                    :value="op.assignmentId"
                  ></v-checkbox>
                </template>
                <template v-else>
                  <v-icon>mdi-check</v-icon>
                </template>
              </td>
              <td>{{op.name}}</td>
              <td>{{op.pointsPossible}}</td>
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
      outcomePotentials: 'outcome/outcomePotentials',
      outcomes: 'outcome/outcomes'
    }),
    exposureId() {
      return parseInt(this.$route.params.exposureId)
    },
    experimentId() {
      return parseInt(this.$route.params.experimentId)
    }
  },
  data() {
    return {
      selectedAssignmentIds: [],
      selectAll: false
    }
  },
  methods: {
    ...mapActions({
      fetchOutcomePotentials: 'outcome/fetchOutcomePotentials',
      fetchOutcomes: 'outcome/fetchOutcomes',
      createOutcome: 'outcome/createOutcome'
    }),

   handleSelectAll(){
     this.selectedAssignmentIds = this.selectAll ? this.outcomePotentials.map((op) => op.assignmentId) : []
   },

    async saveExit() {
      try {
        await Promise.all(this.selectedAssignmentIds.map(async assignmentId => {
          const op = this.outcomePotentials.find(o=>parseInt(o.assignmentId)===parseInt(assignmentId))
          // payload = experimentId, exposureId, title, max_points, external, lmsType, lmsOutcomeId
          return await this.createOutcome([this.experimentId, this.exposureId, op.name, op.pointsPossible, true, op.type, parseInt(assignmentId)])
        })).then(() => {
          this.$router.push({name:'ExperimentSummary'})
        })
      } catch(error) {
        console.error({error})
      }
    }
  },
  async created() {
    await this.fetchOutcomes([this.experimentId, this.exposureId])
    await this.fetchOutcomePotentials(this.experimentId)
  }
}
</script>