import { describe, expect, it, vi, beforeEach } from "vitest";

const push = vi.fn();

vi.mock("vue-router", () => ({
  useRouter: () => ({
    push
  })
}));

import { mountComponent } from "@/test-utils/mount";
import ParticipationTypeConsentOverview from "./ParticipationTypeConsentOverview.vue";
import { navigation as navigationModule } from "@/store/navigation.module";
import { configuration as configurationModule } from "@/store/configuration.module";

describe("ParticipationTypeConsentOverview", () => {
  beforeEach(() => {
    push.mockClear();
  });

  it("defaults the LMS title to 'LMS' when no configuration is loaded", () => {
    const wrapper = mountComponent(ParticipationTypeConsentOverview, {
      props: { experiment: { experimentId: 1 } }
    });

    expect(wrapper.text()).toContain("within LMS");
  });

  it("uses the configured lmsTitle when available", () => {
    const wrapper = mountComponent(ParticipationTypeConsentOverview, {
      props: { experiment: { experimentId: 1 } }
    });

    const configurationStore = configurationModule();
    configurationStore.configurations = { lmsTitle: "Canvas" };

    return wrapper.vm.$nextTick().then(() => {
      expect(wrapper.text()).toContain("within Canvas");
    });
  });

  it("clicking Continue navigates to ParticipationTypeConsentTitle with the experimentId", async () => {
    const wrapper = mountComponent(ParticipationTypeConsentOverview, {
      props: { experiment: { experimentId: 7 } }
    });

    await wrapper.findComponent({ name: "VBtn" }).trigger("click");

    expect(push).toHaveBeenCalledWith({
      name: "ParticipationTypeConsentTitle",
      params: { experiment: 7 }
    });
  });

  it("saveExit pushes to Home by default", () => {
    const wrapper = mountComponent(ParticipationTypeConsentOverview, {
      props: { experiment: { experimentId: 7 } }
    });

    wrapper.vm.saveExit();

    expect(push).toHaveBeenCalledWith({
      name: "Home",
      params: { experiment: 7 }
    });
  });

  it("saveExit pushes to the edit mode's caller page when set", () => {
    const wrapper = mountComponent(ParticipationTypeConsentOverview, {
      props: { experiment: { experimentId: 7 } }
    });

    const navigationStore = navigationModule();
    navigationStore.editMode = { callerPage: { name: "ExperimentSummary" } };

    wrapper.vm.saveExit();

    expect(push).toHaveBeenCalledWith({
      name: "ExperimentSummary",
      params: { experiment: 7 }
    });
  });
});
