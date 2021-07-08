<template>
  <div>
    <h1>
      In this section, you'll set up your experiment assignments
    </h1>
    <p>
      Terracotta will populate Canvas assignments with learning activities and
      materials that change depending on who's looking at them, automatically
      managing experimental variation within the buckets.
    </p>
    <p>
      All you need to do is create your assignments and specify which treatments
      will be contained with each assignment. From your student's perspective,
      they'll be completing assignments as normal within Canvas, with no outward
      appearance that the assignment is different from any other assignment.
    </p>
    <v-btn
      elevation="0"
      color="primary"
      :to="{ name: 'AssignmentExposureSets' }"
      >Continue</v-btn
    >
  </div>
</template>

<script>
export default {
  name: "AssignmentIntro",
  props: ['experiment'],
  methods: {
    saveExit() {
      this.$router.push({name:'Home', params:{experiment: this.experiment.experiment_id}})
    }
  }
};
</script>
