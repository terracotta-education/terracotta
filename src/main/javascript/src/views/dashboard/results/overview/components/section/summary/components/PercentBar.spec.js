import { describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import PercentBar from "./PercentBar.vue";

describe("PercentBar", () => {
  it("renders a middle-of-the-range value as-is", () => {
    const wrapper = mountComponent(PercentBar, { props: { value: 42 } });

    expect(wrapper.find(".progress-text").text()).toBe("42%");
    expect(wrapper.findComponent({ name: "VProgressLinear" }).props("modelValue")).toBe(42);
  });

  it("clamps values above 100 down to 100", () => {
    const wrapper = mountComponent(PercentBar, { props: { value: 150 } });

    expect(wrapper.find(".progress-text").text()).toBe("100%");
  });

  it("clamps negative values up to 0", () => {
    const wrapper = mountComponent(PercentBar, { props: { value: -20 } });

    expect(wrapper.find(".progress-text").text()).toBe("0%");
  });

  it("treats null as 0", () => {
    const wrapper = mountComponent(PercentBar, { props: { value: null } });

    expect(wrapper.find(".progress-text").text()).toBe("0%");
  });

  it("treats NaN as 0 instead of rendering 'NaN%'", () => {
    const wrapper = mountComponent(PercentBar, { props: { value: NaN } });

    expect(wrapper.find(".progress-text").text()).toBe("0%");
  });

  it("defaults to 0 when no value prop is provided", () => {
    const wrapper = mountComponent(PercentBar, { props: {} });

    expect(wrapper.find(".progress-text").text()).toBe("0%");
  });

  it("renders exactly 100 at the boundary without clamping error", () => {
    const wrapper = mountComponent(PercentBar, { props: { value: 100 } });

    expect(wrapper.find(".progress-text").text()).toBe("100%");
  });

  it("sets the --percent-label-width css variable to value + 6%", () => {
    const wrapper = mountComponent(PercentBar, { props: { value: 42 } });

    expect(wrapper.find(".container-progress-bar").attributes("style")).toContain(
      "--percent-label-width: 48%"
    );
  });
});
