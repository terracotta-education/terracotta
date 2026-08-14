<template>
  <figure class="highcharts-figure">
    <div ref="chartContainer"></div>
  </figure>
</template>

<script>
import Highcharts from "highcharts/esm/highcharts.js";
import "highcharts/esm/modules/exporting.js";
import "highcharts/esm/modules/export-data.js";
import "highcharts/esm/modules/offline-exporting.js";
import "highcharts/esm/modules/accessibility.js";
import zipcelx from "zipcelx";

function patchDataRows(H) {
  H.wrap(H.Chart.prototype, "getDataRows", function (proceed, multiLevelHeaders) {
    let rows = proceed.call(this, multiLevelHeaders);
    rows = rows.map(row => {
      if (row.x) row[0] = row.x;
      return row;
    });
    return rows;
  });
}

function patchXlsxExport(H) {
  H.Chart.prototype.downloadXLSX = function () {
    const div = document.createElement("div");
    let name;
    let xlsxRows;

    div.style.display = "none";
    document.body.appendChild(div);

    const rows = this.getDataRows(true);

    xlsxRows = rows.slice(1).map(row =>
      row.map(column => ({
        type: typeof column === "number" ? "number" : "string",
        value: column
      }))
    );

    if (this.options.exporting.filename) {
      name = this.options.exporting.filename;
    } else if (this.title && this.title.textStr) {
      name = this.title.textStr.replace(/ /g, "-").toLowerCase();
    } else {
      name = "chart";
    }

    zipcelx({ filename: name, sheet: { data: xlsxRows } });
  };

  H.getOptions().lang.downloadXLSX = "Download XLSX";
  H.getOptions().exporting.menuItemDefinitions.downloadXLSX = {
    textKey: "downloadXLSX",
    onclick() { this.downloadXLSX(); }
  };

  const menuItems = H.getOptions().exporting.buttons.contextButton.menuItems;
  const xlsIndex = menuItems.indexOf("downloadXLS");
  if (xlsIndex !== -1) menuItems[xlsIndex] = "downloadXLSX";
}

function registerSymbols(H) {
  H.Renderer.prototype.symbols.meanLine = function (x, y, w, h) {
    return ["M", x, y + w / 2, "L", x + h, y + w / 2];
  };

  H.Renderer.prototype.symbols.download = function (x, y, w, h) {
    return [
      "M", x + w * 0.5, y,
      "L", x + w * 0.5, y + h * 0.7,
      "M", x + w * 0.3, y + h * 0.5,
      "L", x + w * 0.5, y + h * 0.7,
      "L", x + w * 0.7, y + h * 0.5,
      "M", x, y + h * 0.9,
      "L", x, y + h,
      "L", x + w, y + h,
      "L", x + w, y + h * 0.9
    ];
  };
}

patchDataRows(Highcharts);
patchXlsxExport(Highcharts);
registerSymbols(Highcharts);
</script>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from "vue";

import {
  milliToMinutes,
  minutesToMillis,
  percent,
  round,
  timeFormat
} from "@/helpers/dashboard/utils.js";
import { getColor, addAttributesToElement } from "@/helpers/ui-utils.js";
import { experiment as useExperimentStore } from "@/store/experiment.module";

defineOptions({
  name: "OutcomeChart"
});

const props = defineProps({
  displayChartData: {
    type: Boolean,
    default: false
  },
  graphData: {
    type: Array,
    default: () => []
  },
  outcomeType: {
    type: String,
    default: "STANDARD"
  },
  type: {
    type: String
  }
});

const chartContainer = ref(null);

const experimentStore = useExperimentStore();
const experiment = computed(() => experimentStore.experiment);

const chartDataType = computed(() => {
  switch (props.type) {
    case "condition": return "Condition";
    case "exposure": return "Exposure";
    default: return "Category";
  }
});

const experimentTitle = computed(() => experiment.value?.title || "experiment");
const displayData = computed(() => props.displayChartData || false);

const computedGraphData = computed(() =>
  (props.graphData ?? []).map(g => {
    switch (props.outcomeType) {
      case "TIME_ON_TASK":
        return { title: g.title, scores: g.scores.map(s => round(s)), mean: round(g.mean) };
      case "AVERAGE_ASSIGNMENT_SCORE":
      case "STANDARD":
      default:
        return { title: g.title, scores: g.scores.map(s => percent(s)), mean: percent(g.mean) };
    }
  })
);

const plotColors = computed(() =>
  Highcharts.getOptions().colors.map(color =>
    Highcharts.color(color).setOpacity(0.75).get()
  )
);

const scoresName = computed(() => {
  switch (props.outcomeType) {
    case "TIME_ON_TASK": return "Time";
    case "AVERAGE_ASSIGNMENT_SCORE":
    case "STANDARD":
    default: return "Percentage";
  }
});

const tooltipFormat = computed(() => {
  switch (props.outcomeType) {
    case "TIME_ON_TASK": return "{point.custom.time}";
    case "AVERAGE_ASSIGNMENT_SCORE":
    case "STANDARD":
    default: return "{point.y}";
  }
});

const means = computed(() => {
  const data = computedGraphData.value.map((gd, index) => {
    switch (props.outcomeType) {
      case "TIME_ON_TASK":
        return { x: index, y: milliToMinutes(gd.mean), custom: { time: timeFormat(gd.mean) } };
      case "AVERAGE_ASSIGNMENT_SCORE":
      case "STANDARD":
      default:
        return { x: index, y: gd.mean };
    }
  });
  return {
    data,
    marker: { symbol: "meanLine", lineWidth: 2, radius: 8, lineColor: "rgba(102, 102, 102, .75)" },
    name: "Mean",
    tooltip: { pointFormat: `mean: ${tooltipFormat.value}` },
    type: "scatter"
  };
});

const scores = computed(() => {
  const data = [];
  computedGraphData.value.forEach((cgd, dataIndex) => {
    cgd.scores.forEach(score => {
      switch (props.outcomeType) {
        case "TIME_ON_TASK":
          data.push({ x: dataIndex, y: milliToMinutes(score), color: plotColors.value[dataIndex], custom: { time: timeFormat(score) } });
          break;
        case "AVERAGE_ASSIGNMENT_SCORE":
        case "STANDARD":
        default:
          data.push({ x: dataIndex, y: score, color: plotColors.value[dataIndex] });
          break;
      }
    });
  });
  return {
    data,
    jitter: { x: 0.24, y: 0 },
    marker: { radius: 4, symbol: "circle" },
    name: scoresName.value,
    tooltip: { pointFormat: tooltipFormat.value },
    type: "scatter"
  };
});

const series = computed(() => [means.value, scores.value]);

const xAxis = computed(() => ({
  categories: [...new Set(computedGraphData.value.map(cgd => cgd.title))],
  // Highcharts 13 defaults axis label fill to var(--highcharts-neutral-color-80), which
  // this app never defines, so it falls through to a faded, inherited text color. Pin the
  // pre-v13 default explicitly (matches Highcharts' own light-theme palette value).
  labels: { style: { color: "#333333" } },
  title: {
    style: { color: "#fff", fontSize: 0.0 },
    text: chartDataType.value
  }
}));

const yAxis = computed(() => {
  switch (props.outcomeType) {
    case "TIME_ON_TASK": {
      const allScores = [];
      computedGraphData.value.forEach(cgd => {
        allScores.push(...cgd.scores.map(s => Math.ceil(milliToMinutes(s))));
      });
      let min = allScores.length
        ? allScores.reduce((prev, curr) => (prev < curr ? prev : curr))
        : 0;
      min = min !== 0 ? min - 1 : min;
      return {
        min,
        max: (allScores.length ? allScores.reduce((prev, curr) => (prev > curr ? prev : curr)) : 99) + 1,
        labels: { style: { color: "#333333" } },
        title: { text: "Time (minutes)" }
      };
    }
    case "AVERAGE_ASSIGNMENT_SCORE":
    case "STANDARD":
      return { min: null, max: null, labels: { style: { color: "#333333" } }, title: { text: "Percentage" } };
    default:
      return { min: 0, max: 100, labels: { style: { color: "#333333" } }, title: { text: "" } };
  }
});

const options = computed(() => ({
  chart: {
    // Highcharts 13 defaults chart.backgroundColor to var(--highcharts-background-color), which
    // this app never defines, so it falls through to black. Pin the pre-v13 default explicitly.
    backgroundColor: "#ffffff",
    accessibility: {
      keyboardNavigation: {
        focusBorder: {
          style: { lineWidth: 3, color: getColor("--blue-primary"), borderRadius: 4 },
          margin: 4
        }
      }
    },
    events: {
      exportData({ dataRows }) {
        for (let i = 1; i < dataRows.length; i++) {
          if (dataRows[i].xValues?.[ 0]) dataRows[i].xValues[0] = 0;
          if (dataRows[i].x) dataRows[i].x = 0;
        }
        switch (dataRows[0][2]) {
          case "Time":
            dataRows[0][2] = "Mean (ms)";
            dataRows[0][3] = "Time";
            dataRows[0][4] = "Time (ms)";
            for (let i = 1; i < dataRows.length; i++) {
              dataRows[i][4] = dataRows[i][2] ? Math.ceil(minutesToMillis(dataRows[i][2])) : dataRows[i][2];
              dataRows[i][3] = dataRows[i][4] ? timeFormat(dataRows[i][4]) : dataRows[i][4];
              dataRows[i][2] = dataRows[i][1] ? Math.ceil(minutesToMillis(dataRows[i][1])) : dataRows[i][1];
              dataRows[i][1] = dataRows[i][2] ? timeFormat(dataRows[i][2]) : dataRows[i][2];
            }
            break;
          default:
            break;
        }
      }
    }
  },
  credits: { enabled: false },
  exporting: {
    buttons: {
      contextButton: {
        enabled: displayData.value,
        menuItems: [
          "viewFullscreen", "printChart", "separator",
          "downloadPNG", "downloadJPEG", "downloadPDF", "downloadSVG",
          "separator", "downloadCSV"
        ],
        symbol: "download",
        symbolStroke: "rgba(102, 102, 102, .75)",
        theme: {
          "stroke-width": 1,
          stroke: "silver",
          r: 4,
          states: { hover: { fill: "rgba(231, 234, 238, .5)" } }
        },
        verticalAlign: "bottom",
        x: 0,
        y: -60
      }
    },
    filename: `${experimentTitle.value}_${chartDataType.value}`
  },
  plotOptions: {
    series: {
      showInLegend: false,
      stickyTracking: false,
      tooltip: { headerFormat: "" }
    }
  },
  series: series.value,
  title: { text: "" },
  tooltip: { snap: 0 },
  xAxis: xAxis.value,
  yAxis: yAxis.value
}));

let chart = null;

const createChart = () => {
  if (!chartContainer.value) return;
  chart = Highcharts.chart(chartContainer.value, options.value);
  chart.update({
    series: [{
      marker: {
        radius: chart.series[0].xAxis.width / chart.series[0].xAxis.categories.length / 4
      }
    }],
    yAxis: yAxis.value
  });
};

const destroyChart = () => {
  if (chart) {
    chart.destroy();
    chart = null;
  }
};

const resetChart = () => {
  destroyChart();
  createChart();
};

const addChartA11yLabel = async () => {
  await nextTick();
  addAttributesToElement("svg.highcharts-root", [
    { name: "aria-label", value: `${experimentTitle.value} ${chartDataType.value} chart` }
  ]);
};

watch(
  () => props.graphData,
  async () => {
    resetChart();
    await addChartA11yLabel();
  },
  { deep: true }
);

onMounted(async () => {
  createChart();
  await addChartA11yLabel();
});

onBeforeUnmount(() => {
  destroyChart();
});
</script>

<style scoped>
figure.highcharts-figure {
  width: 100% !important;
  height: 200% !important;
  display: block !important;
  & div.highcharts-container {
    width: 100% !important;
    height: 100% !important;
    display: block !important;
  }
  & .highcharts-contextbutton {
    filter: drop-shadow(-2px 2px 2px #e7eaee);
  }
}
</style>
