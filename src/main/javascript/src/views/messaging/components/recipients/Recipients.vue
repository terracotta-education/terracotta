<template>
<v-expansion-panels
  v-if="loaded"
  :disabled="!hasMessageRuleAssignments"
  :class="{'validation-error': validationErrors.hasErrors}"
  class="my-6"
  flat
>
  <v-expansion-panel>
    <v-expansion-panel-header
      class="recipients-header"
    >
      <v-icon>mdi-filter-outline</v-icon>
      <span
        class="ml-4"
      >
        Select recipients
      </span>
      <v-chip
        class="ml-4"
        color="primary"
        text-color="white"
        small
      >
        {{ ruleCount }} rule{{ ruleCount !== 1 ? "s" : "" }} applied
      </v-chip>
    </v-expansion-panel-header>
    <v-expansion-panel-content>
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
                v-if="!readOnly"
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
              class="rule-row"
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
            class="px-0"
            text
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
          v-if="!readonly && hasInitialRuleSets && !hasRuleSets"
          @click="resetRuleSets"
          color="primary"
          class="px-0"
          text
        >
          Reset
        </v-btn>
      </v-row>
      <v-row
        v-if="hasRuleSets"
      >
        <span
          class="my-4"
        >
          <b>THEN</b>
        </span>
      </v-row>
      <v-row
        v-if="hasRuleSets"
        class="my-2"
      >
        <toggle
          :selectedOption="matchType"
          :options="toggleOptions.matchType"
          :readOnly="readOnly"
          @update="updateMatchTypeToggle"
        />
        <span
          class="ml-4 my-auto"
        >
          Matching recipients
        </span>
      </v-row>
      <v-row
        v-if="hasRuleSets"
      >
        <span>Rules are processed in order.</span>
      </v-row>
    </v-expansion-panel-content>
  </v-expansion-panel>
</v-expansion-panels>
</template>

<script>
import { mapGetters } from "vuex";
import { validations } from "@/helpers/messaging/validation";
import Toggle from "@/views/messaging/components/recipients/components/form/Toggle.vue";

export default {
  components: {
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
    },
    readOnly: {
      type: Boolean,
      default: false
    }
  },
  data: () => ({
    displayedRuleSets: [],
    initialRuleSets: [],
    validationErrors: null,
    loaded: false
  }),
  watch: {
    validatedErrors: {
      handler(newValidatedErrors) {
        this.validationErrors = newValidatedErrors || validations.message.recipients;
      },
      immediate: true
    }
  },
  computed: {
    ...mapGetters({
      allMessageContainers: "messagingMessageContainer/messageContainers",
      allMessageRuleAssignments: "messagingMessage/assignments"
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
    matchType: {
      get() {
        return this.configuration.matchType || "INCLUDE";
      },
      set(newMatchType) {
        this.configuration.matchType = newMatchType;
      }
    },
    ruleSets: {
      get() {
        return this.message.ruleSets;
      },
      set(newRuleSets) {
        this.message.ruleSets = newRuleSets;
      }
    },
    hasRuleSets() {
      return this.ruleSets.length > 0;
    },
    hasInitialRuleSets() {
      return this.initialRuleSets.length > 0;
    },
    toggleOptions() {
      return {
        ruleOperator: [
          "AND",
          "OR"
        ],
        matchType: [
          "INCLUDE",
          "EXCLUDE"
        ]
      };
    },
    ruleCount() {
      return Object.values(this.ruleSets)
        .reduce((count, {rules}) => count + rules.length, 0);
    },
    hasMessageRuleAssignments() {
      return this.allMessageRuleAssignments.length > 0;
    },
    hasAvailableRules() {
      return this.ruleCount < 8;
    }
  },
  methods: {
    addRuleSet() {
      this.ruleSets = [
        ...this.ruleSets,
        {
          id: null,
          messageId: this.messageId,
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
      ];
    },
    addRule(ruleSetIndex) {
      this.ruleSets = this.ruleSets.toSpliced(
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
      this.ruleSets = this.ruleSets.toSpliced(
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
      this.ruleSets = this.ruleSets.toSpliced(ruleSetIndex, 1);
    },
    deleteRule(ruleSetIndex, ruleIndex) {
      this.ruleSets = this.ruleSets.toSpliced(
        ruleSetIndex,
        1,
        {
          ...this.ruleSets[ruleSetIndex],
          rules: [...this.ruleSets[ruleSetIndex].rules.toSpliced(ruleIndex, 1)]
        }
      );

      if (ruleIndex === 0 && this.ruleSets[ruleSetIndex].rules.length) {
        // if this was the top-most rule in the ruleset, and there are still rules left, set the next top-most rule's operator to NONE
        this.ruleSets = this.ruleSets.toSpliced(
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
      this.ruleSets = JSON.parse(JSON.stringify(this.initialRuleSets));
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

      this.ruleSets = this.ruleSets.toSpliced(ruleSetIndex, 1, ruleSet);
    },
    updateMatchTypeToggle(newMatchType) {
      this.matchType = newMatchType;
    },
    async initialize() {
      this.initialRuleSets = JSON.parse(JSON.stringify(this.ruleSets));
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
    justify-content: space-between;
    & .rule-assignment {
      max-width: 45%;
      min-width: 45%;
    }
    & .rule-comparison,
    & .rule-value {
      max-width: 20%;
      min-width: 20%;
    }
  }
  & .rule-actions {
    display: flex;
    align-items: center;
    justify-content: left;
    max-width: 10%;
    min-width: 10%;
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
