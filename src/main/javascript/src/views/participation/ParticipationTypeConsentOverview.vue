<template>
  <div>
    <h1 class="mb-5">
      Because you would like
      <strong>
        students to be invited to agree to participate in study within
        {{ lmsTitle }}
      </strong>,
      we'll need to create a consent assignment.
    </h1>

    <v-btn
      elevation="0"
      color="primary"
      class="mb-4"
      @click="setConsent"
    >
      Continue
    </v-btn>
  </div>
</template>

<script setup>
import { computed } from "vue";
import { useRouter } from "vue-router";

import { navigation as navigationModule } from "@/store/navigation.module";
import { configuration as configurationModule } from "@/store/configuration.module";

defineOptions({
  name: "ParticipationTypeConsentOverview"
});

const props = defineProps({
  experiment: {
    type: Object,
    required: true
  }
});

const router = useRouter();

const navigationStore = navigationModule();
const configurationStore = configurationModule();

const editMode = computed(() => {
  return navigationStore.editMode;
});

const configurations = computed(() => {
  return configurationStore.configurations;
});

const getSaveExitPage = computed(() => {
  return editMode.value?.callerPage?.name || "Home";
});

const lmsTitle = computed(() => {
  return configurations.value?.lmsTitle || "LMS";
});

const setConsent = () => {
  router.push({
    name: "ParticipationTypeConsentTitle",
    params: {
      experiment: props.experiment.experimentId
    }
  });
};

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
