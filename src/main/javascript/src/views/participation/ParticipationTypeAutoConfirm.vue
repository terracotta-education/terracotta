<template>
  <div>
    <v-card
      class="mb-8 pt-5 px-5 mx-auto bg-red-lighten-5 rounded-lg"
      variant="outlined"
    >
      <p>
        <strong>
          Are you sure you want to include all students in your experiment
          automatically?
        </strong>
      </p>

      <p>
        One of the basic principles of ethical research is showing respect for
        research participants. One way of showing this respect is by providing
        people an opportunity to make decisions for themselves about whether
        they want to participate in a study.
      </p>

      <p>
        Terracotta is designed to make this process easy. If you want, we can
        create a short assignment where your students will provide consent to be
        included in this experiment.
      </p>
    </v-card>

    <v-btn
      :to="{
        name: nextPage('ParticipationDistribution')
      }"
      elevation="0"
      color="primary"
      class="mb-4"
    >
      Yes, I want to proceed
    </v-btn>

    <br />

    <v-btn
      variant="outlined"
      class="consent-btn"
      color="primary"
      elevation="0"
      @click="goToConsentPage"
    >
      No, I want to create a consent assignment instead
    </v-btn>

    <br />
  </div>
</template>

<script setup>
import { computed } from "vue";
import { useRouter } from "vue-router";

import { experiment as experimentModule } from "@/store/experiment.module";
import { navigation as navigationModule } from "@/store/navigation.module";

defineOptions({
  name: "ParticipationTypeAutoConfirm"
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

const saveExitPage = computed(() => {
  return editMode.value?.callerPage?.name || "Home";
});

const conditions = computed(() => {
  return props.experiment.conditions || [];
});

const singleConditionExperiment = computed(() => {
  return conditions.value.length === 1;
});

const nextPage = toPage => {
  if (singleConditionExperiment.value) {
    return "ParticipationSummary";
  }

  return toPage;
};

const goToConsentPage = async () => {
  const updatedExperiment = {
    ...props.experiment,
    participationType: "CONSENT"
  };

  const response = await experimentStore.updateExperiment(updatedExperiment);

  if (response?.status === 200) {
    router.push({
      name: "ParticipationTypeConsentOverview",
      params: {
        experiment: props.experiment.experimentId
      }
    });
  }
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
// bg-red-lighten-5 (a Vuetify color utility class) generates no CSS in this project's
// build - see ExperimentType.vue's .card-warning for the full explanation. The project's
// own $red map has no "lighten-5"-equivalent pale variant (its "base" is used below,
// as the border color, not the fill), so use Vuetify's literal palette value directly.
.v-card.bg-red-lighten-5 {
  background-color: #ffebee;
  border-color: map.get($red, "base") !important;
}

.consent-btn {
  border: none;
  padding: 0 !important;
  max-width: 100%;
  height: auto !important;

  // Vuetify's default .v-btn__content is white-space: nowrap, which keeps this
  // button's long label on one line and forces the button (and the page) wider
  // than the viewport on narrow screens - let it wrap instead.
  :deep(.v-btn__content) {
    white-space: normal;
  }
}
</style>
