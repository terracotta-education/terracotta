<template>
  <div>
    <template v-if="loaded && exposures && exposures.length>0 && assignments">
      <h1 class="mb-3">Your Assignments</h1>
      <div class="mb-6">
        <v-expansion-panels class="v-expansion-panels--outlined mb-7"
                            flat
                            v-for="(exposure, eIndex) in exposures"
                            :key="eIndex">
          <v-expansion-panel class="py-3">
            <v-expansion-panel-header>
              <strong>
                {{ exposure.title }}
                <span
                  :class="{'red--text':!assignmentIsBalanced(exposure.exposureId)}">({{ assignments.filter(a => a.exposureId === exposure.exposureId).length }})</span>
              </strong>
            </v-expansion-panel-header>
            <v-expansion-panel-content>
              <v-list class="pa-0  mb-3">
                <v-list-item class="justify-center px-0"
                             v-for="(assignment, aIndex) in assignments.filter(a=>a.exposureId===exposure.exposureId)"
                             :key="aIndex">
                  <v-list-item-content>
                    <p class="ma-0 pa-0">{{ assignment.title }} ({{ assignment.treatments && assignment.treatments.length || 0 }}/{{ conditions.length || 0 }})</p>
                  </v-list-item-content>
                  <v-list-item-action>
                    <v-btn
                      icon
                      outlined
                      text
                      tile
                      :to="{name:'AssignmentTreatmentSelect', params: {'exposure_id':assignment.exposureId, 'assignment_id':assignment.assignmentId}}">
                      <v-icon>mdi-pencil</v-icon>
                    </v-btn>
                  </v-list-item-action>
                  <v-list-item-action>
                    <v-btn
                      icon
                      outlined
                      text
                      tile
                      @click="handleDeleteAssignment(exposure.exposureId, assignment)">
                      <v-icon>mdi-delete</v-icon>
                    </v-btn>
                  </v-list-item-action>
                </v-list-item>
              </v-list>
              <template v-if="!assignmentIsBalanced(exposure.exposureId)">
                <div class="red--text mb-3">Add an assignment to balance the experiment</div>
              </template>
              <v-btn
                elevation="0"
                plain
                color="primary"
                class="px-0"
                :to="{ name: 'AssignmentCreateAssignment', params:{'exposure_id': parseInt(exposure.exposureId)} }"
              >add assignment</v-btn>
            </v-expansion-panel-content>
          </v-expansion-panel>
        </v-expansion-panels>
        <v-btn
          elevation="0"
          color="primary"
          :to="{ name: 'ExperimentSummary' }"
          :disabled="(shortestLength !== longestLength && exposures.length !== 1)|| longestLength < 1"
        >Finish</v-btn>
      </div>
    </template>
  </div>
</template>

<script>
import {mapActions, mapGetters, mapMutations} from 'vuex'

export default {
  name: 'YourAssignments',
  props: ['experiment'],
  computed: {
    exposure_id() {
      return parseInt(this.$route.params.exposure_id)
    },
    experiment_id() {
      return parseInt(this.experiment.experimentId)
    },
    ...mapGetters({
      assignments: 'assignment/assignments',
      conditions: 'experiment/conditions',
      exposures: 'exposures/exposures'
    })
  },
  data: () => ({
    shortestLength: 0,
    longestLength: 0,
    loaded: false
  }),
  methods: {
    ...mapMutations({
      resetAssignments: 'assignment/resetAssignments',
    }),
    ...mapActions({
      fetchExposures: 'exposures/fetchExposures',
      fetchAssignmentsByExposure: 'assignment/fetchAssignmentsByExposure',
      deleteAssignment: 'assignment/deleteAssignment',
    }),
    assignmentIsBalanced(eid) {
      // if Exposure Set Assignment array length is less than other Exposure Set Assignment arrays
      let eArr = []
      let longestArr = []
      let shortest = 0
      let longest = 0

      for (const e of this.exposures) {
        eArr = [...eArr, {
          exposureId: e.exposureId,
          assignments: this.assignments.filter(a => a.exposureId === e.exposureId)
        }]
      }

      const curArrLength = eArr.find(e => e.exposureId === eid)?.assignments?.length

      eArr.forEach((e) => {
        const l = e.assignments.length

        shortest = (l <= longest)? l : shortest

        if (l > longest) {
          longestArr = [e]
          longest = l
        } else if (l == longest) {
          longestArr.push(e)
        }
      });
      // update the component so we can use these values in the template
      this.shortestLength = shortest
      this.longestLength = longest
      // return true if current exposure set assignments list is longer or equal
      // to the longest assignment list in the exposure sets
      return !!curArrLength && curArrLength >= longest && curArrLength > 0
    },
    async handleDeleteAssignment(eid, a) {
      // DELETE ASSIGNMENT
      const reallyDelete = await this.$swal({
        icon: 'question',
        text: `Are you sure you want to delete the assignment "${a.title}"?`,
        showCancelButton: true,
        confirmButtonText: 'Yes, delete it',
        cancelButtonText: 'No, cancel',
      })
      if (reallyDelete?.isConfirmed) {
        try {
          return await this.deleteAssignment([
            this.experiment_id,
            eid,
            a.assignmentId
          ])
        } catch (error) {
          console.error("handleDeleteQuestion | catch", {error})
        }
      }

    },
    saveExit() {
      this.$router.push({name: 'Home'})
    }
  },
  async created() {
    // reset assignments to get a clean list
    await this.resetAssignments()
    // update assignments on load
    await this.fetchExposures(this.experiment_id)
    for (const e of this.exposures) {
      await this.fetchAssignmentsByExposure([this.experiment_id, e.exposureId])
    }

    // forward to create assignment if assignments array for selected exposure is empty
    if (
      this.exposure_id &&
      this.exposures.find(e => parseInt(e.exposureId) === this.exposure_id) &&
      !this.assignments.find(a => parseInt(a.exposureId) === this.exposure_id)
    ) {
      this.$router.push({name: 'AssignmentCreateAssignment', params:{exposure_id: this.exposure_id}})
    } else {
      // we don't want to display empty fields
      this.loaded = true
    }
  },
}
</script>