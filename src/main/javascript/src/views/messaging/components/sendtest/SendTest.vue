<template>
<v-expansion-panels
  class="my-6"
  flat
>
  <v-expansion-panel>
    <v-expansion-panel-header
      class="send-test-header"
    >
      <v-icon>mdi-play-outline</v-icon>
      <span
        class="ml-4"
      >
        Send Test Message
      </span>
    </v-expansion-panel-header>
    <v-expansion-panel-content>
      <v-row>
        <v-col
          cols="9"
        >
          <span>
            This test message won&apos;t contain your content; its purpose is to ensure that your message sends properly.
            <b>It will only be sent to you.</b>
            The Preview Message functionality allows you to see your message as your recipients will see it.
          </span>
        </v-col>
        <v-col
          cols="3"
        >
          <v-btn
            :disabled="!isValidEmail || isSending"
            @click="sendMessage"
            class="d-flex mt-4"
            color="primary"
          >
            Send Test Now
          </v-btn>
          <span
            v-if="isSending"
            class="send-status"
          >
            Sending...
          </span>
          <span
            v-if="!isSending &&isSent"
            class="send-status"
          >
            Email sent!
          </span>
        </v-col>
      </v-row>
      <v-row
        class="mt-6">
        <v-col
          cols="9"
          class="d-flex flex-row align-left"
        >
          <span
            class="address"
          >
            Send test to:
          </span>
          <div
            v-if="!isEditing"
            class="d-flex flex-row align-left ml-4"
          >
            <span
              class="address font-weight-bold"
            >
              <u>{{ to }}</u>
            </span>
            <v-btn
              class="ml-2"
              color="primary"
              @click="editEmail"
              text
            >
              Edit
            </v-btn>
          </div>
          <div
            v-else
            class="d-flex flex-row align-left ml-4"
          >
            <v-text-field
              v-model="to"
              :hide-details="isValidEmail"
              :error-messages="!isValidEmail ? ['Invalid email address'] : null"
              label="Email"
              type="email"
              outlined
              dense
            />
            <v-btn
              class="ml-2"
              color="primary"
              @click="cancelEdit"
              text
            >
              Cancel
            </v-btn>
            <v-btn
              :disabled="!isValidEmail"
              class="ml-2"
              color="primary"
              @click="saveEdit"
              text
            >
              Save
            </v-btn>
          </div>
        </v-col>
      </v-row>
    </v-expansion-panel-content>
  </v-expansion-panel>
</v-expansion-panels>
</template>

<script>
import { mapActions, mapGetters } from "vuex";
import { validations } from "@/helpers/messaging/validation.js";

export default {
  name: "SendTest",
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
    email: {
      type: String,
      required: true,
    }
  },
  data: () => ({
    to: null,
    toPreEdit: null,
    isEditing: false,
    isSending: false,
    isSent: false,
    validationErrors: null
  }),
  watch: {
    email: {
      handler(newEmail) {
        this.to = newEmail;
      },
      immediate: true
    }
  },
  computed: {
    ...mapGetters({
      allMessageContainers: "messagingMessageContainer/messageContainers",
      allConditionalTexts: "messagingConditionalText/messageConditionalTexts",
      conditionalText: "messagingConditionalText/messageConditionalText"
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
    conditionalTexts() {
      return this.allConditionalTexts || [];
    },
    subject() {
      return this.configuration.subject;
    },
    html() {
      return this.content.html;
    },
    isValidEmail() {
      if (!this.to) {
        return false;
      }

      return this.to && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.to);
    }
  },
  methods: {
    ...mapActions({
      send: "messagingMessage/sendTest",
    }),
    async sendMessage() {
      this.isSending = true;

      await this.send([
        this.experimentId,
        this.exposureId,
        this.containerId,
        this.messageId,
        {
          to: this.to,
          subject: this.subject,
          message: this.html
        }
      ]);

      this.isSending = false;
      this.isSent = true;
    },
    editEmail() {
      this.isEditing = true;
      this.toPreEdit = this.to;
    },
    saveEdit() {
      this.isEditing = false;
    },
    cancelEdit() {
      this.isEditing = false;
      this.to = this.toPreEdit;
    }
  },
  mounted() {
    this.validationErrors = validations.message;
  }
};
</script>

<style scoped>
.v-expansion-panels {
  border: 1px solid #9e9e9e;
  border-radius: 4px;
  & .v-expansion-panel-content__wrap {
    padding: 10px 20px;
  }
}
.send-test-header {
  display: flex;
  align-content: start;
  > * {
    max-width: fit-content;
  }
}
.address {
  max-height: fit-content;
  margin: auto 0;
}
.send-status {
  min-width: 100%;
  margin-top: 8px;
  color: #9e9e9e;
  font-size: 0.9em;
}
</style>
