<template>
  <div>
    <h1>
      In this section, you'll decide how students in your class become
      participants in your experiment
    </h1>

    <p>
      Terracotta can allow students to provide consent to participate, and can
      also allow you the opportunity to manually indicate who has and hasn't
      agreed to participate.
    </p>

    <v-btn
      :to="{ name: 'ExperimentParticipationSelectionMethod' }"
      elevation="0"
      color="primary"
    >
      Continue
    </v-btn>
  </div>
</template>

<script setup>
import { computed } from "vue";
import { useRouter } from "vue-router";

import { navigation as navigationModule } from "@/store/navigation.module";

defineOptions({
  name: "ParticipationIntro"
});

const props = defineProps({
  experiment: {
    type: Object,
    required: true
  }
});

const router = useRouter();

const navigationStore = navigationModule();

const editMode = computed(() => {
  return navigationStore.editMode;
});

const getSaveExitPage = computed(() => {
  return editMode.value?.callerPage?.name || "Home";
});

const saveExit = () => {
  router.push({
    name: getSaveExitPage.value,
    params: {
      experiment: props.experiment.experimentId
    }
  });
};

defineExpose({
  saveExit
});
</script>
