import { describe, expect, it, vi, beforeEach } from "vitest";

const push = vi.fn();

vi.mock("vue-router", () => ({
  useRouter: () => ({
    push
  })
}));

import { mountComponent } from "@/test-utils/mount";
import ParticipationTypeConsentTitle from "./ParticipationTypeConsentTitle.vue";
import { consent as consentModule } from "@/store/consent.module";
import { navigation as navigationModule } from "@/store/navigation.module";
import { configuration as configurationModule } from "@/store/configuration.module";

describe("ParticipationTypeConsentTitle", () => {
  beforeEach(() => {
    push.mockClear();
  });

  it("renders the intro copy with the default LMS title", () => {
    const wrapper = mountComponent(ParticipationTypeConsentTitle, {
      props: { experiment: { experimentId: 1 } }
    });

    expect(wrapper.text()).toContain("in LMS and will be the way");
  });

  it("uses the configured lmsTitle when available", () => {
    const wrapper = mountComponent(ParticipationTypeConsentTitle, {
      props: { experiment: { experimentId: 1 } }
    });

    const configurationStore = configurationModule();
    configurationStore.configurations = { lmsTitle: "Canvas" };

    return wrapper.vm.$nextTick().then(() => {
      expect(wrapper.text()).toContain("in Canvas and will be the way");
    });
  });

  it("prefills the title from the experiment's consent when present", () => {
    const wrapper = mountComponent(ParticipationTypeConsentTitle, {
      props: {
        experiment: {
          experimentId: 1,
          consent: { title: "Existing title" }
        }
      }
    });

    const field = wrapper.findComponent({ name: "VTextField" });
    expect(field.props("modelValue")).toBe("Existing title");
  });

  it("defaults the Next button to disabled with an empty title", () => {
    const wrapper = mountComponent(ParticipationTypeConsentTitle, {
      props: { experiment: { experimentId: 1 } }
    });

    const button = wrapper.findComponent({ name: "VBtn" });
    expect(button.props("disabled")).toBe(true);
  });

  it("typing a title enables Next and updates the consent store", async () => {
    const wrapper = mountComponent(ParticipationTypeConsentTitle, {
      props: { experiment: { experimentId: 1 } }
    });

    const field = wrapper.findComponent({ name: "VTextField" });
    await field.setValue("My Study Consent");

    const consentStore = consentModule();
    expect(consentStore.title).toBe("My Study Consent");

    const button = wrapper.findComponent({ name: "VBtn" });
    expect(button.props("disabled")).toBe(false);
  });

  it("disables Next when the title is only whitespace", async () => {
    const wrapper = mountComponent(ParticipationTypeConsentTitle, {
      props: { experiment: { experimentId: 1 } }
    });

    const field = wrapper.findComponent({ name: "VTextField" });
    await field.setValue("   ");

    const button = wrapper.findComponent({ name: "VBtn" });
    expect(button.props("disabled")).toBe(true);
  });

  it("disables Next when the title is over 255 characters", async () => {
    const wrapper = mountComponent(ParticipationTypeConsentTitle, {
      props: { experiment: { experimentId: 1 } }
    });

    const field = wrapper.findComponent({ name: "VTextField" });
    await field.setValue("a".repeat(256));

    const button = wrapper.findComponent({ name: "VBtn" });
    expect(button.props("disabled")).toBe(true);
  });

  it("submitting the form navigates to ParticipationTypeConsentFile", async () => {
    const wrapper = mountComponent(ParticipationTypeConsentTitle, {
      props: { experiment: { experimentId: 9 } }
    });

    const field = wrapper.findComponent({ name: "VTextField" });
    await field.setValue("My Study Consent");

    await wrapper.find("form").trigger("submit");

    expect(push).toHaveBeenCalledWith({
      name: "ParticipationTypeConsentFile",
      params: { experiment: 9 }
    });
  });

  it("saveExit navigates to Home by default", () => {
    const wrapper = mountComponent(ParticipationTypeConsentTitle, {
      props: { experiment: { experimentId: 9 } }
    });

    wrapper.vm.saveExit();

    expect(push).toHaveBeenCalledWith({
      name: "Home",
      params: { experiment: 9 }
    });
  });

  it("saveExit navigates to the edit mode's caller page when set", () => {
    const wrapper = mountComponent(ParticipationTypeConsentTitle, {
      props: { experiment: { experimentId: 9 } }
    });

    const navigationStore = navigationModule();
    navigationStore.editMode = { callerPage: { name: "ExperimentSummary" } };

    wrapper.vm.saveExit();

    expect(push).toHaveBeenCalledWith({
      name: "ExperimentSummary",
      params: { experiment: 9 }
    });
  });
});
