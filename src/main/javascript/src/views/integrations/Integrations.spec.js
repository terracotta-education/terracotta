import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import Integrations from "./Integrations.vue";

function buildIntegrationData(overrides = {}) {
  return {
    preview: true,
    client: "Qualtrics",
    launchToken: "token-123",
    score: 8,
    status: "OK",
    url: "https://example.com/survey",
    errorCode: "E001",
    moreAttemptsAvailable: false,
    ...overrides
  };
}

describe("Integrations", () => {
  let dispatchSpy;

  beforeEach(() => {
    dispatchSpy = vi.spyOn(window.parent.document, "dispatchEvent");
  });

  afterEach(() => {
    dispatchSpy.mockRestore();
  });

  it("shows the success card with the launch token and score on a valid preview", () => {
    const wrapper = mountComponent(Integrations, {
      props: { integrationData: buildIntegrationData() }
    });

    expect(wrapper.text()).toContain(
      "Successfully returned to Terracotta following preview"
    );
    expect(wrapper.text()).toContain("token-123");
    expect(wrapper.text()).toContain("8");
  });

  it("shows the invalid-score card when score is missing on a preview", () => {
    const wrapper = mountComponent(Integrations, {
      props: {
        integrationData: buildIntegrationData({ score: null })
      }
    });

    expect(wrapper.text()).toContain(
      "Returned to Terracotta following Qualtrics preview with invalid or missing score"
    );
    expect(wrapper.text()).toContain("https://example.com/survey");
  });

  it("uses Custom Web Activity copy for the invalid-score card when that is the client", () => {
    const wrapper = mountComponent(Integrations, {
      props: {
        integrationData: buildIntegrationData({
          client: "Custom Web Activity",
          score: Number.NaN
        })
      }
    });

    expect(wrapper.text()).toContain(
      "Returned to Terracotta following custom web activity preview with invalid or missing score"
    );
  });

  it("shows the preview error card with the invalid submission token title when status is not OK during preview", () => {
    const wrapper = mountComponent(Integrations, {
      props: {
        integrationData: buildIntegrationData({ status: "ERROR" })
      }
    });

    expect(wrapper.text()).toContain("Invalid submission token");
  });

  it("shows the submission error card with a reattempt button when more attempts are available", () => {
    const wrapper = mountComponent(Integrations, {
      props: {
        integrationData: buildIntegrationData({
          preview: false,
          status: "ERROR",
          moreAttemptsAvailable: true
        })
      }
    });

    expect(wrapper.text()).toContain("Invalid submission attempt");

    const reattemptButton = wrapper
      .findAllComponents({ name: "VBtn" })
      .find(btn => btn.text().includes("Reattempt assignment"));

    expect(reattemptButton).toBeTruthy();
  });

  it("hides the reattempt button when no more attempts are available", () => {
    const wrapper = mountComponent(Integrations, {
      props: {
        integrationData: buildIntegrationData({
          preview: false,
          status: "ERROR",
          moreAttemptsAvailable: false
        })
      }
    });

    const reattemptButton = wrapper
      .findAllComponents({ name: "VBtn" })
      .find(btn => btn.text().includes("Reattempt assignment"));

    expect(reattemptButton).toBeUndefined();
  });

  it("dispatches integrations_reattempt when the reattempt button is clicked", async () => {
    const integrationData = buildIntegrationData({
      preview: false,
      status: "ERROR",
      moreAttemptsAvailable: true
    });

    const wrapper = mountComponent(Integrations, {
      props: { integrationData }
    });

    const reattemptButton = wrapper
      .findAllComponents({ name: "VBtn" })
      .find(btn => btn.text().includes("Reattempt assignment"));

    await reattemptButton.trigger("click");

    expect(dispatchSpy).toHaveBeenCalled();
    const event = dispatchSpy.mock.calls.at(-1)[0];
    expect(event.type).toBe("integrations_reattempt");
    expect(event.detail.integrationData).toEqual(integrationData);
  });

  it("renders nothing but dispatches integrations_score for a successful, non-preview submission", () => {
    const wrapper = mountComponent(Integrations, {
      props: {
        integrationData: buildIntegrationData({
          preview: false,
          status: "OK"
        })
      }
    });

    expect(wrapper.findComponent({ name: "VApp" }).exists()).toBe(false);
    expect(dispatchSpy).toHaveBeenCalled();
    expect(dispatchSpy.mock.calls.at(-1)[0].type).toBe("integrations_score");
  });

  it("does not dispatch integrations_score while previewing", () => {
    mountComponent(Integrations, {
      props: { integrationData: buildIntegrationData({ preview: true }) }
    });

    expect(dispatchSpy).not.toHaveBeenCalled();
  });
});
