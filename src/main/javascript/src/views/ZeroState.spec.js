import { describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import ZeroState from "./ZeroState.vue";

const baseProps = {
  experimentExportEnabled: false,
  experimentImportRequests: {},
  importRequestAlerts: []
};

describe("ZeroState", () => {
  it("renders the create-experiment call to action", () => {
    const wrapper = mountComponent(ZeroState, { props: baseProps });

    expect(wrapper.text()).toContain("Experimental research in the LMS");
    expect(wrapper.find(".experiment-btn").exists()).toBe(true);
  });

  it("hides the import-experiment button when experimentExportEnabled is false", () => {
    const wrapper = mountComponent(ZeroState, { props: baseProps });

    expect(wrapper.text()).not.toContain("OR IMPORT AN EXPERIMENT");
  });

  it("shows the import-experiment button when experimentExportEnabled is true", () => {
    const wrapper = mountComponent(ZeroState, {
      props: { ...baseProps, experimentExportEnabled: true }
    });

    expect(wrapper.text()).toContain("OR IMPORT AN EXPERIMENT");
  });

  it("emits startExperiment when the create button is clicked", async () => {
    const wrapper = mountComponent(ZeroState, { props: baseProps });

    await wrapper.find(".experiment-btn").trigger("click");

    expect(wrapper.emitted("startExperiment")).toBeTruthy();
  });

  it("emits handleImportExperiment when the import button is clicked", async () => {
    const wrapper = mountComponent(ZeroState, {
      props: { ...baseProps, experimentExportEnabled: true }
    });

    const buttons = wrapper.findAll(".experiment-btn");
    await buttons[1].trigger("click");

    expect(wrapper.emitted("handleImportExperiment")).toBeTruthy();
  });

  it("renders no import request alerts when the list is empty", () => {
    const wrapper = mountComponent(ZeroState, { props: baseProps });

    expect(wrapper.find(".alert-request").exists()).toBe(false);
  });

  it("renders an import request alert and its errors", () => {
    const wrapper = mountComponent(ZeroState, {
      props: {
        ...baseProps,
        experimentImportRequests: {
          "5": { showAlert: true }
        },
        importRequestAlerts: [
          {
            id: "5",
            type: "error",
            text: "Something went wrong.",
            showErrors: true,
            errors: ["Bad row 1", "Bad row 2"]
          }
        ]
      }
    });

    expect(wrapper.find(".alert-request").exists()).toBe(true);
    expect(wrapper.text()).toContain("Something went wrong.");
    expect(wrapper.text()).toContain("Bad row 1");
    expect(wrapper.text()).toContain("Bad row 2");
  });

  it("emits handleImportRequestAlertDismiss when an alert is closed", async () => {
    const wrapper = mountComponent(ZeroState, {
      props: {
        ...baseProps,
        experimentImportRequests: {
          "5": { showAlert: true }
        },
        importRequestAlerts: [
          {
            id: "5",
            type: "info",
            text: "Processing",
            showErrors: false
          }
        ]
      }
    });

    const alertComponent = wrapper.findComponent({ name: "VAlert" });
    alertComponent.vm.$emit("click:close");

    expect(wrapper.emitted("handleImportRequestAlertDismiss")).toBeTruthy();
    expect(wrapper.emitted("handleImportRequestAlertDismiss")[0]).toEqual(["5"]);
  });

  it("emits handleImportRequestAlertVisibilityChange when the alert's model value changes", async () => {
    const wrapper = mountComponent(ZeroState, {
      props: {
        ...baseProps,
        experimentImportRequests: {
          "5": { showAlert: true }
        },
        importRequestAlerts: [
          {
            id: "5",
            type: "info",
            text: "Processing",
            showErrors: false
          }
        ]
      }
    });

    const alertComponent = wrapper.findComponent({ name: "VAlert" });
    alertComponent.vm.$emit("update:model-value", false);

    expect(
      wrapper.emitted("handleImportRequestAlertVisibilityChange")
    ).toBeTruthy();
    expect(
      wrapper.emitted("handleImportRequestAlertVisibilityChange")[0]
    ).toEqual(["5", false]);
  });
});
