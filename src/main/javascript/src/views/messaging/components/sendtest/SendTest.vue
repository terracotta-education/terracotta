<template>
  <v-expansion-panels class="my-6">
    <v-expansion-panel elevation="0" @click="panelExpansion">
      <v-expansion-panel-title class="send-test-header">
        <v-icon>mdi-play-outline</v-icon>

        <span class="ml-4">
          Send Test Message
        </span>
      </v-expansion-panel-title>

      <v-expansion-panel-text>
        <v-row>
          <v-col cols="9">
            <span>
              This test message won't contain your content; its purpose is
              to ensure that your message sends properly.
              <b>It will only be sent to you.</b>
              The Preview Message functionality allows you to see your message
              as your recipients will see it.
            </span>
          </v-col>

          <v-col cols="3">
            <v-btn
              :disabled="!isValidEmail || isSending"
              class="d-flex mt-4"
              color="primary"
              @click="sendMessage"
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
              v-if="!isSending && isSent"
              class="send-status"
            >
              Email sent!
            </span>
          </v-col>
        </v-row>

        <v-row class="mt-6">
          <v-col
            cols="9"
            class="d-flex flex-row align-left"
          >
            <span class="address">
              Send test to:
            </span>

            <div
              v-if="!isEditing"
              class="d-flex flex-row align-left ml-4"
            >
              <span class="address font-weight-bold">
                <u>{{ to }}</u>
              </span>

              <v-btn
                class="ml-2"
                color="primary"
                variant="text"
                @click="editEmail"
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
                variant="outlined"
                density="compact"
              />

              <v-btn
                class="ml-2"
                color="primary"
                variant="text"
                @click="cancelEdit"
              >
                Cancel
              </v-btn>

              <v-btn
                :disabled="!isValidEmail"
                class="ml-2"
                color="primary"
                variant="text"
                @click="saveEdit"
              >
                Save
              </v-btn>
            </div>
          </v-col>
        </v-row>
      </v-expansion-panel-text>
    </v-expansion-panel>
  </v-expansion-panels>
</template>

<script setup>
import {
  ref,
  computed,
  watch,
  onMounted
} from "vue";

import { validations } from "@/helpers/messaging/validation.js";
import { deleteAttributesFromElement } from "@/helpers/ui-utils.js";

import { container as messagingMessageContainerModule } from "@/store/messaging/container.module";
import { message as messagingMessageModule } from "@/store/messaging/message.module";

defineOptions({
  name: "SendTest"
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
  email: {
    type: String,
    required: true
  }
});

const messagingMessageContainerStore = messagingMessageContainerModule();
const messagingMessageStore = messagingMessageModule();

const to = ref(null);
const toPreEdit = ref(null);
const isEditing = ref(false);
const isSending = ref(false);
const isSent = ref(false);
const validationErrors = ref(null);

watch(
  () => props.email,
  value => {
    to.value = value;
  },
  {
    immediate: true
  }
);

const allMessageContainers = computed(() => {
  return messagingMessageContainerStore.messageContainers || [];
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

const subject = computed(() => {
  return configuration.value.subject;
});

const html = computed(() => {
  return content.value.html;
});

const isValidEmail = computed(() => {
  return Boolean(
    to.value &&
      /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(to.value)
  );
});

const sendMessage = async () => {
  isSending.value = true;

  await messagingMessageStore.sendTest([
    props.experimentId,
    props.exposureId,
    props.containerId,
    props.messageId,
    {
      to: to.value,
      subject: subject.value,
      message: html.value
    }
  ]);

  isSending.value = false;
  isSent.value = true;
};

const editEmail = () => {
  isEditing.value = true;
  toPreEdit.value = to.value;
};

const saveEdit = () => {
  isEditing.value = false;
};

const cancelEdit = () => {
  isEditing.value = false;
  to.value = toPreEdit.value;
};

const panelExpansion = () => {
  window.setTimeout(() => {
    deleteAttributesFromElement(
      ".v-expansion-panel",
      ["aria-expanded"]
    );
  }, 1000);
};

onMounted(() => {
  validationErrors.value = validations.message;

  deleteAttributesFromElement(
    ".v-expansion-panel",
    ["aria-expanded"]
  );
});
</script>

<style lang="scss" scoped>
.v-expansion-panels {
  :deep(.v-expansion-panel) {
    margin-bottom: 0 !important;
  }

  :deep(.v-expansion-panel-text__wrapper) {
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
  color: map.get($grey, "darker");
  font-size: 0.9em;
}
</style>
