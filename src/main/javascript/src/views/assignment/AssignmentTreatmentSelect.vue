<template>
  <div v-if="assignment">
    <h1 class="pa-0 mb-7">
      Now, let’s create different versions of
      <strong>{{ assignment.title }}</strong>
      for each condition
    </h1>

    <template v-if="conditions.length">
      <v-expansion-panels class="v-expansion-panels--outlined mb-7">
        <v-expansion-panel class="py-3">
          <v-expansion-panel-title>
            {{ assignment.title }}
            ({{ assignment.treatments?.length || 0 }}/{{ conditions.length }})
          </v-expansion-panel-title>

          <v-expansion-panel-text>
            <v-list class="pa-0">
              <v-list-item
                v-for="condition in conditions"
                :key="condition.conditionId"
                class="justify-center px-0"
              >
                <v-list-item-title>
                  <p class="ma-0 pa-0">
                    {{ condition.name }}
                  </p>
                </v-list-item-title>

                <template #append>
                  <template v-if="hasTreatment(condition)">
                    <v-btn
                      variant="outlined"
                      icon="mdi-pencil"
                      :aria-label="`Edit ${condition.name} version`"
                      @click="goToBuilder(condition.conditionId)"
                    />
                  </template>

                  <template v-else>
                    <v-btn
                      color="primary"
                      variant="outlined"
                      @click="goToBuilder(condition.conditionId)"
                    >
                      Create
                    </v-btn>
                  </template>
                </template>
              </v-list-item>
            </v-list>
          </v-expansion-panel-text>
        </v-expansion-panel>
      </v-expansion-panels>
    </template>

    <template v-else>
      <p>no conditions</p>
    </template>
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
  useRouter
} from "vue-router";

import Swal from "sweetalert2";

import { assignment as assignmentModule } from "@/store/assignment.module";
import { experiment as experimentModule } from "@/store/experiment.module";
import { treatment as treatmentModule } from "@/store/treatment.module";
import { assessment as assessmentModule } from "@/store/assessment.module";

defineOptions({
  name: "AssignmentTreatmentSelect"
});

const props = defineProps({
  experiment: {
    type: Object,
    required: true
  }
});

const route = useRoute();
const router = useRouter();

const assignmentStore = assignmentModule();
const experimentStore = experimentModule();
const treatmentStore = treatmentModule();
const assessmentStore = assessmentModule();

const conditionTreatments = ref([]);

const assignment = computed(() => {
  return assignmentStore.assignment;
});

const conditions = computed(() => {
  return experimentStore.conditions || [];
});

const assignmentId = computed(() => {
  return Number.parseInt(route.params.assignmentId, 10);
});

const exposureId = computed(() => {
  return Number.parseInt(route.params.exposureId, 10);
});

const handleCreateTreatment = async conditionId => {
  try {
    return await treatmentStore.createTreatment([
      props.experiment.experimentId,
      conditionId,
      assignmentId.value
    ]);
  } catch (error) {
    console.error(
      "handleCreateTreatment | catch",
      { error }
    );

    return null;
  }
};

const handleCreateAssessment = async (
  conditionId,
  treatment
) => {
  try {
    return await assessmentStore.createAssessment([
      props.experiment.experimentId,
      conditionId,
      treatment.treatmentId
    ]);
  } catch (error) {
    console.error(
      "handleCreateAssessment | catch",
      { error }
    );

    return null;
  }
};

const goToBuilder = async conditionId => {
  const treatment =
    await handleCreateTreatment(conditionId);

  if (
    !treatment ||
    ![200, 201].includes(treatment.status)
  ) {
    await Swal.fire(
      `There was a problem creating your treatment: ${treatment?.data || ""}`
    );

    return false;
  }

  const assessment =
    await handleCreateAssessment(
      conditionId,
      treatment.data
    );

  if (
    !assessment ||
    ![200, 201].includes(assessment.status)
  ) {
    await Swal.fire(
      `There was a problem creating your assessment: ${assessment?.data || ""}`
    );

    return false;
  }

  router.push({
    name: "TerracottaBuilder",
    params: {
      experimentId: props.experiment.experimentId,
      exposureId: exposureId.value,
      assignmentId: assignmentId.value,
      conditionId,
      treatmentId: treatment.data?.treatmentId,
      assessmentId: assessment.data?.assessmentId
    }
  });

  return true;
};

const hasTreatment = condition => {
  return !!conditionTreatments.value.find(
    conditionTreatment => {
      return (
        conditionTreatment.treatment &&
        conditionTreatment.condition.conditionId === condition.conditionId &&
        Number.parseInt(
          conditionTreatment.treatment.assignmentId,
          10
        ) === assignmentId.value
      );
    }
  );
};

const checkConditionTreatments = async () => {
  const checkedTreatments = [];

  for (const condition of conditions.value) {
    const treatment =
      await treatmentStore.checkTreatment([
        props.experiment.experimentId,
        condition.conditionId
      ]);

    const matchingTreatment =
      treatment?.data?.find(
        item =>
          Number.parseInt(item.assignmentId, 10) ===
          assignmentId.value
      );

    if (matchingTreatment) {
      checkedTreatments.push({
        treatment: matchingTreatment,
        condition
      });
    }
  }

  conditionTreatments.value = checkedTreatments;
};

const saveExit = () => {
  router.push({
    name: "Home"
  });
};

onMounted(async () => {
  await assignmentStore.fetchAssignment([
    props.experiment.experimentId,
    exposureId.value,
    assignmentId.value
  ]);

  await checkConditionTreatments();
});

defineExpose({
  saveExit
});
</script>
