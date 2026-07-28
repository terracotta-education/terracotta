import { describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import { alert } from "@/store/alert.module";
import StatusAlert from "./StatusAlert.vue";

describe("StatusAlert", () => {
  it("renders no visible alert when the store has no alert", () => {
    const wrapper = mountComponent(StatusAlert);

    const alertComponent = wrapper.findComponent({ name: "VAlert" });

    expect(alertComponent.props("modelValue")).toBe(false);
  });

  it("renders the alert type and message from the store", () => {
    const wrapper = mountComponent(StatusAlert);

    const alertStore = alert();
    alertStore.success("Everything worked");

    return wrapper.vm.$nextTick().then(() => {
      const alertComponent = wrapper.findComponent({ name: "VAlert" });

      expect(alertComponent.props("modelValue")).toBe(true);
      expect(alertComponent.props("type")).toBe("success");
      expect(wrapper.text()).toContain("Everything worked");
    });
  });

  it("clears the store alert when the alert is dismissed", async () => {
    const wrapper = mountComponent(StatusAlert);

    const alertStore = alert();
    alertStore.error("Something broke");

    await wrapper.vm.$nextTick();

    const alertComponent = wrapper.findComponent({ name: "VAlert" });
    alertComponent.vm.$emit("update:model-value", false);

    await wrapper.vm.$nextTick();

    expect(alertStore.hasAlert).toBe(false);
    expect(alertStore.message).toBe(null);
  });
});
