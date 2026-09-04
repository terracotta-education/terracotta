import { describe, expect, it, vi } from "vitest";

import { mountComponent } from "@/test-utils/mount";

const route = { fullPath: "/experiments/1/summary" };

vi.mock("vue-router", () => ({
  useRoute: () => route
}));

import Experiment from "./Experiment.vue";

describe("Experiment", () => {
  it("renders a router-view keyed on the current route's fullPath", () => {
    const wrapper = mountComponent(Experiment, {
      global: { stubs: { RouterView: true } }
    });

    const routerView = wrapper.findComponent({ name: "RouterView" });

    expect(routerView.exists()).toBe(true);
  });

  it("keys the router-view on the current route's fullPath", () => {
    const wrapper = mountComponent(Experiment, {
      global: { stubs: { RouterView: true } }
    });

    const routerView = wrapper.findComponent({ name: "RouterView" });

    expect(routerView.vm.$.vnode.key).toBe(route.fullPath);
  });

  it("uses a different key for a different route", () => {
    route.fullPath = "/experiments/1/steps";

    const wrapper = mountComponent(Experiment, {
      global: { stubs: { RouterView: true } }
    });

    const routerView = wrapper.findComponent({ name: "RouterView" });

    expect(routerView.vm.$.vnode.key).toBe("/experiments/1/steps");
  });
});
