<template>
  <figure
    class="highcharts-figure"
  >
    <div
      id="container-chart"
    >
    </div>
  </figure>
</template>

<script>
import Highcharts from "highcharts";
import exportData from "highcharts/modules/export-data";
import exportingInit from "highcharts/modules/exporting";
import zipcelx from "zipcelx";
import { mapGetters } from "vuex";
import { milliToMinutes, minutesToMillis, percent, round, timeFormat } from "@/helpers/dashboard/utils.js";

exportingInit(Highcharts);
exportData(Highcharts);

(function(H) {
  H.wrap(H.Chart.prototype, 'getDataRows', function(proceed, multiLevelHeaders) {
    var rows = proceed.call(this, multiLevelHeaders);

    rows = rows.map(row => {
      if (row.x) {
        row[0] = row.x;
      }
      return row;
    });

    return rows;
  });
}(Highcharts));

(function (H) {
  H.Chart.prototype.downloadXLSX = function () {
    const div = document.createElement('div');
    let name,
      xlsxRows = [];
    div.style.display = 'none';
    document.body.appendChild(div);
    const rows = this.getDataRows(true);
    xlsxRows = rows.slice(1).map(function (row) {
      return row.map(function (column) {
        return {
          type: typeof column === 'number' ? 'number' : 'string',
          value: column
        };
      });
    });

    // Get the filename, copied from the Chart.fileDownload function
    if (this.options.exporting.filename) {
        name = this.options.exporting.filename;
    } else if (this.title && this.title.textStr) {
        name = this.title.textStr.replace(/ /g, '-').toLowerCase();
    } else {
        name = 'chart';
    }

    zipcelx({
      filename: name,
      sheet: {
        data: xlsxRows
      }
    });
  };

  // Default lang string, overridable in i18n options
  H.getOptions().lang.downloadXLSX = 'Download XLSX';

  // Add the menu item handler
  H.getOptions().exporting.menuItemDefinitions.downloadXLSX = {
    textKey: 'downloadXLSX',
    onclick: function () {
      this.downloadXLSX();
    }
  };

  // Replace the menu item
  const menuItems =
    H.getOptions().exporting.buttons.contextButton.menuItems;
  menuItems[menuItems.indexOf('downloadXLS')] = 'downloadXLSX';
}(Highcharts));

export default {
  name : "Chart",
  props: [
    "displayChartData",
    "graphData",
    "outcomeType",
    "type"
  ],
  data: () => ({
    chart: null
  }),
  computed: {
    ...mapGetters({
      experiment: "experiment/experiment"
    }),
    chartDataType() {
      switch (this.type) {
        case "condition":
          return "Condition";
        case "exposure":
          return "Exposure";
        default:
          return "Category";
      }
    },
    computedGraphData() {
      // process the chart-specific data format
      return this.graphData.map(
        (g) => {
          switch (this.outcomeType) {
            case "TIME_ON_TASK":
              return {
                title: g.title,
                scores: g.scores.map((s) => round(s)),
                mean: round(g.mean)
              }
            case "AVERAGE_ASSIGNMENT_SCORE":
            case "STANDARD":
            default:
              return {
                title: g.title,
                scores: g.scores.map((s) => percent(s)),
                mean: percent(g.mean)
              }
          }
        }
      );
    },
    displayData() {
      // should the chart data be shown?
      return this.displayChartData || false;
    },
    experimentTitle() {
      return this.experiment?.title || "experiment"
    },
    means() {
      var means = this.computedGraphData.map((gd, i) => {
        switch (this.outcomeType) {
          case "TIME_ON_TASK":
            return {
              x: i, // index of x-axis
              y: milliToMinutes(gd.mean), // value to plot
              custom: {
                time: timeFormat(gd.mean)
              }
            };
          case "AVERAGE_ASSIGNMENT_SCORE":
          case "STANDARD":
          default:
            return {
              x: i, // index of x-axis
              y: gd.mean // value to plot
            };
        }
      });
      return {
        data: means,
        marker:{
          symbol: "meanLine", // custom line for the point
          lineWidth: 2, // default line stroke width
          radius: 8, // default length of the line
          lineColor: "rgba(102, 102, 102, .75)"
        },
        name: "Mean",
        tooltip: {
          pointFormat: "mean: " + this.tooltipFormat // label for the mean line
        },
        type: "scatter" // scatter plot
      };
    },
    options() {
      return {
        chart: {
          events: {
            exportData({ dataRows }) {
              for (let i = 1; i < dataRows.length; i++) {
                // set the condition / exposure name
                if (dataRows[i].xValues) {
                  if (dataRows[i].xValues[0]) {
                    dataRows[i].xValues[0] = 0;
                  }
                }
                if (dataRows[i].x) {
                  dataRows[i].x = 0;
                }
              }
              // datarows[0] = ["Condition/Exposure","Mean","Time/Percentage"]
              switch (dataRows[0][2]) {
                case "Time":
                  // this is a TIME_ON_TASK outcome chart; header is "Time"
                  dataRows[0][2] = "Mean (ms)"
                  dataRows[0][3] = "Time";
                  dataRows[0][4] = "Time (ms)";
                  // datarows[0] = ["Condition/Exposure","Mean","Mean (ms)","Time","Time (ms)"]
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
        credits: {
          enabled: false // no credits footer
        },
        exporting: {
          buttons: {
            contextButton: {
              enabled: this.displayData,
              menuItems: [
                "viewFullscreen",
                "printChart",
                "separator",
                "downloadPNG",
                "downloadJPEG",
                "downloadPDF",
                "downloadSVG",
                "separator",
                "downloadCSV"
              ],
              symbol: "download",
              symbolStroke: "rgba(102, 102, 102, .75)",
              theme: {
                'stroke-width': 1,
                stroke: 'silver',
                r: 4,
                states: {
                  hover: {
                    fill: "rgba(231, 234, 238, .5)"
                  }
                }
              },
              verticalAlign: "bottom",
              x: 0,
              y: -60
            }
          },
          filename: this.experimentTitle + "_" + this.chartDataType
        },
        plotOptions: {
          series: {
            showInLegend: false, // no chart data legend
            stickyTracking: false, // hide tooltip on mouseout
            tooltip: {
              headerFormat: "" // no tooltip header
            }
          }
        },
        series: this.series,
        title: {
          text: "" // no chart title
        },
        tooltip: {
          snap: 0 // proximity for point hover
        },
        xAxis: this.xAxis
      }
    },
    plotColors() {
      // a pre-defined set of point colors
      return Highcharts.getOptions().colors.map(color =>
          Highcharts.color(color).setOpacity(0.75).get()
      );
    },
    scores() {
      var scores = [];
      this.computedGraphData.forEach((cgd, i) => {
        cgd.scores.forEach((s) => {
          switch (this.outcomeType) {
            case "TIME_ON_TASK":
              scores.push({
                x: i, // index of x-axis
                y: milliToMinutes(s), // value to plot
                color: this.plotColors[i], // color of points
                custom: {
                  time: timeFormat(s)
                }
              });
              break;
            case "AVERAGE_ASSIGNMENT_SCORE":
            case "STANDARD":
            default:
              scores.push({
                x: i, // index of x-axis
                y: s, // value to plot
                color: this.plotColors[i] // color of points
              });
              break;
          }
        })
      });
      return {
        data: scores,
        jitter: {
          x: 0.24, // "noise units" for x-axis data
          y: 0
        },
        marker: {
          radius: 4, // width of the point
          symbol: "circle"
        },
        name: this.scoresName,
        tooltip: {
          pointFormat: this.tooltipFormat // label for the points
        },
        type: "scatter" // scatter plot
      };
    },
    scoresName() {
      switch (this.outcomeType) {
        case "TIME_ON_TASK":
          return "Time";
        case "AVERAGE_ASSIGNMENT_SCORE":
        case "STANDARD":
        default:
          return "Percentage";
      }
    },
    series() {
      return [
        // data for graphs, this order is important!
        this.means,
        this.scores
      ]
    },
    tooltipFormat() {
      switch (this.outcomeType) {
        case "TIME_ON_TASK":
          return "{point.custom.time}";
        case "AVERAGE_ASSIGNMENT_SCORE":
        case "STANDARD":
        default:
          return "{point.y}";
      }
    },
    xAxis() {
      return {
        categories: [...new Set(this.computedGraphData.map((cgd) => cgd.title))], // x-axis labels
        title: {
          style: {
            color: "#fff",
            fontSize: 0.0
          },
          text: this.chartDataType
        }
      }
    },
    yAxis() {
      switch (this.outcomeType) {
        case "TIME_ON_TASK": {
          let allScores = [];
          // add all scores to a new array; converted to minutes
          this.computedGraphData.forEach(cgd => allScores.push(...(cgd.scores.map((s) => Math.ceil(milliToMinutes(s))))));
          // min yAxis value
          let min = allScores.length > 0 ? allScores.reduce((prev, curr) => prev < curr ? prev : curr) : min;
          min = min !== 0 ? min - 1 : min;
          return {
            min: min,
            max: (allScores.length > 0 ? allScores.reduce((prev, curr) => prev > curr ? prev : curr) : 99) + 1, // max yAxis value
            title: {
              text: "Time (minutes)"
            }
          }
        }
        case "AVERAGE_ASSIGNMENT_SCORE":
        case "STANDARD":
          return {
            min: null,
            max: null,
            title: {
              text: "Percentage"
            }
          }
        default:
          return {
            min: 0,
            max: 100,
            title: {
              text: ""
            }
          }
      }
    }
  },
  watch: {
    graphData: {
      handler() {
        this.resetChart();
      },
      deep: true
    }
  },
  methods: {
    createChart() {
      this.chart = Highcharts.chart(
        "container-chart",
        this.options
      );
      this.chart.update({
        series: [{
          marker: {
            // set calculated mean line width dynamically
            radius: this.chart.series[0].xAxis.width / this.chart.series[0].xAxis.categories.length / 4
          }
        }],
        yAxis: this.yAxis
      });
    },
    destroyChart() {
      if (this.chart) {
        this.chart.destroy();
      }
    },
    resetChart() {
      this.destroyChart();
      this.createChart();
    }
  },
  mounted() {
    this.createChart();
  }
}

Highcharts.Renderer.prototype.symbols.meanLine = function(x, y, w, h) {
  // custom SVG mean data point line
  return ['M',x,y + w / 2,'L',x + h,y + w / 2];
};

Highcharts.Renderer.prototype.symbols.download = function (x, y, w, h) {
  // custom download button
  return [
    // Arrow stem
    'M', x + w * 0.5, y,
    'L', x + w * 0.5, y + h * 0.7,
    // Arrow head
    'M', x + w * 0.3, y + h * 0.5,
    'L', x + w * 0.5, y + h * 0.7,
    'L', x + w * 0.7, y + h * 0.5,
    // Box
    'M', x, y + h * 0.9,
    'L', x, y + h,
    'L', x + w, y + h,
    'L', x + w, y + h * 0.9
  ];
};
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
