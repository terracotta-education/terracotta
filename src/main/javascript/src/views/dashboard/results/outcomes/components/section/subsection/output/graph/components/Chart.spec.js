import { describe, expect, it, vi, beforeEach } from "vitest";
import { createPinia, setActivePinia } from "pinia";

import { mountComponent } from "@/test-utils/mount";
import { experiment as useExperimentStore } from "@/store/experiment.module";
import Chart from "./Chart.vue";

const { chartInstances, chartMock, wrapMock } = vi.hoisted(() => {
  const chartInstances = [];
  const chartMock = vi.fn((container, options) => {
    const instance = {
      options,
      destroy: vi.fn(),
      update: vi.fn(),
      series: [
        { xAxis: { width: 400, categories: options.xAxis.categories } }
      ]
    };
    chartInstances.push(instance);
    return instance;
  });
  return { chartInstances, chartMock, wrapMock: vi.fn() };
});

vi.mock("highcharts/esm/highcharts.js", () => {
  const colorChain = { setOpacity: () => ({ get: () => "rgba(0,0,0,0.75)" }) };
  const optionsObj = {
    lang: {},
    exporting: {
      menuItemDefinitions: {},
      buttons: { contextButton: { menuItems: ["downloadXLS"] } }
    },
    colors: ["#111111", "#222222"]
  };

  const Highcharts = {
    wrap: wrapMock,
    Chart: { prototype: {} },
    Renderer: { prototype: { symbols: {} } },
    getOptions: () => optionsObj,
    color: () => colorChain,
    chart: chartMock
  };

  return { default: Highcharts };
});

vi.mock("highcharts/esm/modules/exporting.js", () => ({}));
vi.mock("highcharts/esm/modules/export-data.js", () => ({}));
vi.mock("highcharts/esm/modules/offline-exporting.js", () => ({}));
vi.mock("highcharts/esm/modules/accessibility.js", () => ({}));
vi.mock("zipcelx", () => ({ default: vi.fn() }));

const mountChart = props => {
  const pinia = createPinia();
  setActivePinia(pinia);
  useExperimentStore().experiment = { experimentId: 1, title: "My Experiment" };

  return mountComponent(Chart, { props, pinia });
};

describe("Chart (OutcomeChart)", () => {
  beforeEach(() => {
    chartInstances.length = 0;
    chartMock.mockClear();
  });

  it("creates a Highcharts chart on mount with a mean series and a scores series", () => {
    mountChart({
      type: "condition",
      outcomeType: "STANDARD",
      displayChartData: true,
      graphData: [
        { title: "A", mean: 0.5, scores: [0.4, 0.6] },
        { title: "B", mean: 0.3, scores: [0.2] }
      ]
    });

    expect(chartMock).toHaveBeenCalledTimes(1);
    const options = chartMock.mock.calls[0][1];

    expect(options.series[0].name).toBe("Mean");
    expect(options.series[0].data).toHaveLength(2);
    expect(options.series[1].name).toBe("Percentage");
    expect(options.series[1].data).toHaveLength(3);
  });

  it("converts scores/means to percentages for STANDARD and AVERAGE_ASSIGNMENT_SCORE outcome types", () => {
    mountChart({
      type: "condition",
      outcomeType: "STANDARD",
      displayChartData: true,
      graphData: [{ title: "A", mean: 0.5, scores: [0.4] }]
    });

    const options = chartMock.mock.calls[0][1];

    expect(options.series[0].data[0].y).toBe(50);
    expect(options.series[1].data[0].y).toBe(40);
  });

  it("converts millisecond scores/means to minutes for TIME_ON_TASK", () => {
    mountChart({
      type: "condition",
      outcomeType: "TIME_ON_TASK",
      displayChartData: true,
      graphData: [{ title: "A", mean: 60000, scores: [30000] }]
    });

    const options = chartMock.mock.calls[0][1];

    expect(options.series[0].data[0].y).toBe(1);
    expect(options.series[1].data[0].y).toBe(0.5);
    expect(options.yAxis.title.text).toBe("Time (minutes)");
  });

  it("labels the x-axis according to the type prop", () => {
    mountChart({
      type: "exposure",
      outcomeType: "STANDARD",
      displayChartData: true,
      graphData: [{ title: "A", mean: 0.1, scores: [] }]
    });

    const options = chartMock.mock.calls[0][1];

    expect(options.xAxis.title.text).toBe("Exposure");
    expect(options.xAxis.categories).toEqual(["A"]);
  });

  it("enables the export context button only when displayChartData is true", () => {
    mountChart({
      type: "condition",
      outcomeType: "STANDARD",
      displayChartData: false,
      graphData: [{ title: "A", mean: 0.1, scores: [] }]
    });

    const options = chartMock.mock.calls[0][1];

    expect(options.exporting.buttons.contextButton.enabled).toBe(false);
  });

  it("destroys the chart instance when the component unmounts", () => {
    const wrapper = mountChart({
      type: "condition",
      outcomeType: "STANDARD",
      displayChartData: true,
      graphData: [{ title: "A", mean: 0.1, scores: [] }]
    });

    const instance = chartInstances[0];
    wrapper.unmount();

    expect(instance.destroy).toHaveBeenCalled();
  });

  it("recreates the chart when graphData changes", async () => {
    const wrapper = mountChart({
      type: "condition",
      outcomeType: "STANDARD",
      displayChartData: true,
      graphData: [{ title: "A", mean: 0.1, scores: [] }]
    });

    const firstInstance = chartInstances[0];

    await wrapper.setProps({
      graphData: [{ title: "A", mean: 0.1, scores: [] }, { title: "B", mean: 0.9, scores: [] }]
    });

    expect(firstInstance.destroy).toHaveBeenCalled();
    expect(chartMock).toHaveBeenCalledTimes(2);
    expect(chartMock.mock.calls[1][1].xAxis.categories).toEqual(["A", "B"]);
  });
});
