import { describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import ExposureTabs from "./ExposureTabs.vue";

const exposures = [
  { exposureId: 1 },
  { exposureId: 2 },
  { exposureId: 3 }
];

describe("ExposureTabs", () => {
  it("renders one tab per exposure with its set number and component count", () => {
    const wrapper = mountComponent(ExposureTabs, {
      props: {
        modelValue: 0,
        exposures,
        rows: [
          [{}, {}],
          [{}],
          []
        ],
        balanced: true,
        singleConditionExperiment: false
      }
    });

    const tabs = wrapper.findAllComponents({ name: "VTab" });

    expect(tabs).toHaveLength(3);
    expect(tabs[0].text()).toContain("Set 1");
    expect(tabs[0].text()).toContain("2 Components");
    expect(tabs[1].text()).toContain("Set 2");
    expect(tabs[1].text()).toContain("1 Component");
    expect(tabs[2].text()).toContain("Set 3");
    expect(tabs[2].text()).toContain("0 Components");
  });

  it("applies the balanced class when balanced is true", () => {
    const wrapper = mountComponent(ExposureTabs, {
      props: {
        modelValue: 0,
        exposures: [{ exposureId: 1 }],
        rows: [[]],
        balanced: true
      }
    });

    expect(wrapper.find(".section-tab-components-balanced").exists()).toBe(true);
    expect(wrapper.find(".section-tab-components-unbalanced").exists()).toBe(false);
  });

  it("applies the unbalanced class when balanced is false", () => {
    const wrapper = mountComponent(ExposureTabs, {
      props: {
        modelValue: 0,
        exposures: [{ exposureId: 1 }],
        rows: [[]],
        balanced: false
      }
    });

    expect(wrapper.find(".section-tab-components-unbalanced").exists()).toBe(true);
    expect(wrapper.find(".section-tab-components-balanced").exists()).toBe(false);
  });

  it("does not render tabs for a single condition experiment", () => {
    const wrapper = mountComponent(ExposureTabs, {
      props: {
        modelValue: 0,
        exposures,
        rows: [[], [], []],
        singleConditionExperiment: true
      }
    });

    expect(wrapper.findComponent({ name: "VTabs" }).exists()).toBe(false);
    expect(wrapper.findAllComponents({ name: "VDivider" }).length).toBeGreaterThan(0);
  });

  it("emits update:modelValue when a tab is selected", async () => {
    const wrapper = mountComponent(ExposureTabs, {
      props: {
        modelValue: 0,
        exposures,
        rows: [[], [], []]
      }
    });

    const tabs = wrapper.findAllComponents({ name: "VTab" });

    await tabs[2].trigger("click");

    expect(wrapper.emitted("update:modelValue")).toBeTruthy();
    expect(wrapper.emitted("update:modelValue")[0]).toEqual([2]);
  });
});
