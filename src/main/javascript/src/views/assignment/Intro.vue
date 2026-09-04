<template>
  <div>
    <h1>
      In this section, you'll set up your experiment assignments
    </h1>

    <p>
      Terracotta will populate {{ lmsTitle }} assignments with learning
      activities and materials that change depending on who's looking at them,
      automatically managing experimental variation within the buckets.
    </p>

    <p>
      All you need to do is create your assignments and specify which
      treatments will be contained within each assignment. From your students'
      perspective, they'll be completing assignments as normal within
      {{ lmsTitle }}, with no outward appearance that the assignment is
      different from any other assignment.
    </p>

    <v-btn
      :to="{ name: 'AssignmentExposureSets' }"
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

import { configuration as configurationModule } from "@/store/configuration.module";

defineOptions({
  name: "AssignmentIntro"
});

const props = defineProps({
  experiment: {
    type: Object,
    required: true
  }
});

const router = useRouter();

const configurationStore = configurationModule();

const configurations = computed(() => {
  return configurationStore.configurations;
});

const lmsTitle = computed(() => {
  return configurations.value?.lmsTitle || "LMS";
});

const saveExit = () => {
  router.push({
    name: "Home",
    params: {
      experiment: props.experiment.experimentId
    }
  });
};

defineExpose({
  saveExit
});
</script>
