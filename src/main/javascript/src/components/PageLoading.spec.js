import { describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import PageLoading from "./PageLoading.vue";

describe("PageLoading", () => {
  it("is hidden by default and shows the default message", () => {
    const wrapper = mountComponent(PageLoading);

    const container = wrapper.find(".page-loading-container");

    expect(container.exists()).toBe(true);
    expect(container.attributes("style")).toContain("display: none");
    expect(wrapper.text()).toContain("Loading. Please wait.");
  });

  it("is visible when display is true and renders a custom message", () => {
    const wrapper = mountComponent(PageLoading, {
      props: {
        display: true,
        message: "Hang tight..."
      }
    });

    const container = wrapper.find(".page-loading-container");

    expect(container.attributes("style") || "").not.toContain("display: none");
    expect(wrapper.text()).toContain("Hang tight...");
  });

  it("renders the Spinner child component", () => {
    const wrapper = mountComponent(PageLoading, {
      props: { display: true }
    });

    expect(wrapper.findComponent({ name: "LoadingSpinner" }).exists()).toBe(true);
  });

  it("applies custom container and spinner inline styles", () => {
    const wrapper = mountComponent(PageLoading, {
      props: {
        display: true,
        containerStyles: "z-index: 1234;",
        spinnerStyles: "top: 10px;"
      }
    });

    expect(wrapper.find(".page-loading-container").attributes("style")).toContain(
      "z-index: 1234"
    );
    expect(wrapper.find(".spinner-container").attributes("style")).toContain(
      "top: 10px"
    );
  });
});
