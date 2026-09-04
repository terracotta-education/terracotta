import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

const pushMock = vi.fn();

vi.mock("vue-router", () => ({
  useRouter: () => ({ push: pushMock })
}));

import { experiment as experimentModule } from "@/store/experiment.module";
import { navigation as navigationModule } from "@/store/navigation.module";
import { mountComponent } from "@/test-utils/mount";
import Summary from "./Summary.vue";

describe("DesignSummary", () => {
  let pinia;
  let experimentStore;
  let navigationStore;

  const experiment = {
    experimentId: 1,
    title: "My study",
    description: "Why it matters",
    exposureType: "BETWEEN",
    conditions: [
      { conditionId: 10, name: "Condition A", defaultCondition: false },
      { conditionId: 11, name: "Condition B", defaultCondition: true }
    ]
  };

  beforeEach(() => {
    pinia = createPinia();
    setActivePinia(pinia);

    experimentStore = experimentModule();
    navigationStore = navigationModule();

    experimentStore.setExperiment(experiment);
  });

  function mount() {
    return mountComponent(Summary, {
      pinia,
      props: { experiment: experimentStore.experiment }
    });
  }

  async function expandAllPanels(wrapper) {
    const titles = wrapper.findAllComponents({ name: "VExpansionPanelTitle" });
    for (const title of titles) {
      await title.trigger("click");
    }
    await wrapper.vm.$nextTick();
  }

  it("shows the title, description, conditions and exposure type from the experiment", async () => {
    const wrapper = mount();

    await expandAllPanels(wrapper);

    expect(wrapper.text()).toContain("My study");
    expect(wrapper.text()).toContain("Why it matters");
    expect(wrapper.text()).toContain("Condition A");
    expect(wrapper.text()).toContain("Condition B");
    expect(wrapper.text()).toContain("One condition");
  });

  it("displays 'All conditions' for a WITHIN exposure type", async () => {
    experimentStore.setExperiment({ ...experiment, exposureType: "WITHIN" });

    const wrapper = mount();

    await expandAllPanels(wrapper);

    expect(wrapper.text()).toContain("All conditions");
  });

  it("hides the conditions panel when there are no conditions yet", async () => {
    experimentStore.setExperiment({ ...experiment, conditions: [] });

    const wrapper = mount();

    await expandAllPanels(wrapper);

    expect(wrapper.text()).not.toContain("Condition A");
  });

  it("marks the default condition with a check icon", async () => {
    const wrapper = mount();

    await expandAllPanels(wrapper);

    const items = wrapper.findAllComponents({ name: "VListItem" });
    const defaultItem = items.find(item => item.text().includes("Condition B"));

    expect(defaultItem.findComponent({ name: "VIcon" }).exists()).toBe(true);
  });

  it("navigates to the participation intro when Continue is clicked", async () => {
    const wrapper = mount();

    await wrapper.findComponent({ name: "VBtn" }).trigger("click");

    expect(pushMock).toHaveBeenCalledWith({
      name: "ExperimentParticipationIntro",
      params: { experiment: 1 }
    });
  });

  it("hides the Continue button and instead uses the caller page when in edit mode", async () => {
    navigationStore.saveEditMode({ callerPage: { name: "ExperimentSummary" } });

    const wrapper = mount();

    expect(wrapper.findComponent({ name: "VBtn" }).exists()).toBe(false);

    await wrapper.vm.saveExit();

    expect(pushMock).toHaveBeenCalledWith({
      name: "ExperimentSummary",
      params: { experiment: 1 }
    });
  });

  it("saveExit falls back to Home when there is no edit-mode caller page", () => {
    const wrapper = mount();

    wrapper.vm.saveExit();

    expect(pushMock).toHaveBeenCalledWith({
      name: "Home",
      params: { experiment: 1 }
    });
  });
});
