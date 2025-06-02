<template>
<div>
  <page-loading
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
        <v-col
          class="messages-header"
        >
          <v-row
            class="sub-header"
            justify="space-between"
            no-gutters
          >
            <div
              class="messages-icon-container mr-4"
            >
              <v-icon
                color="white"
              >
                mdi-message-text-outline
              </v-icon>
            </div>
            <h2
              class="pb-0 my-auto"
            >
              {{ container.configuration.title }}
            </h2>
          </v-row>
        </v-col>
        <v-col
          class="messages-header"
        >
          <v-row
            class="sub-header"
            justify="space-between"
            no-gutters
          >
            <div
              class="sub-header-chip mr-8"
            >
              <h4
                class="mr-2 my-auto pb-0"
              >
                Message Type
              </h4>
              <v-chip
                color="#EBFFEE"
                class="py-2"
                label
              >
                {{ messageTypeLabel}}
              </v-chip>
            </div>
            <div
              class="sub-header-chip"
            >
              <h4
                class="mr-2 my-auto pb-0"
              >
                Treatment
              </h4>
              <v-chip
                v-if="container.messages.length > 1"
                :color="conditionColorMapping[getConditionName(message.conditionId)]"
                class="py-2"
                label
              >
                {{ getConditionName(message.conditionId) }}
              </v-chip>
              <v-chip
                v-if="container.messages.length == 1"
                color="lightgrey"
                class="v-chip--only-one"
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
        <v-tab>Treatment</v-tab>
        <v-tab>Settings</v-tab>
      </v-tabs>
      <v-divider />
      <v-tabs-items
        v-model="tab"
      >
        <v-tab-item>
          <!-- treatment tab -->
          <div
            class="treatment-tab d-flex flex-row justify-between"
          >
            <div
              :class="columnSectionClass.treatment"
              class="treatment-tab-message d-flex flex-column px-2"
            >
              <div>
                <v-switch
                  v-model="enabled"
                  :disabled="readOnly"
                  :flat="true"
                  :ripple="false"
                  label="Include a message for this treatment"
                  messages="(Turning this off means students in this treatment group will not receive a message.)"
                  class="enabled-switch"
                  inset
                >
                </v-switch>
              </div>
              <div
                v-show="enabled"
                :class="columnSectionClass.treatment"
                class="treatment-tab-message-container justify-between col-12"
              >
                <div
                  class="col-12"
                >
                  <recipients
                    :experimentId="experimentId"
                    :exposureId="exposureId"
                    :containerId="containerId"
                    :messageId="messageId"
                    :contentId="contentId"
                    :readOnly="readOnly"
                    :disabled="!hasMessageRuleAssignments"
                    :maxRuleCount="maxRuleCount"
                    :validatedErrors="validationErrors.recipients"
                  />
                  <v-text-field
                    v-model="subject"
                    :disabled="readOnly"
                    :hide-details="validationErrors.subject === null"
                    :error-messages="validationErrors.subject"
                    label="Subject line"
                    class="mb-6"
                    outlined
                    required
                    dense
                  />
                  <div
                    :class="{'validation-error': validationErrors.body !== null}"
                    class="editor-container"
                  >
                    <tip-tap-editor
                      :content="initialContent"
                      :editorType="editor"
                      :readOnly="readOnly"
                      :conditionalTextToPlace="conditionalTextToPlace"
                      :pipedTextToPlace="pipedTextToPlace"
                      :allowMentions="true"
                      @edited="handleEditedBody"
                      @cursor="handleCursor"
                      required
                    />
                    <editor-sub-menu
                      :experimentId="experimentId"
                      :exposureId="exposureId"
                      :containerId="containerId"
                      :messageId="messageId"
                      :contentId="contentId"
                      :readOnly="readOnly"
                      :pipedText="pipedText"
                      @insertPipedText="insertPipedText"
                      @insertConditionalText="insertConditionalText"
                      @addConditionalText="addConditionalText"
                      @editConditionalText="editConditionalText"
                    />
                  </div>
                  <div
                    v-if="validationErrors.result !== null"
                    class="v-text-field__details pt-2 px-3"
                  >
                    <div
                      class="v-messages theme--light error--text"
                      role="alert"
                    >
                      <div
                        class="v-messages__wrapper"
                      >
                        <div
                          class="v-messages__message"
                        >
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
                      offset-y
                      close-on-click
                      close-on-content-click
                    >
                      <template
                        v-slot:activator="{ on, attrs }"
                      >
                        <v-btn
                          v-bind="attrs"
                          v-on="on"
                          color="primary"
                          elevation="0"
                          class="copy-btn mb-3 mt-3"
                          plain
                        >
                          Copy message from <v-icon>mdi-chevron-down</v-icon>
                        </v-btn>
                      </template>
                      <v-list>
                          <template
                            v-for="(container, index) in messagesAvailableToCopy"
                          >
                            <v-menu
                              v-if="container.messages.length > 0 && hasMessagesNotCurrent(container.messages)"
                              :key="container.id"
                              transition="slide-x-transition"
                              offset-x
                              open-on-hover
                              close-on-click
                              close-on-content-click
                            >
                              <template
                                v-slot:activator="{ on, attrs }"
                              >
                                <v-list-item
                                  :key="index"
                                  v-bind="attrs"
                                  v-on="on"
                                >
                                  <v-list-item-title>
                                    {{ container.configuration.title }}
                                  </v-list-item-title>
                                  <v-list-item-action
                                    class="justify-end"
                                  >
                                    <v-icon>mdi-menu-right</v-icon>
                                  </v-list-item-action>
                                </v-list-item>
                              </template>
                              <v-list>
                                <template
                                  v-for="message in container.messages"
                                >
                                  <v-list-item
                                    v-if="message.id != messageId"
                                    :key="message.id"
                                    @click="copy(message)"
                                  >
                                    <v-list-item-title>
                                      Message
                                      <v-chip
                                        v-if="container.messages.length > 1"
                                        label
                                        :color="conditionColorMapping[getConditionName(message.conditionId)]"
                                      >
                                        {{ getConditionName(message.conditionId) }}
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
                      @click="showPipedTextUploader = true"
                      color="primary"
                      text
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
                      :value="showPipedTextUploader"
                      :opacity="0.75"
                      :dark="false"
                      :light="true"
                    >
                      <piped-text-file-uploader
                        :experimentId="experimentId"
                        :exposureId="exposureId"
                        :containerId="containerId"
                        :messageId="messageId"
                        :contentId="contentId"
                        :readOnly="readOnly"
                        @close="showPipedTextUploader = false"
                      />
                    </v-overlay>
                    <preview
                      v-if="!readOnly"
                      :experimentId="experimentId"
                      :exposureId="exposureId"
                      :containerId="containerId"
                      :messageId="messageId"
                      :contentId="contentId"
                    />
                    <send-test
                      v-if="type === 'EMAIL' && !readOnly"
                      :experimentId="experimentId"
                      :exposureId="exposureId"
                      :containerId="containerId"
                      :messageId="messageId"
                      :email="container.ownerEmail"
                      @validated="handleValidationErrors"
                    />
                  </div>
                </div>
              </div>
            </div>
            <div
              v-if="openConditionalTextEditor"
              :class="columnSectionClass.conditionaltext"
              class="treatment-tab-conditional-text"
            >
              <conditional-text
                :experimentId="experimentId"
                :exposureId="exposureId"
                :containerId="containerId"
                :messageId="messageId"
                :contentId="contentId"
                :maxRuleCount="maxRuleCount"
                :validatedErrors="validationErrors.conditionalText"
                :pipedText="pipedText"
                @conditionalTextCreated="insertConditionalText"
                @conditionalTextUpdated="updateConditionalText"
                @cancel="cancelConditionalText"
              />
            </div>
          </div>
        </v-tab-item>
        <v-tab-item>
          <!-- settings tab -->
          <div
            class="settings-container d-flex flex-column px-2"
          >
            <to-consented-only
              :selected="toConsentedOnly"
              :experiment="experiment"
              :readOnly="readOnly"
              @updated="updateToConsentedOnly"
            />
            <type
              :type="type"
              :readOnly="readOnly"
              @updated="updateType"
              label="Send all messages in this treatment as:"
            />
            <reply-to
              v-if="showReplyTo"
              :replyTos="replyTo"
              :required="false"
              :readOnly="readOnly"
              @updated="updateReplyTo"
              ref="replyTo"
            />
            <scheduler
              :sendAt="sendAt"
              :readOnly="readOnly"
              @updated="updateSendAt"
              label="Decide when you would like the message to be sent."
            />
          </div>
        </v-tab-item>
      </v-tabs-items>
    </div>
  </div>
</div>
</template>

<script>
import { mapGetters, mapActions, mapMutations } from "vuex";
import { message as messageStatus } from "@/helpers/messaging/status.js";
import { shrinkContainer, widenContainer, adjustBodyTopPadding } from "@/helpers/ui-utils.js";
import { initValidations, validateMessage } from "@/helpers/messaging/validation.js";
import ConditionalText from "@/views/messaging/components/conditional/ConditionalText.vue";
import EditorSubMenu from "@/views/messaging/components/menu/editorsubmenu/EditorSubMenu.vue";
import PipedTextFileUploader from "@/views/messaging/components/piped/PipedTextFileUploader.vue";
import PageLoading from "@/components/PageLoading";
import Preview from "@/views/messaging/components/preview/Preview.vue";
import Recipients from "@/views/messaging/components/recipients/Recipients.vue";
import ReplyTo from "@/views/messaging/components/form/ReplyTo.vue";
import Scheduler from "@/views/messaging/components/form/Scheduler.vue";
import SendTest from "@/views/messaging/components/sendtest/SendTest.vue";
import TipTapEditor from "@/components/editor/TipTapEditor";
import ToConsentedOnly from "@/views/messaging/components/form/ToConsentedOnly.vue";
import Type from "@/views/messaging/components/form/Type.vue";

export default {
  name: "Message",
  components: {
    ConditionalText,
    EditorSubMenu,
    PageLoading,
    PipedTextFileUploader,
    Preview,
    Recipients,
    ReplyTo,
    Scheduler,
    SendTest,
    TipTapEditor,
    ToConsentedOnly,
    Type
  },
  data: () => ({
    tab: 0,
    initialContent: null,
    messagesAvailableToCopy: [],
    removedAttachments: [],
    copyMenuOpen: false,
    openConditionalTextEditor: false,
    addingNewConditionalText: false,
    conditionalTextToPlace: null,
    pipedTextToPlace: null,
    editorCursorPosition: null,
    loaded: false,
    maxRuleCount: 8,
    validationErrors: null,
    showPipedTextUploader: false,
    isUploadSuccessful: false
  }),
  watch: {
    messageConditionalTextEditId: {
      handler(newIdToEdit) {
        this.openConditionalTextEditor = newIdToEdit !== null || this.addingNewConditionalText;
      },
      immediate: true
    },
    allMessageRuleAssignments: {
      handler(newAllMessageRuleAssignments) {
        if (!newAllMessageRuleAssignments.length) {
          return;
        }

        // initiate updating the assignments in the message rule sets
        this.ruleSets = [...this.ruleSets];

        this.setMessageConditionalTexts(
          this.conditionalTexts
            .map(
              conditionalText => ({
                ...conditionalText,
                ruleSets: conditionalText.ruleSets
                  .map(
                    ruleSet => ({
                      ...ruleSet,
                      rules: ruleSet.rules
                        .map(
                          rule => ({
                            ...rule,
                            assignment: newAllMessageRuleAssignments.find(assignment => assignment.lmsId === rule.lmsAssignmentId) || rule.assignment
                          })
                        )
                    })
                  )
              })
            )
        );
      },
      immediate: true
    },
    allConditionalTexts: {
      handler(newAllConditionalTexts) {
        this.conditionalTexts = newAllConditionalTexts || [];
      },
      immediate: false
    },
    pipedTextMessage: {
      handler(newPipedTextMessage) {
        // upload successful
        if (newPipedTextMessage?.content) {
          // update persisted content
          this.content = {
            ...newPipedTextMessage.content,
            conditionalTexts: this.conditionalTexts,
            html: this.html
          };
          // update placeholders in editor's current body
          this.handleUpdatePlaceholders(this.content);
          this.showPipedTextUploader = false;
          this.isUploadSuccessful = true;
        } else {
          // an error occurred
          this.showPipedTextUploader = false;
          this.setMessagePipedText(this.pipedText);
          this.$swal({
            icon: "error",
            title: "Error uploading CSV",
            customClass: {
              htmlContainer: "swal-validation-error",
            },
            html: `<div>
              ${newPipedTextMessage.validationErrors.length > 1 ? "Errors" : "An error"} occurred while uploading the CSV file:
              <ul class="my-2 error">
                ${newPipedTextMessage.validationErrors.map(error => `<li>${error}</li>`).join("")}
              </ul>
              Please check the file format and try again or contact support.</div>`,
          });
        }
      },
      immediate: false
    }
  },
  computed: {
    ...mapGetters({
      editMode: "navigation/editMode",
      conditionColorMapping: "condition/conditionColorMapping",
      experiment: "experiment/experiment",
      allMessageContainers: "messagingMessageContainer/messageContainers",
      allMessageRuleAssignments: "messagingMessage/assignments",
      isLoadingAllMessageRuleAssignments: "messagingMessage/isLoading",
      messageConditionalTextEditId: "messagingConditionalText/messageConditionalTextEditId",
      allConditionalTexts: "messagingConditionalText/messageConditionalTexts",
      conditionalText: "messagingConditionalText/messageConditionalText",
      pipedTextMessage: "messagingMessage/message"
    }),
    container() {
      return this.allMessageContainers.find(messageContainer => messageContainer.id === this.$route.params.containerId);
    },
    message() {
      return this.container.messages.find(message => message.id === this.$route.params.messageId);
    },
    containerId() {
      return this.container.id;
    },
    messageId() {
      return this.message.id;
    },
    contentId() {
      return this.content.id;
    },
    experimentId() {
      return this.experiment.experimentId;
    },
    exposureId() {
      return this.container.exposureId;
    },
    conditions() {
      return this.experiment.conditions || [];
    },
    pipedTextItems() {
      return this.pipedText?.items || [];
    },
    attachments: {
      get() {
        return this.content.attachments || [];
      },
      set(newAttachments) {
        this.content.attachments = newAttachments;
      }
    },
    conditionalTexts: {
      get() {
        return this.allConditionalTexts || [];
      },
      set(newConditionalTexts) {
        this.content.conditionalTexts = newConditionalTexts;
        this.setMessageConditionalTexts(newConditionalTexts);
      }
    },
    configuration: {
      get() {
        return this.message.configuration;
      },
      set(newConfiguration) {
        this.message.configuration = newConfiguration;
      }
    },
    content: {
      get() {
        return this.message.content;
      },
      set(newContent) {
        this.message.content = newContent;
      }
    },
    enabled: {
      get() {
        return this.configuration.enabled;
      },
      set(newEnabled) {
        this.configuration = {
          ...this.configuration,
          enabled: newEnabled
        };
      }
    },
    html: {
      get() {
        return this.content.html;
      },
      set(newHtml) {
        this.content.html = newHtml;
      }
    },
    isCopy: {
      get() {
        return this.message.isCopy || false;
      },
      set(newIsCopy) {
        this.message.isCopy = newIsCopy;
      }
    },
    matchType: {
      get() {
        return this.configuration.matchType || "INCLUDE";
      },
      set(newMatchType) {
        this.configuration.matchType = newMatchType;
      }
    },
    pipedText: {
      get() {
        return this.content?.pipedText || null;
      },
      set(newPipedText) {
        this.content = {
          ...this.content,
          pipedText: newPipedText
        };
      }
    },
    replyTo: {
      get() {
        return this.configuration?.replyTo || [];
      },
      set(newReplyTo) {
        this.configuration.replyTo = newReplyTo;
      }
    },
    ruleSets: {
      get() {
        return this.message.ruleSets || [];
      },
      set(newRuleSets) {
        this.message.ruleSets = newRuleSets
          .map(
            ruleSet => ({
              ...ruleSet,
              rules: ruleSet.rules
                .map(
                  rule => ({
                    ...rule,
                    assignment: this.allMessageRuleAssignments.find(assignment => assignment.lmsId === rule.lmsAssignmentId) || rule.assignment
                  })
                )
            })
          );
      }
    },
    sendAt: {
      get() {
        return this.configuration.sendAt;
      },
      set(newSendAt) {
        this.configuration.sendAt = newSendAt;
        this.configuration.sendAtTimezoneOffset = new Date().getTimezoneOffset();
      }
    },
    subject: {
      get() {
        return this.configuration.subject;
      },
      set(newSubject) {
        this.configuration.subject = newSubject;
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
        return this.configuration.type;
      },
      set(newType) {
        this.configuration.type = newType;
      }
    },
    configurationId() {
      return this.configuration.id;
    },
    editor() {
      if (!this.configuration || !this.configuration.type) {
        return null;
      }

      switch (this.type) {
        case "EMAIL":
          return "html";
        case "CONVERSATION":
        case "NONE":
        default:
          return "basic";
      }
    },
    showReplyTo() {
      return this.type === "EMAIL";
    },
    showAttachments() {
      return this.type !== null;
    },
    getSaveExitPage() {
      return this.editMode?.callerPage?.name || "Home";
    },
    readOnly() {
      return this.configuration.status === "SENT";
    },
    messageTypeLabel() {
      switch (this.type) {
        case "CONVERSATION":
          return "Canvas Message";
        case "EMAIL":
          return "Email";
        case "NONE":
        default:
          return "N/A";
      }
    },
    hasMessageRuleAssignments() {
      return this.allMessageRuleAssignments.length > 0;
    },
    pipedTextUploadedFilename() {
      return this.pipedText?.fileName || "";
    },
    columnSectionClass() {
      return {
        treatment: this.openConditionalTextEditor ? "col-8" : "col-12",
        conditionaltext: this.openConditionalTextEditor ? "col-4" : ""
      }
    }
  },
  methods: {
    ...mapActions({
      update: "messagingMessage/update",
      createAttachment: "messagingContentAttachment/create",
      removeAttachment: "messagingContentAttachment/remove",
      fetchMessageRuleAssignments: "messagingMessage/getAssignments",
      resetConditionalTexts: "messagingConditionalText/reset",
      updatePlaceholders: "messagingMessage/updatePlaceholders"
    }),
    ...mapMutations({
      setAssignments: "messagingMessage/setAssignments",
      setMessagePipedText: "messagingMessage/setPipedText",
      setMessageConditionalText: "messagingConditionalText/setMessageConditionalText",
      setMessageConditionalTexts: "messagingConditionalText/setMessageConditionalTexts",
      setMessageConditionalTextEditId: "messagingConditionalText/setMessageConditionalTextEditId"
    }),
    updateToConsentedOnly(newToConsentedOnly) {
      this.toConsentedOnly = newToConsentedOnly;
    },
    updateReplyTo(newReplyTo) {
      this.replyTo = newReplyTo.map(
        (replyTo) => ({
          ...replyTo,
          messageConfigurationId: this.configurationId
        })
      );
    },
    updateSendAt(newSendAt) {
      this.sendAt = newSendAt;
    },
    updateType(newType) {
      this.type = newType;
    },
    handleEditedBody(body) {
      this.html = body;
    },
    getCondition(conditionId) {
      return this.conditions.find((condition) => condition.conditionId === conditionId);
    },
    getConditionName(conditionId) {
      return this.getCondition(conditionId)?.name || "No condition";
    },
    hasMessagesNotCurrent(messages) {
      return messages.some((message) => message.id !== this.messageId);
    },
    async addConditionalText() {
      this.addingNewConditionalText = true;
      this.setMessageConditionalTextEditId(null);
      this.openConditionalTextEditor = true;
    },
    editConditionalText(conditionalTextId) {
      this.setMessageConditionalTextEditId(conditionalTextId);
      this.openConditionalTextEditor = true;
      this.addingNewConditionalText = false;
    },
    insertConditionalText(conditionalText) {
      this.conditionalTextToPlace = {
        ...conditionalText,
        cursorPosition: this.editorCursorPosition,
        status: "insert"
      };
      this.setMessageConditionalText(null);
      this.setMessageConditionalTextEditId(null);
      this.openConditionalTextEditor = false;
      this.addingNewConditionalText = false;
    },
    updateConditionalText(conditionalText) {
      this.conditionalTextToPlace = {
        ...conditionalText,
        cursorPosition: this.editorCursorPosition,
        status: "update"
      };
      this.setMessageConditionalText(null);
      this.setMessageConditionalTextEditId(null);
      this.openConditionalTextEditor = false;
      this.addingNewConditionalText = false;
    },
    cancelConditionalText() {
      this.openConditionalTextEditor = false;
      this.addingNewConditionalText = false;
    },
    insertPipedText(item) {
      this.pipedTextToPlace = {
        ...item,
        cursorPosition: this.editorCursorPosition
      };
    },
    async handleUpdatePlaceholders(content) {
      const updatedContent = await this.updatePlaceholders(
        [
          this.experimentId,
          this.exposureId,
          this.containerId,
          this.messageId,
          this.contentId,
          content
        ]
      );

      this.initialContent = updatedContent.html;
      this.conditionalTexts = updatedContent.conditionalTexts;
    },
    handleCursor(cursorPosition) {
      this.editorCursorPosition = cursorPosition;
    },
    handleValidationErrors(validationErrors) {
      this.validationErrors = validationErrors;
      this.$swal("Please complete all required sections.");
    },
    findMessagesAvailableToCopy() {
      this.messagesAvailableToCopy = [];
      this.allMessageContainers
        .filter((messageContainer) => messageContainer.configuration.status !== messageStatus.deleted)
        .forEach(
          (messageContainer) => {
            var hasAvailableMessage = this.hasMessagesNotCurrent(messageContainer.messages);

            if (hasAvailableMessage) {
              this.messagesAvailableToCopy.push(messageContainer);
            }
          }
        );
    },
    async copy(from) {
      this.copyMenuOpen = false;
      this.attachments = from.content.attachments
        .map(
          attachment => ({
            ...attachment,
            id: null
          })
        );

      let conditionalTextMapping = {};

      if (from.content.conditionalTexts.length) {
        from.content.conditionalTexts.forEach(
          conditionalText => {
            let newId = crypto.randomUUID();
            conditionalTextMapping[conditionalText.id] = newId;
            from.content.html = from.content.html.replaceAll(conditionalText.id, newId);
          }
        );
      }

      this.conditionalTexts = from.content.conditionalTexts
        .map(
          conditionalText => ({
            ...conditionalText,
            id: conditionalTextMapping[conditionalText.id] || null,
            contentId: this.contentId,
            isNew: true,
            result: {
              ...conditionalText.result,
              id: null,
              conditionalTextId: null
            },
            ruleSets: conditionalText.ruleSets
              .map(
                ruleSet => ({
                  ...ruleSet,
                  //id: null,
                  //conditionalTextId: null,
                  rules: ruleSet.rules
                    .map(
                      rule => ({
                        ...rule,
                        //id: null,
                        //ruleSetId: null,
                        assignment: this.allMessageRuleAssignments.find(assignment => assignment.lmsId === rule.lmsAssignmentId) || rule.assignment
                      })
                    )
                })
              )
          })
        );
      this.enabled = from.configuration.enabled;
      this.html = from.content.html;
      this.initialContent = from.content.html;
      this.matchType = from.configuration.matchType || "INCLUDE";

      let pipedTextItemMapping = {};
      const newPipedTextId = crypto.randomUUID();

      if (from.content.pipedText) {
        from.content.pipedText.items.forEach(
          item => {
            let newId = crypto.randomUUID();
            pipedTextItemMapping[item.id] = newId;
            from.content.html = from.content.html.replaceAll(item.id, newId);
          }
        );
      }

      this.pipedText = from.content.pipedText
        .map(
          pipedText => ({
            ...pipedText,
            id: newPipedTextId,
            contentId: this.contentId,
            items: pipedText.items.map(
              item => ({
                ...item,
                id: pipedTextItemMapping[item.id] || null,
                pipedTextId: newPipedTextId,
                values: item.values.map(
                  value => ({
                    ...value,
                    id: null,
                    pipedTextItemId: pipedTextItemMapping[item.id] || null
                  })
                )
              })
            )
          })
        );

      this.replyTo = from.configuration.replyTo
        .map(
          replyTo => ({
            ...replyTo,
            id: null,
            messageConfigurationId: this.configurationId
          })
        );
      this.ruleSets = from.ruleSets
        .map(
          ruleSet => ({
            ...ruleSet,
            //id: null,
            messageId: this.messageId,
            rules: ruleSet.rules
              .map(
                rule => ({
                  ...rule,
                  //id: null,
                  ruleSetId: null,
                  assignment: this.allMessageRuleAssignments.find(assignment => assignment.lmsId === rule.lmsAssignmentId) || rule.assignment
                })
              )
          })
        );
      this.sendAt = from.configuration.sendAt;
      this.sendAtTimezoneOffset = from.configuration.sendAtTimezoneOffset;
      this.subject = from.configuration.subject;
      this.toConsentedOnly = from.configuration.toConsentedOnly;
      this.type = from.configuration.type;
      this.isCopy = true;
    },
    async saveExit() {
      if (!this.readOnly) {
        // wait for reply-to child to complete validation
        let replyToValid = this.$refs.replyTo ? await this.$refs.replyTo.updateReplyTo() : true;

        this.validationErrors = validateMessage(this.message, this.conditionalTexts, this.conditionalText);

        if (this.validationErrors.hasErrors || !replyToValid) {
          const currentTab = this.tab;

          switch (currentTab) {
            case 0: // treatment tab
              if (!this.validationErrors.body
                  && !this.validationErrors.subject
                  && !this.validationErrors.recipients.hasErrors
                  && !this.validationErrors.conditionalText.hasErrors
                  && !replyToValid
              ) {
                this.tab = 1; // switch to settings tab if treatment tab has no errors but settings has errors
              }
              break;
            case 1: // settings tab
              if ((this.validationErrors.body
                  || this.validationErrors.subject
                  || this.validationErrors.recipients.hasErrors
                  || this.validationErrors.conditionalText.hasErrors)
                  && replyToValid
              ) {
                this.tab = 0; // switch to treatment tab if settings tab has no errors but treatment has errors
              }
              break;
            default:
              break;
          }

          if ((this.validationErrors.body
              || this.validationErrors.subject
              || this.validationErrors.recipients.hasErrors
              || this.validationErrors.conditionalText.hasErrors)
          ) {
            // show "complete all required sections" alert if treatment tab has errors
            this.handleValidationErrors(this.validationErrors);
          }


          return false;
        }

        if (this.conditionalText) {
          // a conditional text is being edited or created; alert user to finish it
          this.$swal(`Please finish ${this.conditionalText.id ? "editing" : "creating"} the conditional text before saving the message.`);

          return false;
        }

        // if message has been created from a copy; null out IDs
        if (this.isCopy) {
          this.message.content.conditionalTexts.forEach(
            conditionalText => {
              conditionalText.ruleSets.forEach(
                ruleSet => {
                  ruleSet.id = null;
                  ruleSet.conditionalTextId = null;
                  ruleSet.rules.forEach(
                    rule => {
                      rule.id = null;
                      rule.ruleSetId = null;
                    }
                  );
                }
              );
            }
          );

          this.message.ruleSets.forEach(
            ruleSet => {
              ruleSet.id = null;
              ruleSet.rules.forEach(
                rule => {
                  rule.id = null;
                  rule.ruleSetId = null;
                }
              );
            }
          );
        }

        await this.update(
          [
            this.experimentId,
            this.exposureId,
            this.containerId,
            this.messageId,
            this.message
          ]
        );
      }

      this.$router.push({
        name: "ExperimentSummary",
        params: {
          experimentId: this.experimentId,
          exposureId: this.exposureId
        }
      });
    },
    async initialize() {
      this.fetchMessageRuleAssignments();
      this.resetConditionalTexts();
      this.initialContent = this.html;
      this.setMessageConditionalTexts(this.content.conditionalTexts);
      this.setMessagePipedText(this.pipedText);
      this.validationErrors = initValidations().message;

      if (!this.readOnly) {
        this.findMessagesAvailableToCopy();
      }
    }
  },
  async mounted() {
    this.openConditionalTextEditor = false;
    widenContainer();
    adjustBodyTopPadding();
    await this.initialize();
    this.loaded = true;
  },
  beforeUnmount() {
    this.openConditionalTextEditor = false;
    shrinkContainer();
    adjustBodyTopPadding("");
  }
}
</script>

<style scoped>
.message-container {
  min-width: 100%;
  max-width: 100%;
  & .treatment-tab {
    & .treatment-tab-message {
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
      background-color: #fafafa;
      padding: 10px;
      transition: width 0.3s ease;
      border: 1px solid rgba(0, 0, 0, .12);
      border-top: none;
    }
  }
}
.enabled-switch {
  & .v-messages__message {
    line-height: 20px;
    font-size: 16px;
    color: rgba(0, 0, 0, .6);
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
  > span.v-btn__content {
    opacity: 1 !important;
  }
}
.upload-status {
  max-width: fit-content;
  color: #9e9e9e;
  font-size: 0.9em;
}
</style>

<style>
.swal-validation-error {
  text-align: left !important;
  & ul.error {
    margin: 16px;
    list-style-type: circle !important;
  }
}
</style>
