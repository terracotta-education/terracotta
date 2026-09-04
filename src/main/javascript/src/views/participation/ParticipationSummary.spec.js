import { describe, expect, it, vi, beforeEach } from "vitest";

const push = vi.fn();

vi.mock("vue-router", () => ({
  useRouter: () => ({
    push
  }),
  onBeforeRouteUpdate: vi.fn()
}));

vi.mock("@/services", () => ({
  consentService: {
    getConsentFile: vi.fn()
  }
}));

import { createPinia, setActivePinia } from "pinia";
import { mountComponent } from "@/test-utils/mount";
import ParticipationSummary from "./ParticipationSummary.vue";
import { consentService } from "@/services";
import { navigation as navigationModule } from "@/store/navigation.module";

const flush = () => new Promise(resolve => setTimeout(resolve));

describe("ParticipationSummary", () => {
  beforeEach(() => {
    push.mockClear();
    consentService.getConsentFile.mockReset();
  });

  const mountView = (experimentOverrides = {}, configureStores) => {
    const pinia = createPinia();
    setActivePinia(pinia);

    if (configureStores) {
      configureStores();
    }

    return mountComponent(ParticipationSummary, {
      pinia,
      props: {
        experiment: {
          experimentId: 1,
          participationType: "CONSENT",
          consent: { title: "My consent title" },
          ...experimentOverrides
        }
      },
      global: {
        stubs: {
          VuePdfEmbed: true
        }
      }
    });
  };

  it("renders the completion heading", () => {
    const wrapper = mountView();

    expect(wrapper.text()).toContain("You've completed section 2.");
  });

  it.each([
    ["CONSENT", "Invited students to consent"],
    ["MANUAL", "Manually determined students"],
    ["AUTO", "Automatically included all students"]
  ])(
    "describes participationType %s as %s",
    async (participationType, label) => {
      const wrapper = mountView({ participationType });

      const selectionMethodTrigger = wrapper
        .findAll(".v-expansion-panel-title")
        .find(el => el.text().includes("Selection Method"));
      await selectionMethodTrigger.trigger("click");
      await flush();

      expect(wrapper.text()).toContain(label);
    }
  );

  it("shows the consent-specific panels only for CONSENT participation", () => {
    const consentWrapper = mountView({ participationType: "CONSENT" });
    expect(consentWrapper.text()).toContain("Component Title");
    expect(consentWrapper.text()).toContain("Informed Consent");

    const manualWrapper = mountView({ participationType: "MANUAL" });
    expect(manualWrapper.text()).not.toContain("Component Title");
    expect(manualWrapper.text()).not.toContain("Informed Consent");
  });

  it("renders the consent assignment title inside the Component Title panel", async () => {
    const wrapper = mountView({
      participationType: "CONSENT",
      consent: { title: "Research Consent Form" }
    });

    const titlePanelTrigger = wrapper
      .findAll(".v-expansion-panel-title")
      .find(el => el.text().includes("Component Title"));
    await titlePanelTrigger.trigger("click");
    await flush();

    expect(wrapper.text()).toContain("Research Consent Form");
  });

  it("downloads and displays the consent PDF when 'View consent file' is clicked", async () => {
    consentService.getConsentFile.mockResolvedValue({
      status: 200,
      base: "base64pdfdata"
    });

    const wrapper = mountView({ participationType: "CONSENT" });

    const informedConsentTrigger = wrapper
      .findAll(".v-expansion-panel-title")
      .find(el => el.text().includes("Informed Consent"));
    await informedConsentTrigger.trigger("click");
    await flush();

    const viewButton = wrapper
      .findAll("button.pdfButton")
      .find(button => button.text().includes("View consent file"));
    await viewButton.trigger("click");
    await flush();
    await wrapper.vm.$nextTick();

    expect(consentService.getConsentFile).toHaveBeenCalledWith(1);
    expect(wrapper.findComponent({ name: "VuePdfEmbed" }).exists()).toBe(true);

    const closeButton = wrapper
      .findAll("button.pdfButton")
      .find(button => button.text().includes("Close preview"));
    expect(closeButton).toBeTruthy();
  });

  it("hides the PDF preview again after 'Close preview' is clicked", async () => {
    consentService.getConsentFile.mockResolvedValue({
      status: 200,
      base: "base64pdfdata"
    });

    const wrapper = mountView({ participationType: "CONSENT" });

    const informedConsentTrigger = wrapper
      .findAll(".v-expansion-panel-title")
      .find(el => el.text().includes("Informed Consent"));
    await informedConsentTrigger.trigger("click");
    await flush();

    let viewButton = wrapper
      .findAll("button.pdfButton")
      .find(button => button.text().includes("View consent file"));
    await viewButton.trigger("click");
    await flush();
    await wrapper.vm.$nextTick();

    const closeButton = wrapper
      .findAll("button.pdfButton")
      .find(button => button.text().includes("Close preview"));
    await closeButton.trigger("click");
    await wrapper.vm.$nextTick();

    expect(wrapper.findComponent({ name: "VuePdfEmbed" }).exists()).toBe(
      false
    );
    viewButton = wrapper
      .findAll("button.pdfButton")
      .find(button => button.text().includes("View consent file"));
    expect(viewButton).toBeTruthy();
  });

  it("shows the Continue button and navigates to nextSection when not in edit mode", async () => {
    const wrapper = mountView({ participationType: "MANUAL" });

    const button = wrapper
      .findAllComponents({ name: "VBtn" })
      .find(b => b.text().includes("Continue to components"));
    expect(button).toBeTruthy();

    await button.trigger("click");

    expect(push).toHaveBeenCalledWith({
      name: "ExperimentSummary",
      params: { experiment: 1 }
    });
  });

  it("hides the Continue button when in edit mode", () => {
    const wrapper = mountView({ participationType: "MANUAL" }, () => {
      navigationModule().editMode = {
        callerPage: { name: "ExperimentSummary" }
      };
    });

    const button = wrapper
      .findAllComponents({ name: "VBtn" })
      .find(b => b.text().includes("Continue to components"));
    expect(button).toBeFalsy();
  });

  it("saveExit navigates to the edit mode's caller page when set", () => {
    const wrapper = mountView({ participationType: "MANUAL" }, () => {
      navigationModule().editMode = {
        callerPage: { name: "ExperimentSummary" }
      };
    });

    wrapper.vm.saveExit();

    expect(push).toHaveBeenCalledWith({
      name: "ExperimentSummary",
      params: { experiment: 1 }
    });
  });
});
