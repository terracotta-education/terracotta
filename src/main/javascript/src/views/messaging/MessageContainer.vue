<template>
  <div v-if="isLoaded">
    <v-row>
      <v-col cols="10">
        <v-text-field
          v-model="title"
          :disabled="readOnly"
          :hide-details="validationErrors.title === null"
          :error-messages="validationErrors.title"
          label="Message container title"
          variant="outlined"
        />
      </v-col>
    </v-row>

    <p class="text-medium-emphasis pb-0">
      This will create an unpublished message container in Canvas. Please note:
      the message container title is not the same as your message's subject
      line, which you will create for each treatment.
    </p>

    <v-divider />

    <v-tabs
      v-model="tab"
      class="tabs"
    >
      <v-tab value="settings">
        Settings
      </v-tab>
    </v-tabs>

    <v-divider />

    <v-window v-model="tab">
      <v-window-item
        value="settings"
        class="my-5 px-2"
      >
        <h4 class="mb-4">
          Settings at this level, the container level, will be applied to all
          treatments within the container. Settings can also be applied at the
          treatment level.
        </h4>

        <Type
          :type="type"
          :read-only="readOnly"
          :validated-errors="validationErrors.type"
          label="Send all messages in the container as:"
          @updated="updateType"
        />

        <ToConsentedOnly
          :selected="toConsentedOnly"
          :read-only="readOnly"
          :experiment="experiment"
          @updated="updateToConsentedOnly"
        />

        <ReplyTo
          v-if="showReplyTo"
          ref="replyTo"
          :reply-tos="replyToList"
          :read-only="readOnly"
          @updated="updateReplyTo"
        />

        <Scheduler
          :send-at="sendAt"
          :read-only="readOnly"
          :validated-errors="validationErrors.sendAt"
          label="Decide when you would like the message to be sent."
          @updated="updateSendAt"
        />
      </v-window-item>
    </v-window>
  </div>
</template>

<script setup>
import {
  ref,
  computed,
  onMounted,
  onBeforeUnmount,
} from "vue";

import {
  useRoute,
  useRouter
} from "vue-router";

import Swal from "sweetalert2";
import { editableMessageStatuses } from "@/helpers/messaging/status.js";
import {
  shrinkContainer,
  widenContainer,
  adjustBodyTopPadding,
  statusAlert,
  createStatusAlert
} from "@/helpers/ui-utils.js";
import {
  initValidations,
  validateContainer
} from "@/helpers/messaging/validation.js";

import ReplyTo from "@/views/messaging/components/form/ReplyTo.vue";
import Scheduler from "@/views/messaging/components/form/Scheduler.vue";
import ToConsentedOnly from "@/views/messaging/components/form/ToConsentedOnly.vue";
import Type from "@/views/messaging/components/form/Type.vue";

import { experiment as experimentModule } from "@/store/experiment.module";
import { container as messagingMessageContainerModule } from "@/store/messaging/container.module";
import { alert as alertModule } from "@/store/alert.module";

defineOptions({
  name: "MessageContainer"
});

const route = useRoute();
const router = useRouter();


const experimentStore = experimentModule();
const messagingMessageContainerStore = messagingMessageContainerModule();
const alertStore = alertModule();

const isLoaded = ref(false);
const tab = ref("settings");
const container = ref(null);
const validationErrors = ref(null);
const replyTo = ref(null);

const experiments = computed(() => {
  return experimentStore.experiments || [];
});

const allMessageContainers = computed(() => {
  return messagingMessageContainerStore.messageContainers || [];
});

const alertStatuses = computed(() => {
  return alertStore.statuses;
});

const experimentId = computed(() => {
  return Number.parseInt(route.params.experimentId, 10);
});

const exposureId = computed(() => {
  return route.query.exposureId;
});

const experiment = computed(() => {
  return experiments.value.find(
    item => item.experimentId === experimentId.value
  );
});

const versions = {
  multiple: "MULTIPLE",
  single: "SINGLE"
};

const modes = {
  new: "NEW",
  edit: "EDIT"
};

const version = computed(() => {
  return route.query.version || versions.multiple;
});

const mode = computed(() => {
  return route.query.mode || modes.new;
});

const isNew = computed(() => {
  return mode.value === modes.new;
});

const single = computed(() => {
  return version.value === versions.single;
});

const configuration = computed(() => {
  return container.value?.configuration || {};
});

const configurationId = computed(() => {
  return configuration.value.id;
});

const containerId = computed(() => {
  return container.value?.id || null;
});

const replyToList = computed({
  get() {
    return configuration.value?.replyTo || [];
  },

  set(value) {
    configuration.value.replyTo = value;
  }
});

const sendAt = computed({
  get() {
    return configuration.value?.sendAt || null;
  },

  set(value) {
    configuration.value.sendAt = value;
    configuration.value.sendAtTimezoneOffset =
      new Date().getTimezoneOffset();
  }
});

const title = computed({
  get() {
    return configuration.value?.title || "";
  },

  set(value) {
    configuration.value.title = value;
  }
});

const toConsentedOnly = computed({
  get() {
    return configuration.value?.toConsentedOnly || false;
  },

  set(value) {
    configuration.value.toConsentedOnly = value;
  }
});

const type = computed({
  get() {
    return configuration.value?.type || null;
  },

  set(value) {
    configuration.value.type = value;
  }
});

const readOnly = computed(() => {
  return !editableMessageStatuses.includes(configuration.value.status);
});

const showReplyTo = computed(() => {
  return configuration.value.type === "EMAIL";
});

const updateReplyTo = value => {
  replyToList.value = value.map(item => ({
    ...item,
    containerConfigurationId: configurationId.value
  }));
};

const updateSendAt = value => {
  sendAt.value = value;
};

const updateType = value => {
  type.value = value;
};

const updateToConsentedOnly = value => {
  toConsentedOnly.value = value;
};

const createDraftContainer = () => {
  return {
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
          email: experiment.value?.createdByEmail || ""
        }
      ],
      sendAt: null,
      sendAtTimezoneOffset: new Date().getTimezoneOffset(),
      type: "NONE",
      order: 1
    }
  };
};

const loadExistingContainer = () => {
  const existingContainer = allMessageContainers.value.find(
    messageContainer => messageContainer.id === route.query.containerId
  );

  return {
    ...existingContainer,
    configuration: {
      ...existingContainer.configuration,
      sendAtTimezoneOffset: new Date().getTimezoneOffset()
    }
  };
};

const saveExit = async () => {
  const replyToValid = replyTo.value
    ? await replyTo.value.updateReplyTo()
    : true;

  if (!replyToValid) {
    return false;
  }

  validationErrors.value = validateContainer(container.value);

  if (validationErrors.value.hasErrors) {
    Swal.fire("Please complete all required sections.");
    return false;
  }

  if (!containerId.value) {
    const newContainer =
      await messagingMessageContainerStore.create([
        experimentId.value,
        exposureId.value,
        single.value
      ]);

    newContainer.configuration.replyTo = replyToList.value.map(item => ({
      ...item,
      containerConfigurationId: newContainer.configuration.id,
      messageConfigurationId: null
    }));

    container.value = {
      ...newContainer,
      configuration: {
        ...newContainer.configuration,
        title: title.value,
        sendAt: sendAt.value,
        sendAtTimezoneOffset: new Date().getTimezoneOffset(),
        type: type.value,
        toConsentedOnly: toConsentedOnly.value
      }
    };
  }

  await messagingMessageContainerStore.update([
    experimentId.value,
    exposureId.value,
    containerId.value,
    container.value
  ]);

  createStatusAlert(
    statusAlert(alertStatuses.value.success, "Message container saved successfully.")
  );

  router.push({
    name: "ExperimentSummary",
    params: {
      experimentId: experimentId.value
    }
  });

  return true;
};

onMounted(() => {
  widenContainer();
  adjustBodyTopPadding();

  container.value = isNew.value
    ? createDraftContainer()
    : loadExistingContainer();

  validationErrors.value = initValidations().container;
  isLoaded.value = true;
});

onBeforeUnmount(() => {
  shrinkContainer();
  adjustBodyTopPadding("");
});

defineExpose({
  saveExit
});
</script>

<style lang="scss">
.v-expansion-panels {
  &,
  & > div {
    width: 100%;
  }
}

.terracotta-builder {
  .v-expansion-panel-title {
    &--active {
      border-bottom: 2px solid map.get($grey, "lighter");
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
    border-top: 1px solid map.get($grey, "lighter");
    border-bottom: 1px solid map.get($grey, "lighter");
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
