<template>
  <div>
    <template v-if="assignments && assignments.length>0">
      <ul>
        <li v-for="(assignment, i) in assignments" :key="i">
          <router-link :to="{name:'AssignmentTreatmentSelect', params: {exposure_id:assignment.exposureId, assignment_id:assignment.assignmentId}}">{{assignment.title}}</router-link>
        </li>
      </ul>
      <v-btn
        elevation="0"
        color="primary"
        :to="{ name: 'AssignmentCreateAssignment', params:{exposure_id: this.exposure_id} }"
      >create assignment</v-btn>
    </template>
  </div>
</template>

<script>
import {mapActions, mapGetters} from 'vuex'

export default {
  name: 'YourAssignments',
  props: ['experiment'],
  computed: {
    exposure_id () {
      return parseInt(this.$route.params.exposure_id)
    },
    ...mapGetters({
      assignments: 'assignment/assignments'
    })
  },
  data: () => ({}),
  methods: {
    ...mapActions({
      fetchAssignments: 'assignment/fetchAssignments'
    }),
    saveExit() {
      this.$router.push({name:'Home'})
    }
  },
  async created() {
    // update assignments on load
    await this.fetchAssignments([this.experiment.experimentId, this.exposure_id])
    // forward to create assignment if assignments array is empty
    if (this.assignments.length < 1 && this.exposure_id) {
      this.$router.push({name: 'AssignmentCreateAssignment', params:{exposure_id: this.exposure_id}})
    }
  }
}
</script>