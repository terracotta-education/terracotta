import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

const pushMock = vi.fn();

vi.mock("vue-router", () => ({
  useRouter: () => ({ push: pushMock })
}));

vi.mock("@/services", () => ({
  conditionService: {
    updateAll: vi.fn()
  }
}));

import { conditionService } from "@/services";
import { navigation as navigationModule } from "@/store/navigation.module";
import { mountComponent } from "@/test-utils/mount";
import DefaultCondition from "./DefaultCondition.vue";

describe("DefaultCondition", () => {
  let pinia;
  let navigationStore;

  const experiment = {
    experimentId: 1,
    conditions: [
      { experimentId: 1, conditionId: 10, name: "Condition A", defaultCondition: false },
      { experimentId: 1, conditionId: 11, name: "Condition B", defaultCondition: true }
    ]
  };

  beforeEach(() => {
    pinia = createPinia();
    setActivePinia(pinia);

    navigationStore = navigationModule();

    vi.clearAllMocks();
    conditionService.updateAll.mockResolvedValue({ status: 200 });
  });

  function mount() {
    return mountComponent(DefaultCondition, {
      pinia,
      props: { experiment }
    });
  }

  it("renders a radio option per condition, pre-selecting the current default", () => {
    const wrapper = mount();

    const radios = wrapper.findAll("input.radio-default-condition");
    expect(radios).toHaveLength(2);
    expect(radios[1].element.checked).toBe(true);
    expect(radios[0].element.checked).toBe(false);
    expect(wrapper.text()).toContain("Condition A");
    expect(wrapper.text()).toContain("Condition B");
  });

  it("enables the Next button once a default is selected", () => {
    const wrapper = mount();

    expect(wrapper.findComponent({ name: "VBtn" }).props("disabled")).toBe(
      false
    );
  });

  it("selecting a different condition saves it as the new default", async () => {
    const wrapper = mount();

    const radios = wrapper.findAll("input.radio-default-condition");
    await radios[0].setValue();

    expect(conditionService.updateAll).toHaveBeenCalledWith([
      expect.objectContaining({ conditionId: 10, defaultCondition: 1 }),
      expect.objectContaining({ conditionId: 11, defaultCondition: 0 })
    ]);
  });

  it("hides the Next button while in edit mode", () => {
    navigationStore.saveEditMode({ callerPage: { name: "ExperimentSummary" } });

    const wrapper = mount();

    expect(wrapper.findComponent({ name: "VBtn" }).exists()).toBe(false);
  });

  it("saveExit saves the selection and navigates to the caller page / Home", async () => {
    const wrapper = mount();

    await wrapper.vm.saveExit();
    await flushPromises();

    expect(conditionService.updateAll).toHaveBeenCalled();
    expect(pushMock).toHaveBeenCalledWith({
      name: "Home",
      params: { experiment: 1 }
    });
  });
});

function flushPromises() {
  return new Promise(resolve => setTimeout(resolve));
}
