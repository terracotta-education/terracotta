import { describe, expect, it, vi, beforeEach } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import { EventBus } from "@/helpers/event-bus";
import Input from "./Input.vue";

const mockExperimentStore = vi.fn();
const mockExposuresStore = vi.fn();
const mockResultsDashboardStore = vi.fn();
const mockGetOutcomes = vi.fn();
const mockClearOutcomes = vi.fn();

vi.mock("@/store/experiment.module", () => ({
  experiment: () => mockExperimentStore()
}));

vi.mock("@/store/exposures.module", () => ({
  exposures: () => mockExposuresStore()
}));

vi.mock("@/store/dashboard/results.module", () => ({
  resultsDashboard: () => mockResultsDashboardStore()
}));

vi.mock("./subsection/input/Selector.vue", () => ({
  default: {
    name: "OutcomeSelector",
    emits: ["hasSelections", "hasCleared"],
    template: "<div class=\"selector-stub\" />"
  }
}));

describe("SectionInput", () => {
  beforeEach(() => {
    mockGetOutcomes.mockClear();
    mockClearOutcomes.mockClear();

    mockExperimentStore.mockReturnValue({
      experiment: { experimentId: 42 },
      conditions: [{ conditionId: 1 }, { conditionId: 2 }]
    });
    mockExposuresStore.mockReturnValue({
      exposures: [{ exposureId: 1 }]
    });
    mockResultsDashboardStore.mockReturnValue({
      getOutcomes: mockGetOutcomes,
      clearOutcomes: mockClearOutcomes
    });
  });

  it("summarizes a single exposure set without the plural exposure-set language", () => {
    const wrapper = mountComponent(Input);

    expect(wrapper.text()).toContain("Your experiment has 2 conditions.");
    expect(wrapper.text()).toContain(
      "Select the outcomes you want to compare between conditions."
    );
  });

  it("summarizes multiple exposure sets using pluralized language", () => {
    mockExposuresStore.mockReturnValue({
      exposures: [{ exposureId: 1 }, { exposureId: 2 }]
    });

    const wrapper = mountComponent(Input);

    expect(wrapper.text()).toContain(
      "Your experiment has 2 conditions and 2 exposure sets."
    );
    expect(wrapper.text()).toContain(
      "Select the outcomes you want to compare between conditions/exposure sets."
    );
  });

  it("emits a statusPageNav event on the EventBus when the status page link is clicked", async () => {
    const emitSpy = vi.spyOn(EventBus, "emit");
    const wrapper = mountComponent(Input);

    const statusPageLink = wrapper
      .findAll("a")
      .find(link => link.text().includes("added on the status page"));
    expect(statusPageLink).toBeTruthy();

    await statusPageLink.trigger("click");

    expect(emitSpy).toHaveBeenCalledWith("statusPageNav");

    emitSpy.mockRestore();
  });

  it("fetches outcomes and emits hasSelection(true) when the selector reports selections", async () => {
    const wrapper = mountComponent(Input);
    const selections = { outcomeIds: ["o1"], alternateId: { id: null, exposures: [] } };

    wrapper.findComponent({ name: "OutcomeSelector" }).vm.$emit("hasSelections", selections);
    await wrapper.vm.$nextTick();

    expect(mockGetOutcomes).toHaveBeenCalledWith([42, selections]);
    expect(wrapper.emitted("hasSelection")).toBeTruthy();
    expect(wrapper.emitted("hasSelection").at(-1)).toEqual([true]);
  });

  it("clears outcomes and emits hasSelection(false) when the selector reports a cleared selection", async () => {
    const wrapper = mountComponent(Input);
    const selector = wrapper.findComponent({ name: "OutcomeSelector" });

    selector.vm.$emit("hasSelections", { outcomeIds: ["o1"], alternateId: { id: null, exposures: [] } });
    await wrapper.vm.$nextTick();

    selector.vm.$emit("hasCleared");
    await wrapper.vm.$nextTick();

    expect(mockClearOutcomes).toHaveBeenCalled();
    expect(wrapper.emitted("hasSelection").at(-1)).toEqual([false]);
  });
});
