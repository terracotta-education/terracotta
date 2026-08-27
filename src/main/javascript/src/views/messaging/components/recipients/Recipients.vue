<template>
  <v-expansion-panels
    v-if="loaded"
    :disabled="!hasMessageRuleAssignments"
    :class="{ 'validation-error': validationErrors.hasErrors }"
    class="my-6"
  >
    <v-expansion-panel elevation="0" @click="panelExpansion">
      <v-expansion-panel-title class="recipients-header">
        <v-icon>mdi-filter-outline</v-icon>

        <span class="ml-4">
          Select recipients
        </span>

        <v-chip
          class="ml-4"
          color="primary"
          variant="flat"
          size="small"
        >
          {{ ruleCount }} rule{{ ruleCount !== 1 ? "s" : "" }} applied
        </v-chip>
      </v-expansion-panel-title>

      <v-expansion-panel-text>
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
                v-if="!readOnly"
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
              class="rule-row"
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
              class="px-0"
              variant="text"
              @click="addRuleSet"
            >
              Add a new set of rules
            </v-btn>

            <span
              v-if="!readOnly && !hasAvailableRules"
              class="warn-max-rules ml-4"
            >
              You have used the maximum number of rules ({{ maxRuleCount }})
            </span>
          </div>

          <v-btn
            v-if="!readOnly && hasInitialRuleSets && !hasRuleSets"
            color="primary"
            class="px-0"
            variant="text"
            @click="resetRuleSets"
          >
            Reset
          </v-btn>
        </v-row>

        <v-row v-if="hasRuleSets">
          <span class="my-4">
            <b>THEN</b>
          </span>
        </v-row>

        <v-row
          v-if="hasRuleSets"
          class="my-2"
        >
          <Toggle
            :selected-option="matchType"
            :options="toggleOptions.matchType"
            :read-only="readOnly"
            @update="updateMatchTypeToggle"
          />

          <span class="ml-4 my-auto">
            Matching recipients
          </span>
        </v-row>

        <v-row v-if="hasRuleSets">
          <span>Rules are processed in order.</span>
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
  nextTick,
  onMounted,
  onBeforeUnmount
} from "vue";

import { validations } from "@/helpers/messaging/validation";
import {
  deleteAttributesFromElement,
  createStatusAlert,
  statusAlert
} from "@/helpers/ui-utils.js";

import Toggle from "@/views/messaging/components/recipients/components/form/Toggle.vue";

import { container as messagingMessageContainerModule } from "@/store/messaging/container.module";
import { message as messagingMessageModule } from "@/store/messaging/message.module";
import { alert as alertModule } from "@/store/alert.module";

defineOptions({
  name: "MessageRecipients"
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
  },
  readOnly: {
    type: Boolean,
    default: false
  }
});

const messagingMessageContainerStore = messagingMessageContainerModule();
const messagingMessageStore = messagingMessageModule();
const alertStore = alertModule();

const initialRuleSets = ref([]);
const validationErrors = ref(
  props.validatedErrors || validations.message.recipients
);
const loaded = ref(false);

const allMessageContainers = computed(() => {
  return messagingMessageContainerStore.messageContainers || [];
});

const allMessageRuleAssignments = computed(() => {
  return messagingMessageStore.assignments || [];
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

const configuration = computed(() => {
  return message.value?.configuration || {};
});

const matchType = computed({
  get() {
    return configuration.value.matchType || "INCLUDE";
  },

  set(value) {
    configuration.value.matchType = value;
  }
});

const ruleSets = computed({
  get() {
    return message.value?.ruleSets || [];
  },

  set(value) {
    message.value.ruleSets = value;
  }
});

const hasRuleSets = computed(() => {
  return ruleSets.value.length > 0;
});

const hasInitialRuleSets = computed(() => {
  return initialRuleSets.value.length > 0;
});

const toggleOptions = {
  ruleOperator: [
    "AND",
    "OR"
  ],
  matchType: [
    "INCLUDE",
    "EXCLUDE"
  ]
};

const ruleCount = computed(() => {
  return ruleSets.value.reduce(
    (count, ruleSet) => count + ruleSet.rules.length,
    0
  );
});

const hasMessageRuleAssignments = computed(() => {
  return allMessageRuleAssignments.value.length > 0;
});

const hasAvailableRules = computed(() => {
  return ruleCount.value < props.maxRuleCount;
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

const addRuleSet = () => {
  ruleSets.value = [
    ...ruleSets.value,
    {
      id: null,
      messageId: props.messageId,
      operator: ruleSets.value.length ? "AND" : "NONE",
      rules: [
        createEmptyRule()
      ]
    }
  ];

  createStatusAlert(
    statusAlert(
      alertStatuses.value.success,
      "New rule set added"
    )
  );
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

  createStatusAlert(
    statusAlert(
      alertStatuses.value.success,
      "New rule added"
    )
  );
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

  createStatusAlert(
    statusAlert(
      alertStatuses.value.success,
      "Rule cleared"
    )
  );
};

const deleteRuleSet = ruleSetIndex => {
  ruleSets.value = ruleSets.value.toSpliced(
    ruleSetIndex,
    1
  );

  createStatusAlert(
    statusAlert(
      alertStatuses.value.success,
      "Rule set deleted"
    )
  );
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

  createStatusAlert(
    statusAlert(
      alertStatuses.value.success,
      "Rule deleted"
    )
  );
};

const resetRuleSets = () => {
  ruleSets.value = clone(initialRuleSets.value);

  createStatusAlert(
    statusAlert(
      alertStatuses.value.success,
      "Rule sets reset"
    )
  );
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

const updateMatchTypeToggle = value => {
  matchType.value = value;
};

const patchRuleSelectAria = async () => {
  await nextTick();

  const ruleNodes = document.querySelectorAll(
    ".v-select.rule-variable .v-field, .v-select.rule-comparison .v-field"
  );

  ruleNodes.forEach(node => {
    const ariaOwnsId = node.getAttribute("aria-owns");

    node.setAttribute("role", "combobox");
    node.setAttribute("aria-controls", ariaOwnsId);
  });
};

const initialize = async () => {
  initialRuleSets.value = clone(ruleSets.value);
};

let panelExpansionTimer = null;

const panelExpansion = () => {
  panelExpansionTimer = window.setTimeout(() => {
    deleteAttributesFromElement(
      ".v-expansion-panel",
      ["aria-expanded"]
    );
  }, 1000);
};

onBeforeUnmount(() => {
  window.clearTimeout(panelExpansionTimer);
});

watch(
  () => props.validatedErrors,
  value => {
    validationErrors.value =
      value || validations.message.recipients;
  },
  {
    immediate: true
  }
);

watch(
  ruleSets,
  () => {
    patchRuleSelectAria();
  },
  {
    deep: true,
    immediate: true
  }
);

onMounted(async () => {
  await initialize();

  deleteAttributesFromElement(
    ".v-expansion-panel",
    ["aria-expanded"]
  );

  loaded.value = true;
});
</script>

<style lang="scss" scoped>
.v-expansion-panels {
  border: 1px solid #9e9e9e;
  border-radius: 4px;
}

// .v-expansion-panel is <v-expansion-panel>'s own root, rendered directly in this
// template, so it already carries this component's scope attribute - :deep() here
// would require a scoped ancestor further up, which doesn't exist for a root element,
// and never matches.
.v-expansion-panel {
  margin-bottom: 0 !important;
  border: none !important;
}

:deep(.v-expansion-panel-text__wrapper) {
  padding: 10px 20px;
}

.recipients-header {
  display: flex;
  align-content: start;

  > * {
    max-width: fit-content;
  }
}

.rule-sets {
  & .v-card {
    border: 1px solid #9e9e9e;
    border-radius: 4px;
  }

  & .rule-row {
    /* Vuetify 3's default v-row column-gap (24px) doesn't exist in Vuetify 2 -
       with up to 5 flex children here (the optional operator Toggle row, plus
       variable/comparison/value/actions) that's up to 4 gaps (~96px) on top of
       the percentage widths below, easily enough to force an unwanted wrap even
       at comfortable widths. Zero it and let flex-wrap: wrap (the default) do
       its normal job - comparison/value/actions should move below variable as
       the row narrows, they just shouldn't be forced there by leftover gap.
       Only the column-gap (horizontal, between items sharing a line) needs to
       be zero though - row-gap (vertical, between wrapped lines) should match
       the Toggle row's own my-2 margin-bottom (8px) above it, not collapse to
       nothing once variable/comparison/value/actions start wrapping onto their
       own lines at narrow widths. */
    column-gap: 0;
    row-gap: 8px;
    justify-content: space-between;

    /* the global .v-input margin (10px total per input) plus the four items'
       percentage widths (95% combined) leaves only a few px of slack across the
       row at comfortable widths - enough to trigger the same unwanted-wrap
       issue as the gap above, so trim it specifically here rather than touch
       .v-input's margin globally. */
    & .v-input {
      margin: 0 2px !important;
    }

    /* the Toggle row's own my-2 margin-bottom (8px) would otherwise compound
       with row-gap above (also 8px) for this one transition only, making the
       gap below the Toggle row twice as tall as every other wrapped-line gap
       in this row. row-gap alone is enough once this is zeroed. */
    & > .v-row.my-2 {
      margin-bottom: 0;
    }

    & .rule-variable {
      max-width: 45%;
      min-width: 45%;
    }

    & .rule-comparison,
    & .rule-value {
      max-width: 20%;
      /* not a plain 20% - that never varies relative to the row, so these two
         could never individually run out of room and wrap on their own, no
         matter how narrow the row got. A pixel floor lets them behave like
         rule-actions already does: fit inline while there's room, wrap below
         once the row can no longer fit their minimum usable width. */
      min-width: max(20%, 120px);
    }
  }

  & .rule-actions {
    display: flex;
    align-items: center;
    justify-content: left;
    max-width: 10%;
    /* same fix as rule-comparison/rule-value: a plain 10% never varies relative
       to the row, so its two fit-content buttons (Clear + the delete icon, natural
       combined width ~130px) would overflow past the row's right edge rather
       than the row wrapping this below - not fitting inside its own box, but
       still declared as "fitting" the row from the flex algorithm's point of
       view since 10% was satisfied regardless of the overflowing content. */
    min-width: max(10%, 130px);
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
  /* Vuetify 3 automatically adds margin-top between adjacent sibling v-rows
     (.v-row + .v-row, part of its gap-based grid system); Vuetify 2 didn't, and
     none of the rows here want it - every intentional gap in this component
     comes from an explicit spacing utility class on the row itself instead.
     Not margin-left/right too: Vuetify 3's .v-row has no default left/right
     margin to begin with (unlike Vuetify 2's negative-margin gutters), and the
     nested Toggle row below relies on ml-1 for its own left indent. */
  margin-top: 0;
  min-width: 100%;
}

.v-row.my-2 {
  /* restore my-2's own top margin - the blanket reset above exists only to
     cancel Vuetify 3's unwanted auto-margin between sibling rows, not to
     override an explicit spacing utility that's actually wanted here. */
  margin-top: 8px;
}

.v-card {
  min-width: 100%;
  padding: 10px;
}

.v-input {
  margin: 0 5px !important;
}

.add-rule-set {
  max-width: fit-content;

  > button {
    padding: 0;
    max-height: fit-content;
  }
}

.validation-error {
  border: 2px solid red !important;
}
</style>
