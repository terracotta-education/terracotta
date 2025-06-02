<template>
<v-expansion-panels
  v-if="loaded"
  class="mt-6"
  flat
>
  <v-expansion-panel>
    <v-expansion-panel-header
      class="preview-header"
    >
      <v-icon>mdi-message-text-outline</v-icon>
      <span
        class="ml-4"
      >
        Preview Message
      </span>
    </v-expansion-panel-header>
    <v-expansion-panel-content>
      <div
        class="preview-message-container"
      >
        <fieldset
          class="preview-message"
        >
          <legend>Message</legend>
          <div
            v-html="previewMessageBody"
            :class="{ 'preview-message-conversation-body': type === 'CONVERSATION' }"
            class="preview-message-body"
          ></div>
          <v-overlay
            :absolute="true"
            :value="!isFetching && showRefreshButton"
            :opacity="0.75"
          >
            <v-btn
              @click="handlePreview(selectedParticipant)"
              color="primary"
            >
              Refresh
            </v-btn>
          </v-overlay>
          <v-overlay
            :absolute="true"
            :value="isFetching"
            :opacity="0.75"
          >
            <v-progress-circular
              indeterminate
              size="64"
            ></v-progress-circular>
          </v-overlay>
        </fieldset>
        <fieldset
          class="preview-participant-list"
          outlined
        >
          <legend>Preview As</legend>
          <v-list
            dense
            subheader
          >
            <v-list-item-group
              v-model="selectedParticipant"
            >
              <v-list-item
                v-for="(participant) in availableParticipants"
                :key="participant.id"
                :value="participant.id"
                @click="handlePreview(participant.id)"
                active-class="selected-participant"
                class="preview-participant"
                link
              >
                {{ participant.user.displayName }}
              </v-list-item>
            </v-list-item-group>
          </v-list>
        </fieldset>
      </div>
    </v-expansion-panel-content>
  </v-expansion-panel>
</v-expansion-panels>
</template>

<script>
import { mapGetters, mapActions, mapMutations } from "vuex";

export default {
  props: {
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
  },
  data: () => ({
    selectedParticipant: null,
    loaded: false,
    isFetching: false,
    showRefreshButton: false
  }),
  watch: {
    body: {
      handler() {
        if (this.selectedParticipant) {
          this.showRefreshButton = true;
        }
      },
      immediate: true
    }
  },
  computed: {
    ...mapGetters({
      allMessageContainers: "messagingMessageContainer/messageContainers",
      allConditionalTexts: "messagingConditionalText/messageConditionalTexts",
      conditionalText: "messagingConditionalText/messageConditionalText",
      exposures: "exposures/exposures",
      participants: "participants/participants",
      previewMessage: "messagingMessage/preview",
    }),
    container() {
      return this.allMessageContainers.find(messageContainer => messageContainer.id === this.containerId);
    },
    message() {
      return this.container.messages.find(message => message.id === this.messageId);
    },
    configuration() {
      return this.message.configuration;
    },
    content() {
      return this.message.content;
    },
    body() {
      return this.content?.html || "";
    },
    type() {
      return this.configuration?.type || "";
    },
    exposure() {
      return this.exposures.find(exposure => exposure.exposureId === this.exposureId);
    },
    ruleSets() {
      return this.message?.ruleSets || [];
    },
    conditionalTexts() {
      return this.content?.conditionalTexts || [];
    },
    pipedText() {
      return this.content?.pipedText || null;
    },
    availableParticipants() {
      return this.participants.toSorted((a,b) => (a.user.displayName > b.user.displayName) ? 1 : ((b.user.displayName > a.user.displayName) ? -1 : 0));
    },
    previewMessageBody() {
      return this.previewMessage?.body || "<p>Please select a user to preview their message.</p>";
    },
  },
  methods: {
    ...mapActions({
      fetchPreview: "messagingMessage/fetchPreview",
      fetchParticipants: "participants/fetchParticipants"
    }),
    ...mapMutations({
      setPreview: "messagingMessage/setPreview",
      setParticipants: "participants/setParticipants",
    }),
    async handlePreview(participantId) {
      this.selectedParticipant = participantId;
      this.isFetching = true;

      await this.fetchPreview([
        this.experimentId,
        this.exposureId,
        this.containerId,
        this.messageId,
        {
          id: participantId,
          body: this.body,
          ruleSets: this.ruleSets,
          conditionalTexts: this.conditionalTexts,
          pipedText: this.pipedText
        }
      ]);

      this.showRefreshButton = false;
      this.isFetching = false;
    },
    async initialize() {
      this.setPreview(null);
      await this.fetchParticipants(this.experimentId);
      // strip participantId
      this.setParticipants(
        this.participants.map(
          participant => {
            return {
              ...participant,
              participantId: null
            };
          }
        )
      );

    }
  },
  async mounted() {
    await this.initialize();
    this.loaded = true;
  }
}
</script>

<style scoped>
.v-expansion-panels {
  border: 1px solid #9e9e9e;
  border-radius: 4px;
  & .v-expansion-panel-content__wrap {
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
      background-color: rgba(29, 157, 255, .15);
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
