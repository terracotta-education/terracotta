import { describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import Output from "./Output.vue";

const stubs = {
  OutcomeGraph: true,
  OutcomeTables: true
};

describe("SectionOutput", () => {
  it("shows the empty-state card and hides the tables when no output is selected", () => {
    const wrapper = mountComponent(Output, {
      props: { showOutputPanel: false },
      global: { stubs }
    });

    expect(wrapper.find(".no-outcomes-selected").exists()).toBe(true);
    expect(wrapper.findComponent({ name: "OutcomeTables" }).exists()).toBe(false);

    const graph = wrapper.findComponent({ name: "OutcomeGraph" });
    expect(graph.props("displayOutput")).toBe(false);
    expect(graph.props("type")).toBe("condition");
  });

  it("hides the empty-state card and shows the tables once output is selected", () => {
    const wrapper = mountComponent(Output, {
      props: { showOutputPanel: true },
      global: { stubs }
    });

    expect(wrapper.find(".no-outcomes-selected").exists()).toBe(false);
    expect(wrapper.findComponent({ name: "OutcomeTables" }).exists()).toBe(true);
    expect(wrapper.findComponent({ name: "OutcomeGraph" }).props("displayOutput")).toBe(true);
  });

  it("passes the type reported by the tables section down to the graph", async () => {
    const wrapper = mountComponent(Output, {
      props: { showOutputPanel: true },
      global: { stubs }
    });

    wrapper.findComponent({ name: "OutcomeTables" }).vm.$emit("type", "exposure");
    await wrapper.vm.$nextTick();

    expect(wrapper.findComponent({ name: "OutcomeGraph" }).props("type")).toBe("exposure");
  });
});
