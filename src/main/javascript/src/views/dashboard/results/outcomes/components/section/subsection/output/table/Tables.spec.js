import { describe, expect, it } from "vitest";
import { createPinia, setActivePinia } from "pinia";

import { mountComponent } from "@/test-utils/mount";
import { exposures as useExposuresStore } from "@/store/exposures.module";
import Tables from "./Tables.vue";

const stubs = {
  Conditions: { name: "Conditions", template: "<div class=\"conditions-stub\" />" },
  Exposures: { name: "Exposures", template: "<div class=\"exposures-stub\" />" }
};

const setupStore = exposures => {
  const pinia = createPinia();
  setActivePinia(pinia);

  useExposuresStore().exposures = exposures;

  return pinia;
};

describe("Tables (OutcomeTables)", () => {
  it("hides the toggle buttons and only shows the conditions table when there is a single exposure", () => {
    const pinia = setupStore([{ exposureId: "e1", title: "Exposure 1" }]);

    const wrapper = mountComponent(Tables, { pinia, global: { stubs } });

    expect(wrapper.find(".buttons").exists()).toBe(false);
    expect(wrapper.findComponent({ name: "Conditions" }).exists()).toBe(true);
    expect(wrapper.findComponent({ name: "Exposures" }).exists()).toBe(false);
  });

  it("shows the toggle buttons and defaults to the conditions table when there are multiple exposures", () => {
    const pinia = setupStore([
      { exposureId: "e1", title: "Exposure 1" },
      { exposureId: "e2", title: "Exposure 2" }
    ]);

    const wrapper = mountComponent(Tables, { pinia, global: { stubs } });
    const buttons = wrapper.findAll(".buttons button");

    expect(buttons).toHaveLength(2);
    expect(wrapper.findComponent({ name: "Conditions" }).exists()).toBe(true);
    expect(wrapper.findComponent({ name: "Exposures" }).exists()).toBe(false);
  });

  it("switches to the exposures table and emits type when the exposure toggle is clicked", async () => {
    const pinia = setupStore([
      { exposureId: "e1", title: "Exposure 1" },
      { exposureId: "e2", title: "Exposure 2" }
    ]);

    const wrapper = mountComponent(Tables, { pinia, global: { stubs } });
    const buttons = wrapper.findAll(".buttons button");

    await buttons[1].trigger("click");

    expect(wrapper.findComponent({ name: "Conditions" }).exists()).toBe(false);
    expect(wrapper.findComponent({ name: "Exposures" }).exists()).toBe(true);
    expect(wrapper.emitted("type")).toEqual([["exposure"]]);
  });

  it("switches back to the conditions table and emits type when the condition toggle is clicked again", async () => {
    const pinia = setupStore([
      { exposureId: "e1", title: "Exposure 1" },
      { exposureId: "e2", title: "Exposure 2" }
    ]);

    const wrapper = mountComponent(Tables, { pinia, global: { stubs } });
    const buttons = wrapper.findAll(".buttons button");

    await buttons[1].trigger("click");
    await buttons[0].trigger("click");

    expect(wrapper.findComponent({ name: "Conditions" }).exists()).toBe(true);
    expect(wrapper.emitted("type")).toEqual([["exposure"], ["condition"]]);
  });
});
