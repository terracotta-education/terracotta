<template>
  <div>
    <h1>Create a title for your experiment</h1>

    <form
      v-if="experiment"
      class="my-5"
      @submit.prevent="saveTitle('ExperimentDesignDescription')"
    >
      <v-text-field
        v-model="experimentStore.experiment.title"
        :rules="rules"
        label="Experiment title"
        placeholder="e.g. Lorem ipsum"
        variant="outlined"
        required
      />

      <v-btn
        v-if="!editMode"
        :disabled="!!titleError"
        elevation="0"
        color="primary"
        class="mr-4"
        type="submit"
      >
        Next
      </v-btn>
    </form>
  </div>
</template>

<script setup>
import { computed } from "vue";
import { useRouter } from "vue-router";
import Swal from "sweetalert2";

import { experiment as experimentModule } from "@/store/experiment.module";
import { navigation as navigationModule } from "@/store/navigation.module";

defineOptions({
  name: "DesignTitle"
});

const props = defineProps({
  experiment: {
    type: Object,
    required: true
  }
});

const router = useRouter();

const experimentStore = experimentModule();
const navigationStore = navigationModule();

const editMode = computed(() => {
  return navigationStore.editMode;
});

const getSaveExitPage = computed(() => {
  return editMode.value?.callerPage?.name || "Home";
});

const rules = [
  value => value && !!value.trim() || "Title is required",
  value => (value || "").length <= 255 || "A maximum of 255 characters is allowed"
];

// the first rule the title fails, or null when it passes them all
const titleError = computed(() => {
  const failure = rules
    .map(rule => rule(props.experiment.title))
    .find(result => result !== true);

  return failure || null;
});

const saveTitle = async path => {
  // Save & Exit, and Enter in edit mode (no Next button to disable), would otherwise send a
  // title the server rejects
  if (titleError.value) {
    await Swal.fire(titleError.value);
    return;
  }

  try {
    const response = await experimentStore.updateExperiment(props.experiment);

    if (response?.status === 200) {
      router.push({
        name: path,
        params: {
          experiment: props.experiment.experimentId
        }
      });

      return;
    }

    if (response?.message) {
      await Swal.fire(`Error: ${response.message}`);
      return;
    }

    await Swal.fire("There was an error saving your experiment.");
  } catch {
    await Swal.fire("There was an error saving your experiment.");
  }
};

const saveExit = () => {
  saveTitle(getSaveExitPage.value);
};

defineExpose({
  saveExit
});
</script>
