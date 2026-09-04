import { describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import ConditionDeleteAlert from "./ConditionDeleteAlert.vue";

describe("ConditionDeleteAlert", () => {
  it("renders the single-remaining-condition warning with a tooltip when singleConditionRemainsAfterDelete is true", () => {
    const wrapper = mountComponent(ConditionDeleteAlert, {
      props: {
        singleConditionRemainsAfterDelete: true,
        conditionName: "Treatment A"
      }
    });

    expect(wrapper.text()).toContain(
      'Are you sure you want to delete "Treatment A"?'
    );
    expect(wrapper.text()).toContain("only one condition");
    expect(wrapper.findComponent({ name: "ToolTip" }).exists()).toBe(true);
  });

  it("renders the simple confirmation message when singleConditionRemainsAfterDelete is false", () => {
    const wrapper = mountComponent(ConditionDeleteAlert, {
      props: {
        singleConditionRemainsAfterDelete: false,
        conditionName: "Treatment A"
      }
    });

    expect(wrapper.text()).toContain(
      'Do you really want to delete "Treatment A"?'
    );
    expect(wrapper.findComponent({ name: "ToolTip" }).exists()).toBe(false);
  });

  it("falls back to 'this condition' when no conditionName is provided", () => {
    const wrapper = mountComponent(ConditionDeleteAlert, {
      props: {
        singleConditionRemainsAfterDelete: false
      }
    });

    expect(wrapper.text()).toContain(
      "Do you really want to delete this condition?"
    );
  });
});
