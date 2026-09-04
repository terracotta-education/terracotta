import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

const pushMock = vi.fn();

vi.mock("vue-router", () => ({
  useRoute: () => ({ params: { experimentId: "1" } }),
  useRouter: () => ({ push: pushMock }),
  onBeforeRouteUpdate: vi.fn()
}));

vi.mock("sweetalert2", () => ({
  default: {
    fire: vi.fn().mockResolvedValue({ isConfirmed: true })
  }
}));

vi.mock("@/services", () => ({
  conditionService: {
    create: vi.fn(),
    updateAll: vi.fn(),
    delete: vi.fn()
  },
  experimentService: {
    update: vi.fn()
  },
  exposuresService: {
    createExposures: vi.fn()
  },
  groupsService: {
    createAndAssignGroups: vi.fn()
  }
}));

import Swal from "sweetalert2";
import { conditionService, experimentService } from "@/services";
import { experiment as experimentModule } from "@/store/experiment.module";
import { navigation as navigationModule } from "@/store/navigation.module";
import { mountComponent } from "@/test-utils/mount";
import Conditions from "./Conditions.vue";

function flushPromises() {
  return new Promise(resolve => setTimeout(resolve));
}

describe("DesignConditions", () => {
  let pinia;
  let experimentStore;

  beforeEach(() => {
    pinia = createPinia();
    setActivePinia(pinia);

    experimentStore = experimentModule();
    navigationModule();

    experimentStore.setExperiment({
      experimentId: 1,
      exposureType: "NOSET",
      distributionType: "NOSET",
      conditions: [
        { experimentId: 1, conditionId: 1, name: "Condition 1", defaultCondition: true },
        { experimentId: 1, conditionId: 2, name: "Condition 2", defaultCondition: false }
      ]
    });

    vi.clearAllMocks();
    conditionService.updateAll.mockResolvedValue({ status: 200 });
  });

  function mount() {
    return mountComponent(Conditions, { pinia });
  }

  it("renders one name field per existing condition, in conditionId order", () => {
    const wrapper = mount();

    const fields = wrapper.findAllComponents({ name: "VTextField" });
    expect(fields).toHaveLength(2);
    expect(fields[0].props("modelValue")).toBe("Condition 1");
    expect(fields[1].props("modelValue")).toBe("Condition 2");
  });

  it("shows a delete button per condition (after the first) when deletion is allowed", () => {
    const wrapper = mount();

    expect(wrapper.findAll(".delete_condition")).toHaveLength(1);
  });

  it("hides delete buttons once the experiment is beyond the NOSET exposure type", () => {
    experimentStore.setExperiment({
      experimentId: 1,
      exposureType: "BETWEEN",
      distributionType: "EVEN",
      conditions: experimentStore.experiment.conditions
    });

    const wrapper = mount();

    expect(wrapper.findAll(".delete_condition")).toHaveLength(0);
    expect(wrapper.text()).toMatch(/not able to\s+delete conditions/);
  });

  it("adds a new condition when 'Add another condition' is clicked", async () => {
    conditionService.create.mockResolvedValue({
      experimentId: 1,
      conditionId: 3,
      name: ""
    });

    const wrapper = mount();

    await wrapper.find(".add_condition").trigger("click");
    await flushPromises();
    await wrapper.vm.$nextTick();

    expect(conditionService.create).toHaveBeenCalledWith(1);
    expect(wrapper.findAllComponents({ name: "VTextField" })).toHaveLength(3);
  });

  it("flags a validation error and blocks save when two conditions share a name", async () => {
    experimentStore.experiment.conditions[1].name = "Condition 1";

    const wrapper = mount();
    await wrapper.vm.$nextTick();

    await wrapper.find("form").trigger("submit");
    await flushPromises();

    expect(conditionService.updateAll).not.toHaveBeenCalled();
    expect(Swal.fire).toHaveBeenCalledWith(
      expect.stringContaining("Multiple conditions have the same name.")
    );
  });

  it("saves conditions and navigates to the experiment-type step on submit", async () => {
    const wrapper = mount();
    await wrapper.vm.$nextTick();

    await wrapper.find("form").trigger("submit");
    await flushPromises();

    expect(conditionService.updateAll).toHaveBeenCalledWith(
      experimentStore.experiment.conditions
    );
    expect(pushMock).toHaveBeenCalledWith({ name: "ExperimentDesignType" });
  });

  it("navigates straight to the summary step for a single-condition experiment and flips it to BETWEEN/EVEN", async () => {
    experimentStore.setExperiment({
      experimentId: 1,
      exposureType: "NOSET",
      distributionType: "NOSET",
      conditions: [
        { experimentId: 1, conditionId: 1, name: "Only condition", defaultCondition: false }
      ]
    });
    experimentService.update.mockResolvedValue({ status: 200 });

    const wrapper = mount();
    await wrapper.vm.$nextTick();

    await wrapper.find("form").trigger("submit");
    await flushPromises();

    expect(experimentStore.experiment.conditions[0].defaultCondition).toBe(true);
    expect(experimentService.update).toHaveBeenCalledWith(
      expect.objectContaining({ exposureType: "BETWEEN", distributionType: "EVEN" })
    );
    expect(pushMock).toHaveBeenCalledWith({ name: "ExperimentDesignSummary" });
  });

  it("deletes a non-default condition after confirming", async () => {
    conditionService.delete.mockResolvedValue({ status: 200 });

    const wrapper = mount();
    await wrapper.vm.$nextTick();

    await wrapper.find(".delete_condition").trigger("click");
    await flushPromises();

    expect(conditionService.delete).toHaveBeenCalledWith(
      expect.objectContaining({ conditionId: 2 })
    );
  });

  it("refuses to delete the default condition", async () => {
    experimentStore.experiment.conditions = [
      { experimentId: 1, conditionId: 1, name: "Condition 1", defaultCondition: true },
      { experimentId: 1, conditionId: 2, name: "Condition 2", defaultCondition: true }
    ];

    const wrapper = mount();
    await wrapper.vm.$nextTick();

    await wrapper.find(".delete_condition").trigger("click");
    await flushPromises();

    expect(Swal.fire).toHaveBeenCalledWith(
      "You are attempting to delete the default condition. You must set another condition as default first."
    );
    expect(conditionService.delete).not.toHaveBeenCalled();
  });

  it("creates default conditions on mount when the experiment has none yet", async () => {
    conditionService.create.mockResolvedValue({
      experimentId: 1,
      conditionId: 100,
      name: ""
    });
    experimentStore.setExperiment({
      experimentId: 1,
      exposureType: "NOSET",
      conditions: []
    });

    mount();
    await flushPromises();

    expect(conditionService.create).toHaveBeenCalledTimes(2);
  });

  it("saveExit skips saving and navigates away when every condition name is blank", async () => {
    experimentStore.experiment.conditions.forEach(c => (c.name = "   "));

    const wrapper = mount();
    await wrapper.vm.$nextTick();

    await wrapper.vm.saveExit();
    await flushPromises();

    expect(conditionService.updateAll).not.toHaveBeenCalled();
    expect(pushMock).toHaveBeenCalledWith({
      name: "Home",
      params: { experimentId: 1 }
    });
  });
});
