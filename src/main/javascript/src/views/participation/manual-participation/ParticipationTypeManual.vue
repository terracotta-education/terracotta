<template>
  <div>
    <h1 class="my-3">
      Because you would like to
      <strong>
        manually determine who can participate in the experiment
      </strong>,
      we've set you up to select students who are enrolled in your class.
    </h1>

    <v-card
      class="pt-5 px-5 mx-auto bg-blue-lighten-5 rounded-lg"
      variant="outlined"
    >
      <p>
        <strong>Tip:</strong>
        If you are working with minors, we suggest you collect parental consents
        before proceeding. You can
        <a
          :href="parentalPermissionFileUrl"
          download="Terracotta_ParentalPermission_template.docx"
          class="link-download-template"
        >download a permission template here</a>. <!-- don't format this -->
      </p>
    </v-card>

    <div class="mt-5">
      <v-btn
        :to="{ name: 'ParticipationTypeManualSelection' }"
        elevation="0"
        color="primary"
      >
        Continue
      </v-btn>

      <router-link
        :to="{ name: nextPage('ParticipationDistribution') }"
        class="plain-link ml-3"
      >
        Skip participant selection for now
      </router-link>
    </div>
  </div>
</template>

<script setup>
import { computed } from "vue";
import { useRouter } from "vue-router";

import { configuration as configurationModule } from "@/store/configuration.module";
import { navigation as navigationModule } from "@/store/navigation.module";

defineOptions({
  name: "ParticipationTypeManual"
});

const props = defineProps({
  experiment: {
    type: Object,
    required: true
  }
});

const router = useRouter();

const configurationStore = configurationModule();
const navigationStore = navigationModule();

const configurations = computed(() => configurationStore.get);

const editMode = computed(() => {
  return navigationStore.editMode;
});

const saveExitPage = computed(() => {
  return editMode.value?.callerPage?.name || "Home";
});

const conditions = computed(() => {
  return props.experiment.conditions || [];
});

const singleConditionExperiment = computed(() => {
  return conditions.value.length === 1;
});

const parentalPermissionFileUrl = computed(() => {
  return configurations.value?.parentalPermissionTemplateUrl;
});

const nextPage = toPage => {
  if (singleConditionExperiment.value) {
    return "ParticipationSummary";
  }

  return toPage;
};

const saveExit = () => {
  router.push({
    name: saveExitPage.value,
    params: {
      experiment: props.experiment.experimentId
    }
  });
};

defineExpose({
  saveExit
});
</script>

<style lang="scss" scoped>
a.link-download-template {
  color: map.get($blue, "base");
}
</style>
