import { describe, expect, it, vi, beforeEach } from "vitest";
import { createPinia, setActivePinia } from "pinia";

const push = vi.fn();
const onBeforeRouteUpdate = vi.fn();
const route = { params: { experimentId: "10" } };

vi.mock("vue-router", () => ({
  useRoute: () => route,
  useRouter: () => ({ push }),
  onBeforeRouteUpdate: (...args) => onBeforeRouteUpdate(...args)
}));

vi.mock("@/services", () => ({
  exposuresService: { getAll: vi.fn() }
}));

import { mountComponent } from "@/test-utils/mount";
import ExposureSets from "./ExposureSets.vue";
import { exposuresService } from "@/services";
import { exposures as exposuresModule } from "@/store/exposures.module";

const experiment = {
  experimentId: 10,
  exposureType: "WITHIN",
  conditions: [{ conditionId: 1 }, { conditionId: 2 }]
};

const exposuresFixture = [
  {
    exposureId: 100,
    groupConditionList: [
      { groupName: "Group A", conditionName: "Control" },
      { groupName: "Group B", conditionName: "Treatment" }
    ]
  },
  {
    exposureId: 101,
    groupConditionList: [
      { groupName: "Group A", conditionName: "Treatment" },
      { groupName: "Group B", conditionName: "Control" }
    ]
  }
];

function mountExposureSets() {
  const pinia = createPinia();
  setActivePinia(pinia);

  const wrapper = mountComponent(ExposureSets, {
    pinia,
    props: { experiment }
  });

  return { wrapper, exposuresStore: exposuresModule() };
}

describe("ExposureSets", () => {
  beforeEach(() => {
    push.mockClear();
    onBeforeRouteUpdate.mockClear();
    exposuresService.getAll.mockReset();
  });

  it("fetches exposures for the current experiment on mount", async () => {
    exposuresService.getAll.mockResolvedValue(exposuresFixture);

    const { wrapper } = mountExposureSets();
    await wrapper.vm.$nextTick();
    await new Promise(resolve => setTimeout(resolve));

    expect(exposuresService.getAll).toHaveBeenCalledWith("10");
  });

  it("registers an onBeforeRouteUpdate guard to refetch exposures", () => {
    mountExposureSets();

    expect(onBeforeRouteUpdate).toHaveBeenCalledWith(expect.any(Function));
  });

  it("renders one toggle button per exposure set and selects the first by default", async () => {
    exposuresService.getAll.mockResolvedValue(exposuresFixture);

    const { wrapper } = mountExposureSets();
    await wrapper.vm.$nextTick();
    await new Promise(resolve => setTimeout(resolve));
    await wrapper.vm.$nextTick();

    const toggleButtons = wrapper
      .findAllComponents({ name: "VBtn" })
      .filter(btn => !btn.text().includes("Continue"));
    expect(toggleButtons).toHaveLength(2);

    // first exposure's groups/conditions should be shown by default
    expect(wrapper.text()).toContain("Group A will receive");
    expect(wrapper.text()).toContain("Control");
  });

  it("shows the within-subject phrasing when exposureType is WITHIN", async () => {
    exposuresService.getAll.mockResolvedValue(exposuresFixture);

    const { wrapper } = mountExposureSets();
    await wrapper.vm.$nextTick();
    await new Promise(resolve => setTimeout(resolve));

    expect(wrapper.text()).toContain("exposed to every condition");
    expect(wrapper.text()).toContain("2 conditions");
    expect(wrapper.text()).toContain("2 exposure sets");
  });

  it("shows the between-subject phrasing when exposureType is not WITHIN", async () => {
    exposuresService.getAll.mockResolvedValue(exposuresFixture);

    const pinia = createPinia();
    setActivePinia(pinia);

    const wrapper = mountComponent(ExposureSets, {
      pinia,
      props: {
        experiment: { ...experiment, exposureType: "BETWEEN" }
      }
    });
    await wrapper.vm.$nextTick();
    await new Promise(resolve => setTimeout(resolve));

    expect(wrapper.text()).toContain("exposed to only one condition");
  });

  it("links Continue to AssignmentExposureSetsIntro with the selected exposure", async () => {
    exposuresService.getAll.mockResolvedValue(exposuresFixture);

    const { wrapper } = mountExposureSets();
    await wrapper.vm.$nextTick();
    await new Promise(resolve => setTimeout(resolve));
    await wrapper.vm.$nextTick();

    const continueButton = wrapper
      .findAllComponents({ name: "VBtn" })
      .find(btn => btn.text().includes("Continue"));

    expect(continueButton.props("to")).toEqual({
      name: "AssignmentExposureSetsIntro",
      params: {
        numberOfExperimentSets: 2,
        exposureId: 100
      }
    });
  });

  it("saveExit pushes to Home", async () => {
    exposuresService.getAll.mockResolvedValue([]);

    const { wrapper } = mountExposureSets();
    await wrapper.vm.$nextTick();
    await new Promise(resolve => setTimeout(resolve));

    wrapper.vm.saveExit();

    expect(push).toHaveBeenCalledWith({ name: "Home" });
  });
});
