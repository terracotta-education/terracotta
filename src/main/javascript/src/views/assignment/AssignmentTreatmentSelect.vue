<template>
  <div>
    <h1 class="pa-0 mb-7">Now, let’s upload your treatments for each condition for <strong>{{ assignment.title }}</strong></h1>

    <v-expansion-panels class="v-expansion-panels--outlined mb-7" flat>
      <v-expansion-panel class="py-3">
        <v-expansion-panel-header>{{ assignment.title }} (0/{{ conditions.length }})</v-expansion-panel-header>
        <v-expansion-panel-content>
          <v-list class="pa-0">

            <v-list-item class="justify-center px-0"
              v-for="condition in conditions"
              :key="condition.conditionId">
              <v-list-item-content>
                <p class="ma-0 pa-0">{{ condition.name }}</p>
              </v-list-item-content>

              <v-list-item-action>
                <v-btn
                  color="primary"
                  outlined
                  @click="goToBuilder(condition.conditionId)"
                >Select</v-btn>
              </v-list-item-action>
            </v-list-item>

          </v-list>
        </v-expansion-panel-content>
      </v-expansion-panel>
    </v-expansion-panels>

    <v-btn
      color="primary">
      Next
    </v-btn>
  </div>
</template>

<script>
import {mapActions, mapGetters} from 'vuex';

export default {
  name: 'AssignmentTreatmentSelect',
  props: ['experiment'],
  computed: {
    ...mapGetters({
      assignment: 'assignment/assignment',
      conditions: 'experiment/conditions',
    }),
    assignment_id() {
      return parseInt(this.$route.params.assignment_id)
    },
  },
  methods: {
    ...mapActions({
      createTreatment: 'treatment/createTreatment',
      createAssessment: 'assessment/createAssessment',
      fetchAssignment: 'assignment/fetchAssignment',
    }),
    async handleCreateTreatment(conditionId) {
      // POST TREATMENT
      try {
        return await this.createTreatment([
          this.experiment.experimentId,
          conditionId,
          this.assignment_id,
        ])
      } catch (error) {
        console.error("handleCreateTreatment | catch", {error})
      }
    },
    async handleCreateAssessment(conditionId, treatment) {
      console.log(treatment)
      // POST ASSESSMENT TITLE & HTML (description)
      try {
        return await this.createAssessment([
          this.experiment.experimentId,
          conditionId,
          treatment.treatmentId
        ])
      } catch (error) {
        console.error("handleCreateAssessment | catch", {error})
      }
    },
    async goToBuilder(conditionId) {
      // create the treatment
      const treatment = await this.handleCreateTreatment(conditionId)
      // create the assessment
      const assessment = await this.handleCreateAssessment(conditionId, treatment?.data)

      // show an alert if there's a problem creating the treatment or assessment
      if (!treatment || !assessment) {
        alert('There was a problem creating your assessment')
        return false
      }

      // send user to builder with the treatment and assessment ids
      this.$router.push({
        name: 'TerracottaBuilder',
        params: {
          experiment_id: this.experiment.experimentId,
          condition_id: conditionId,
          treatment_id: treatment?.data?.treatmentId,
          assessment_id: assessment?.data?.assessmentId
        },
      });
    },
    saveExit() {
      this.$router.push({name:'Home', params:{experiment: this.experiment.experimentId}})
    }
  },
  created() {
    // this.fetchAssignment([this.experiment.experimentId, exposure_id, this.assignment_id])
  },
};
</script>
