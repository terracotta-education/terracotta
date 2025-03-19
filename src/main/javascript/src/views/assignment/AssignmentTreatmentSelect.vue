<template>
  <div v-if="assignment">
    <h1 class="pa-0 mb-7">Now, let’s create different versions of <strong>{{
        assignment.title
      }}</strong> for each condition</h1>

    <template v-if="conditions">
      <v-expansion-panels class="v-expansion-panels--outlined mb-7" flat>
        <v-expansion-panel class="py-3">
          <v-expansion-panel-header>{{ assignment.title }} ({{ assignment.treatments && assignment.treatments.length || 0 }}/{{ conditions.length }})
          </v-expansion-panel-header>
          <v-expansion-panel-content>
            <v-list class="pa-0">

              <v-list-item class="justify-center px-0"
                           v-for="condition in conditions"
                           :key="condition.conditionId">
                <v-list-item-content>
                  <p class="ma-0 pa-0">{{ condition.name }}</p>
                </v-list-item-content>

                <v-list-item-action>
                  <template v-if="hasTreatment(condition)">
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
                    >Create
                    </v-btn>
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
    assignmentId() {
      return parseInt(this.$route.params.assignmentId)
    },
    exposureId() {
      return parseInt(this.$route.params.exposureId)
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
          this.assignmentId,
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

      if (![200, 201].includes(treatment.status)) {
        this.$swal(
          `There was a problem creating your treatment: ${treatment.data}`
        );
        return false;
      }

      // create the assessment
      const assessment = await this.handleCreateAssessment(conditionId, treatment?.data)

      if (![200, 201].includes(assessment.status)) {
        this.$swal(
          `There was a problem creating your assessment: ${assessment.data}`
        );
        return false;
      }

      // // send user to builder with the treatment and assessment ids
      this.$router.push({
        name: 'TerracottaBuilder',
        params: {
          experimentId: this.experiment.experimentId,
          conditionId: conditionId,
          treatmentId: treatment?.data?.treatmentId,
          assessmentId: assessment?.data?.assessmentId
        },
      });
    },
    hasTreatment(condition) {
      // if condition has treatment, return boolean for template
      return !!this.conditionTreatments.find(conditionTreatment => {
        return conditionTreatment.treatment &&
          conditionTreatment.condition.conditionId === condition.conditionId &&
          conditionTreatment.treatment.assignmentId === this.assignmentId
      })
    },
    async checkConditionTreatments() {
      // loop conditions and build condition/treatment manifest
      // (templates don't like async methods for conditions)
      for (let c of this.conditions) {
        const t = await this.checkTreatment([this.experiment.experimentId, c.conditionId])

        if (t?.data?.find(o=>parseInt(o.assignmentId)===this.assignmentId)) {
          const ctObj = {
            treatment: t.data ? t.data.find(o=>parseInt(o.assignmentId)===this.assignmentId) : null,
            condition: c
          }

          this.conditionTreatments = [
            ...this.conditionTreatments.filter((o) =>
              o.conditionId === ctObj.conditionId &&
              o.treatment.assignmentId === this.assignmentId
            ),
            {...ctObj}
          ];
        }
      }
    },
    saveExit() {
      this.$router.push({name: 'Home'})
    }
  },
  async created() {
    await this.fetchAssignment([this.experiment.experimentId, this.exposureId, this.assignmentId])
    await this.checkConditionTreatments()
  },
};
</script>
