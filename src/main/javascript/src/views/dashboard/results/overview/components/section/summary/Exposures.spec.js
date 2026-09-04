import { describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import Exposures from "./Exposures.vue";

describe("OverviewExposuresSummary", () => {
  it("shows 'Between-Subject' and 'one' condition messaging for BETWEEN", () => {
    const wrapper = mountComponent(Exposures, {
      props: { exposureType: "BETWEEN" }
    });

    const summaryData = wrapper.findComponent({ name: "SummaryCount" });

    expect(summaryData.props("value")).toBe("Between-Subject");
    expect(wrapper.text()).toContain("Exposed to one condition");
  });

  it("shows 'Within-Subject' and 'every' condition messaging for WITHIN", () => {
    const wrapper = mountComponent(Exposures, {
      props: { exposureType: "WITHIN" }
    });

    const summaryData = wrapper.findComponent({ name: "SummaryCount" });

    expect(summaryData.props("value")).toBe("Within-Subject");
    expect(wrapper.text()).toContain("Exposed to every condition");
  });

  it("falls back to N/A messaging for an unknown/missing exposureType", () => {
    const wrapper = mountComponent(Exposures, { props: {} });

    const summaryData = wrapper.findComponent({ name: "SummaryCount" });

    expect(summaryData.props("value")).toBe("N/A");
    expect(wrapper.text()).toContain("Exposed to N/A condition");
  });

  it("uses the 'Experiment type' title", () => {
    const wrapper = mountComponent(Exposures, {
      props: { exposureType: "BETWEEN" }
    });

    const summaryData = wrapper.findComponent({ name: "SummaryCount" });

    expect(summaryData.props("title")).toBe("Experiment type");
  });
});
