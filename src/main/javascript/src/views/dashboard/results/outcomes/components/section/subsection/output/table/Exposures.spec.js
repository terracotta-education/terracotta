import { describe, expect, it } from "vitest";
import { createPinia, setActivePinia } from "pinia";

import { mountComponent } from "@/test-utils/mount";
import { resultsDashboard as useResultsDashboardStore } from "@/store/dashboard/results.module";
import Exposures from "./Exposures.vue";

const stubs = {
  DataTable: {
    name: "DataTable",
    template: "<div class=\"data-table-stub\" />",
    props: ["tableData", "outcomeType", "titleHeader"]
  }
};

const setupStore = outcomes => {
  const pinia = createPinia();
  setActivePinia(pinia);

  useResultsDashboardStore().resultsDashboard = {
    experimentId: 1,
    overview: null,
    outcomes
  };

  return pinia;
};

describe("Exposures (OutcomeExposuresTable)", () => {
  it("passes the exposures rows, outcome type and title header down to the data table", () => {
    const pinia = setupStore({
      outcomeType: "TIME_ON_TASK",
      exposures: {
        rows: [
          { title: "Exposure A", number: 5, mean: 60000, standardDeviation: 1000 }
        ]
      }
    });

    const wrapper = mountComponent(Exposures, { pinia, global: { stubs } });
    const dataTable = wrapper.findComponent({ name: "DataTable" });

    expect(dataTable.props("tableData")).toEqual([
      { title: "Exposure A", number: 5, mean: 60000, standardDeviation: 1000 }
    ]);
    expect(dataTable.props("outcomeType")).toBe("TIME_ON_TASK");
    expect(dataTable.props("titleHeader")).toBe("Exposure");
  });

  it("falls back to an empty array when there is no exposures data", () => {
    const pinia = setupStore(null);

    const wrapper = mountComponent(Exposures, { pinia, global: { stubs } });
    const dataTable = wrapper.findComponent({ name: "DataTable" });

    expect(dataTable.props("tableData")).toEqual([]);
    expect(dataTable.props("outcomeType")).toBe("");
  });
});
