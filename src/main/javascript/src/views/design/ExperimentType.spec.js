import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

const pushMock = vi.fn();

vi.mock("vue-router", () => ({
  useRouter: () => ({ push: pushMock })
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
import { experimentService } from "@/services";
import { experiment as experimentModule } from "@/store/experiment.module";
import { navigation as navigationModule } from "@/store/navigation.module";
import { api as apiModule } from "@/store/api.module";
import { mountComponent } from "@/test-utils/mount";
import ExperimentType from "./ExperimentType.vue";

describe("ExperimentType", () => {
  let pinia;
  let experimentStore;
  let navigationStore;
  let apiStore;

  beforeEach(() => {
    pinia = createPinia();
    setActivePinia(pinia);

    experimentStore = experimentModule();
    navigationStore = navigationModule();
    apiStore = apiModule();

    experimentStore.setExperiment({
      experimentId: 1,
      exposureType: "NOSET",
      conditions: [{ conditionId: 10 }, { conditionId: 11 }]
    });

    vi.clearAllMocks();
  });

  function mount() {
    return mountComponent(ExperimentType, { pinia });
  }

  it("shows the condition count and both exposure-type panels when conditions exist", () => {
    const wrapper = mount();

    expect(wrapper.text()).toContain("2 conditions");
    expect(wrapper.text()).toContain("All conditions");
    expect(wrapper.text()).toContain("Only one condition");
  });

  it("shows a no-conditions alert when there are none", () => {
    experimentStore.setExperiment({
      experimentId: 1,
      exposureType: "NOSET",
      conditions: []
    });

    const wrapper = mount();

    expect(wrapper.text()).toContain("No conditions found");
  });

  it("selecting a type saves the experiment with the chosen exposureType", async () => {
    experimentService.update.mockResolvedValue({ status: 200 });

    const wrapper = mount();

    const selectButtons = wrapper
      .findAll("button")
      .filter(btn => btn.text() === "Select");

    await selectButtons[0].trigger("click");
    await flushPromises();

    expect(experimentService.update).toHaveBeenCalledWith(
      expect.objectContaining({ exposureType: "WITHIN" })
    );
  });

  it("reports the step via apiStore.reportStep when not in edit mode", async () => {
    experimentService.update.mockResolvedValue({ status: 200 });
    const reportStepSpy = vi.spyOn(apiStore, "reportStep");

    const wrapper = mount();

    const selectButtons = wrapper
      .findAll("button")
      .filter(btn => btn.text() === "Select");

    await selectButtons[0].trigger("click");
    await flushPromises();

    expect(reportStepSpy).toHaveBeenCalledWith({
      experimentId: 1,
      step: "exposure_type"
    });
    expect(pushMock).toHaveBeenCalledWith({
      name: "ExperimentDesignDefaultCondition",
      params: { experiment: 1 }
    });
  });

  it("does not report the step when in edit mode", async () => {
    experimentService.update.mockResolvedValue({ status: 200 });
    navigationStore.saveEditMode({ callerPage: { name: "ExperimentSummary" } });
    const reportStepSpy = vi.spyOn(apiStore, "reportStep");

    const wrapper = mount();

    const selectButtons = wrapper
      .findAll("button")
      .filter(btn => btn.text() === "Select");

    await selectButtons[0].trigger("click");
    await flushPromises();

    expect(reportStepSpy).not.toHaveBeenCalled();
  });

  it("shows a Swal error when saving fails", async () => {
    experimentService.update.mockResolvedValue({ message: "boom" });

    const wrapper = mount();

    const selectButtons = wrapper
      .findAll("button")
      .filter(btn => btn.text() === "Select");

    await selectButtons[0].trigger("click");
    await flushPromises();

    expect(Swal.fire).toHaveBeenCalledWith("Error: boom");
  });

  it("saveExit navigates to the caller page / Home with the experimentId", () => {
    const wrapper = mount();

    wrapper.vm.saveExit();

    expect(pushMock).toHaveBeenCalledWith({
      name: "Home",
      params: { experimentId: 1 }
    });
  });
});

function flushPromises() {
  return new Promise(resolve => setTimeout(resolve));
}
