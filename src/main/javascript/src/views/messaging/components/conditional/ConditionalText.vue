<template>
<div
  v-if="this.conditionalText"
>
  <v-row
    class="mb-4"
  >
    <span>{{ isEdit ? "Update" : "Insert" }} Conditional Text</span>
  </v-row>
  <v-row
    class="mb-4"
  >
    <v-text-field
      v-model="conditionalText.label"
      :hide-details="validationErrors.label === null"
      :error-messages="validationErrors.label"
      :disabled="readOnly"
      label="Label"
      class="conditional-text-label"
      outlined
      dense
    />
  </v-row>
  <v-row
    v-if="hasRuleSets"
    justify="space-between"
  >
    <span><b>IF</b></span>
    <v-btn
      v-if="!readOnly"
      @click="resetRuleSets"
      color="primary"
      class="px-0"
      text
    >
      Reset
    </v-btn>
  </v-row>
  <v-row
    v-for="(ruleSet, ruleSetIndex) in ruleSets"
    :key="ruleSetIndex"
    class="rule-sets mb-2"
  >
    <v-row
      v-if="toggleOptions.ruleOperator.includes(ruleSet.operator)"
      class="mb-2"
    >
      <toggle
        :selectedOption="ruleSet.operator"
        :options="toggleOptions.ruleOperator"
        :readOnly="readOnly"
        @update="updateOperatorToggle($event, ruleSetIndex)"
      />
    </v-row>
    <v-card
      :class="{'validation-error': validationErrors.ruleSets[ruleSetIndex] ? validationErrors.ruleSets[ruleSetIndex].message !== null || validationErrors.ruleSets[ruleSetIndex].hasRulesError : false}"
      outlined
    >
        <v-row
          justify="space-between mb-4"
        >
          <span
            class="my-auto"
          >
            Rule Set {{ ruleSetIndex + 1 }}
          </span>
          <v-btn
            v-if="!readOnly && ruleSets.length > 1"
            @click="deleteRuleSet(ruleSetIndex)"
            color="primary"
            class="px-0"
            text
          >
            Delete Rule Set
          </v-btn>
        </v-row>
        <v-row
          v-for="(rule, ruleIndex) in ruleSet.rules"
          :key="ruleIndex"
          class="rule-row d-flex flex-column"
          justify="space-between"
        >
          <v-row
            v-if="toggleOptions.ruleOperator.includes(rule.operator)"
            class="my-2 ml-1"
          >
            <toggle
              :selectedOption="rule.operator"
              :options="toggleOptions.ruleOperator"
              :readOnly="readOnly"
              @update="updateOperatorToggle($event, ruleSetIndex, ruleIndex)"
            />
          </v-row>
          <v-select
            v-model="rule.assignment"
            :items="allMessageRuleAssignments"
            :hide-selected="true"
            :hide-details="validationErrors.ruleSets[ruleSetIndex] && validationErrors.ruleSets[ruleSetIndex].rules[ruleIndex] ? validationErrors.ruleSets[ruleSetIndex].rules[ruleIndex].variable === null : true"
            :error-messages="validationErrors.ruleSets[ruleSetIndex] && validationErrors.ruleSets[ruleSetIndex].rules[ruleIndex] ? validationErrors.ruleSets[ruleSetIndex].rules[ruleIndex].variable : null"
            :disabled="readOnly"
            @change="updateRule(ruleSetIndex, ruleIndex, rule)"
            item-text="title"
            label="Variable"
            class="rule-assignment"
            return-object
            outlined
            dense
          />
          <v-select
            v-model="rule.comparison"
            :items="rule.assignment ? rule.assignment.comparisons : []"
            :disabled="readOnly || !rule.assignment"
            :hide-selected="true"
            :hide-details="validationErrors.ruleSets[ruleSetIndex] && validationErrors.ruleSets[ruleSetIndex].rules[ruleIndex] ? validationErrors.ruleSets[ruleSetIndex].rules[ruleIndex].comparison === null : true"
            :error-messages="validationErrors.ruleSets[ruleSetIndex] && validationErrors.ruleSets[ruleSetIndex].rules[ruleIndex] ? validationErrors.ruleSets[ruleSetIndex].rules[ruleIndex].comparison : null"
            @change="updateRule(ruleSetIndex, ruleIndex, rule)"
            item-text="label"
            class="rule-comparison"
            return-object
            outlined
            dense
          />
          <v-text-field
            v-model="rule.value"
            :disabled="readOnly || !rule.comparison || !rule.comparison.requiresValue"
            :hide-details="validationErrors.ruleSets[ruleSetIndex] && validationErrors.ruleSets[ruleSetIndex].rules[ruleIndex] ? validationErrors.ruleSets[ruleSetIndex].rules[ruleIndex].value === null : true"
            :error-messages="validationErrors.ruleSets[ruleSetIndex] && validationErrors.ruleSets[ruleSetIndex].rules[ruleIndex] ? validationErrors.ruleSets[ruleSetIndex].rules[ruleIndex].value : null"
            @change="updateRule(ruleSetIndex, ruleIndex, rule)"
            label="Value"
            type="number"
            class="rule-value"
            outlined
            dense
          />
          <div
            class="rule-actions ml-2"
          >
            <v-btn
              v-if="!readOnly"
              @click="clearRule(ruleSetIndex, ruleIndex)"
              color="primary"
              class="px-0"
              text
            >
              Clear
            </v-btn>
            <v-btn
              v-if="!readOnly && ruleSet.rules.length > 1"
              @click="deleteRule(ruleSetIndex, ruleIndex)"
              class="ml-2 px-0"
              text
            >
              <v-icon>mdi-delete-outline</v-icon>
            </v-btn>
          </div>
        </v-row>
        <v-row>
          <v-btn
            v-if="!readOnly"
            :disabled="!hasAvailableRules"
            @click="addRule(ruleSetIndex)"
            color="primary"
            class="px-0"
            text
          >
            Add a new rule
          </v-btn>
        </v-row>
    </v-card>
  </v-row>
  <v-row
    justify="space-between"
  >
    <div
      class="add-rule-set"
    >
      <v-btn
        v-if="!readOnly"
        :disabled="!hasAvailableRules"
        @click="addRuleSet"
        color="primary"
        class="px-0 my-2"
        text
      >
        Add a new set of rules
      </v-btn>
      <span
        v-if="!readOnly && !hasAvailableRules"
        class="warn-max-rules"
      >
        You have used the maximum number of rules ({{ maxRuleCount }})
      </span>
    </div>
    <v-btn
      v-if="!readOnly && hasOriginalRuleSets.length && !hasRuleSets"
      @click="resetRuleSets"
      color="primary"
      class="px-0"
      text
    >
      Reset
    </v-btn>
  </v-row>
  <v-row
    class="results-row mb-2"
  >
    <span
      class="my-6"
    >
      <b>THEN INSERT THIS TEXT</b>
    </span>
    <div
      :class="{'validation-error': validationErrors.result !== null}"
      class="editor-container"
    >
      <tip-tap-editor
        :content="initialResultContent"
        :pipedTextToPlace="pipedTextToPlace"
        :readOnly="readOnly"
        @edited="handleEditedResultBody"
        @cursor="handleCursor"
        editorType="basic"
        required
      />
      <editor-sub-menu
        v-if="!readOnly && pipedTextItems.length > 0"
        :experimentId="experimentId"
        :exposureId="exposureId"
        :containerId="containerId"
        :messageId="messageId"
        :contentId="contentId"
        :conditionalTexts="conditionalTexts"
        :pipedTextItems="pipedTextItems"
        :showAttachments="false"
        :showConditionalText="false"
        @insertPipedText="insertPipedText"
      />
    </div>
    <div
      v-if="!readOnly && validationErrors.result !== null"
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
            {{ validationErrors.result }}
          </div>
        </div>
      </div>
    </div>
  </v-row>
  <v-row
    class="action-buttons"
  >
    <v-btn
      @click="cancel"
      color="primary"
      text
    >
      {{ readOnly ? 'CLOSE' : 'CANCEL' }}
    </v-btn>
    <v-btn
      v-if="!readOnly"
      @click="isEdit ? handleUpdate() : handleSave()"
      color="primary"
    >
      {{ isEdit ? "UPDATE" : "SAVE & INSERT" }}
    </v-btn>
  </v-row>
</div>
</template>

<script>
import { mapGetters, mapMutations } from "vuex";
import { validations, validateConditionalText } from "@/helpers/messaging/validation";
import EditorSubMenu from "@/views/messaging/components/menu/editorsubmenu/EditorSubMenu.vue";
import TipTapEditor from "@/components/editor/TipTapEditor";
import Toggle from "@/views/messaging/components/recipients/components/form/Toggle.vue";

export default {
  components: {
    EditorSubMenu,
    TipTapEditor,
    Toggle
  },
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
    },
    maxRuleCount: {
      type: Number,
      default: 8
    },
    validatedErrors: {
      type: Object,
      default: null
    }
  },
  data: () => ({
    initialResultContent: null,
    originalRuleSets: [],
    isEdit: false,
    validationErrors: null,
    editorCursorPosition: null,
    pipedTextToPlace: null
  }),
  watch: {
    messageConditionalTextEditId: {
      handler() {
        this.isEdit = false;
        this.initialize();
      },
      immediate: true
    },
    validatedErrors: {
      handler(newValidatedErrors) {
        this.validationErrors = newValidatedErrors || validations.message.conditionalText;
      },
      immediate: true
    }
  },
  computed: {
    ...mapGetters({
      allMessageContainers: "messagingMessageContainer/messageContainers",
      allMessageRuleAssignments: "messagingMessage/assignments",
      conditionalTexts: "messagingConditionalText/messageConditionalTexts",
      conditionalText: "messagingConditionalText/messageConditionalText",
      messageConditionalTextEditId: "messagingConditionalText/messageConditionalTextEditId",
      pipedText: "messagingMessage/pipedText"
    }),
    container() {
      return this.allMessageContainers.find(messageContainer => messageContainer.id === this.containerId);
    },
    message() {
      return this.container.messages.find(message => message.id === this.messageId);
    },
    content: {
      get() {
        return this.message.content;
      },
      set(newContent) {
        this.message.content = newContent;
      }
    },
    ruleSets: {
      get() {
        return this.conditionalText.ruleSets || [];
      },
      set(newRuleSets) {
        this.conditionalText.ruleSets = newRuleSets
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
    result: {
      get() {
        return this.conditionalText.result;
      },
      set(newResult) {
        this.setMessageConditionalText(
          {
            ...this.conditionalText,
            result: newResult
          }
        );
      }
    },
    resultHtml() {
      return this.result.html;
    },
    type() {
      return this.message.configuration.type;
    },
    hasRuleSets() {
      return this.ruleSets.length > 0;
    },
    hasOriginalRuleSets() {
      return this.originalRuleSets.length > 0;
    },
    toggleOptions() {
      return {
        ruleOperator: [
          "AND",
          "OR"
        ]
      };
    },
    ruleCount() {
      return Object.values(this.ruleSets)
        .reduce((count, {rules}) => count + rules.length, 0);
    },
    hasAvailableRules() {
      return this.ruleCount < this.maxRuleCount;
    },
    readOnly() {
      return this.message.configuration.status === "SENT";
    },
    pipedTextItems() {
      return this.pipedText?.items || [];
    }
  },
  methods: {
    ...mapMutations({
      addMessageConditionalTexts: "messagingConditionalText/addMessageConditionalTexts",
      setMessageConditionalText: "messagingConditionalText/setMessageConditionalText",
      setMessageConditionalTextEditId: "messagingConditionalText/setMessageConditionalTextEditId"
    }),
    addRuleSet() {
      this.ruleSets.push(
        {
          id: null,
          contentId: this.contentId,
          operator: this.ruleSets.length ? "AND" : "NONE",
          rules: [{
            id: null,
            ruleSetId: null,
            operator: "NONE",
            lmsAssignmentId: null,
            assignment: null,
            comparison: null,
            value: null
          }]
        }
      );
    },
    addRule(ruleSetIndex) {
      this.ruleSets.splice(
        ruleSetIndex,
        1,
        {
          ...this.ruleSets[ruleSetIndex],
          rules: [
            ...this.ruleSets[ruleSetIndex].rules,
            {
              id: null,
              ruleSetId: this.ruleSets[ruleSetIndex].id,
              operator: this.ruleSets[ruleSetIndex].rules.length ? "AND" : "NONE",
              lmsAssignmentId: null,
              assignment: null,
              comparison: null,
              value: null
            }
          ]
        }
      );
    },
    clearRule(ruleSetIndex, ruleIndex) {
      this.ruleSets.splice(
        ruleSetIndex,
        1,
        {
          ...this.ruleSets[ruleSetIndex],
          rules: this.ruleSets[ruleSetIndex].rules.toSpliced(
            ruleIndex,
            1,
            {
              ...this.ruleSets[ruleSetIndex].rules[ruleIndex],
              lmsAssignmentId: null,
              assignment: null,
              comparison: null,
              value: null
            }
          )
        }
      );
    },
    deleteRuleSet(ruleSetIndex) {
      this.ruleSets.splice(ruleSetIndex, 1);
    },
    deleteRule(ruleSetIndex, ruleIndex) {
      this.ruleSets.splice(
        ruleSetIndex,
        1,
        {
          ...this.ruleSets[ruleSetIndex],
          rules: [...this.ruleSets[ruleSetIndex].rules.toSpliced(ruleIndex, 1)]
        }
      );

      if (ruleIndex === 0 && this.ruleSets[ruleSetIndex].rules.length) {
        // if this was the top-most rule in the ruleset, and there are still rules left, set the next top-most rule's operator to NONE
        this.ruleSets.splice(
          ruleSetIndex,
          1,
          {
            ...this.ruleSets[ruleSetIndex],
            rules: [{
              ...this.ruleSets[ruleSetIndex].rules[0],
              operator: "NONE"
            }]
          }
        );
      }
    },
    resetRuleSets() {
      this.ruleSets = JSON.parse(JSON.stringify(this.originalRuleSets));
    },
    updateRule(ruleSetIndex, ruleIndex, rule) {
      this.ruleSets = this.ruleSets.toSpliced(
        ruleSetIndex,
        1,
        {
          ...this.ruleSets[ruleSetIndex],
          rules: this.ruleSets[ruleSetIndex].rules.toSpliced(
            ruleIndex,
            1,
            {
              ...rule,
              assignment: rule.assignment || null,
              comparison: rule.assignment ? rule.comparison : null,
              value: rule.assignment && rule.comparison && rule.comparison.requiresValue ? rule.value : null,
              lmsAssignmentId: rule.assignment ? rule.assignment.lmsId : null
            }
          )
        }
      );
    },
    updateOperatorToggle(newValue, ruleSetIndex, ruleIndex) {
      let ruleSet = this.ruleSets[ruleSetIndex];

      if (!ruleIndex) {
        ruleSet.operator = newValue;
      } else {
        ruleSet.rules[ruleIndex].operator = newValue;
      }

      this.ruleSets.splice(ruleSetIndex, 1, ruleSet);
    },
    handleEditedResultBody(body) {
      this.result = {
        ...this.result,
        html: body
      };
    },
    handleCursor(cursorPosition) {
      this.editorCursorPosition = cursorPosition;
    },
    insertPipedText(item) {
      this.pipedTextToPlace = {
        ...item,
        cursorPosition: this.editorCursorPosition
      };
    },
    cancel() {
      this.setMessageConditionalText(null);
      this.setMessageConditionalTextEditId(null);
      this.isEdit = false;
      this.originalRuleSets = [];
      this.$emit("cancel");
    },
    async handleSave() {
      this.validationErrors = validateConditionalText(this.conditionalTexts, this.conditionalText);

      if (this.validationErrors.hasErrors) {
        this.$swal("Please complete all required sections.");
        return;
      }

      this.addMessageConditionalTexts([this.conditionalText]);
      this.setMessageConditionalText(this.conditionalText);
      this.$emit("conditionalTextCreated", this.conditionalText);
    },
    async handleUpdate() {
      this.validationErrors = validateConditionalText(this.conditionalTexts, this.conditionalText);

      if (this.validationErrors.hasErrors) {
        this.$swal("Please complete all required sections.");
        return;
      }

      this.addMessageConditionalTexts([this.conditionalText]);
      this.setMessageConditionalText(this.conditionalText);
      this.$emit("conditionalTextUpdated", this.conditionalText);
    },
    async initialize() {
      if (this.messageConditionalTextEditId) {
        // editing an existing conditional text
        this.isEdit = true;
        this.setMessageConditionalText(this.conditionalTexts.find(conditionalText => conditionalText.id === this.messageConditionalTextEditId));
        this.initialResultContent = this.resultHtml;
        this.ruleSets = this.ruleSets
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
        this.originalRuleSets = JSON.parse(JSON.stringify(this.ruleSets));
        return;
      }

      // creating a new conditional text; initialize with default values
      this.originalRuleSets = [];
      this.initialResultContent = null
      this.setMessageConditionalText(
        {
          id: crypto.randomUUID(),
          contentId: this.contentId,
          isNew: true,
          label: null,
          result: {
            id: null,
            conditionalTextId: null,
            html: null
          },
          ruleSets: []
        }
      );
      this.addRuleSet();
      this.content = {
        ...this.content,
        conditionalTexts: [this.conditionalText]
      }
    }
  },
  async mounted() {
    this.initialize();
  }
}
</script>

<style scoped>
.rule-sets {
  & .v-card {
    border: 1px solid #9e9e9e;
    border-radius: 4px;
  }
  & .rule-row {
    & .rule-assignment,
    & .rule-comparison,
    & .rule-value,
    & .rule-actions {
      max-width: 100%;
      min-width: 100%;
      margin: 6px 0;
    }
  }
  & .rule-actions {
    display: flex;
    align-items: center;
    justify-content: center;
    max-height: 56px;
    & .v-btn {
      max-height: fit-content;
      min-height: fit-content;
      max-width: fit-content;
      min-width:  fit-content;
      > span {
        max-width: fit-content;
      }
    }
  }
}
.row {
  margin: 0;
  min-width: 100%
}
.v-card {
  min-width: 100%;
  padding: 10px;
}
.action-buttons {
  justify-content: right;
}
.results-row {
  flex-direction: column;
  align-items: start;
  & .editor-container {
    min-width: 100%;
    border-radius: 4px;
  }
}
.add-rule-set {
  display: flex;
  flex-direction: column;
  max-width: fit-content;
  > button {
    max-height: fit-content;
    max-width: fit-content;
  }
}
.validation-error {
  border: 2px solid red !important;
}
</style>
