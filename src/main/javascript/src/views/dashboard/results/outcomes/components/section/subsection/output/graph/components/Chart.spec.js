import { describe, expect, it, vi, beforeEach } from "vitest";
import { createPinia, setActivePinia } from "pinia";

import { mountComponent } from "@/test-utils/mount";
import { experiment as useExperimentStore } from "@/store/experiment.module";
import Highcharts from "highcharts/esm/highcharts.js";
import zipcelx from "zipcelx";
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

// Chart.vue's plain (non-setup) <script> block calls Highcharts.wrap(...) exactly once,
// as a side effect of importing the component, before any test body runs. Vitest's default
// `clearMocks: true` wipes every vi.fn()'s call history before each test, so that single
// call must be captured here (during module evaluation/collection) rather than read from
// wrapMock.mock.calls inside an it() block, where it would already have been cleared.
const [, , wrappedGetDataRows] = wrapMock.mock.calls[0] || [];

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

  it("falls back to the 'Category' axis label when type is not condition/exposure", () => {
    mountChart({
      outcomeType: "STANDARD",
      displayChartData: true,
      graphData: [{ title: "A", mean: 0.1, scores: [] }]
    });

    const options = chartMock.mock.calls[0][1];

    expect(options.xAxis.title.text).toBe("Category");
  });

  it("uses the default 0-100 y-axis range for an unrecognized outcomeType", () => {
    mountChart({
      type: "condition",
      outcomeType: "SOMETHING_UNKNOWN",
      displayChartData: true,
      graphData: [{ title: "A", mean: 0.1, scores: [] }]
    });

    const options = chartMock.mock.calls[0][1];

    expect(options.yAxis).toEqual({
      min: 0,
      max: 100,
      labels: { style: { color: "#333333" } },
      title: { text: "" }
    });
  });

  it("computes min/max across multiple TIME_ON_TASK scores", () => {
    mountChart({
      type: "condition",
      outcomeType: "TIME_ON_TASK",
      displayChartData: true,
      graphData: [{ title: "A", mean: 60000, scores: [30000, 600000] }]
    });

    const options = chartMock.mock.calls[0][1];

    // min/max are derived from Math.ceil(milliToMinutes(score)) across all scores,
    // exercising the reduce() comparator on both branches (>1 element required).
    expect(options.yAxis.min).toBe(0);
    expect(options.yAxis.max).toBe(11);
  });
});

describe("Chart.vue module-level Highcharts patches", () => {
  it("normalizes rows with a truthy x value when Highcharts requests data rows", () => {
    // Highcharts.wrap is mocked out (vi.fn()), so the wrapped getDataRows
    // implementation itself never runs unless we invoke the captured callback directly.
    expect(wrappedGetDataRows).toBeInstanceOf(Function);

    const proceed = vi.fn(() => [
      { x: 5, 0: "old" },
      { 0: "untouched" }
    ]);

    const rows = wrappedGetDataRows.call({}, proceed, true);

    expect(proceed).toHaveBeenCalledWith(true);
    expect(rows[0][0]).toBe(5);
    expect(rows[1][0]).toBe("untouched");
  });

  describe("downloadXLSX", () => {
    beforeEach(() => {
      zipcelx.mockClear();
      document.body.innerHTML = "";
    });

    it("uses options.exporting.filename when present", () => {
      const context = {
        getDataRows: vi.fn(() => [
          ["Category", "Mean", "Percentage"],
          ["A", 50, 40]
        ]),
        options: { exporting: { filename: "custom-name" } },
        title: { textStr: "Some Title" }
      };

      Highcharts.Chart.prototype.downloadXLSX.call(context);

      expect(context.getDataRows).toHaveBeenCalledWith(true);
      expect(zipcelx).toHaveBeenCalledWith(
        expect.objectContaining({
          filename: "custom-name",
          sheet: {
            data: [[
              { type: "string", value: "A" },
              { type: "number", value: 50 },
              { type: "number", value: 40 }
            ]]
          }
        })
      );
    });

    it("falls back to a slugified chart title when no filename is configured", () => {
      const context = {
        getDataRows: vi.fn(() => [
          ["Category", "Mean", "Percentage"],
          ["A", 50, 40]
        ]),
        options: { exporting: {} },
        title: { textStr: "My Chart Title" }
      };

      Highcharts.Chart.prototype.downloadXLSX.call(context);

      expect(zipcelx).toHaveBeenCalledWith(
        expect.objectContaining({ filename: "my-chart-title" })
      );
    });

    it("falls back to 'chart' when there is neither a filename nor a title", () => {
      const context = {
        getDataRows: vi.fn(() => [
          ["Category", "Mean", "Percentage"],
          ["A", 50, 40]
        ]),
        options: { exporting: {} },
        title: null
      };

      Highcharts.Chart.prototype.downloadXLSX.call(context);

      expect(zipcelx).toHaveBeenCalledWith(
        expect.objectContaining({ filename: "chart" })
      );
    });
  });

  it("registers a downloadXLSX export menu item whose onclick delegates to the chart instance", () => {
    const menuItem = Highcharts.getOptions().exporting.menuItemDefinitions.downloadXLSX;
    expect(menuItem.textKey).toBe("downloadXLSX");

    const context = {
      downloadXLSX: vi.fn()
    };

    menuItem.onclick.call(context);

    expect(context.downloadXLSX).toHaveBeenCalled();
  });

  it("registers meanLine and download renderer symbols usable by Highcharts", () => {
    const { meanLine, download } = Highcharts.Renderer.prototype.symbols;

    expect(meanLine(10, 20, 30, 40)).toEqual(["M", 10, 35, "L", 50, 35]);

    const downloadPath = download(0, 0, 10, 10);
    expect(downloadPath[0]).toBe("M");
    expect(downloadPath).toContain("L");
  });
});

describe("Chart (OutcomeChart) exportData events callback", () => {
  beforeEach(() => {
    chartInstances.length = 0;
    chartMock.mockClear();
  });

  it("relabels TIME_ON_TASK export rows and normalizes x-values", () => {
    mountChart({
      type: "condition",
      outcomeType: "TIME_ON_TASK",
      displayChartData: true,
      graphData: [{ title: "A", mean: 60000, scores: [30000] }]
    });

    const { exportData } = chartMock.mock.calls[0][1].chart.events;

    const headerRow = ["Category", "Mean", "Time"];
    const dataRow = ["A", 2, 3];
    dataRow.xValues = [1];
    dataRow.x = 1;

    const dataRows = [headerRow, dataRow];

    exportData({ dataRows });

    expect(dataRows[0][2]).toBe("Mean (ms)");
    expect(dataRows[0][3]).toBe("Time");
    expect(dataRows[0][4]).toBe("Time (ms)");
    expect(dataRow.xValues[0]).toBe(0);
    expect(dataRow.x).toBe(0);
  });

  it("leaves non-TIME_ON_TASK export rows unchanged (default switch branch)", () => {
    mountChart({
      type: "condition",
      outcomeType: "STANDARD",
      displayChartData: true,
      graphData: [{ title: "A", mean: 0.5, scores: [0.4] }]
    });

    const { exportData } = chartMock.mock.calls[0][1].chart.events;

    const headerRow = ["Category", "Mean", "Percentage"];
    const dataRow = ["A", 50, 40];

    const dataRows = [headerRow, dataRow];

    expect(() => exportData({ dataRows })).not.toThrow();
    expect(dataRows[0][2]).toBe("Percentage");
  });
});
