<template>
  <div class="experiment-outcome">
    <nav>
      <RouterLink
        v-if="previousStep"
        :to="backTo"
        :aria-disabled="isSaving"
        :class="{ disabled: isSaving }"
      >
        <v-icon>mdi-chevron-left</v-icon>
        Back
      </RouterLink>

      <v-btn
        :disabled="isSaving"
        color="primary"
        elevation="0"
        class="save-button"
        @click="handleSaveClick"
      >
        {{ stepActionText || "SAVE & EXIT" }}
      </v-btn>
    </nav>

    <article class="experiment-outcome__body">
      <v-row>
        <v-col cols="12">
          <RouterView v-slot="{ Component }">
            <component
              v-if="Component"
              :is="Component"
              ref="childComponent"
              :key="route.fullPath"
            />
          </RouterView>
        </v-col>
      </v-row>
    </article>
  </div>
</template>

<script setup>
import {
  ref,
  computed,
  onMounted
} from "vue";

import {
  useRoute,
  onBeforeRouteUpdate,
  RouterLink,
  RouterView
} from "vue-router";

import { experiment as experimentModule } from "@/store/experiment.module";

defineOptions({
  name: "ExperimentOutcome"
});

const route = useRoute();

const experimentStore = experimentModule();

const childComponent = ref(null);
const saveButtonClicked = ref(false);

const isSaving = computed(() => {
  return saveButtonClicked.value;
});

const previousStep = computed(() => {
  return route.meta.previousStep;
});

const stepActionText = computed(() => {
  return route.meta.stepActionText;
});

const backTo = computed(() => {
  if (isSaving.value) {
    return "";
  }

  return {
    name: previousStep.value
  };
});

const fetchExperiment = async experimentId => {
  await experimentStore.fetchExperimentById(experimentId);
};

const handleSaveClick = async () => {
  saveButtonClicked.value = true;

  await childComponent.value?.saveExit?.();

  saveButtonClicked.value = false;
};

onMounted(async () => {
  await fetchExperiment(route.params.experimentId);
});

onBeforeRouteUpdate(async to => {
  await fetchExperiment(to.params.experimentId);
});
</script>

<style lang="scss" scoped>
.experiment-outcome {
  min-height: 100vh;
  display: flex;
  flex-direction: column;

  > nav {
    flex-shrink: 0;
    padding: 30px;
    display: flex;
    flex-wrap: wrap;
    row-gap: 16px;
    justify-content: space-between;

    a {
      text-decoration: none;
      color: map.get($blue, "primary");

      * {
        vertical-align: sub;
        color: map.get($blue, "primary");
      }

      &.disabled {
        pointer-events: none;
        opacity: 0.6;
      }
    }

    & .save-button {
      background: none !important;
      border: none;
      padding: 0 !important;
      color: map.get($blue, "primary") !important;
      cursor: pointer;
    }
  }

  > article {
    flex: 1;
    padding: 0 30px;
  }
}
</style>
