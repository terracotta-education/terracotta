<template>
  <div>
    <h1 class="mb-5">
      Because you have <strong>{{ this.numExposureSets }} exposure sets</strong>,
      you’ll need to include a minimum of <strong>{{ this.numExposureSets }} assignments</strong> in this experiment,
      one for each set.
      <br><br>
      Let’s create your assignments now.
    </h1>

    <v-btn
      elevation="0"
      color="primary"
      :to="{ name: 'AssignmentCreateAssignment' }"
      >Continue</v-btn
    >

    <v-card
      class="mt-10 pt-5 px-5 mx-auto blue lighten-5 rounded-lg"
      outlined
    >
      <p><strong>Note:</strong> If you want to include more assignments, you should try to have the same number of assignments in each exposure set for a balanced experiment. Once we've got everything setup, you'll have an opportunity to specify experimental outcomes (such as test performance, attendance, or other data) separately for each of the exposures. </p>
    </v-card>
  </div>
</template>

<script>
export default {
  name: "AssignmentExposureSetsIntro",
  props: ['experiment'],
  computed: {
    numExposureSets() {
      return this.$route.params.numberOfExperimentSets;
    }
  },
  methods: {
    saveExit() {
      this.$router.push({name:'Home', params:{experiment: this.experiment.experimentId}})
    }
  }
};
</script>
