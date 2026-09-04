import { describe, expect, it } from "vitest";
import { createPinia, setActivePinia } from "pinia";

import { mountComponent } from "@/test-utils/mount";
import { resultsDashboard as useResultsDashboardStore } from "@/store/dashboard/results.module";
import Graph from "./Graph.vue";

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

const stubs = {
  Chart: { name: "Chart", template: "<div class=\"chart-stub\" />", props: ["displayChartData", "graphData", "outcomeType", "type"] }
};

describe("Graph (OutcomeGraph)", () => {
  it("passes condition rows to the chart, sorted alphabetically and excluding Overall", () => {
    const pinia = setupStore({
      outcomeType: "STANDARD",
      conditions: {
        rows: [
          { title: "Overall", mean: 0.9, scores: [0.9] },
          { title: "Zeta", mean: 0.5, scores: [0.5] },
          { title: "Alpha", mean: 0.7, scores: [0.7] }
        ]
      }
    });

    const wrapper = mountComponent(Graph, {
      props: { type: "condition", displayOutput: true },
      pinia,
      global: { stubs }
    });

    const chart = wrapper.findComponent({ name: "Chart" });
    const graphData = chart.props("graphData");

    expect(graphData.map(d => d.title)).toEqual(["Alpha", "Zeta"]);
    expect(chart.props("outcomeType")).toBe("STANDARD");
    expect(chart.props("type")).toBe("condition");
  });

  it("uses exposure rows when type is exposure", () => {
    const pinia = setupStore({
      outcomeType: "STANDARD",
      exposures: {
        rows: [
          { title: "Exposure B", mean: 0.4, scores: [0.4] },
          { title: "Exposure A", mean: 0.6, scores: [0.6] }
        ]
      },
      conditions: { rows: [{ title: "Condition A", mean: 0.1, scores: [] }] }
    });

    const wrapper = mountComponent(Graph, {
      props: { type: "exposure", displayOutput: true },
      pinia,
      global: { stubs }
    });

    const chart = wrapper.findComponent({ name: "Chart" });

    expect(chart.props("graphData").map(d => d.title)).toEqual(["Exposure A", "Exposure B"]);
  });

  it("nulls out mean and scores when displayOutput is false", () => {
    const pinia = setupStore({
      outcomeType: "STANDARD",
      conditions: { rows: [{ title: "Alpha", mean: 0.7, scores: [0.7, 0.8] }] }
    });

    const wrapper = mountComponent(Graph, {
      props: { type: "condition", displayOutput: false },
      pinia,
      global: { stubs }
    });

    const chart = wrapper.findComponent({ name: "Chart" });

    expect(chart.props("displayChartData")).toBe(false);
    expect(chart.props("graphData")).toEqual([{ title: "Alpha", mean: null, scores: [] }]);
  });

  it("returns an empty dataset when there is no matching outcomes data", () => {
    const pinia = setupStore(null);

    const wrapper = mountComponent(Graph, {
      props: { type: "condition", displayOutput: true },
      pinia,
      global: { stubs }
    });

    const chart = wrapper.findComponent({ name: "Chart" });

    expect(chart.props("graphData")).toEqual([]);
  });
});
