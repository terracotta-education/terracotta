<template>
  <div>
    <h1>
      Because you have
      <strong>{{ numberOfConditions }} conditions</strong>
      and would like your students to be

      <template v-if="exposureType === 'WITHIN'">
        <strong>exposed to every condition</strong>
        (within-subject),
      </template>

      <template v-else>
        <strong>exposed to only one condition</strong>
        (between),
      </template>

      we will set you up with {{ numberOfExperimentSets }} exposure sets.
    </h1>

    <div class="mt-3">
      <strong>Exposure Set:</strong>

      <v-slide-group show-arrows>
        <v-btn-toggle
          v-model="selectedExposure"
          color="primary"
          mandatory
        >
          <v-btn
            v-for="(item, index) in exposures"
            :key="item.exposureId"
            :value="item"
          >
            {{ index + 1 }}
          </v-btn>
        </v-btn-toggle>
      </v-slide-group>
    </div>

    <v-card
      class="mt-5 pa-5 mx-auto bg-blue-lighten-5 rounded-lg"
      variant="outlined"
    >
      <p
        v-for="group in sortedGroups"
        :key="group"
        class="pa-0 my-0"
      >
        {{ group }} will receive

        <v-chip
          :color="conditionColorMapping[groupNameConditionMapping[group]]"
          class="ma-2"
        >
          {{ groupNameConditionMapping[group] }}
        </v-chip>
      </p>
    </v-card>

    <v-btn
      :to="{
        name: 'AssignmentExposureSetsIntro',
        params: {
          numberOfExperimentSets,
          exposureId: selectedExposure?.exposureId
        }
      }"
      class="mt-5"
      elevation="0"
      color="primary"
    >
      Continue
    </v-btn>
  </div>
</template>

<script setup>
import {
  ref,
  computed,
  watch,
  onMounted
} from "vue";

import {
  useRoute,
  useRouter,
  onBeforeRouteUpdate
} from "vue-router";

import { exposures as exposuresModule } from "@/store/exposures.module";
import { condition as conditionModule } from "@/store/condition.module";

defineOptions({
  name: "AssignmentExposureSets"
});

const props = defineProps({
  experiment: {
    type: Object,
    required: true
  }
});

const route = useRoute();
const router = useRouter();

const exposuresStore = exposuresModule();
const conditionStore = conditionModule();

const selectedExposure = ref(null);

const exposures = computed(() => {
  return exposuresStore.exposures || [];
});

const conditionColorMapping = computed(() => {
  return conditionStore.conditionColorMapping || {};
});

const exposureType = computed(() => {
  return props.experiment.exposureType;
});

const numberOfConditions = computed(() => {
  return props.experiment.conditions?.length || 0;
});

const numberOfExperimentSets = computed(() => {
  return exposures.value.length;
});

const groupNameConditionMapping = computed(() => {
  const groupConditionMap = {};

  selectedExposure.value?.groupConditionList?.forEach(group => {
    groupConditionMap[group.groupName] = group.conditionName;
  });

  return groupConditionMap;
});

const sortedGroups = computed(() => {
  return [
    ...(selectedExposure.value?.groupConditionList || [])
  ]
    .map(group => group.groupName)
    .sort();
});

watch(
  exposures,
  newExposures => {
    if (!selectedExposure.value && newExposures.length > 0) {
      selectedExposure.value = newExposures[0];
    }
  },
  { immediate: true }
);

const fetchExposures = async experimentId => {
  await exposuresStore.fetchExposures(experimentId);
};

const saveExit = () => {
  router.push({
    name: "Home"
  });
};

onMounted(async () => {
  await fetchExposures(route.params.experimentId);
});

onBeforeRouteUpdate(async to => {
  await fetchExposures(to.params.experimentId);
});

defineExpose({
  saveExit
});
</script>
