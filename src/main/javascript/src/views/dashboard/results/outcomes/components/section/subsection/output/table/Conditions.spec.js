import { describe, expect, it } from "vitest";
import { createPinia, setActivePinia } from "pinia";

import { mountComponent } from "@/test-utils/mount";
import { resultsDashboard as useResultsDashboardStore } from "@/store/dashboard/results.module";
import Conditions from "./Conditions.vue";

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

describe("Conditions (OutcomeConditionsTable)", () => {
  it("passes the conditions rows, outcome type and title header down to the data table", () => {
    const pinia = setupStore({
      outcomeType: "STANDARD",
      conditions: {
        rows: [
          { title: "Condition A", number: 10, mean: 0.5, standardDeviation: 0.1 }
        ]
      }
    });

    const wrapper = mountComponent(Conditions, { pinia, global: { stubs } });
    const dataTable = wrapper.findComponent({ name: "DataTable" });

    expect(dataTable.props("tableData")).toEqual([
      { title: "Condition A", number: 10, mean: 0.5, standardDeviation: 0.1 }
    ]);
    expect(dataTable.props("outcomeType")).toBe("STANDARD");
    expect(dataTable.props("titleHeader")).toBe("Condition");
  });

  it("falls back to an empty array when there is no conditions data", () => {
    const pinia = setupStore(null);

    const wrapper = mountComponent(Conditions, { pinia, global: { stubs } });
    const dataTable = wrapper.findComponent({ name: "DataTable" });

    expect(dataTable.props("tableData")).toEqual([]);
    expect(dataTable.props("outcomeType")).toBe("");
  });
});
