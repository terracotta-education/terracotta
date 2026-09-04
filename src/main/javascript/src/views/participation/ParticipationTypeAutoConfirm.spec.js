import { describe, expect, it, vi, beforeEach } from "vitest";

const push = vi.fn();

vi.mock("vue-router", () => ({
  useRouter: () => ({
    push
  })
}));

vi.mock("@/services", () => ({
  experimentService: {
    update: vi.fn()
  }
}));

import { mountComponent } from "@/test-utils/mount";
import ParticipationTypeAutoConfirm from "./ParticipationTypeAutoConfirm.vue";
import { experimentService } from "@/services";
import { navigation as navigationModule } from "@/store/navigation.module";

describe("ParticipationTypeAutoConfirm", () => {
  beforeEach(() => {
    push.mockClear();
    experimentService.update.mockReset();
  });

  it("renders the confirmation copy", () => {
    const wrapper = mountComponent(ParticipationTypeAutoConfirm, {
      props: { experiment: { experimentId: 1, conditions: [] } }
    });

    expect(wrapper.text()).toContain(
      "include all students in your experiment"
    );
  });

  it("links the Yes button to ParticipationSummary for a single-condition experiment", () => {
    const wrapper = mountComponent(ParticipationTypeAutoConfirm, {
      props: {
        experiment: {
          experimentId: 1,
          conditions: [{ conditionId: 1 }]
        }
      }
    });

    const buttons = wrapper.findAllComponents({ name: "VBtn" });
    expect(buttons[0].props("to")).toEqual({ name: "ParticipationSummary" });
  });

  it("links the Yes button to ParticipationDistribution for a multi-condition experiment", () => {
    const wrapper = mountComponent(ParticipationTypeAutoConfirm, {
      props: {
        experiment: {
          experimentId: 1,
          conditions: [{ conditionId: 1 }, { conditionId: 2 }]
        }
      }
    });

    const buttons = wrapper.findAllComponents({ name: "VBtn" });
    expect(buttons[0].props("to")).toEqual({
      name: "ParticipationDistribution"
    });
  });

  it("clicking No updates the experiment to CONSENT and navigates on success", async () => {
    experimentService.update.mockResolvedValue({ status: 200 });

    const wrapper = mountComponent(ParticipationTypeAutoConfirm, {
      props: { experiment: { experimentId: 5, conditions: [] } }
    });

    await wrapper.find(".consent-btn").trigger("click");
    await wrapper.vm.$nextTick();
    await new Promise(resolve => setTimeout(resolve));

    expect(experimentService.update).toHaveBeenCalledWith(
      expect.objectContaining({
        experimentId: 5,
        participationType: "CONSENT"
      })
    );
    expect(push).toHaveBeenCalledWith({
      name: "ParticipationTypeConsentOverview",
      params: { experiment: 5 }
    });
  });

  it("clicking No does not navigate when the update fails", async () => {
    experimentService.update.mockResolvedValue({ status: 500 });

    const wrapper = mountComponent(ParticipationTypeAutoConfirm, {
      props: { experiment: { experimentId: 5, conditions: [] } }
    });

    await wrapper.find(".consent-btn").trigger("click");
    await wrapper.vm.$nextTick();
    await new Promise(resolve => setTimeout(resolve));

    expect(push).not.toHaveBeenCalled();
  });

  it("saveExit pushes to Home by default", () => {
    const wrapper = mountComponent(ParticipationTypeAutoConfirm, {
      props: { experiment: { experimentId: 5, conditions: [] } }
    });

    wrapper.vm.saveExit();

    expect(push).toHaveBeenCalledWith({
      name: "Home",
      params: { experiment: 5 }
    });
  });

  it("saveExit pushes to the edit mode's caller page when set", () => {
    const wrapper = mountComponent(ParticipationTypeAutoConfirm, {
      props: { experiment: { experimentId: 5, conditions: [] } }
    });

    const navigationStore = navigationModule();
    navigationStore.editMode = { callerPage: { name: "ExperimentSummary" } };

    wrapper.vm.saveExit();

    expect(push).toHaveBeenCalledWith({
      name: "ExperimentSummary",
      params: { experiment: 5 }
    });
  });
});
