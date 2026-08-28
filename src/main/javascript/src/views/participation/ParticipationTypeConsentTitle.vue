<template>
  <div>
    <h1>Create a title for your consent assignment</h1>

    <p>
      This will create an <strong>unpublished consent assignment</strong> in
      {{ lmsTitle }} and will be the way your students will read, review and
      sign your study's informed consent. The consent assignment will be a
      prerequisite for your first study treatment assignments.
    </p>

    <form
      v-if="experiment && consent"
      class="my-5"
      @submit.prevent="saveTitle('ParticipationTypeConsentFile')"
    >
      <v-text-field
        v-model="title"
        :rules="rules"
        label="Assignment title"
        placeholder="e.g. Lorem ipsum"
        autofocus
        variant="outlined"
        required
      />

      <v-btn
        :disabled="!title || !title.trim() || title.length > 255"
        elevation="0"
        color="primary"
        class="mr-4"
        type="submit"
      >
        Next
      </v-btn>
    </form>

    <v-card
      class="mt-10 pt-5 px-5 mx-auto rounded-lg note-card"
      variant="flat"
    >
      <p>
        <strong>Note:</strong>
        Once the assignment is created, you won't be able to change the title.
      </p>
    </v-card>
  </div>
</template>

<script setup>
import { ref, computed } from "vue";
import { useRouter } from "vue-router";

import { consent as consentModule } from "@/store/consent.module";
import { navigation as navigationModule } from "@/store/navigation.module";
import { configuration as configurationModule } from "@/store/configuration.module";

defineOptions({
  name: "ParticipationTypeConsentTitle"
});

const props = defineProps({
  experiment: {
    type: Object,
    required: true
  }
});

const router = useRouter();

const consentStore = consentModule();
const navigationStore = navigationModule();
const configurationStore = configurationModule();

const titleProxy = ref("");

const rules = [
  value => value && !!value.trim() || "Title is required",
  value => (value || "").length <= 255 || "A maximum of 255 characters is allowed"
];

const consent = computed(() => {
  return consentStore.consent;
});

const editMode = computed(() => {
  return navigationStore.editMode;
});

const configurations = computed(() => {
  return configurationStore.configurations;
});

const getSaveExitPage = computed(() => {
  return editMode.value?.callerPage?.name || "Home";
});

const title = computed({
  get() {
    if (titleProxy.value !== "") {
      return titleProxy.value;
    }

    return props.experiment?.consent?.title || consent.value?.title || "";
  },

  set(value) {
    titleProxy.value = value;
    consentStore.setConsentTitle(value);
  }
});

const lmsTitle = computed(() => {
  return configurations.value?.lmsTitle || "LMS";
});

const saveTitle = path => {
  router.push({
    name: path,
    params: {
      experiment: props.experiment.experimentId
    }
  });
};

const saveExit = () => {
  saveTitle(getSaveExitPage.value);
};

defineExpose({
  saveExit
});
</script>

<style lang="scss" scoped>
// bg-blue-lighten-5 (a Vuetify color utility class) never generates any CSS in this
// project's build - vite-plugin-vuetify's configFile-based recompile doesn't reach the
// $color-pack-gated utility classes - so set the background directly instead. Same
// gotcha/fix as ExperimentType.vue's .card-warning.
.note-card {
  background-color: map.get($blue, "lighten-5");
}
</style>
