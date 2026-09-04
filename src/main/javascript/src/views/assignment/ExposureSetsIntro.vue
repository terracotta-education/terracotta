<template>
  <div>
    <h1 class="mb-5">
      Because you have
      <strong>{{ numExposureSets }} exposure sets</strong>,
      you'll need to include a minimum of
      <strong>{{ numExposureSets }} assignments</strong>
      in this experiment, one for each set.

      <br />
      <br />

      Let's create your assignments now.
    </h1>

    <v-btn
      :to="{
        name: 'AssignmentYourAssignments',
        params: {
          exposureId
        }
      }"
      elevation="0"
      color="primary"
    >
      Continue
    </v-btn>

    <v-card
      class="mt-10 pt-5 px-5 mx-auto bg-blue-lighten-5 rounded-lg"
      variant="outlined"
    >
      <p>
        <strong>Note:</strong>
        If you want to include more assignments, you should try to have the same
        number of assignments in each exposure set for a balanced experiment.
        Once we've got everything setup, you'll have an opportunity to specify
        experimental outcomes, such as test performance, attendance, or other
        data, separately for each of the exposures.
      </p>
    </v-card>
  </div>
</template>

<script setup>
import { computed } from "vue";

import {
  useRoute,
  useRouter
} from "vue-router";

defineOptions({
  name: "AssignmentExposureSetsIntro"
});

defineProps({
  experiment: {
    type: Object,
    required: true
  }
});

const route = useRoute();
const router = useRouter();

const numExposureSets = computed(() => {
  return route.params.numberOfExperimentSets;
});

const exposureId = computed(() => {
  return route.params.exposureId;
});

const saveExit = () => {
  router.push({
    name: "Home"
  });
};

defineExpose({
  saveExit
});
</script>
