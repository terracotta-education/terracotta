<template>
  <div
    v-if="experiment && exposureId && outcome"
  >
    <h1 class="mb-6">
      {{ exposureTitle }}
    </h1>

    <form @submit.prevent="saveExit">
      <v-row>
        <v-col cols="12">
          <v-text-field
            v-model="outcome.title"
            :rules="rules"
            name="outcomeTitle"
            class="pb-0 mb-0"
            label="Outcome name"
            autofocus
            variant="outlined"
            required
          />
        </v-col>
      </v-row>

      <v-row>
        <v-col cols="4">
          <v-text-field
            v-model="outcome.maxPoints"
            :rules="numberRule"
            type="number"
            name="outcomeMaxPoints"
            label="Total Points"
            variant="outlined"
            required
          />
        </v-col>
      </v-row>

      <v-row>
        <v-col cols="12">
          <v-table class="mb-9 v-data-table--light-header">
            <thead>
              <tr>
                <th class="text-left">
                  Student Name
                </th>

                <th
                  class="text-left"
                  width="250"
                >
                  Numeric Score
                </th>
              </tr>
            </thead>

            <tbody>
              <tr
                v-for="participant in participantFilteredList"
                :key="participant.participantId"
              >
                <td>{{ participant.user.displayName }}</td>

                <td v-if="participantScoreList.length">
                  <v-text-field
                    v-model="getParticipantScore(participant).scoreNumeric"
                    :name="String(participant.participantId)"
                    type="number"
                    placeholder="---"
                    style="max-width: 50px;"
                    required
                  />
                </td>
              </tr>
            </tbody>
          </v-table>
        </v-col>
      </v-row>
    </form>
  </div>
</template>

<script setup>
import {
  computed,
  onMounted
} from "vue";

import {
  useRoute,
  useRouter
} from "vue-router";

import Swal from "sweetalert2";

import { statusAlert } from "@/helpers/ui-utils.js";

import { experiment as experimentModule } from "@/store/experiment.module";
import { exposures as exposuresModule } from "@/store/exposures.module";
import { outcome as outcomeModule } from "@/store/outcome.module";
import { alert as alertModule } from "@/store/alert.module";
import { participants as participantsModule } from "@/store/participants.module";

defineOptions({
  name: "OutcomeScoring"
});

const route = useRoute();
const router = useRouter();

const experimentStore = experimentModule();
const exposuresStore = exposuresModule();
const outcomeStore = outcomeModule();
const alertStore = alertModule();
const participantsStore = participantsModule();

const rules = [
  value => value && !!value.trim() || "required",
  value => (value || "").length <= 255 ||
    "A maximum of 255 characters is allowed"
];

const numberRule = [
  value => value && !Number.isNaN(value) || "required",
  value => !Number.isNaN(Number.parseFloat(value)) && value >= 0 ||
    "The point value cannot be negative"
];

const experiment = computed(() => {
  return experimentStore.experiment;
});

const exposures = computed(() => {
  return exposuresStore.exposures || [];
});

const outcome = computed(() => {
  return outcomeStore.outcome;
});

const alertStatuses = computed(() => {
  return alertStore.statuses;
});

const outcomeScores = computed(() => {
  return outcomeStore.outcomeScores || [];
});

const participants = computed(() => {
  return participantsStore.participants || [];
});

const exposureId = computed(() => {
  return Number.parseInt(route.params.exposureId, 10);
});

const experimentId = computed(() => {
  return Number.parseInt(route.params.experimentId, 10);
});

const outcomeId = computed(() => {
  return Number.parseInt(route.params.outcomeId, 10);
});

const exposureTitle = computed(() => {
  return exposures.value.find(
    exposure => exposure.exposureId === exposureId.value
  )?.title || "";
});

const participantFilteredList = computed(() => {
  return participants.value
    .filter(participant => participant.user.displayName !== null)
    .map(participant => {
      const displayName = participant.user.displayName;
      let sortableName = displayName;

      if (displayName.includes(" ")) {
        const parts = displayName.split(" ");

        if (parts.length > 1) {
          sortableName = `${parts[parts.length - 1]}${parts[parts.length - 2]}`;
        }
      }

      return {
        ...participant,
        user: {
          ...participant.user,
          sortableName
        }
      };
    })
    .sort((a, b) => {
      return a.user.sortableName
        .toLowerCase()
        .localeCompare(b.user.sortableName.toLowerCase());
    });
});

const participantScoreList = computed(() => {
  const scoreByParticipantId = new Map(
    outcomeScores.value
      .filter(score => score.outcomeId === outcomeId.value)
      .map(score => [score.participantId, score])
  );

  return participantFilteredList.value.map(participant => {
    const score = scoreByParticipantId.get(participant.participantId);

    const item = {
      experimentId: experimentId.value,
      participantId: participant.participantId,
      scoreNumeric: 0
    };

    if (score) {
      item.outcomeScoreId = score.outcomeScoreId;
      item.outcomeId = outcomeId.value;
      item.scoreNumeric = Number.parseInt(score.scoreNumeric, 10);
    }

    return item;
  });
});

// O(1) lookup instead of a linear find() over participantScoreList for every rendered row
const participantScoreMap = computed(() => {
  return new Map(
    participantScoreList.value.map(item => [item.participantId, item])
  );
});

const exitDisabled = computed(() => {
  return (
    outcome.value.title.length < 1 ||
    outcome.value.title.length > 255 ||
    outcome.value.maxPoints < 0 ||
    outcomeScores.value
      .filter(score => score.outcomeId === outcomeId.value)
      .some(score => score.scoreNumeric > outcome.value.maxPoints)
  );
});

const getParticipantScore = participant => {
  return participantScoreMap.value.get(participant.participantId);
};

const saveExit = async () => {
  if (exitDisabled.value) {
    Swal.fire({
      text: "Could not update outcome due to entered data.",
      icon: "error"
    });

    return;
  }

  await outcomeStore.updateOutcome([
    experimentId.value,
    exposureId.value,
    outcome.value
  ]);

  await outcomeStore.updateOutcomeScores([
    experimentId.value,
    exposureId.value,
    outcomeId.value,
    participantScoreList.value
  ]);

  router.push({
    name: router.currentRoute.value.meta.previousStep,
    params: {
      ...statusAlert(
        alertStatuses.value.success,
        "Outcome and scores updated successfully."
      )
    }
  });
};

onMounted(async () => {
  await outcomeStore.fetchOutcomeById([
    experimentId.value,
    exposureId.value,
    outcomeId.value
  ]);

  const result = await participantsStore.fetchParticipants([
    experimentId.value
  ]);

  if (result === null) {
    Swal.fire({
      text: "Could not load participants.",
      icon: "error"
    });

    return;
  }

  await outcomeStore.fetchOutcomeScores([
    experimentId.value,
    exposureId.value,
    outcomeId.value
  ]);
});

defineExpose({
  saveExit
});
</script>
