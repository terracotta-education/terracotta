<template>
  <div>
    <PageLoading
      v-if="isLoadingAllMessageRuleAssignments"
      :display="true"
      message="Preparing your message. Please wait."
    />

    <div
      v-if="!isLoadingAllMessageRuleAssignments && loaded"
      class="message-container"
    >
      <div>
        <v-row
          class="main-messages-header mb-3"
          justify="space-between"
          no-gutters
        >
          <v-col class="messages-header">
            <v-row
              class="sub-header"
              justify="space-between"
              no-gutters
            >
              <div class="messages-icon-container mr-4">
                <v-icon color="white">
                  mdi-message-text-outline
                </v-icon>
              </div>

              <h2 class="pb-0 my-auto">
                {{ container.configuration.title }}
              </h2>
            </v-row>
          </v-col>

          <v-col class="messages-header">
            <v-row
              class="sub-header"
              justify="space-between"
              no-gutters
            >
              <div class="sub-header-chip mr-8">
                <h4 class="mr-2 my-auto">
                  Message Type
                </h4>

                <v-chip
                  color="#EBFFEE"
                  variant="flat"
                  density="compact"
                  label
                >
                  {{ messageTypeLabel }}
                </v-chip>
              </div>

              <div class="sub-header-chip">
                <h4 class="mr-2 my-auto">
                  Treatment
                </h4>

                <v-chip
                  v-if="container.messages.length > 1"
                  :color="conditionColorMapping[getConditionName(message.conditionId)]"
                  variant="flat"
                  density="compact"
                  label
                >
                  {{ getConditionName(message.conditionId) }}
                </v-chip>

                <v-chip
                  v-else
                  color="lightgrey"
                  variant="flat"
                  density="compact"
                  label
                >
                  Only One Version
                </v-chip>
              </div>
            </v-row>
          </v-col>
        </v-row>

        <v-tabs
          v-model="tab"
          class="tabs"
        >
          <v-tab value="treatment">
            Treatment
          </v-tab>

          <v-tab value="settings">
            Settings
          </v-tab>
        </v-tabs>

        <v-divider />

        <v-window v-model="tab">
          <v-window-item value="treatment">
            <div class="treatment-tab d-flex flex-row justify-space-between">
              <div
                class="treatment-tab-message d-flex flex-column px-2"
              >
                <div>
                  <v-switch
                    v-model="enabled"
                    :disabled="readOnly"
                    :ripple="false"
                    label="Include a message for this treatment"
                    messages="(Turning this off means students in this treatment group will not receive a message.)"
                    :class="['enabled-switch', { 'enabled-switch--on': enabled }]"
                    inset
                  />
                </div>

                <div
                  v-show="enabled"
                  class="treatment-tab-message-container justify-space-between"
                >
                  <div class="col-12">
                    <Recipients
                      :experiment-id="experimentId"
                      :exposure-id="exposureId"
                      :container-id="containerId"
                      :message-id="messageId"
                      :content-id="contentId"
                      :read-only="readOnly"
                      :disabled="!hasMessageRuleAssignments"
                      :max-rule-count="maxRuleCount"
                      :validated-errors="validationErrors.recipients"
                    />

                    <v-text-field
                      v-model="subject"
                      :disabled="readOnly"
                      :hide-details="validationErrors.subject === null"
                      :error-messages="validationErrors.subject"
                      label="Subject line"
                      class="mb-6"
                      variant="outlined"
                      required
                      density="compact"
                    />

                    <div
                      :class="{ 'validation-error': validationErrors.body !== null }"
                      class="editor-container"
                    >
                      <TipTapEditor
                        :content="initialContent"
                        :editor-type="editor"
                        :read-only="readOnly"
                        :conditional-text-to-place="conditionalTextToPlace"
                        :piped-text-to-place="pipedTextToPlace"
                        :allow-mentions="true"
                        required
                        @edited="handleEditedBody"
                        @cursor="handleCursor"
                      />

                      <EditorSubMenu
                        :experiment-id="experimentId"
                        :exposure-id="exposureId"
                        :container-id="containerId"
                        :message-id="messageId"
                        :content-id="contentId"
                        :read-only="readOnly"
                        :piped-text="pipedText"
                        @insert-piped-text="insertPipedText"
                        @insert-conditional-text="insertConditionalText"
                        @add-conditional-text="addConditionalText"
                        @edit-conditional-text="editConditionalText"
                      />
                    </div>

                    <div
                      v-if="validationErrors.result !== null"
                      class="v-text-field__details pt-2 px-3"
                    >
                      <div
                        class="v-messages error-text"
                        role="alert"
                      >
                        <div class="v-messages__wrapper">
                          <div class="v-messages__message">
                            {{ validationErrors.body }}
                          </div>
                        </div>
                      </div>
                    </div>

                    <div
                      v-if="!readOnly"
                      class="my-4"
                    >
                      For personalized merge tags, upload a CSV below. For instructions on formatting your CSV, please see
                      <a
                        href="https://terracotta-education.atlassian.net/wiki/spaces/TC/pages/edit-v2/509378566?draftShareId=9609d543-9da1-4d49-8135-559d604f34d6"
                        target="_blank"
                        rel="noopener noreferrer"
                      >
                        this knowledge base article
                      </a>.
                    </div>

                    <div
                      v-if="!readOnly"
                      class="my-4"
                    >
                      <v-menu
                        v-if="messagesAvailableToCopy.length > 0"
                        v-model="copyMenuOpen"
                        close-on-click
                        close-on-content-click
                        location="bottom"
                      >
                        <template #activator="{ props: menuProps }">
                          <v-btn
                            v-bind="menuProps"
                            color="primary"
                            elevation="0"
                            class="copy-btn mb-3 mt-3"
                            variant="text"
                          >
                            Copy message from
                            <v-icon>mdi-chevron-down</v-icon>
                          </v-btn>
                        </template>

                        <v-list>
                          <template
                            v-for="copyContainer in messagesAvailableToCopy"
                            :key="copyContainer.id"
                          >
                            <v-menu
                              v-if="copyContainer.messages.length > 0 && hasMessagesNotCurrent(copyContainer.messages)"
                              transition="slide-x-transition"
                              open-on-hover
                              close-on-click
                              close-on-content-click
                              location="end"
                            >
                              <template #activator="{ props: nestedMenuProps }">
                                <v-list-item
                                  v-bind="nestedMenuProps"
                                >
                                  <v-list-item-title>
                                    {{ copyContainer.configuration.title }}
                                  </v-list-item-title>

                                  <template #append>
                                    <v-icon>mdi-menu-right</v-icon>
                                  </template>
                                </v-list-item>
                              </template>

                              <v-list>
                                <template
                                  v-for="copyMessage in copyContainer.messages"
                                  :key="copyMessage.id"
                                >
                                  <v-list-item
                                    v-if="copyMessage.id !== messageId"
                                    @click="copy(copyMessage)"
                                  >
                                    <v-list-item-title>
                                      Message

                                      <v-chip
                                        v-if="copyContainer.messages.length > 1"
                                        :color="conditionColorMapping[getConditionName(copyMessage.conditionId)]"
                                        variant="flat"
                                        density="compact"
                                        label
                                      >
                                        {{ getConditionName(copyMessage.conditionId) }}
                                      </v-chip>
                                    </v-list-item-title>
                                  </v-list-item>
                                </template>
                              </v-list>
                            </v-menu>
                          </template>
                        </v-list>
                      </v-menu>

                      <v-btn
                        :disabled="readOnly"
                        color="primary"
                        variant="text"
                        @click="showPipedTextUploader = true"
                      >
                        UPLOAD MERGE TAGS CSV
                      </v-btn>

                      <span
                        v-if="isUploadSuccessful"
                        class="upload-status"
                      >
                        Uploaded "{{ pipedTextUploadedFilename }}" successfully!
                      </span>

                      <v-overlay
                        v-model="showPipedTextUploader"
                        :opacity="0.5"
                        location="center center"
                      >
                        <PipedTextFileUploader
                          :experiment-id="experimentId"
                          :exposure-id="exposureId"
                          :container-id="containerId"
                          :message-id="messageId"
                          :content-id="contentId"
                          :read-only="readOnly"
                          @close="showPipedTextUploader = false"
                        />
                      </v-overlay>

                      <Preview
                        v-if="!readOnly"
                        :experiment-id="experimentId"
                        :exposure-id="exposureId"
                        :container-id="containerId"
                        :message-id="messageId"
                        :content-id="contentId"
                      />

                      <SendTest
                        v-if="type === 'EMAIL' && !readOnly"
                        :experiment-id="experimentId"
                        :exposure-id="exposureId"
                        :container-id="containerId"
                        :message-id="messageId"
                        :email="container.ownerEmail"
                        @validated="handleValidationErrors"
                      />
                    </div>
                  </div>
                </div>
              </div>

              <div
                v-if="openConditionalTextEditor"
                class="treatment-tab-conditional-text"
              >
                <ConditionalText
                  :experiment-id="experimentId"
                  :exposure-id="exposureId"
                  :container-id="containerId"
                  :message-id="messageId"
                  :content-id="contentId"
                  :max-rule-count="maxRuleCount"
                  :validated-errors="validationErrors.conditionalText"
                  :piped-text="pipedText"
                  @conditional-text-created="insertConditionalText"
                  @conditional-text-updated="updateConditionalText"
                  @cancel="cancelConditionalText"
                />
              </div>
            </div>
          </v-window-item>

          <v-window-item value="settings">
            <div class="settings-container d-flex flex-column px-2">
              <ToConsentedOnly
                :selected="toConsentedOnly"
                :experiment="experiment"
                :read-only="readOnly"
                @updated="updateToConsentedOnly"
              />

              <Type
                :type="type"
                :read-only="readOnly"
                label="Send all messages in this treatment as:"
                @updated="updateType"
              />

              <ReplyTo
                v-if="showReplyTo"
                ref="replyTo"
                :reply-tos="replyTo"
                :required="false"
                :read-only="readOnly"
                @updated="updateReplyTo"
              />

              <Scheduler
                :send-at="sendAt"
                :read-only="readOnly"
                label="Decide when you would like the message to be sent."
                @updated="updateSendAt"
              />
            </div>
          </v-window-item>
        </v-window>
      </div>
    </div>
  </div>
</template>

<script setup>
import {
  ref,
  computed,
  nextTick,
  onMounted,
  onBeforeUnmount,
  watch
} from "vue";

import {
  useRoute,
  useRouter
} from "vue-router";

import Swal from "sweetalert2";
import { message as messageStatus } from "@/helpers/messaging/status.js";
import {
  shrinkContainer,
  widenContainer,
  adjustBodyTopPadding,
  addAttributesToObservedElement,
  statusAlert,
  createStatusAlert
} from "@/helpers/ui-utils.js";
import {
  initValidations,
  validateMessage
} from "@/helpers/messaging/validation.js";

import ConditionalText from "@/views/messaging/components/conditional/ConditionalText.vue";
import EditorSubMenu from "@/views/messaging/components/menu/editorsubmenu/EditorSubMenu.vue";
import PipedTextFileUploader from "@/views/messaging/components/piped/PipedTextFileUploader.vue";
import PageLoading from "@/components/PageLoading.vue";
import Preview from "@/views/messaging/components/preview/Preview.vue";
import Recipients from "@/views/messaging/components/recipients/Recipients.vue";
import ReplyTo from "@/views/messaging/components/form/ReplyTo.vue";
import Scheduler from "@/views/messaging/components/form/Scheduler.vue";
import SendTest from "@/views/messaging/components/sendtest/SendTest.vue";
import TipTapEditor from "@/components/editor/TipTapEditor.vue";
import ToConsentedOnly from "@/views/messaging/components/form/ToConsentedOnly.vue";
import Type from "@/views/messaging/components/form/Type.vue";

import { condition as conditionModule } from "@/store/condition.module";
import { experiment as experimentModule } from "@/store/experiment.module";
import { container as messagingMessageContainerModule } from "@/store/messaging/container.module";
import { message as messagingMessageModule } from "@/store/messaging/message.module";
import { conditionaltext as messagingConditionalTextModule } from "@/store/messaging/conditionaltext.module";
import { alert as alertModule } from "@/store/alert.module";

defineOptions({
  name: "MessageEditor"
});

const route = useRoute();
const router = useRouter();


const conditionStore = conditionModule();
const experimentStore = experimentModule();
const messagingMessageContainerStore = messagingMessageContainerModule();
const messagingMessageStore = messagingMessageModule();
const messagingConditionalTextStore = messagingConditionalTextModule();
const alertStore = alertModule();

const tab = ref("treatment");
const initialContent = ref(null);
const messagesAvailableToCopy = ref([]);
const copyMenuOpen = ref(false);
const openConditionalTextEditor = ref(false);
const addingNewConditionalText = ref(false);
const conditionalTextToPlace = ref(null);
const pipedTextToPlace = ref(null);
const editorCursorPosition = ref(null);
const loaded = ref(false);
const maxRuleCount = ref(8);
const validationErrors = ref(null);
const showPipedTextUploader = ref(false);
const isUploadSuccessful = ref(false);
const replyTo = ref(null);

const conditionColorMapping = computed(() => conditionStore.conditionColorMapping || {});
const experiment = computed(() => experimentStore.experiment);
const allMessageContainers = computed(() => messagingMessageContainerStore.messageContainers || []);
const allMessageRuleAssignments = computed(() => messagingMessageStore.assignments || []);
const isLoadingAllMessageRuleAssignments = computed(() => messagingMessageStore.isLoading);
const messageConditionalTextEditId = computed(() => messagingConditionalTextStore.messageConditionalTextEditId);
const allConditionalTexts = computed(() => messagingConditionalTextStore.messageConditionalTexts || []);
const conditionalText = computed(() => messagingConditionalTextStore.messageConditionalText);
const pipedTextMessage = computed(() => messagingMessageStore.message);
const alertStatuses = computed(() => alertStore.statuses);

const container = computed(() => {
  return allMessageContainers.value.find(
    messageContainer => messageContainer.id === route.query.containerId
  );
});

const message = computed(() => {
  return container.value?.messages?.find(
    currentMessage => currentMessage.id === route.query.messageId
  );
});

const containerId = computed(() => container.value?.id);
const messageId = computed(() => message.value?.id);
const contentId = computed(() => content.value?.id);
const experimentId = computed(() => experiment.value?.experimentId);
const exposureId = computed(() => container.value?.exposureId);
const conditions = computed(() => experiment.value.conditions || []);

const attachments = computed({
  get() {
    return content.value?.attachments || [];
  },
  set(newAttachments) {
    if (content.value) content.value.attachments = newAttachments;
  }
});

const conditionalTexts = computed({
  get() {
    return allConditionalTexts.value || [];
  },
  set(newConditionalTexts) {
    if (content.value) content.value.conditionalTexts = newConditionalTexts;
    messagingConditionalTextStore.addMessageConditionalTexts(newConditionalTexts);
  }
});

const configuration = computed({
  get() {
    return message.value?.configuration;
  },
  set(newConfiguration) {
    if (message.value) message.value.configuration = newConfiguration;
  }
});

const content = computed({
  get() {
    return message.value?.content;
  },
  set(newContent) {
    if (message.value) message.value.content = newContent;
  }
});

const enabled = computed({
  get() {
    return configuration.value?.enabled;
  },
  set(newEnabled) {
    configuration.value = {
      ...configuration.value,
      enabled: newEnabled
    };
  }
});

const html = computed({
  get() {
    return content.value?.html;
  },
  set(newHtml) {
    if (content.value) content.value.html = newHtml;
  }
});

const isCopy = computed({
  get() {
    return message.value?.isCopy || false;
  },
  set(newIsCopy) {
    if (message.value) message.value.isCopy = newIsCopy;
  }
});

const matchType = computed({
  get() {
    return configuration.value?.matchType || "INCLUDE";
  },
  set(newMatchType) {
    if (configuration.value) configuration.value.matchType = newMatchType;
  }
});

const pipedText = computed({
  get() {
    return content.value?.pipedText || null;
  },
  set(newPipedText) {
    content.value = {
      ...content.value,
      pipedText: newPipedText
    };
  }
});

const replyToList = computed({
  get() {
    return configuration.value?.replyTo || [];
  },
  set(newReplyTo) {
    configuration.value.replyTo = newReplyTo;
  }
});

const ruleSets = computed({
  get() {
    return message.value?.ruleSets || [];
  },
  set(newRuleSets) {
    if (message.value) message.value.ruleSets = newRuleSets.map(ruleSet => ({
      ...ruleSet,
      rules: ruleSet.rules.map(rule => ({
        ...rule,
        assignment:
          allMessageRuleAssignments.value.find(
            assignment => assignment.lmsId === rule.lmsAssignmentId
          ) || rule.assignment
      }))
    }));
  }
});

const sendAt = computed({
  get() {
    return configuration.value?.sendAt;
  },
  set(newSendAt) {
    if (configuration.value) {
      configuration.value.sendAt = newSendAt;
      configuration.value.sendAtTimezoneOffset = new Date().getTimezoneOffset();
    }
  }
});

const subject = computed({
  get() {
    return configuration.value?.subject;
  },
  set(newSubject) {
    if (configuration.value) configuration.value.subject = newSubject;
  }
});

const toConsentedOnly = computed({
  get() {
    return configuration.value?.toConsentedOnly || false;
  },
  set(newToConsentedOnly) {
    configuration.value.toConsentedOnly = newToConsentedOnly;
  }
});

const type = computed({
  get() {
    return configuration.value.type;
  },
  set(newType) {
    configuration.value.type = newType;
  }
});

const configurationId = computed(() => configuration.value.id);

const editor = computed(() => {
  if (!configuration.value?.type) {
    return null;
  }

  return type.value === "EMAIL"
    ? "html"
    : "basic";
});

const showReplyTo = computed(() => type.value === "EMAIL");
const readOnly = computed(() => configuration.value.status === "SENT");

const messageTypeLabel = computed(() => {
  switch (type.value) {
    case "CONVERSATION":
      return "Canvas Message";
    case "EMAIL":
      return "Email";
    case "NONE":
    default:
      return "N/A";
  }
});

const hasMessageRuleAssignments = computed(() => allMessageRuleAssignments.value.length > 0);
const pipedTextUploadedFilename = computed(() => pipedText.value?.fileName || "");


const updateToConsentedOnly = value => {
  toConsentedOnly.value = value;
};

const updateReplyTo = value => {
  replyToList.value = value.map(currentReplyTo => ({
    ...currentReplyTo,
    messageConfigurationId: configurationId.value
  }));
};

const updateSendAt = value => {
  sendAt.value = value;
};

const updateType = value => {
  type.value = value;
};

const handleEditedBody = body => {
  html.value = body;
};

const getCondition = conditionId => {
  return conditions.value.find(condition => condition.conditionId === conditionId);
};

const getConditionName = conditionId => {
  return getCondition(conditionId)?.name || "No condition";
};

const hasMessagesNotCurrent = messages => {
  return messages.some(currentMessage => currentMessage.id !== messageId.value);
};

const addConditionalText = async () => {
  addingNewConditionalText.value = true;
  messagingConditionalTextStore.setMessageConditionalTextEditId(null);
  openConditionalTextEditor.value = true;
};

const editConditionalText = conditionalTextId => {
  messagingConditionalTextStore.setMessageConditionalTextEditId(conditionalTextId);
  openConditionalTextEditor.value = true;
  addingNewConditionalText.value = false;
};

const insertConditionalText = value => {
  conditionalTextToPlace.value = {
    ...value,
    cursorPosition: editorCursorPosition.value,
    status: "insert"
  };

  messagingConditionalTextStore.setMessageConditionalText(null);
  messagingConditionalTextStore.setMessageConditionalTextEditId(null);
  openConditionalTextEditor.value = false;
  addingNewConditionalText.value = false;
};

const updateConditionalText = value => {
  conditionalTextToPlace.value = {
    ...value,
    cursorPosition: editorCursorPosition.value,
    status: "update"
  };

  messagingConditionalTextStore.setMessageConditionalText(null);
  messagingConditionalTextStore.setMessageConditionalTextEditId(null);
  openConditionalTextEditor.value = false;
  addingNewConditionalText.value = false;
};

const cancelConditionalText = () => {
  openConditionalTextEditor.value = false;
  addingNewConditionalText.value = false;
};

const insertPipedText = item => {
  pipedTextToPlace.value = {
    ...item,
    cursorPosition: editorCursorPosition.value
  };
};

const handleUpdatePlaceholders = async currentContent => {
  const updatedContent = await messagingMessageStore.updatePlaceholders([
    experimentId.value,
    exposureId.value,
    containerId.value,
    messageId.value,
    contentId.value,
    currentContent
  ]);

  initialContent.value = updatedContent.html;
  conditionalTexts.value = updatedContent.conditionalTexts;
};

const handleCursor = cursorPosition => {
  editorCursorPosition.value = cursorPosition;
};

const handleValidationErrors = errors => {
  validationErrors.value = errors;
  Swal.fire("Please complete all required sections.");
};

const findMessagesAvailableToCopy = () => {
  messagesAvailableToCopy.value = allMessageContainers.value.filter(messageContainer => {
    return (
      messageContainer.configuration.status !== messageStatus.deleted &&
      hasMessagesNotCurrent(messageContainer.messages)
    );
  });
};

const copy = async from => {
  copyMenuOpen.value = false;

  attachments.value = from.content.attachments.map(attachment => ({
    ...attachment,
    id: null
  }));

  const copiedHtml = ref(from.content.html);
  const conditionalTextMapping = {};

  if (from.content.conditionalTexts?.length) {
    from.content.conditionalTexts.forEach(currentConditionalText => {
      const newId = crypto.randomUUID();

      conditionalTextMapping[currentConditionalText.id] = newId;
      copiedHtml.value = copiedHtml.value.replaceAll(currentConditionalText.id, newId);
    });
  }

  conditionalTexts.value = (from.content.conditionalTexts || []).map(currentConditionalText => ({
    ...currentConditionalText,
    id: conditionalTextMapping[currentConditionalText.id] || null,
    contentId: contentId.value,
    isNew: true,
    result: {
      ...currentConditionalText.result,
      id: null,
      conditionalTextId: null
    },
    ruleSets: currentConditionalText.ruleSets.map(ruleSet => ({
      ...ruleSet,
      rules: ruleSet.rules.map(rule => ({
        ...rule,
        assignment:
          allMessageRuleAssignments.value.find(
            assignment => assignment.lmsId === rule.lmsAssignmentId
          ) || rule.assignment
      }))
    }))
  }));

  enabled.value = from.configuration.enabled;
  html.value = copiedHtml.value;
  initialContent.value = copiedHtml.value;
  matchType.value = from.configuration.matchType || "INCLUDE";

  const newPipedTextId = crypto.randomUUID();
  const pipedTextItemMapping = {};

  if (from.content.pipedText?.items?.length) {
    from.content.pipedText.items.forEach(item => {
      const newId = crypto.randomUUID();

      pipedTextItemMapping[item.id] = newId;
      copiedHtml.value = copiedHtml.value.replaceAll(item.id, newId);
    });

    pipedText.value = {
      ...from.content.pipedText,
      id: newPipedTextId,
      contentId: contentId.value,
      items: from.content.pipedText.items.map(item => ({
        ...item,
        id: pipedTextItemMapping[item.id] || null,
        pipedTextId: newPipedTextId,
        values: item.values.map(value => ({
          ...value,
          id: null,
          pipedTextItemId: pipedTextItemMapping[item.id] || null
        }))
      }))
    };

    html.value = copiedHtml.value;
    initialContent.value = copiedHtml.value;
  } else {
    pipedText.value = null;
  }

  replyToList.value = from.configuration.replyTo.map(currentReplyTo => ({
    ...currentReplyTo,
    id: null,
    messageConfigurationId: configurationId.value
  }));

  ruleSets.value = from.ruleSets.map(ruleSet => ({
    ...ruleSet,
    messageId: messageId.value,
    rules: ruleSet.rules.map(rule => ({
      ...rule,
      ruleSetId: null,
      assignment:
        allMessageRuleAssignments.value.find(
          assignment => assignment.lmsId === rule.lmsAssignmentId
        ) || rule.assignment
    }))
  }));

  sendAt.value = from.configuration.sendAt;
  configuration.value.sendAtTimezoneOffset = from.configuration.sendAtTimezoneOffset;
  subject.value = from.configuration.subject;
  toConsentedOnly.value = from.configuration.toConsentedOnly;
  type.value = from.configuration.type;
  isCopy.value = true;
};

const prepareCopiedMessageForSave = () => {
  message.value?.content?.conditionalTexts?.forEach(currentConditionalText => {
    currentConditionalText.ruleSets.forEach(ruleSet => {
      ruleSet.id = null;
      ruleSet.conditionalTextId = null;

      ruleSet.rules.forEach(rule => {
        rule.id = null;
        rule.ruleSetId = null;
      });
    });
  });

  message.value?.ruleSets?.forEach(ruleSet => {
    ruleSet.id = null;

    ruleSet.rules.forEach(rule => {
      rule.id = null;
      rule.ruleSetId = null;
    });
  });
};

const validateBeforeSave = async () => {
  const replyToValid = replyTo.value
    ? await replyTo.value.updateReplyTo()
    : true;

  validationErrors.value = validateMessage(
    message.value,
    conditionalTexts.value,
    conditionalText.value
  );

  if (!validationErrors.value.hasErrors && replyToValid) {
    return true;
  }

  if (
    tab.value === "treatment" &&
    !validationErrors.value.body &&
    !validationErrors.value.subject &&
    !validationErrors.value.recipients.hasErrors &&
    !validationErrors.value.conditionalText.hasErrors &&
    !replyToValid
  ) {
    tab.value = "settings";
  }

  if (
    tab.value === "settings" &&
    (
      validationErrors.value.body ||
      validationErrors.value.subject ||
      validationErrors.value.recipients.hasErrors ||
      validationErrors.value.conditionalText.hasErrors
    ) &&
    replyToValid
  ) {
    tab.value = "treatment";
  }

  if (
    validationErrors.value.body ||
    validationErrors.value.subject ||
    validationErrors.value.recipients.hasErrors ||
    validationErrors.value.conditionalText.hasErrors
  ) {
    handleValidationErrors(validationErrors.value);
  }

  return false;
};

const saveExit = async () => {
  if (!readOnly.value) {
    const valid = await validateBeforeSave();

    if (!valid) {
      return false;
    }

    if (conditionalText.value) {
      Swal.fire(
        `Please finish ${conditionalText.value.id ? "editing" : "creating"} the conditional text before saving the message.`
      );

      return false;
    }

    if (isCopy.value) {
      prepareCopiedMessageForSave();
    }

    if (content.value) content.value.conditionalTexts = allConditionalTexts.value;

    await messagingMessageStore.update([
      experimentId.value,
      exposureId.value,
      containerId.value,
      messageId.value,
      message.value
    ]);
  }

  createStatusAlert(
    statusAlert(alertStatuses.value.success, "Message saved successfully")
  );

  router.push({
    name: "ExperimentSummary",
    params: {
      experimentId: experimentId.value
    }
  });

  return true;
};

const initialize = async () => {
  messagingMessageStore.getAssignments([
    experimentId.value,
    exposureId.value,
    containerId.value
  ]);
  messagingConditionalTextStore.reset();

  initialContent.value = html.value;
  messagingConditionalTextStore.addMessageConditionalTexts(content.value?.conditionalTexts);
  messagingMessageStore.setPipedText(pipedText.value);
  validationErrors.value = initValidations().message;

  if (!readOnly.value) {
    findMessagesAvailableToCopy();
  }
};

onMounted(async () => {
  openConditionalTextEditor.value = false;
  widenContainer();
  adjustBodyTopPadding();

  await initialize();

  loaded.value = true;

  await nextTick();

  addAttributesToObservedElement(
    ".treatment-tab",
    "treatment-tab-conditional-text",
    ".tiptap.ProseMirror",
    [
      {
        name: "aria-label",
        value: "message editor content"
      }
    ]
  );
});

onBeforeUnmount(() => {
  openConditionalTextEditor.value = false;
  shrinkContainer();
  adjustBodyTopPadding("");
});

watch(messageConditionalTextEditId, value => {
  openConditionalTextEditor.value = value !== null || addingNewConditionalText.value;
}, { immediate: true });

watch(allMessageRuleAssignments, assignments => {
  if (!assignments.length) {
    return;
  }

  ruleSets.value = [...ruleSets.value];

  messagingConditionalTextStore.addMessageConditionalTexts(
    conditionalTexts.value.map(currentConditionalText => ({
      ...currentConditionalText,
      ruleSets: currentConditionalText.ruleSets.map(ruleSet => ({
        ...ruleSet,
        rules: ruleSet.rules.map(rule => ({
          ...rule,
          assignment:
            assignments.find(assignment => assignment.lmsId === rule.lmsAssignmentId) ||
            rule.assignment
        }))
      }))
    }))
  );
}, { immediate: true });

watch(allConditionalTexts, value => {
  conditionalTexts.value = value || [];

});

watch(pipedTextMessage, value => {
  if (!value) {
    return;
  }

  if (value.content) {
    content.value = {
      ...value.content,
      conditionalTexts: conditionalTexts.value,
      html: html.value
    };

    handleUpdatePlaceholders(content.value);
    showPipedTextUploader.value = false;
    isUploadSuccessful.value = true;
    return;
  }

  showPipedTextUploader.value = false;
  messagingMessageStore.setPipedText(pipedText.value);

  Swal.fire({
    icon: "error",
    title: "Error uploading CSV",
    customClass: {
      htmlContainer: "swal-validation-error"
    },
    html: `<div>
      ${value.validationErrors.length > 1 ? "Errors" : "An error"} occurred while uploading the CSV file:
      <ul class="my-2 error">
        ${value.validationErrors.map(error => `<li>${error}</li>`).join("")}
      </ul>
      Please check the file format and try again or contact support.
    </div>`
  });
});

defineExpose({
  saveExit
});
</script>

<style lang="scss" scoped>
.message-container {
  min-width: 100%;
  max-width: 100%;

  & .treatment-tab {
    min-width: 100%;

    & .treatment-tab-message {
      flex: 1 1 auto;
      min-width: 0;

      & .treatment-tab-message-container {
        min-width: 100%;
        max-width: 100%;
        flex-direction: row;

        > div {
          min-width: 100%;
          max-width: 100%;
        }
      }

      & .editor-container {
        min-width: 100%;
        border-radius: 4px;
      }
    }

    & .treatment-tab-conditional-text {
      flex: 0 0 33%;
      min-width: 0;
      background-color: map.get($grey, "extreme-light");
      padding: 10px;
      border: 1px solid rgba(0, 0, 0, 0.12);
      border-top: none;
    }
  }
}

.enabled-switch {
  :deep(.v-messages__message) {
    line-height: 20px;
    font-size: 16px;
    color: rgba(0, 0, 0, 0.6);
  }

  // Vuetify 2's selection-control messages picked up the input's own color automatically
  // once checked; Vuetify 3 doesn't carry that over, so match it explicitly here.
  &.enabled-switch--on {
    :deep(.v-messages__message) {
      color: rgb(var(--v-theme-primary));
    }
  }
}

.main-messages-header {
  align-items: center;

  & .messages-header {
    align-items: center;
    max-width: fit-content;
    min-width: fit-content;

    & .sub-header {
      align-items: center;

      & .sub-header-chip {
        display: flex;
        flex-direction: row;
        justify-content: space-between;
        & h4 {
          padding-bottom: 0 !important;
        }
      }

      & .messages-icon-container {
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 50%;
        background-color: rgba(220, 183, 179, 1);
        min-height: 41px !important;
        min-width: 41px !important;
      }

      > * {
        min-width: fit-content;
        max-width: fit-content;
      }
    }
  }
}

.validation-error {
  border: 2px solid red !important;
}

.copy-btn {
  :deep(.v-btn__content) {
    opacity: 1 !important;
  }
}

.upload-status {
  max-width: fit-content;
  color: #9e9e9e;
  font-size: 0.9em;
}

.error-text {
  color: map.get($red, "base") !important;
}
</style>

<style>
.swal-validation-error {
  text-align: left !important;
}

.swal-validation-error ul.error {
  margin: 16px;
  list-style-type: circle !important;
}
</style>
