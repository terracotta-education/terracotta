<template>
  <div v-if="conditionalText">
    <v-row class="mb-4">
      <span>
        {{ isEdit ? "Update" : "Insert" }} Conditional Text
      </span>
    </v-row>

    <v-row class="mb-4">
      <v-text-field
        v-model="conditionalText.label"
        :hide-details="validationErrors.label === null"
        :error-messages="validationErrors.label"
        :disabled="readOnly"
        label="Label"
        class="conditional-text-label"
        variant="outlined"
        density="compact"
      />
    </v-row>

    <v-row
      v-if="hasRuleSets"
      justify="space-between"
    >
      <span>
        <b>IF</b>
      </span>

      <v-btn
        v-if="!readOnly"
        color="primary"
        class="px-0"
        variant="text"
        @click="resetRuleSets"
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
        <Toggle
          :selected-option="ruleSet.operator"
          :options="toggleOptions.ruleOperator"
          :read-only="readOnly"
          @update="updateOperatorToggle($event, ruleSetIndex)"
        />
      </v-row>

      <v-card
        :class="{
          'validation-error':
            validationErrors.ruleSets[ruleSetIndex]
              ? validationErrors.ruleSets[ruleSetIndex].message !== null ||
                validationErrors.ruleSets[ruleSetIndex].hasRulesError
              : false
        }"
        variant="outlined"
      >
        <v-row justify="space-between mb-4">
          <span class="my-auto">
            Rule Set {{ ruleSetIndex + 1 }}
          </span>

          <v-btn
            v-if="!readOnly && ruleSets.length > 1"
            color="primary"
            class="px-0"
            variant="text"
            @click="deleteRuleSet(ruleSetIndex)"
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
            <Toggle
              :selected-option="rule.operator"
              :options="toggleOptions.ruleOperator"
              :read-only="readOnly"
              @update="updateOperatorToggle($event, ruleSetIndex, ruleIndex)"
            />
          </v-row>

          <v-select
            v-model="rule.assignment"
            :items="allMessageRuleAssignments"
            :hide-selected="true"
            :hide-details="getRuleError(ruleSetIndex, ruleIndex, 'variable') === null"
            :error-messages="getRuleError(ruleSetIndex, ruleIndex, 'variable')"
            :disabled="readOnly"
            item-title="title"
            label="Variable"
            class="rule-variable"
            return-object
            variant="outlined"
            density="compact"
            @update:model-value="updateRule(ruleSetIndex, ruleIndex, rule)"
          />

          <v-select
            v-model="rule.comparison"
            :items="rule.assignment ? rule.assignment.comparisons : []"
            :disabled="readOnly || !rule.assignment"
            :hide-selected="true"
            :hide-details="getRuleError(ruleSetIndex, ruleIndex, 'comparison') === null"
            :error-messages="getRuleError(ruleSetIndex, ruleIndex, 'comparison')"
            item-title="label"
            class="rule-comparison"
            aria-label="comparison selector"
            return-object
            variant="outlined"
            density="compact"
            @update:model-value="updateRule(ruleSetIndex, ruleIndex, rule)"
          />

          <v-text-field
            v-model="rule.value"
            :disabled="readOnly || !rule.comparison || !rule.comparison.requiresValue"
            :hide-details="getRuleError(ruleSetIndex, ruleIndex, 'value') === null"
            :error-messages="getRuleError(ruleSetIndex, ruleIndex, 'value')"
            label="Value"
            type="number"
            class="rule-value"
            variant="outlined"
            density="compact"
            @update:model-value="updateRule(ruleSetIndex, ruleIndex, rule)"
          />

          <div class="rule-actions ml-2">
            <v-btn
              v-if="!readOnly"
              color="primary"
              class="px-0"
              variant="text"
              @click="clearRule(ruleSetIndex, ruleIndex)"
            >
              Clear
            </v-btn>

            <v-btn
              v-if="!readOnly && ruleSet.rules.length > 1"
              :aria-label="`delete rule number ${ruleIndex + 1} from rule set number ${ruleSetIndex + 1}`"
              class="ml-2 px-0"
              variant="text"
              @click="deleteRule(ruleSetIndex, ruleIndex)"
            >
              <v-icon>mdi-delete-outline</v-icon>
            </v-btn>
          </div>
        </v-row>

        <v-row>
          <v-btn
            v-if="!readOnly"
            :disabled="!hasAvailableRules"
            color="primary"
            class="px-0"
            variant="text"
            @click="addRule(ruleSetIndex)"
          >
            Add a new rule
          </v-btn>
        </v-row>
      </v-card>
    </v-row>

    <v-row justify="space-between">
      <div class="add-rule-set">
        <v-btn
          v-if="!readOnly"
          :disabled="!hasAvailableRules"
          color="primary"
          class="px-0 my-2"
          variant="text"
          @click="addRuleSet"
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
        v-if="!readOnly && hasOriginalRuleSets && !hasRuleSets"
        color="primary"
        class="px-0"
        variant="text"
        @click="resetRuleSets"
      >
        Reset
      </v-btn>
    </v-row>

    <v-row class="results-row mb-2">
      <span class="my-6">
        <b>THEN INSERT THIS TEXT</b>
      </span>

      <div
        :class="{ 'validation-error': validationErrors.result !== null }"
        class="editor-container"
      >
        <TipTapEditor
          :content="initialResultContent"
          :piped-text-to-place="pipedTextToPlace"
          :read-only="readOnly"
          editor-type="basic"
          required
          @edited="handleEditedResultBody"
          @cursor="handleCursor"
        />

        <EditorSubMenu
          v-if="!readOnly && pipedTextItems.length > 0"
          :experiment-id="experimentId"
          :exposure-id="exposureId"
          :container-id="containerId"
          :message-id="messageId"
          :content-id="contentId"
          :show-attachments="false"
          :show-conditional-text="false"
          @insert-piped-text="insertPipedText"
        />
      </div>

      <div
        v-if="!readOnly && validationErrors.result !== null"
        class="v-text-field__details pt-2 px-3"
      >
        <div
          class="v-messages error-text"
          role="alert"
        >
          <div class="v-messages__wrapper">
            <div class="v-messages__message">
              {{ validationErrors.result }}
            </div>
          </div>
        </div>
      </div>
    </v-row>

    <v-row
      class="action-buttons"
      density="compact"
    >
      <v-btn
        color="primary"
        variant="text"
        @click="cancel"
      >
        {{ readOnly ? "CLOSE" : "CANCEL" }}
      </v-btn>

      <v-btn
        v-if="!readOnly"
        color="primary"
        @click="isEdit ? handleUpdate() : handleSave()"
      >
        {{ isEdit ? "UPDATE" : "SAVE & INSERT" }}
      </v-btn>
    </v-row>
  </div>
</template>

<script setup>
import {
  ref,
  computed,
  watch,
  nextTick,
  onMounted,
} from "vue";

import Swal from "sweetalert2";
import {
  statusAlert,
  createStatusAlert
} from "@/helpers/ui-utils.js";

import {
  validations,
  validateConditionalText
} from "@/helpers/messaging/validation";

import EditorSubMenu from "@/views/messaging/components/menu/editorsubmenu/EditorSubMenu.vue";
import TipTapEditor from "@/components/editor/TipTapEditor.vue";
import Toggle from "@/views/messaging/components/recipients/components/form/Toggle.vue";

import { container as messagingMessageContainerModule } from "@/store/messaging/container.module";
import { message as messagingMessageModule } from "@/store/messaging/message.module";
import { conditionaltext as messagingConditionalTextModule } from "@/store/messaging/conditionaltext.module";
import { alert as alertModule } from "@/store/alert.module";

defineOptions({
  name: "ConditionalText"
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
  },
  maxRuleCount: {
    type: Number,
    default: 8
  },
  validatedErrors: {
    type: Object,
    default: null
  }
});

const emit = defineEmits([
  "conditionalTextCreated",
  "conditionalTextUpdated",
  "cancel"
]);


const messagingMessageContainerStore = messagingMessageContainerModule();
const messagingMessageStore = messagingMessageModule();
const messagingConditionalTextStore = messagingConditionalTextModule();
const alertStore = alertModule();

const initialResultContent = ref(null);
const originalRuleSets = ref([]);
const isEdit = ref(false);
const validationErrors = ref(
  props.validatedErrors || validations.message.conditionalText
);
const editorCursorPosition = ref(null);
const pipedTextToPlace = ref(null);

const allMessageContainers = computed(() => {
  return messagingMessageContainerStore.messageContainers || [];
});

const allMessageRuleAssignments = computed(() => {
  return messagingMessageStore.assignments || [];
});

const conditionalTexts = computed(() => {
  return messagingConditionalTextStore.messageConditionalTexts || [];
});

const conditionalText = computed({
  get() {
    return messagingConditionalTextStore.messageConditionalText;
  },

  set(value) {
    messagingConditionalTextStore.setMessageConditionalText(value);
  }
});

const messageConditionalTextEditId = computed(() => {
  return messagingConditionalTextStore.messageConditionalTextEditId;
});

const pipedText = computed(() => {
  return messagingMessageStore.pipedText;
});

const alertStatuses = computed(() => {
  return alertStore.statuses;
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

const content = computed({
  get() {
    return message.value?.content;
  },

  set(value) {
    message.value.content = value;
  }
});

const ruleSets = computed({
  get() {
    return conditionalText.value?.ruleSets || [];
  },

  set(value) {
    conditionalText.value = {
      ...conditionalText.value,
      ruleSets: value.map(ruleSet => ({
        ...ruleSet,
        rules: ruleSet.rules.map(rule => ({
          ...rule,
          assignment:
            allMessageRuleAssignments.value.find(
              assignment => assignment.lmsId === rule.lmsAssignmentId
            ) || rule.assignment
        }))
      }))
    };
  }
});

const result = computed({
  get() {
    return conditionalText.value?.result || {};
  },

  set(value) {
    conditionalText.value = {
      ...conditionalText.value,
      result: value
    };
  }
});

const resultHtml = computed(() => {
  return result.value.html;
});

const hasRuleSets = computed(() => {
  return ruleSets.value.length > 0;
});

const hasOriginalRuleSets = computed(() => {
  return originalRuleSets.value.length > 0;
});

const toggleOptions = {
  ruleOperator: [
    "AND",
    "OR"
  ]
};

const ruleCount = computed(() => {
  return ruleSets.value.reduce(
    (count, ruleSet) => count + ruleSet.rules.length,
    0
  );
});

const hasAvailableRules = computed(() => {
  return ruleCount.value < props.maxRuleCount;
});

const readOnly = computed(() => {
  return message.value?.configuration?.status === "SENT";
});

const pipedTextItems = computed(() => {
  return pipedText.value?.items || [];
});

const clone = value => {
  return JSON.parse(JSON.stringify(value));
};

const getRuleError = (
  ruleSetIndex,
  ruleIndex,
  field
) => {
  return validationErrors.value?.ruleSets?.[ruleSetIndex]?.rules?.[ruleIndex]?.[field] || null;
};

const createEmptyRule = (
  ruleSetId = null,
  operator = "NONE"
) => {
  return {
    id: null,
    ruleSetId,
    operator,
    lmsAssignmentId: null,
    assignment: null,
    comparison: null,
    value: null
  };
};

const patchAria = async () => {
  await nextTick();

  const ruleNodes = document.querySelectorAll(
    ".v-select.rule-variable .v-field, .v-select.rule-comparison .v-field"
  );

  ruleNodes.forEach(node => {
    const ariaOwnsId = node.getAttribute("aria-owns");

    node.setAttribute("role", "combobox");
    node.setAttribute("aria-controls", ariaOwnsId);
  });

  const editor = document.querySelector(".tiptap.ProseMirror");

  if (editor) {
    editor.setAttribute(
      "aria-label",
      "message editor content"
    );
  }
};

const setStatus = messageText => {
  createStatusAlert(
    statusAlert(
      alertStatuses.value.success,
      messageText
    )
  );
};

const addRuleSet = () => {
  ruleSets.value = [
    ...ruleSets.value,
    {
      id: null,
      contentId: props.contentId,
      operator: ruleSets.value.length ? "AND" : "NONE",
      rules: [
        createEmptyRule()
      ]
    }
  ];

  setStatus("Conditional text rule set added");
};

const addRule = ruleSetIndex => {
  const ruleSet = ruleSets.value[ruleSetIndex];

  ruleSets.value = ruleSets.value.toSpliced(
    ruleSetIndex,
    1,
    {
      ...ruleSet,
      rules: [
        ...ruleSet.rules,
        createEmptyRule(
          ruleSet.id,
          ruleSet.rules.length ? "AND" : "NONE"
        )
      ]
    }
  );

  setStatus("Conditional text rule added");
};

const clearRule = (
  ruleSetIndex,
  ruleIndex
) => {
  const ruleSet = ruleSets.value[ruleSetIndex];

  ruleSets.value = ruleSets.value.toSpliced(
    ruleSetIndex,
    1,
    {
      ...ruleSet,
      rules: ruleSet.rules.toSpliced(
        ruleIndex,
        1,
        {
          ...ruleSet.rules[ruleIndex],
          lmsAssignmentId: null,
          assignment: null,
          comparison: null,
          value: null
        }
      )
    }
  );

  setStatus("Conditional text rule cleared");
};

const deleteRuleSet = ruleSetIndex => {
  ruleSets.value = ruleSets.value.toSpliced(
    ruleSetIndex,
    1
  );

  setStatus("Conditional text rule set deleted");
};

const deleteRule = (
  ruleSetIndex,
  ruleIndex
) => {
  const ruleSet = ruleSets.value[ruleSetIndex];
  const updatedRules = ruleSet.rules.toSpliced(
    ruleIndex,
    1
  );

  if (
    ruleIndex === 0 &&
    updatedRules.length
  ) {
    updatedRules[0] = {
      ...updatedRules[0],
      operator: "NONE"
    };
  }

  ruleSets.value = ruleSets.value.toSpliced(
    ruleSetIndex,
    1,
    {
      ...ruleSet,
      rules: updatedRules
    }
  );

  setStatus("Conditional text rule deleted");
};

const resetRuleSets = () => {
  ruleSets.value = clone(originalRuleSets.value);
  setStatus("Conditional text rule sets reset");
};

const updateRule = (
  ruleSetIndex,
  ruleIndex,
  rule
) => {
  const ruleSet = ruleSets.value[ruleSetIndex];

  ruleSets.value = ruleSets.value.toSpliced(
    ruleSetIndex,
    1,
    {
      ...ruleSet,
      rules: ruleSet.rules.toSpliced(
        ruleIndex,
        1,
        {
          ...rule,
          assignment: rule.assignment || null,
          comparison: rule.assignment
            ? rule.comparison
            : null,
          value:
            rule.assignment &&
            rule.comparison &&
            rule.comparison.requiresValue
              ? rule.value
              : null,
          lmsAssignmentId: rule.assignment
            ? rule.assignment.lmsId
            : null
        }
      )
    }
  );

  setStatus("Conditional text rule updated");
};

const updateOperatorToggle = (
  value,
  ruleSetIndex,
  ruleIndex = null
) => {
  const ruleSet = {
    ...ruleSets.value[ruleSetIndex],
    rules: [
      ...ruleSets.value[ruleSetIndex].rules
    ]
  };

  if (ruleIndex === null) {
    ruleSet.operator = value;
  } else {
    ruleSet.rules[ruleIndex] = {
      ...ruleSet.rules[ruleIndex],
      operator: value
    };
  }

  ruleSets.value = ruleSets.value.toSpliced(
    ruleSetIndex,
    1,
    ruleSet
  );
};

const handleEditedResultBody = body => {
  result.value = {
    ...result.value,
    html: body
  };
};

const handleCursor = cursorPosition => {
  editorCursorPosition.value = cursorPosition;
};

const insertPipedText = item => {
  pipedTextToPlace.value = {
    ...item,
    cursorPosition: editorCursorPosition.value
  };

  setStatus("Conditional text piped text inserted");
};

const cancel = () => {
  conditionalText.value = null;
  messagingConditionalTextStore.setMessageConditionalTextEditId(null);
  isEdit.value = false;
  originalRuleSets.value = [];

  setStatus("Conditional text cancelled");

  emit("cancel");
};

const validate = () => {
  validationErrors.value = validateConditionalText(
    conditionalTexts.value,
    conditionalText.value
  );

  if (validationErrors.value.hasErrors) {
    Swal.fire("Please complete all required sections.");
    return false;
  }

  return true;
};

const saveConditionalText = statusMessage => {
  if (!validate()) {
    return;
  }

  messagingConditionalTextStore.addMessageConditionalTexts([
    conditionalText.value
  ]);

  setStatus(statusMessage);
};

const handleSave = async () => {
  saveConditionalText(
    "Conditional text saved and inserted"
  );

  if (!validationErrors.value.hasErrors) {
    emit(
      "conditionalTextCreated",
      conditionalText.value
    );
  }
};

const handleUpdate = async () => {
  saveConditionalText("Conditional text updated");

  if (!validationErrors.value.hasErrors) {
    emit(
      "conditionalTextUpdated",
      conditionalText.value
    );
  }
};

const initialize = async () => {
  if (messageConditionalTextEditId.value) {
    isEdit.value = true;

    conditionalText.value = clone(conditionalTexts.value.find(
      item => item.id === messageConditionalTextEditId.value
    ));

    initialResultContent.value = resultHtml.value;

    ruleSets.value = ruleSets.value.map(ruleSet => ({
      ...ruleSet,
      rules: ruleSet.rules.map(rule => ({
        ...rule,
        assignment:
          allMessageRuleAssignments.value.find(
            assignment => assignment.lmsId === rule.lmsAssignmentId
          ) || rule.assignment
      }))
    }));

    originalRuleSets.value = clone(ruleSets.value);

    return;
  }

  originalRuleSets.value = [];
  initialResultContent.value = null;

  conditionalText.value = {
    id: crypto.randomUUID(),
    contentId: props.contentId,
    isNew: true,
    label: null,
    result: {
      id: null,
      conditionalTextId: null,
      html: null
    },
    ruleSets: []
  };

  addRuleSet();

  content.value = {
    ...content.value,
    conditionalTexts: [
      conditionalText.value
    ]
  };
};

watch(
  () => props.validatedErrors,
  value => {
    validationErrors.value =
      value || validations.message.conditionalText;
  },
  {
    immediate: true
  }
);

watch(
  conditionalText,
  () => {
    patchAria();
  },
  {
    deep: true,
    immediate: true
  }
);

watch(
  messageConditionalTextEditId,
  () => {
    isEdit.value = false;
    initialize();
  },
  {
    immediate: true
  }
);

onMounted(() => {
  initialize();
});
</script>

<style lang="scss" scoped>
.rule-sets {
  & .v-card {
    border: 1px solid map.get($grey, "darker");
    border-radius: 4px;
  }

  & .rule-row {
    & .rule-variable,
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
      min-width: fit-content;

      > span {
        max-width: fit-content;
      }
    }
  }
}

.v-row {
  margin: 0;
  min-width: 100%;
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
  border: 2px solid map.get($red, "base") !important;
}

.error-text {
  color: map.get($red, "base") !important;
}
</style>
