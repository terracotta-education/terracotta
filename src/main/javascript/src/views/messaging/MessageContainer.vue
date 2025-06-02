<template>
  <div
    v-if="isLoaded"
  >
    <v-row>
      <div
        class="col-10"
      >
        <v-text-field
          v-model="title"
          :disabled="readOnly"
          :hide-details="validationErrors.title === null"
          :error-messages="validationErrors.title"
          label="Message container title"
          outlined
        />
      </div>
    </v-row>
    <p
      class="grey--text text--darken-2 pb-0"
    >
      This will create an unpublished message container in Canvas. Please note: the message container title is not the same as your message's subject line, which you will create for each treatment.
    </p>
    <v-divider />
    <v-tabs
      v-model="tab"
      class="tabs"
    >
      <v-tab>Settings</v-tab>
    </v-tabs>
    <v-divider />
    <v-tabs-items
      v-model="tab"
    >
      <v-tab-item
        class="my-5 px-2"
      >
        <h4
          class="mb-4"
        >
          Settings at this level -- the container level -- will be applied to all treatments within the container. Settings can also be applied at the treatment level.
        </h4>
        <type
          :type="type"
          :readOnly="readOnly"
          :validatedErrors="validationErrors.type"
          @updated="updateType"
          label="Send all messages in the container as:"
        />
        <to-consented-only
          :selected="toConsentedOnly"
          :readOnly="readOnly"
          :experiment="experiment"
          @updated="updateToConsentedOnly"
        />
        <reply-to
          v-if="showReplyTo"
          :replyTos="replyTo"
          :readOnly="readOnly"
          @updated="updateReplyTo"
          ref="replyTo"
        />
        <scheduler
          :sendAt="sendAt"
          :readOnly="readOnly"
          :validatedErrors="validationErrors.sendAt"
          @updated="updateSendAt"
          label="Decide when you would like the message to be sent."
        />
      </v-tab-item>
    </v-tabs-items>
  </div>
</template>

<script>
import { mapGetters, mapActions } from "vuex";
import { editableMessageStatuses } from "@/helpers/messaging/status.js";
import { shrinkContainer, widenContainer, adjustBodyTopPadding } from "@/helpers/ui-utils.js";
import { initValidations, validateContainer } from "@/helpers/messaging/validation.js";
import ReplyTo from "@/views/messaging/components/form/ReplyTo.vue";
import Scheduler from "@/views/messaging/components/form/Scheduler.vue";
import ToConsentedOnly from "@/views/messaging/components/form/ToConsentedOnly.vue";
import Type from "@/views/messaging/components/form/Type.vue";

export default {
  name: "MessageContainer",
  components: {
    ReplyTo,
    Scheduler,
    ToConsentedOnly,
    Type
  },
  data: () => ({
    isLoaded: false,
    tab: null,
    container: null,
    validationErrors: null
  }),
  computed: {
    ...mapGetters({
      editMode: "navigation/editMode",
      experiments: "experiment/experiments",
      allMessageContainers: "messagingMessageContainer/messageContainers"
    }),
    experiment() {
      return this.experiments.find(e => e.experimentId === this.experimentId);
    },
    experimentId() {
      return this.$route.params.experimentId;
    },
    exposureId() {
      return this.$route.params.exposureId;
    },
    configuration() {
      return this.container.configuration;
    },
    configurationId() {
      return this.configuration.id;
    },
    containerId() {
      return this.container.id;
    },
    version() {
      return this.$route.params.version || this.versions.multiple;
    },
    mode() {
      return this.$route.params.mode || this.modes.new;
    },
    versions() {
      return {
        multiple: "MULTIPLE",
        single: "SINGLE"
      }
    },
    modes() {
      return {
        new: "NEW",
        edit: "EDIT"
      }
    },
    isNew() {
      return this.mode === this.modes.new;
    },
    single() {
      return this.version === this.versions.single;
    },
    messageTypes() {
      return [
        {id: "CONVERSATION", label: "Conversation"},
        {id: "EMAIL", label: "Email"}
      ]
    },
    replyTo: {
      get() {
        return this.configuration?.replyTo || [];
      },
      set(newReplyTo) {
        this.configuration.replyTo = newReplyTo;
      }
    },
    sendAt: {
      get() {
        return this.configuration?.sendAt || null;
      },
      set(newSendAt) {
        this.configuration.sendAt = newSendAt;
        this.configuration.sendAtTimezoneOffset = new Date().getTimezoneOffset();
      }
    },
    title: {
      get() {
        return this.configuration?.title || "";
      },
      set(newTitle) {
        this.configuration.title = newTitle;
      }
    },
    toConsentedOnly: {
      get() {
        return this.configuration?.toConsentedOnly || false;
      },
      set(newToConsentedOnly) {
        this.configuration.toConsentedOnly = newToConsentedOnly;
      }
    },
    type: {
      get() {
        return this.configuration?.type || null;
      },
      set(newType) {
        this.configuration.type = newType;
      }
    },
    readOnly() {
      return !editableMessageStatuses.includes(this.configuration.status);
    },
    showReplyTo() {
      return this.configuration.type === "EMAIL";
    },

    getSaveExitPage() {
      return this.editMode?.callerPage?.name || "Home";
    }
  },
  methods: {
    ...mapActions({
      createMessageContainer: "messagingMessageContainer/create",
      updateMessageContainer: "messagingMessageContainer/update"
    }),
    updateReplyTo(newReplyTo) {
      this.replyTo = newReplyTo.map(
        r => ({
          ...r,
          containerConfigurationId: this.configurationId
        })
      );
    },
    updateSendAt(newSendAt) {
      this.sendAt = newSendAt;
    },
    updateType(newType) {
      this.type = newType;
    },
    updateToConsentedOnly(newToConsentedOnly) {
      this.toConsentedOnly = newToConsentedOnly;
    },
    async saveExit() {
      // wait for reply-to child to complete validation
      let replyToValid = this.$refs.replyTo ? await this.$refs.replyTo.updateReplyTo() : true;

      if (!replyToValid) {
        // reply-to validation failed; do not proceed with saving and exiting
        return false;
      }

      this.validationErrors = validateContainer(this.container);

      if (this.validationErrors.hasErrors) {
        this.$swal("Please complete all required sections.");
        return false;
      }

      if (!this.containerId) {
        // create new message container
        let newContainer = await this.createMessageContainer(
          [
            this.experimentId,
            this.exposureId,
            this.single
          ]
        );

        // update replyTos to add newly-added ones
        newContainer.configuration.replyTo = [
          ...this.replyTo
            .map(r => ({
              ...r,
              containerConfigurationId: newContainer.configuration.id,
              messageConfigurationId: null
            }))
          ];

        // update newContainer with configuration
        this.container = {
          ...newContainer,
          configuration: {
            ...newContainer.configuration,
            title: this.title,
            sendAt: this.sendAt,
            sendAtTimezoneOffset: new Date().getTimezoneOffset(),
            type: this.type,
            toConsentedOnly: this.toConsentedOnly
          }
        };
      }

      // update existing message container
      await this.updateMessageContainer(
        [
          this.experimentId,
          this.exposureId,
          this.containerId,
          this.container
        ]
      );
      this.$router.push({
        name: "ExperimentSummary",
        params: {
          experiment_id: this.experimentId,
          exposure_id: this.exposureId
        }
      });
    }
  },
  async mounted() {
    widenContainer();
    adjustBodyTopPadding();

    if (this.isNew) {
      // is new; create new message container
      this.container = {
        configuration: {
          id: null,
          containerId: null,
          status: "UNPUBLISHED",
          title: null,
          toConsentedOnly: false,
          replyTo: [
              {
                  id: null,
                  containerConfigurationId: null,
                  messageConfigurationId: null,
                  email: this.experiment.createdByEmail || "",
              }
          ],
          sendAt: null,
          sendAtTimezoneOffset: new Date().getTimezoneOffset(),
          type: "NONE",
          order: 1
        }
      }
    } else {
      // is edit; use existing message container
      this.container = this.allMessageContainers.find(mc => mc.id === this.$route.params.containerId);
      this.container = {
        ...this.container,
        configuration: {
          ...this.container.configuration,
          // ensure timezone offeset is set to current timezone
          sendAtTimezoneOffset: new Date().getTimezoneOffset()
        }
      }
    }

    this.validationErrors = initValidations().container;
    this.isLoaded = true;
  },
  beforeUnmount() {
    shrinkContainer();
    adjustBodyTopPadding("");
  }
}
</script>

<style lang="scss">
.v-expansion-panels {
  &, & > div {
    width: 100%;
  }
}
.terracotta-builder {
  .v-expansion-panel-header {
    &--active {
      border-bottom: 2px solid map-get($grey, "lighten-2");
    }
    h2 {
      display: inline-block;
      max-height: 1em;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;

      > .question-text {
        display: inline;
        font-size: 16px;
        line-height: 1em;
        margin: 0;
        padding: 0;
        vertical-align: middle;
      }
    }
  }
  .tabs {
    border-top: 1px solid map-get($grey, "lighten-2");
    border-bottom: 1px solid map-get($grey, "lighten-2");
  }
  .header-container {
    width: 100%;
    min-height: fit-content;
    padding-bottom: 10px;
  }
  h4.label-treatment,
  h4.label-condition-name {
    display: inline;
    padding-right: 5px;
    padding-bottom: 0;
  }
}
</style>
