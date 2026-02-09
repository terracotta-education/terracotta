<template>
<div>
  <h1>Let's set up your experiment</h1>
  <p>In this section, we'll outline the basic design of your experiment.  Once it is created, you can then select who will participate, and add components to your study.</p>
  <v-btn
    :to="{name: 'ExperimentDesignTitle'}"
    elevation="0"
    color="primary"
  >
    Continue
  </v-btn>
</div>
</template>

<script>
  export default {
    name: "DesignIntro",
    props: {
      experiment: {
        type: Object,
        required: true
      }
    },
    methods: {
      saveExit() {
        this.$router.push({
          name:"Home",
          params: {
            experimentId: this.experiment.experimentId
          }
        });
      }
    }
  }
</script>
