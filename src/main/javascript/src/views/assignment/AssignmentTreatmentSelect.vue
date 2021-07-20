<template>
  <div v-if="assignment">
    <h1 class="pa-0 mb-7">Now, let’s upload your treatments for each condition for <strong>{{ assignment.title }}</strong></h1>

    <template v-if="conditions">
      <v-expansion-panels class="v-expansion-panels--outlined mb-7" flat>
        <v-expansion-panel class="py-3">
          <v-expansion-panel-header>{{ assignment.title }} ({{ tCount }}/{{ conditions.length }})</v-expansion-panel-header>
          <v-expansion-panel-content>
            <v-list class="pa-0">

              <v-list-item class="justify-center px-0"
                           v-for="condition in conditions"
                           :key="condition.conditionId">
                <v-list-item-content>
                  <p class="ma-0 pa-0">{{ condition.name }}</p>
                </v-list-item-content>

                <v-list-item-action>
                  <template v-if="checkConditionTreatment(condition)">
                    <v-btn
                      icon
                      outlined
                      text
                      tile
                      @click="goToBuilder(condition.conditionId)"
                    >
                      <v-icon>mdi-pencil</v-icon>
                    </v-btn>
                  </template>
                  <template v-else>
                    <v-btn
                      color="primary"
                      outlined
                      @click="goToBuilder(condition.conditionId)"
                    >Select</v-btn>
                  </template>
                </v-list-item-action>
              </v-list-item>

            </v-list>
          </v-expansion-panel-content>
        </v-expansion-panel>
      </v-expansion-panels>
    </template>
    <template v-else>
      <p>no conditions</p>
    </template>
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
    exposure_id() {
      return parseInt(this.$route.params.exposure_id)
    },
  },
  data() {
    return {
      tCount: 0,
      conditionTreatments: []
    }
  },
  methods: {
    ...mapActions({
      createTreatment: 'treatment/createTreatment',
      createAssessment: 'assessment/createAssessment',
      fetchAssignment: 'assignment/fetchAssignment',
      checkTreatment: 'treatment/checkTreatment',
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
    checkConditionTreatment(condition) {
      // if condition has treatment, return boolean for template
      return !!this.conditionTreatments.find(conditionTreatment => {
        return conditionTreatment.conditionId === condition.conditionId && conditionTreatment.hasTreatment
      })
    },
    async hasTreatment(condition) {
      // check if condition has treatment, return boolean
      const t = await this.checkTreatment([this.experiment.experimentId, condition.conditionId, this.assignment_id])
      return t?.status === 200;
    },
    async checkConditionTreatments() {
      // loop conditions and build condition/treatment manifest
      // (templates don't like async methods for conditions)
      for (let c of this.conditions) {
        const t = await this.hasTreatment(c)
        const ctObj = {
          conditionId: c.conditionId,
          hasTreatment: t
        }
        this.conditionTreatments = [...this.conditionTreatments.filter((o) => o.conditionId !== ctObj.conditionId), {...ctObj}];
      }
    },
    async treatmentCount() {
      // count treatments for assignment
      // computed properties don't like async/await so we'll call it in created()
      this.tCount = 0

      for (const c of this.conditions) {
        const treatments = await this.checkTreatment([this.experiment.experimentId, c.conditionId, this.assignment_id])
        this.tCount = (treatments?.data?.length>0) ? this.tCount+1 : this.tCount
      }
    },
    saveExit() {
      this.$router.push({name:'Home'})
    }
  },
  async created() {
    await this.fetchAssignment([this.experiment.experimentId, this.exposure_id, this.assignment_id])
    this.treatmentCount()
    await this.checkConditionTreatments()
  },
};
</script>