import { describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import Conditions from "./Conditions.vue";

describe("OverviewConditionsSummary", () => {
  it("passes the condition count through to SummaryData with the 'Conditions' title", () => {
    const wrapper = mountComponent(Conditions, {
      props: { conditionCount: 3 }
    });

    const summaryData = wrapper.findComponent({ name: "SummaryCount" });

    expect(summaryData.exists()).toBe(true);
    expect(summaryData.props("title")).toBe("Conditions");
    expect(summaryData.props("value")).toBe(3);
  });

  it("defaults the count to 0 when conditionCount is not provided", () => {
    const wrapper = mountComponent(Conditions, { props: {} });

    const summaryData = wrapper.findComponent({ name: "SummaryCount" });

    expect(summaryData.props("value")).toBe(0);
  });
});
