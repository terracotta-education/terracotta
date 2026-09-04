import { afterEach, describe, expect, it } from "vitest";
import { createPinia, setActivePinia } from "pinia";
import { flushPromises } from "@vue/test-utils";

import { mountComponent } from "@/test-utils/mount";
import { container as useContainerStore } from "@/store/messaging/container.module";
import { message as useMessageStore } from "@/store/messaging/message.module";
import Recipients from "./Recipients.vue";

const baseProps = {
  experimentId: 1,
  exposureId: "exposure-1",
  containerId: "container-1",
  messageId: "message-1",
  contentId: "content-1"
};

const scoreAssignment = {
  title: "Score",
  lmsId: "lms-score",
  comparisons: [
    { id: "eq", label: "Equals", requiresValue: true },
    { id: "empty", label: "Is empty", requiresValue: false }
  ]
};

const emptyRule = (operator = "NONE") => ({
  id: null,
  ruleSetId: "rs1",
  operator,
  lmsAssignmentId: null,
  assignment: null,
  comparison: null,
  value: null
});

const setupStores = ({
  ruleSets = [],
  assignments = [scoreAssignment]
} = {}) => {
  const pinia = createPinia();
  setActivePinia(pinia);

  const msg = {
    id: "message-1",
    ruleSets,
    configuration: { matchType: "INCLUDE" }
  };

  useContainerStore().messageContainers = [
    { id: "container-1", messages: [msg] }
  ];
  useMessageStore().assignments = assignments;

  return { pinia, msg };
};

const mountRecipients = async (props = {}, storeOptions = {}) => {
  const { pinia, msg } = setupStores(storeOptions);
  const wrapper = mountComponent(Recipients, {
    props: { ...baseProps, ...props },
    pinia
  });

  await flushPromises();

  // The rule-set editing UI lives inside a collapsed v-expansion-panel-text,
  // which Vuetify only renders once the panel has been opened.
  await wrapper.find(".v-expansion-panel-title").trigger("click");
  await flushPromises();

  return { wrapper, msg };
};

const findBtn = (wrapper, text) =>
  wrapper.findAllComponents({ name: "VBtn" }).find(b => b.text() === text);

describe("Recipients", () => {
  let wrapper;

  afterEach(() => {
    wrapper?.unmount();
  });

  it("renders zero rules applied and hides the IF section when there are no rule sets", async () => {
    ({ wrapper } = await mountRecipients());

    expect(wrapper.text()).toContain("0 rules applied");
    expect(findBtn(wrapper, "Add a new set of rules")).toBeTruthy();
  });

  it("adds a rule set on click, showing Rule Set 1 and updating the count", async () => {
    ({ wrapper } = await mountRecipients());

    await findBtn(wrapper, "Add a new set of rules").trigger("click");

    expect(wrapper.text()).toContain("Rule Set 1");
    expect(wrapper.text()).toContain("1 rule applied");
  });

  it("defaults a second rule set's operator to AND and renders a toggle for it", async () => {
    ({ wrapper } = await mountRecipients(
      {},
      { ruleSets: [{ id: "rs1", messageId: "message-1", operator: "NONE", rules: [emptyRule()] }] }
    ));

    await findBtn(wrapper, "Add a new set of rules").trigger("click");

    expect(wrapper.text()).toContain("Rule Set 2");

    const operatorToggles = wrapper
      .findAllComponents({ name: "RecipientToggle" })
      .filter(t => t.props("options")[0] === "AND");
    expect(operatorToggles).toHaveLength(1);
    expect(operatorToggles[0].props("selectedOption")).toBe("AND");
    expect(operatorToggles[0].props("options")).toEqual(["AND", "OR"]);
  });

  it("updates a rule set's operator when its toggle emits update", async () => {
    ({ wrapper } = await mountRecipients(
      {},
      {
        ruleSets: [
          { id: "rs1", messageId: "message-1", operator: "NONE", rules: [emptyRule()] },
          { id: "rs2", messageId: "message-1", operator: "AND", rules: [emptyRule()] }
        ]
      }
    ));

    const toggle = wrapper.findComponent({ name: "RecipientToggle" });
    expect(toggle.props("selectedOption")).toBe("AND");

    await toggle.vm.$emit("update", "OR");
    await wrapper.vm.$nextTick();

    expect(wrapper.findComponent({ name: "RecipientToggle" }).props("selectedOption")).toBe("OR");
  });

  it("adds a rule to a set, defaulting the new rule's operator to AND", async () => {
    ({ wrapper } = await mountRecipients(
      {},
      { ruleSets: [{ id: "rs1", messageId: "message-1", operator: "NONE", rules: [emptyRule()] }] }
    ));

    await findBtn(wrapper, "Add a new rule").trigger("click");

    expect(wrapper.text()).toContain("2 rules applied");

    const operatorToggles = wrapper
      .findAllComponents({ name: "RecipientToggle" })
      .filter(t => t.props("options")[0] === "AND");
    expect(operatorToggles).toHaveLength(1);
    expect(operatorToggles[0].props("selectedOption")).toBe("AND");
  });

  it("disables adding more rules and shows a warning once maxRuleCount is reached", async () => {
    ({ wrapper } = await mountRecipients(
      { maxRuleCount: 1 },
      { ruleSets: [{ id: "rs1", messageId: "message-1", operator: "NONE", rules: [emptyRule()] }] }
    ));

    expect(wrapper.text()).toContain("You have used the maximum number of rules (1)");
    expect(findBtn(wrapper, "Add a new set of rules").props("disabled")).toBe(true);
    expect(findBtn(wrapper, "Add a new rule").props("disabled")).toBe(true);
  });

  it("cascades variable selection into available comparisons, and enables the value field only when required", async () => {
    ({ wrapper } = await mountRecipients(
      {},
      { ruleSets: [{ id: "rs1", messageId: "message-1", operator: "NONE", rules: [emptyRule()] }] }
    ));

    const variableSelect = wrapper.findComponent(".rule-variable");
    await variableSelect.setValue(scoreAssignment);
    await wrapper.vm.$nextTick();

    const comparisonSelect = wrapper.findComponent(".rule-comparison");
    expect(comparisonSelect.props("items")).toEqual(scoreAssignment.comparisons);
    expect(comparisonSelect.props("disabled")).toBe(false);

    let valueField = wrapper.findComponent(".rule-value");
    expect(valueField.props("disabled")).toBe(true);

    await comparisonSelect.setValue(scoreAssignment.comparisons[0]);
    await wrapper.vm.$nextTick();

    valueField = wrapper.findComponent(".rule-value");
    expect(valueField.props("disabled")).toBe(false);
  });

  it("clears a rule's assignment, comparison, and value", async () => {
    const filledRule = {
      ...emptyRule(),
      assignment: scoreAssignment,
      comparison: scoreAssignment.comparisons[0],
      value: "5",
      lmsAssignmentId: scoreAssignment.lmsId
    };

    ({ wrapper } = await mountRecipients(
      {},
      { ruleSets: [{ id: "rs1", messageId: "message-1", operator: "NONE", rules: [filledRule] }] }
    ));

    await findBtn(wrapper, "Clear").trigger("click");

    expect(wrapper.findComponent(".rule-variable").props("modelValue")).toBeNull();
    expect(wrapper.findComponent(".rule-comparison").props("modelValue")).toBeNull();
    expect(wrapper.findComponent(".rule-value").props("modelValue")).toBeFalsy();
  });

  it("deletes a rule and resets the remaining first rule's operator to NONE", async () => {
    ({ wrapper } = await mountRecipients(
      {},
      {
        ruleSets: [
          {
            id: "rs1",
            messageId: "message-1",
            operator: "NONE",
            rules: [emptyRule("NONE"), emptyRule("AND")]
          }
        ]
      }
    ));

    const operatorToggles = () =>
      wrapper
        .findAllComponents({ name: "RecipientToggle" })
        .filter(t => t.props("options")[0] === "AND");

    expect(wrapper.text()).toContain("2 rules applied");
    expect(operatorToggles()).toHaveLength(1);

    await wrapper
      .find("[aria-label='delete rule number 1 from rule set number 1']")
      .trigger("click");

    expect(wrapper.text()).toContain("1 rule applied");
    expect(operatorToggles()).toHaveLength(0);
  });

  it("deletes an entire rule set", async () => {
    ({ wrapper } = await mountRecipients(
      {},
      {
        ruleSets: [
          { id: "rs1", messageId: "message-1", operator: "NONE", rules: [emptyRule()] },
          { id: "rs2", messageId: "message-1", operator: "AND", rules: [emptyRule()] }
        ]
      }
    ));

    expect(wrapper.text()).toContain("Rule Set 2");

    await findBtn(wrapper, "Delete Rule Set").trigger("click");

    expect(wrapper.text()).not.toContain("Rule Set 2");
    expect(wrapper.text()).toContain("1 rule applied");
  });

  it("resets rule sets back to their initial mounted state", async () => {
    ({ wrapper } = await mountRecipients(
      {},
      { ruleSets: [{ id: "rs1", messageId: "message-1", operator: "NONE", rules: [emptyRule()] }] }
    ));

    await findBtn(wrapper, "Add a new set of rules").trigger("click");
    expect(wrapper.text()).toContain("Rule Set 2");

    await findBtn(wrapper, "Reset").trigger("click");

    expect(wrapper.text()).not.toContain("Rule Set 2");
    expect(wrapper.text()).toContain("Rule Set 1");
  });

  it("toggles matchType between INCLUDE and EXCLUDE", async () => {
    let msg;
    ({ wrapper, msg } = await mountRecipients(
      {},
      { ruleSets: [{ id: "rs1", messageId: "message-1", operator: "NONE", rules: [emptyRule()] }] }
    ));

    expect(wrapper.text()).toContain("Matching recipients");

    const matchToggle = wrapper
      .findAllComponents({ name: "RecipientToggle" })
      .find(t => t.props("options")[0] === "INCLUDE");

    await matchToggle.vm.$emit("update", "EXCLUDE");

    expect(msg.configuration.matchType).toBe("EXCLUDE");
  });

  it("applies validation-error styling when validatedErrors flags a rule set error", async () => {
    ({ wrapper } = await mountRecipients(
      {
        validatedErrors: {
          hasErrors: true,
          ruleSets: [
            { message: "Rule set must have at least one rule.", hasRulesError: false, rules: [] }
          ]
        }
      },
      { ruleSets: [{ id: "rs1", messageId: "message-1", operator: "NONE", rules: [] }] }
    ));

    expect(wrapper.findComponent({ name: "VExpansionPanels" }).classes()).toContain(
      "validation-error"
    );
    expect(wrapper.find(".v-card").classes()).toContain("validation-error");
  });

  it("hides all mutating controls and disables inputs when readOnly is true", async () => {
    ({ wrapper } = await mountRecipients(
      { readOnly: true },
      { ruleSets: [{ id: "rs1", messageId: "message-1", operator: "NONE", rules: [emptyRule()] }] }
    ));

    expect(findBtn(wrapper, "Add a new set of rules")).toBeFalsy();
    expect(findBtn(wrapper, "Add a new rule")).toBeFalsy();
    expect(findBtn(wrapper, "Clear")).toBeFalsy();
    expect(findBtn(wrapper, "Delete Rule Set")).toBeFalsy();
    expect(findBtn(wrapper, "Reset")).toBeFalsy();
    expect(wrapper.findComponent(".rule-variable").props("disabled")).toBe(true);
  });
});
