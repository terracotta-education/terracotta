<template>
  <div>
    <h1>Create your component</h1>
    <p>This will create an unpublished component shell in Canvas and will be the way Terracotta will deliver treatments to students.</p>
    <v-row>
      <div class="col-6">
        <v-text-field
          v-model="assignment.title"
          label="Component name"
          :rules="rules"
          outlined
        >
        </v-text-field>
      </div>
    </v-row>
    <v-divider></v-divider>
    <v-tabs
      v-model="tab"
      class="tabs"
    >
      <v-tab>Settings</v-tab>
    </v-tabs>
    <v-divider></v-divider>
    <v-tabs-items v-model="tab">
      <v-tab-item class="my-5">
          <assignment-settings />
      </v-tab-item>
    </v-tabs-items>
  </div>
</template>

<script>
import { mapGetters, mapActions, mapMutations } from "vuex";
import AssignmentSettings from './AssignmentSettings.vue';

export default {
  name: "CreateAssignment",
  components: {
    AssignmentSettings,
  },
  data() {
    return {
      tab: null,
      rules: [
        v => v && !!v.trim() || 'Component Name is required',
        v => (v || '').length <= 255 || 'A maximum of 255 characters is allowed'
      ],
    };
  },
  computed: {
    ...mapGetters({
      assignment: "assignment/assignment",
    }),
    experimentId() {
      return parseInt(this.$route.params.experimentId);
    },
    exposureId() {
      return parseInt(this.$route.params.exposureId);
    },
    conditionIds() {
      return JSON.parse(this.$route.params.conditionIds);
    },
    contDisabled() {
      return !this.assignment.title;
    }
  },
  methods: {
    async saveExit() {
      this.handleSaveAssignment();
    },
    ...mapActions({
      createAssignment: 'assignment/createAssignment',
      createTreatment: 'treatment/createTreatment',
      createAssessment: 'assessment/createAssessment'
    }),
    ...mapMutations({
      setAssignment: "assignment/setAssignment",
    }),
    async handleSaveAssignment() {
      // POST ASSESSMENT TITLE & HTML (description) & SETTINGS
      try {
        const response = await this.createAssignment([this.experimentId, this.exposureId, this.assignment, 1]);
        await this.handleCreateTreatmentsForAssignment(this.experimentId, response.data.assignmentId);

        if (response?.status === 201) {
          this.$router.push({
            name: 'ExperimentSummary',
            params: {
              experimentId: this.experimentId,
              assignmentId: response.data.assignmentId
            }
          })
        } else {
          this.$swal(`${response}`)
        }
      } catch (error) {
        console.error("createAssignment | catch", {error})
        this.$swal('There was an error creating the assignment.')
      }
    },
    async handleCreateTreatmentsForAssignment(experimentId, assignmentId) {
      // create a treatment for each condition
      const treatmentRequests = [];

      this.conditionIds.forEach(conditionId => {
        treatmentRequests.push(this.createTreatment([experimentId, conditionId, assignmentId]));
      });

      const allTreatmentCreateRequests = Promise.all(treatmentRequests);

      try {
        const treatments = await allTreatmentCreateRequests;

        const assessmentRequests = [];

        treatments.forEach(treatment => {
          assessmentRequests.push(this.createAssessmentForTreatment(treatment.data.conditionId, treatment.data.treatmentId));
        });

        const allAssessmentCreateRequests = Promise.all(assessmentRequests);

        await allAssessmentCreateRequests;
      } catch (error) {
          console.log("CreateAssignment.handleCreateTreatmentsForAssignment | catch", error);
      }


    },
    async createAssessmentForTreatment(conditionId, treatmentId) {
      // POST ASSESSMENT TITLE & HTML (description)
      try {
        return await this.createAssessment([
          this.experimentId,
          conditionId,
          treatmentId,
        ]);
      } catch (error) {
        console.error("handleCreateAssessment | catch", { error });
      }
    }
  },
  async created () {
    this.setAssignment({
      numOfSubmissions: null
    });
  }
};
</script>
