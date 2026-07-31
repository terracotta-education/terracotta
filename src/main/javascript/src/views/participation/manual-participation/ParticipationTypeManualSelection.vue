<template>
  <div>
    <page-loading
      :display="loadingParticipants"
      message="We are tranferring students from your LMS course. Depending on the roster size, this may take a few moments."
    />

    <h1 class="my-3">
      Which students can participate in the study?
    </h1>

    <p>Groups</p>

    <v-expansion-panels
      class="v-expansion-panels--icon"
    >
      <v-expansion-panel
        @click="panelExpansion"
      >
        <v-expansion-panel-title>
          Participating ({{ participating.length }})
        </v-expansion-panel-title>

        <v-expansion-panel-text>
          <ListParticipants
            :list-of-participants="participating"
            :move-to-handler="moveToHandler"
            :move-to-options="moveToOptions"
            selected-option="0"
          />
        </v-expansion-panel-text>
      </v-expansion-panel>

      <v-expansion-panel
        @click="panelExpansion"
      >
        <v-expansion-panel-title>
          Not participating ({{ notParticipating.length }})
        </v-expansion-panel-title>

        <v-expansion-panel-text>
          <ListParticipants
            :list-of-participants="notParticipating"
            :move-to-options="moveToOptions"
            :move-to-handler="moveToHandler"
            selected-option="1"
          />
        </v-expansion-panel-text>
      </v-expansion-panel>

      <v-expansion-panel
        @click="panelExpansion"
      >
        <v-expansion-panel-title>
          Unassigned ({{ unassigned.length }})
        </v-expansion-panel-title>

        <v-expansion-panel-text>
          <ListParticipants
            :list-of-participants="unassigned"
            :move-to-options="moveToOptions"
            :move-to-handler="moveToHandler"
            selected-option="2"
          />
        </v-expansion-panel-text>
      </v-expansion-panel>
    </v-expansion-panels>

    <div class="mt-5">
      <v-btn
        elevation="0"
        color="primary"
        @click="
          submitParticipants(
            nextPage('ParticipationDistribution')
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
  computed,
  ref,
  onMounted
} from "vue";

import {
  useRouter,
  onBeforeRouteUpdate
} from "vue-router";

import Swal from "sweetalert2";

import { deleteAttributesFromElement } from "@/helpers/ui-utils.js";

import ListParticipants from "@/components/ListParticipants.vue";
import PageLoading from "@/components/PageLoading.vue";

import { participants as participantsModule } from "@/store/participants.module";
import { navigation as navigationModule } from "@/store/navigation.module";

defineOptions({
  name: "ParticipationTypeManualSelection"
});

const props = defineProps({
  experiment: {
    type: Object,
    required: true
  }
});

const router = useRouter();

const participantsStore = participantsModule();

const loadingParticipants = ref(false);
const navigationStore = navigationModule();

const moveToOptions = ref([
  "Participating",
  "Not participating",
  "Unassigned"
]);

const participants = computed(() => {
  return participantsStore.participants || [];
});

const editMode = computed(() => {
  return navigationStore.editMode;
});

const getSaveExitPage = computed(() => {
  return editMode.value?.callerPage?.name || "Home";
});

const conditions = computed(() => {
  return props.experiment.conditions || [];
});

const singleConditionExperiment = computed(() => {
  return conditions.value.length === 1;
});

const groupParticipants = value => {
  return participants.value.filter(
    participant => participant.consent === value
  );
};

const participating = computed(() => {
  return groupParticipants(true);
});

const notParticipating = computed(() => {
  return groupParticipants(false);
});

const unassigned = computed(() => {
  return groupParticipants(null);
});

const getParticipantIds = participantsList => {
  return participantsList.map(
    participant => participant.user.userId
  );
};

/*
 * Immutable update version
 * Avoids mutating Pinia state objects directly.
 */
const updateParticipantConsent = (
  selectedIds,
  value
) => {
  return participants.value.map(
    participant => ({
      ...participant,
      consent: selectedIds.includes(
        participant.user.userId
      )
        ? value
        : participant.consent
    })
  );
};

const moveToHandler = (
  option,
  tempSelected
) => {
  const selectedIds =
    getParticipantIds(tempSelected);

  let updatedParticipants = [];

  switch (option) {
    case "Participating":
      updatedParticipants =
        updateParticipantConsent(
          selectedIds,
          true
        );
      break;

    case "Not participating":
      updatedParticipants =
        updateParticipantConsent(
          selectedIds,
          false
        );
      break;

    case "Unassigned":
      updatedParticipants =
        updateParticipantConsent(
          selectedIds,
          null
        );
      break;
  }

  participantsStore.setParticipantsGroup(
    updatedParticipants
  );
};

const submitParticipants = async path => {
  try {
    const response =
      await participantsStore.updateParticipants(
        props.experiment.experimentId
      );

    if (response?.status === 200) {
      router.push({
        name: path,
        params: {
          experiment:
            props.experiment.experimentId
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
      "submitParticipants | catch",
      error
    );
  }
};

const saveExit = () => {
  submitParticipants(
    getSaveExitPage.value
  );
};

const nextPage = toPage => {
  if (
    singleConditionExperiment.value
  ) {
    return "ParticipationSummary";
  }

  return toPage;
};

const panelExpansion = () => {
  setTimeout(() => {
    deleteAttributesFromElement(
      ".v-expansion-panel",
      ["aria-expanded"]
    );
  }, 1000);
};

onMounted(async () => {
  // refresh=true here can trigger a full LMS roster sync (throttled server-side, but still
  // synchronous when it does run, and can take a while for a large course) - shown behind
  // page-loading rather than leaving the panels looking empty/stuck with no explanation
  loadingParticipants.value = true;

  await participantsStore.fetchParticipants([
    props.experiment.experimentId,
    true
  ]);

  loadingParticipants.value = false;

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
