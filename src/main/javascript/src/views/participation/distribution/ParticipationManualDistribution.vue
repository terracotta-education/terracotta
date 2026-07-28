<template>
  <div class="participation-manual-distribution-container">
    <h1 class="mb-5">
      Select which students you would like for each condition.
    </h1>

    <div
      class="w-50 mx-auto my-0"
    >
      <p>Conditions</p>

      <v-expansion-panels
        class="v-expansion-panels--icon"
      >
        <v-expansion-panel
          v-for="(condition, index) in conditions"
          :key="condition.conditionId"
          @click="panelExpansion"
        >
          <v-expansion-panel-title>
            {{ condition.name }}
            ({{ arrayDataProxy[index]?.length || 0 }})
          </v-expansion-panel-title>

          <v-expansion-panel-text>
            <ListParticipants
              :list-of-participants="
                arrayDataProxy[index] || []
              "
              :move-to-options="conditionNames"
              :move-to-handler="moveToHandler"
              :selected-option="String(index)"
            />
          </v-expansion-panel-text>
        </v-expansion-panel>
      </v-expansion-panels>

      <ListParticipants
        :list-of-participants="
          arrayDataProxy[
            conditionNames.length - 1
          ] || []
        "
        :move-to-options="conditionNames"
        :move-to-handler="moveToHandler"
        :selected-option="
          String(conditionNames.length - 1)
        "
      />

      <v-btn
        elevation="0"
        color="primary"
        class="mt-10"
        @click="
          submitDistribution(
            'ParticipationSummary'
          )
        "
      >
        Continue
      </v-btn>
    </div>
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
  useRouter,
  onBeforeRouteUpdate
} from "vue-router";

import Swal from "sweetalert2";

import ListParticipants from "@/components/ListParticipants.vue";

import { participantService } from "@/services";
import { deleteAttributesFromElement } from "@/helpers/ui-utils.js";

import { participants as participantsModule } from "@/store/participants.module";
import { exposures as exposuresModule } from "@/store/exposures.module";
import { navigation as navigationModule } from "@/store/navigation.module";

defineOptions({
  name: "ParticipationManualDistribution"
});

const props = defineProps({
  experiment: {
    type: Object,
    required: true
  }
});

const router = useRouter();

const participantsStore =
  participantsModule();

const exposuresStore =
  exposuresModule();

const navigationStore =
  navigationModule();

const arrayDataProxy = ref([]);

/*
 * Stores
 */
const participants = computed(() => {
  return (
    participantsStore.participants || []
  );
});

const exposures = computed(() => {
  return exposuresStore.exposures || [];
});

const editMode = computed(() => {
  return navigationStore.editMode;
});

/*
 * Computed
 */
const getSaveExitPage = computed(() => {
  return (
    editMode.value?.callerPage?.name ||
    "Home"
  );
});

const conditions = computed(() => {
  return (
    props.experiment.conditions || []
  );
});

const conditionNames = computed(() => {
  return [
    ...conditions.value.map(
      condition => condition.name
    ),
    "Unassigned"
  ];
});

const arrayData = computed({
  get() {
    const newArray = [];

    for (
      let i = 0;
      i < conditions.value.length;
      i++
    ) {
      newArray.push([]);
    }

    newArray.push(
      participants.value
    );

    return newArray;
  },

  set(value) {
    arrayDataProxy.value = value;
  }
});

/*
 * Watchers
 */
watch(
  participants,
  () => {
    const participatingStudents =
      participants.value.filter(
        ({ consent }) =>
          consent === true
      );

    const conditionGroupIDMap =
      getConditionGroupIDMap();

    const newArray = [];

    for (
      let i = 0;
      i < conditions.value.length;
      i++
    ) {
      const assigned =
        participatingStudents.filter(
          student =>
            student.groupId ===
            conditionGroupIDMap[i]
        );

      newArray.push(assigned);
    }

    const unassigned =
      participatingStudents.filter(
        student =>
          student.groupId === null
      );

    newArray.push(unassigned);

    arrayData.value = newArray;
  },
  { deep: true }
);

/*
 * Methods
 */
const getConditionGroupIDMap = () => {
  const map = {};

  const firstExposureId =
    exposures.value
      .map(
        exposure =>
          exposure.exposureId
      )
      .sort((a, b) => a - b)[0];

  const firstExposure =
    exposures.value.find(
      exposure =>
        exposure.exposureId ===
        firstExposureId
    );

  firstExposure?.groupConditionList?.forEach(
    ({ groupId }, index) => {
      map[index] = groupId;
    }
  );

  return map;
};

const submitDistribution = async path => {
  const requestBody = [];

  const conditionGroupIDMap =
    getConditionGroupIDMap();

  arrayDataProxy.value.forEach(
    (participantList, index) => {
      participantList.forEach(
        participant => {
          requestBody.push({
            participantId:
              participant.participantId,
            consent:
              participant.consent,
            dropped:
              participant.dropped,
            groupId:
              conditionGroupIDMap[
                index
              ] || null
          });
        }
      );
    }
  );

  try {
    const response =
      await participantService.updateParticipants(
        props.experiment
          .experimentId,
        requestBody
      );

    if (response?.status === 200) {
      router.push({
        name: path,
        params: {
          experiment:
            props.experiment
              .experimentId
        }
      });

      return;
    }

    await Swal.fire(
      response?.error ||
        "Error updating participants"
    );
  } catch (error) {
    console.error(
      "submitDistribution | catch",
      error
    );
  }
};

const moveToHandler = (
  option,
  tempSelected
) => {
  const selectedIds =
    tempSelected.map(
      participant =>
        participant.participantId
    );

  const filtered =
    arrayDataProxy.value.map(
      conditionParticipants =>
        conditionParticipants.filter(
          participant =>
            !selectedIds.includes(
              participant.participantId
            )
        )
    );

  const targetIndex =
    conditionNames.value.indexOf(
      option
    );

  filtered[targetIndex] = [
    ...filtered[targetIndex],
    ...tempSelected
  ];

  arrayData.value = filtered;
};

const saveExit = () => {
  submitDistribution(
    getSaveExitPage.value
  );
};

const panelExpansion = () => {
  setTimeout(() => {
    deleteAttributesFromElement(
      ".v-expansion-panel",
      ["aria-expanded"]
    );
  }, 1000);
};

/*
 * Lifecycle
 */
onMounted(async () => {
  await exposuresStore.fetchExposures(
    props.experiment.experimentId
  );

  await participantsStore.fetchParticipants([
    props.experiment.experimentId
  ]);

  deleteAttributesFromElement(
    ".v-expansion-panel",
    ["aria-expanded"]
  );
});

onBeforeRouteUpdate(
  async (to, from, next) => {
    try {
      await participantsStore.fetchParticipants([
        to.params.experimentId
      ]);

      next();
    } catch {
      next();
    }
  }
);

defineExpose({
  saveExit
});
</script>
