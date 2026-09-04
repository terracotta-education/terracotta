<template>
  <div>
    <h1>Describe your experiment</h1>

    <p>
      Use this space to write down some notes about your research question, why this question is meaningful, and your hypothesis.
    </p>

    <form
      v-if="experiment"
      class="my-5 mb-15"
      @submit.prevent="saveExperiment('ExperimentDesignConditions')"
    >
      <v-textarea
        v-model="experimentStore.experiment.description"
        :rules="requiredText"
        label="Experiment description"
        placeholder="e.g. Lorem ipsum"
        variant="outlined"
        required
      />

      <v-btn
        v-if="!editMode"
        :disabled="!experiment.description || !experiment.description.trim()"
        elevation="0"
        color="primary"
        class="mr-4"
        type="submit"
      >
        Next
      </v-btn>
    </form>

    <h4 class="mb-3">
      Examples
    </h4>

    <v-carousel
      v-model="slide"
      height="auto"
      hide-delimiters
    >
      <v-carousel-item
        v-for="(blurb, index) in blurbs"
        :key="index"
      >
        <v-card
          class="pt-5 px-5 mx-auto"
          variant="outlined"
        >
          <v-card-text
            class="mx-auto"
          >
            <p>{{ blurb }}</p>
          </v-card-text>
        </v-card>
      </v-carousel-item>
    </v-carousel>
  </div>
</template>

<script setup>
import { ref, computed } from "vue";
import { useRouter } from "vue-router";
import Swal from "sweetalert2";

import { experiment as experimentModule } from "@/store/experiment.module";
import { navigation as navigationModule } from "@/store/navigation.module";

defineOptions({
  name: "DesignDescription"
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

const slide = ref(0);

const requiredText = [
  value => value && !!value.trim() || "Description is required"
];

const blurbs = [
  "The study looks to test whether exposure to ones' own lexile level (and seeing how it improves over time) might improve students' learning outcomes.",
  "The study explores how learning about the biology of skin color (vs. social construction of race) impacts students' conception of race. It is important for science teachers to understand the impacts of teaching about skin color biology and how it may impact students' concepts of race.",
  "The purpose of this study is to evaluate the benefits of explicitly including student learning outcomes in the assignment description.  The TILT movement hypothesizes that this will help students be in better control of their learning and will reduce inequity.",
  `This study will test whether presenting assessment questions before students watch an instructional video ("pre-questions") will improve learning outcomes, compared with presenting the same questions after students watch the video.`,
  "This experiment tests whether multiple-choice questions can improve critical thinking performance in Introductory Psychology.  Some students will categorize critical thinking scenarios, while others will answer conventional practice questions about brain structures and psychological functions.",
  "In this study, students will see worked examples of math problems, and will then solve similar problems.  Some of the worked examples will include a common mistake along with a correction, and other worked examples will be entirely correct.  We will examine how exposure to mistakes affects student performance."
];

const editMode = computed(() => {
  return navigationStore.editMode;
});

const getSaveExitPage = computed(() => {
  return editMode.value?.callerPage?.name || "Home";
});

const saveExperiment = async path => {
  try {
    const response = await experimentStore.updateExperiment(props.experiment);

    if (response?.status === 200) {
      router.push({
        name: path,
        params: {
          experimentId: props.experiment.experimentId
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
    await Swal.fire("There was an error saving the experiment.");
  }
};

const saveExit = () => {
  saveExperiment(getSaveExitPage.value);
};

defineExpose({
  saveExit
});
</script>

<style lang="scss" scoped>
:deep(.v-card-text) {
  max-width: 90%;
}
</style>
