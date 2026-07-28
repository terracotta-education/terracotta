import { describe, expect, it, vi } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import ResultsDashboard from "./ResultsDashboard.vue";

vi.mock("./overview/Overview.vue", () => ({
  default: {
    name: "ResultsOverview",
    template: "<div class=\"overview-stub\" />"
  }
}));

vi.mock("./outcomes/Outcomes.vue", () => ({
  default: {
    name: "OutcomesDashboard",
    template: "<div class=\"outcomes-stub\" />"
  }
}));

describe("ResultsDashboard", () => {
  it("renders an Overview tab and an Outcomes tab", () => {
    const wrapper = mountComponent(ResultsDashboard);

    const tabs = wrapper.findAllComponents({ name: "VTab" });
    expect(tabs.map(tab => tab.text())).toEqual(["Overview", "Outcomes"]);
  });

  it("shows the Overview panel by default", () => {
    const wrapper = mountComponent(ResultsDashboard);

    expect(wrapper.findComponent({ name: "ResultsOverview" }).exists()).toBe(true);
  });

  it("switches to the Outcomes panel when the Outcomes tab is clicked", async () => {
    const wrapper = mountComponent(ResultsDashboard);

    const tabs = wrapper.findAllComponents({ name: "VTab" });
    await tabs[1].trigger("click");
    await wrapper.vm.$nextTick();

    expect(wrapper.findComponent({ name: "OutcomesDashboard" }).exists()).toBe(true);
  });
});
