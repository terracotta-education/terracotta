<template>
  <div>
    <h1>
      Add your treatment for
      Assignment 1’s condition: <strong>Biology of skin color</strong>
    </h1>
  </div>
</template>

<script>
export default {
  name: 'TerracottaBuilder',
  props: ['experiment'],
  methods: {
    saveExit() {
      this.$router.push({name:'Home', params:{experiment: this.experiment.experiment_id}})
    }
  }
};
</script>
