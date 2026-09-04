import { afterEach, describe, expect, it, vi } from "vitest";

const push = vi.fn();

vi.mock("vue-router", () => ({
  useRouter: () => ({
    push
  })
}));

vi.mock("sweetalert2", () => ({
  default: {
    fire: vi.fn().mockResolvedValue({})
  }
}));

vi.mock("@/services", () => ({
  experimentService: {
    update: vi.fn()
  },
  apiService: {
    reportStep: vi.fn()
  }
}));

import Swal from "sweetalert2";
import { experimentService, apiService } from "@/services";
import { mountComponent } from "@/test-utils/mount";
import ParticipationDistribution from "./ParticipationDistribution.vue";
import { navigation as navigationModule } from "@/store/navigation.module";

const buildExperiment = (overrides = {}) => ({
  experimentId: 1,
  distributionType: null,
  exposureType: "BETWEEN",
  ...overrides
});

describe("ParticipationDistribution", () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it("renders the Even, Custom, and Manual distribution panels", () => {
    const wrapper = mountComponent(ParticipationDistribution, {
      props: { experiment: buildExperiment() }
    });

    expect(wrapper.text()).toContain("Even");
    expect(wrapper.text()).toContain("Custom");
    expect(wrapper.text()).toContain("Manual");
  });

  it("disables Custom and Manual when the experiment exposure type is WITHIN", () => {
    const wrapper = mountComponent(ParticipationDistribution, {
      props: { experiment: buildExperiment({ exposureType: "WITHIN" }) }
    });

    const panels = wrapper.findAllComponents({ name: "VExpansionPanel" });

    expect(panels[0].props("disabled")).toBeFalsy();
    expect(panels[1].props("disabled")).toBe(true);
    expect(panels[2].props("disabled")).toBe(true);
  });

  it("marks the currently selected distribution type's panel as selected", () => {
    const wrapper = mountComponent(ParticipationDistribution, {
      props: { experiment: buildExperiment({ distributionType: "CUSTOM" }) }
    });

    const panels = wrapper.findAllComponents({ name: "VExpansionPanel" });

    expect(panels[1].classes()).toContain("v-expansion-panel--selected");
    expect(panels[0].classes()).not.toContain("v-expansion-panel--selected");
  });

  it("selecting Even saves the experiment, reports the step, and routes to ParticipationSummary", async () => {
    experimentService.update.mockResolvedValue({ status: 200 });
    apiService.reportStep.mockResolvedValue({ status: 200 });

    const wrapper = mountComponent(ParticipationDistribution, {
      props: { experiment: buildExperiment() }
    });

    await expandPanel(wrapper, 0);
    const selectButtons = wrapper.findAllComponents({ name: "VBtn" });
    await selectButtons[0].trigger("click");
    await flushPromisesAndTicks(wrapper);

    expect(experimentService.update).toHaveBeenCalledWith(
      expect.objectContaining({ distributionType: "EVEN" })
    );
    expect(apiService.reportStep).toHaveBeenCalledWith(
      1,
      "distribution_type",
      null,
      false
    );
    expect(push).toHaveBeenCalledWith({
      name: "ParticipationSummary",
      params: { experiment: 1 }
    });
  });

  it("selecting Custom routes to ParticipationCustomDistribution", async () => {
    experimentService.update.mockResolvedValue({ status: 200 });
    apiService.reportStep.mockResolvedValue({ status: 200 });

    const wrapper = mountComponent(ParticipationDistribution, {
      props: { experiment: buildExperiment() }
    });

    await expandPanel(wrapper, 1);
    const selectButtons = wrapper.findAllComponents({ name: "VBtn" });
    await selectButtons[0].trigger("click");
    await flushPromisesAndTicks(wrapper);

    expect(push).toHaveBeenCalledWith({
      name: "ParticipationCustomDistribution",
      params: { experiment: 1 }
    });
  });

  it("selecting Manual routes to ParticipationManualDistribution", async () => {
    experimentService.update.mockResolvedValue({ status: 200 });
    apiService.reportStep.mockResolvedValue({ status: 200 });

    const wrapper = mountComponent(ParticipationDistribution, {
      props: { experiment: buildExperiment() }
    });

    await expandPanel(wrapper, 2);
    const selectButtons = wrapper.findAllComponents({ name: "VBtn" });
    await selectButtons[0].trigger("click");
    await flushPromisesAndTicks(wrapper);

    expect(push).toHaveBeenCalledWith({
      name: "ParticipationManualDistribution",
      params: { experiment: 1 }
    });
  });

  it("does not report the step when saving while in edit mode", async () => {
    experimentService.update.mockResolvedValue({ status: 200 });

    const wrapper = mountComponent(ParticipationDistribution, {
      props: { experiment: buildExperiment() }
    });

    const navigationStore = navigationModule();
    navigationStore.editMode = { callerPage: { name: "ParticipationSummary" } };

    await expandPanel(wrapper, 0);
    const selectButtons = wrapper.findAllComponents({ name: "VBtn" });
    await selectButtons[0].trigger("click");
    await flushPromisesAndTicks(wrapper);

    expect(apiService.reportStep).not.toHaveBeenCalled();
    expect(push).toHaveBeenCalledWith({
      name: "ParticipationSummary",
      params: { experiment: 1 }
    });
  });

  it("shows an error alert and does not navigate when saving fails", async () => {
    experimentService.update.mockResolvedValue({
      status: 400,
      message: "bad request"
    });

    const wrapper = mountComponent(ParticipationDistribution, {
      props: { experiment: buildExperiment() }
    });

    await expandPanel(wrapper, 0);
    const selectButtons = wrapper.findAllComponents({ name: "VBtn" });
    await selectButtons[0].trigger("click");
    await flushPromisesAndTicks(wrapper);

    expect(Swal.fire).toHaveBeenCalledWith("Error: bad request");
    expect(push).not.toHaveBeenCalled();
  });

  it("saveExit navigates to the caller page from edit mode, defaulting to Home", () => {
    const wrapper = mountComponent(ParticipationDistribution, {
      props: { experiment: buildExperiment({ experimentId: 9 }) }
    });

    wrapper.vm.saveExit();

    expect(push).toHaveBeenCalledWith({
      name: "Home",
      params: { experiment: 9 }
    });
  });
});

async function flushPromisesAndTicks(wrapper) {
  await wrapper.vm.$nextTick();
  await new Promise(resolve => setTimeout(resolve));
  await wrapper.vm.$nextTick();
}

async function expandPanel(wrapper, index) {
  const titles = wrapper.findAllComponents({ name: "VExpansionPanelTitle" });
  await titles[index].trigger("click");
  await wrapper.vm.$nextTick();
}
