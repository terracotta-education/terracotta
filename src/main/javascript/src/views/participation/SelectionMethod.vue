<template>
  <div class="selection-method-container">
    <page-loading
      :display="preparingParticipants"
      message="We are transferring students from your LMS course. Depending on the roster size, this may take a few moments. Please do not navigate away from this page."
    />
    <v-alert
      v-if="displayConsentFileMissingAlert"
      type="warning"
      variant="outlined"
      elevation="0"
    >
      Please complete the participation section in order to continue setting up
      your experiment.
    </v-alert>

    <h1 class="mb-3">
      How will study participation be determined?
    </h1>

    <v-expansion-panels
      v-model="expanded"
      class="v-expansion-panels--icon w-50 mx-auto my-0"
      multiple
    >
      <v-expansion-panel
        v-for="(panel, index) in panels"
        :key="index"
        class="participation-expansion-panel"
        :class="{
          'panel-not-selected':
            panel.type !== initialParticipationType,
          'panel-selected':
            panel.type === initialParticipationType
        }"
        :disabled="
          hasParticipantTypeSelected &&
          panel.type !== initialParticipationType
        "
      >
        <v-expansion-panel-title
          hide-actions
        >
          <img
            :src="panel.img.src"
            :alt="panel.img.alt"
          />

          <strong>{{ panel.header }}</strong>
        </v-expansion-panel-title>

        <v-expansion-panel-text>
          <p>{{ panel.body }}</p>

          <v-btn
            color="primary"
            :loading="loading"
            :disabled="loading"
            @click="setParticipationType(panel.type)"
          >
            Select
          </v-btn>
        </v-expansion-panel-text>
      </v-expansion-panel>
    </v-expansion-panels>
  </div>
</template>

<script setup>
import {
  ref,
  computed,
  onBeforeUnmount,
  onMounted
} from "vue";

import { useRouter } from "vue-router";
import Swal from "sweetalert2";

import { deleteAttributesFromElement } from "@/helpers/ui-utils.js";
import PageLoading from "@/components/PageLoading.vue"

import consentInviteIcon from "@/assets/consent_invite.svg";
import consentManualIcon from "@/assets/consent_manual.svg";
import consentAutomaticIcon from "@/assets/consent_automatic.svg";

import { api as apiModule } from "@/store/api.module";
import { experiment as experimentModule } from "@/store/experiment.module";
import { navigation as navigationModule } from "@/store/navigation.module";
import { configuration as configurationModule } from "@/store/configuration.module";

defineOptions({
  name: "ParticipationSelectionMethod"
});

const props = defineProps({
  experiment: {
    type: Object,
    required: true
  }
});

const router = useRouter();

const apiStore = apiModule();
const experimentStore = experimentModule();
const navigationStore = navigationModule();
const configurationStore = configurationModule();

const loading = ref(false);
const expanded = ref([0, 1, 2]);
const initialParticipationType = ref(null);
const preparingParticipants = ref(false);

const editMode = computed(() => {
  return navigationStore.editMode;
});

const configurations = computed(() => {
  return configurationStore.configurations;
});

const participationType = computed(() => {
  return props.experiment.participationType;
});

const lmsTitle = computed(() => {
  return configurations.value?.lmsTitle || "LMS";
});

const panels = computed(() => {
  return [
    {
      type: "CONSENT",
      img: {
        src: consentInviteIcon,
        alt: "invite students"
      },
      header: "Students will be invited to consent",
      body: `Select this option if you would like to create a consent assignment within ${lmsTitle.value}`
    },
    {
      type: "MANUAL",
      img: {
        src: consentManualIcon,
        alt: "manually decide students"
      },
      header: "Teacher will manually decide",
      body: "Select this option if you are working with minors or will be collecting parental consent"
    },
    {
      type: "AUTO",
      img: {
        src: consentAutomaticIcon,
        alt: "automatically include all students"
      },
      header: "Automatically include all students",
      body: "Select this option if informed consent is not needed to run the study"
    }
  ];
});

const getSaveExitPage = computed(() => {
  return editMode.value?.callerPage?.name || "Home";
});

const hasParticipantTypeSelected = computed(() => {
  return (
    initialParticipationType.value &&
    initialParticipationType.value !== "NOSET"
  );
});

const isConsentType = computed(() => {
  return participationType.value === "CONSENT";
});

const displayConsentFileMissingAlert = computed(() => {
  return (
    editMode.value &&
    isConsentType.value &&
    !props.experiment?.consent
  );
});

const POLL_INTERVAL_MS = 5000;
const POLL_MAX_DURATION_MS = 60 * 60 * 1000; // 1 hour

let statusPollTimer = null;

const stopPolling = () => {
  if (statusPollTimer) {
    clearInterval(statusPollTimer);
    statusPollTimer = null;
  }
};

const navigateAfterParticipationTypeSelected = (selectedParticipationType, experimentId) => {
  switch (selectedParticipationType) {
    case "CONSENT":
      router.push({
        name:
          "ParticipationTypeConsentOverview",
        params: {
          experiment: experimentId
        }
      });
      break;

    case "MANUAL":
      router.push({
        name: "ParticipationTypeManual",
        params: {
          experiment: experimentId
        }
      });
      break;

    case "AUTO":
      router.push({
        name:
          "ParticipationTypeAutoConfirm",
        params: {
          experiment: experimentId
        }
      });
      break;

    default:
      Swal.fire(
        "Select a participation type"
      );
  }
};

const isTerminalPrepareParticipationStatus = status => (
  status === "COMPLETED" ||
  status === "PROCESSED" ||
  status === "FAILED"
);

const handleTerminalPrepareParticipationStatus = async (status, message, selectedParticipationType, experimentId) => {
  if (status === "FAILED") {
    await Swal.fire(
      message
        ? `Error: ${message}`
        : "There was an error preparing participants for this experiment."
    );

    return;
  }

  navigateAfterParticipationTypeSelected(selectedParticipationType, experimentId);
};

// refreshParticipants (kicked off server-side by reportStep) can take several minutes for a
// large course roster, so instead of blocking on that one request, poll its status every 5
// seconds until it reaches a terminal state - giving up after an hour rather than polling
// forever if it never does.
const pollPrepareParticipationStatus = (experimentId, batchId, selectedParticipationType) => {
  const pollStartedAt = Date.now();

  statusPollTimer = setInterval(
    async () => {
      if (Date.now() - pollStartedAt >= POLL_MAX_DURATION_MS) {
        stopPolling();
        preparingParticipants.value = false;

        await Swal.fire(
          "Preparing participants is taking longer than expected. Please try again later."
        );

        return;
      }

      const statusResponse = await apiStore.getStepStatus({
        experimentId,
        batchId
      });
      const status = statusResponse?.data?.status;

      if (!isTerminalPrepareParticipationStatus(status)) {
        // still IN_PROGRESS/PENDING, or the poll request itself failed - keep polling either way
        return;
      }

      stopPolling();
      preparingParticipants.value = false;

      await handleTerminalPrepareParticipationStatus(
        status,
        statusResponse?.data?.message,
        selectedParticipationType,
        experimentId
      );
    },
    POLL_INTERVAL_MS
  );
};

const setParticipationType = async type => {
  initialParticipationType.value = type;

  const experiment = {
    ...props.experiment,
    participationType: type
  };

  const experimentId = experiment.experimentId;
  const step = "participation_type";

  loading.value = true;

  try {
    const response =
      await experimentStore.updateExperiment(
        experiment
      );

    if (response?.status === 200) {
      preparingParticipants.value = true;

      const stepResponse = await apiStore.reportStep({
        experimentId,
        step
      });

      if (stepResponse?.status !== 200) {
        preparingParticipants.value = false;

        await Swal.fire(
          stepResponse?.message
            ? `Error: ${stepResponse.message}`
            : "There was an error preparing participants for this experiment."
        );

        return;
      }

      const batchId = stepResponse?.data?.batchId;
      const initialStatus = stepResponse?.data?.status;

      if (!batchId || !initialStatus) {
        preparingParticipants.value = false;

        await Swal.fire(
          "There was an error preparing participants for this experiment."
        );

        return;
      }

      if (isTerminalPrepareParticipationStatus(initialStatus)) {
        // the roster wasn't due for a sync, so the backend already finished synchronously -
        // no need to poll for something that's already done
        preparingParticipants.value = false;

        await handleTerminalPrepareParticipationStatus(
          initialStatus,
          stepResponse?.data?.message,
          experiment.participationType,
          experimentId
        );

        return;
      }

      pollPrepareParticipationStatus(experimentId, batchId, experiment.participationType);
    } else if (response?.message) {
      await Swal.fire(
        `Error: ${response.message}`
      );
    } else {
      await Swal.fire(
        "There was an error saving your experiment."
      );
    }
  } catch (error) {
    console.error(
      "updateExperiment | catch",
      { error }
    );

    await Swal.fire(
      "There was an error saving the experiment."
    );
  } finally {
    loading.value = false;
  }
};

const saveExit = () => {
  router.push({
    name: getSaveExitPage.value,
    params: {
      experiment: props.experiment.experimentId
    }
  });
};

onMounted(() => {
  initialParticipationType.value =
    participationType.value;

  deleteAttributesFromElement(
    ".v-expansion-panel",
    ["aria-expanded"]
  );
});

onBeforeUnmount(() => {
  stopPolling();
});

defineExpose({
  saveExit
});
</script>

<style lang="scss" scoped>
.v-expansion-panel {
  margin-bottom: 30px !important;
}

.panel-selected {
  border-color: rgba(3, 169, 244, 1) !important;
}

.panel-not-selected {
  border-color: map.get($grey, "lighter") !important;
}

.participation-expansion-panel:focus-within {
  border-color: rgba(0, 0, 0, 0.87) !important;
}

:deep(.v-expansion-panel-title) {
  pointer-events: none;
  flex-direction: column;
}

.selection-method-container {
  :deep(.panel-not-selected) {
    color: rgba(0, 0, 0, 0.8) !important;
  }

  & .v-expansion-panel-title {
    & img {
      display: block;
      opacity: .6;
      min-width: 40px;
      max-width: 40px;
      margin: 0 auto 20px;
    }
  }
}
</style>
