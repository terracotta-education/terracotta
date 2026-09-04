import { describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";
import { flushPromises } from "@vue/test-utils";

import { mountComponent } from "@/test-utils/mount";
import { experiment as useExperimentStore } from "@/store/experiment.module";
import { exposures as useExposuresStore } from "@/store/exposures.module";
import { outcome as useOutcomeStore } from "@/store/outcome.module";
import Selector from "./Selector.vue";

const setupStores = ({ exposures = [], outcomes = [] } = {}) => {
  const pinia = createPinia();
  setActivePinia(pinia);

  useExperimentStore().experiment = { experimentId: 42 };
  useExposuresStore().exposures = exposures;
  useOutcomeStore().outcomes = outcomes;
  useOutcomeStore().fetchOutcomesByExperimentId = vi.fn().mockResolvedValue(null);

  return pinia;
};

const mountSelector = async storeOptions => {
  const pinia = setupStores(storeOptions);
  const wrapper = mountComponent(Selector, { pinia });

  await flushPromises();

  return wrapper;
};

describe("Selector (OutcomeSelector)", () => {
  it("renders one select per exposure, sorted by title", async () => {
    const wrapper = await mountSelector({
      exposures: [
        { exposureId: "e2", title: "Beta" },
        { exposureId: "e1", title: "Alpha" }
      ]
    });

    const selects = wrapper.findAllComponents({ name: "VSelect" });

    expect(selects).toHaveLength(2);
    expect(selects[0].props("label")).toBe("Exposure Set 1");
    expect(selects[1].props("label")).toBe("Exposure Set 2");
  });

  it("includes static outcomes plus outcomes scoped to each exposure as options", async () => {
    const wrapper = await mountSelector({
      exposures: [{ exposureId: "e1", title: "Alpha" }],
      outcomes: [
        { outcomeId: "o1", title: "Outcome A", exposureId: "e1" },
        { outcomeId: "o2", title: "Outcome B", exposureId: "other-exposure" }
      ]
    });

    const select = wrapper.findComponent({ name: "VSelect" });
    const items = select.props("items");

    expect(items.map(i => i.outcomeId)).toEqual([
      "AVERAGE_ASSIGNMENT_SCORE",
      "TIME_ON_TASK",
      "o1"
    ]);
  });

  it("emits hasSelections with the standard outcome id when a regular outcome is selected", async () => {
    const wrapper = await mountSelector({
      exposures: [{ exposureId: "e1", title: "Alpha" }],
      outcomes: [{ outcomeId: "o1", title: "Outcome A", exposureId: "e1" }]
    });

    const select = wrapper.findComponent({ name: "VSelect" });
    await select.setValue("o1");

    const emitted = wrapper.emitted("hasSelections");

    expect(emitted).toBeTruthy();
    expect(emitted[emitted.length - 1][0]).toEqual({
      outcomeIds: ["o1"],
      alternateId: { id: null, exposures: [] }
    });
  });

  it("emits hasCleared once every selection is cleared out", async () => {
    const wrapper = await mountSelector({
      exposures: [{ exposureId: "e1", title: "Alpha" }],
      outcomes: [{ outcomeId: "o1", title: "Outcome A", exposureId: "e1" }]
    });

    const select = wrapper.findComponent({ name: "VSelect" });
    await select.setValue("o1");
    await select.setValue(null);

    expect(wrapper.emitted("hasCleared")).toBeTruthy();
  });

  it("treats a static outcome selection as an alternate id spanning all exposure sets", async () => {
    const wrapper = await mountSelector({
      exposures: [
        { exposureId: "e1", title: "Alpha" },
        { exposureId: "e2", title: "Beta" }
      ]
    });

    const selects = wrapper.findAllComponents({ name: "VSelect" });
    await selects[0].setValue("AVERAGE_ASSIGNMENT_SCORE");

    const emitted = wrapper.emitted("hasSelections");

    expect(emitted).toBeTruthy();
    const lastEmission = emitted[emitted.length - 1][0];

    expect(lastEmission.alternateId.id).toBe("AVERAGE_ASSIGNMENT_SCORE");
    expect(lastEmission.alternateId.exposures).toEqual(["e1", "e2"]);
  });

  it("fetches outcomes for the current experiment on mount", async () => {
    const pinia = setupStores({ exposures: [], outcomes: [] });
    mountComponent(Selector, { pinia });

    await flushPromises();

    expect(useOutcomeStore().fetchOutcomesByExperimentId).toHaveBeenCalledWith([42]);
  });
});
