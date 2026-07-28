<template>
  <v-expansion-panels
    v-if="loaded"
    class="mt-6"
  >
    <v-expansion-panel elevation="0" @click="panelExpansion">
      <v-expansion-panel-title class="preview-header">
        <v-icon>mdi-message-text-outline</v-icon>

        <span class="ml-4">
          Preview Message
        </span>
      </v-expansion-panel-title>

      <v-expansion-panel-text>
        <div class="preview-message-container">
          <fieldset class="preview-message">
            <legend>Message</legend>

            <div
              v-html="previewMessageBody"
              :class="{
                'preview-message-conversation-body': type === 'CONVERSATION'
              }"
              class="preview-message-body"
            />

            <v-overlay
              v-model="showRefreshOverlay"
              contained
              :opacity="0.75"
              class="d-flex align-center justify-center"
            >
              <v-btn
                color="primary"
                @click="handlePreview(selectedParticipant)"
              >
                Refresh
              </v-btn>
            </v-overlay>

            <v-overlay
              v-model="isFetching"
              contained
              :opacity="0.75"
              class="d-flex align-center justify-center"
            >
              <v-progress-circular
                indeterminate
                size="64"
              />
            </v-overlay>
          </fieldset>

          <fieldset class="preview-participant-list">
            <legend>Preview As</legend>

            <v-list
              v-model:selected="selectedParticipantList"
              aria-label="Select a student to preview a message as"
            >
              <v-list-item
                v-for="participant in availableParticipants"
                :key="participant.id"
                :value="participant.id"
                active-class="selected-participant"
                class="preview-participant"
                link
                @click="handlePreview(participant.id)"
              >
                {{ participant.user.displayName }}
              </v-list-item>
            </v-list>
          </fieldset>
        </div>
      </v-expansion-panel-text>
    </v-expansion-panel>
  </v-expansion-panels>
</template>

<script setup>
import {
  ref,
  computed,
  watch,
  nextTick,
  onMounted
} from "vue";

import { deleteAttributesFromElement } from "@/helpers/ui-utils.js";

import { container as messagingMessageContainerModule } from "@/store/messaging/container.module";
import { conditionaltext as messagingConditionalTextModule } from "@/store/messaging/conditionaltext.module";
import { participants as participantsModule } from "@/store/participants.module";
import { message as messagingMessageModule } from "@/store/messaging/message.module";

defineOptions({
  name: "MessagePreview"
});

const props = defineProps({
  experimentId: {
    type: Number,
    required: true
  },
  exposureId: {
    type: String,
    required: true
  },
  containerId: {
    type: String,
    required: true
  },
  messageId: {
    type: String,
    required: true
  },
  contentId: {
    type: String,
    required: true
  }
});

const messagingMessageContainerStore = messagingMessageContainerModule();
const messagingConditionalTextStore = messagingConditionalTextModule();
const participantsStore = participantsModule();
const messagingMessageStore = messagingMessageModule();

const selectedParticipant = ref(null);
const loaded = ref(false);
const isFetching = ref(false);
const showRefreshButton = ref(false);

const allMessageContainers = computed(() => {
  return messagingMessageContainerStore.messageContainers || [];
});

const allConditionalTexts = computed(() => {
  return messagingConditionalTextStore.messageConditionalTexts || [];
});

const participants = computed(() => {
  return participantsStore.participants || [];
});

const previewMessage = computed(() => {
  return messagingMessageStore.preview;
});

const container = computed(() => {
  return allMessageContainers.value.find(
    messageContainer => messageContainer.id === props.containerId
  );
});

const message = computed(() => {
  return container.value?.messages.find(
    currentMessage => currentMessage.id === props.messageId
  );
});

const configuration = computed(() => {
  return message.value?.configuration || {};
});

const content = computed(() => {
  return message.value?.content || {};
});

const body = computed(() => {
  return content.value?.html || "";
});

const type = computed(() => {
  return configuration.value?.type || "";
});

const ruleSets = computed(() => {
  return message.value?.ruleSets || [];
});

const pipedText = computed(() => {
  return content.value?.pipedText || null;
});

const availableParticipants = computed(() => {
  return [...participants.value].sort((a, b) => {
    return a.user.displayName.localeCompare(
      b.user.displayName
    );
  });
});

const previewMessageBody = computed(() => {
  return previewMessage.value?.body ||
    "<p>Please select a user to preview their message.</p>";
});

const selectedParticipantList = computed({
  get() {
    return selectedParticipant.value
      ? [selectedParticipant.value]
      : [];
  },

  set(value) {
    selectedParticipant.value = value?.[0] || null;
  }
});

const showRefreshOverlay = computed({
  get() {
    return !isFetching.value && showRefreshButton.value;
  },

  set(value) {
    showRefreshButton.value = value;
  }
});

watch(
  body,
  () => {
    if (selectedParticipant.value) {
      showRefreshButton.value = true;
    }
  },
  {
    immediate: true
  }
);

const handlePreview = async participantId => {
  selectedParticipant.value = participantId;
  isFetching.value = true;

  await messagingMessageStore.fetchPreview([
    props.experimentId,
    props.exposureId,
    props.containerId,
    props.messageId,
    {
      id: participantId,
      body: body.value,
      ruleSets: ruleSets.value,
      conditionalTexts: allConditionalTexts.value,
      pipedText: pipedText.value
    }
  ]);

  showRefreshButton.value = false;
  isFetching.value = false;
};

const initialize = async () => {
  messagingMessageStore.setPreview(null);

  await participantsStore.fetchParticipants([
    props.experimentId
  ]);

  participantsStore.setParticipants(
    participants.value.map(participant => ({
      ...participant,
      participantId: null
    }))
  );
};

const panelExpansion = () => {
  window.setTimeout(() => {
    deleteAttributesFromElement(
      ".v-expansion-panel",
      ["aria-expanded"]
    );
  }, 1000);
};

onMounted(async () => {
  await initialize();

  loaded.value = true;

  await nextTick();

  deleteAttributesFromElement(
    ".v-expansion-panel",
    ["aria-expanded"]
  );
});
</script>

<style scoped>
.v-expansion-panels {
  :deep(.v-expansion-panel) {
    margin-bottom: 0 !important;
    border: none !important;
  }

  :deep(.v-expansion-panel-text__wrapper) {
    padding: 10px 20px;
  }
}

.preview-header {
  display: flex;
  align-content: start;

  > * {
    max-width: fit-content;
  }
}

.preview-message-container {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  min-height: 500px;
  max-height: 500px;

  & .preview-message,
  & .preview-participant-list {
    min-height: 100%;
    padding: 12px;
    border: thin solid #9e9e9e;
    border-radius: 4px;
  }

  & .preview-message {
    width: 68%;

    & .preview-message-conversation-body {
      white-space: pre-wrap;
    }

    > .preview-message-body {
      overflow-y: scroll;
      height: 100%;
    }
  }

  & .preview-participant-list {
    width: 30%;

    & .v-list {
      min-height: 100%;
      max-height: 100%;
      overflow-y: auto;
      border: none;
    }

    & .preview-participant {
      min-height: fit-content;
      max-height: fit-content;
      padding: 0;
    }

    & .selected-participant {
      background-color: rgba(29, 157, 255, 0.15);
    }
  }

  > div {
    border: 1px solid #9e9e9e;
    border-radius: 4px;
  }

  & legend {
    padding: 4px;
  }
}
</style>
