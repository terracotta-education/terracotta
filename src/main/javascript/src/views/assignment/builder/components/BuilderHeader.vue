<template>
  <div class="header-container">
    <h1>{{ assignmentTitle }}</h1>

    <div v-if="!hasSingleTreatment">
      <h4 class="label-treatment">
        Treatment
      </h4>

      <v-chip
        :color="conditionColor"
        variant="flat"
        density="compact"
        label
      >
          {{ conditionName }}
      </v-chip>
    </div>
  </div>
</template>

<script setup>
defineOptions({
  name: "BuilderHeader"
});

defineProps({
  assignmentTitle: {
    type: String,
    required: true
  },
  conditionName: {
    type: String,
    required: true
  },
  conditionColor: {
    type: String,
    default: null
  },
  hasSingleTreatment: {
    type: Boolean,
    default: false
  }
});
</script>

<style scoped lang="scss">
.header-container {
  width: 100%;
  min-height: fit-content;
  padding-bottom: 10px;
}

h4.label-treatment,
h4.label-condition-name {
  display: inline;
  padding-right: 5px;
  padding-bottom: 0;
}

span.v-chip.v-chip--label,
span.v-chip.v-chip--label > .v-chip__content {
  min-height: fit-content !important;
  height: unset !important;
  max-width: 400px !important;
}
</style>
