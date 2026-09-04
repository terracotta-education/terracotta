import { describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import DataTable from "./DataTable.vue";

describe("DataTable (outcomes output table)", () => {
  it("renders the title header plus the N, mean and standard deviation columns", () => {
    const wrapper = mountComponent(DataTable, {
      props: {
        tableData: [],
        titleHeader: "Condition",
        outcomeType: "STANDARD"
      }
    });

    const headerText = wrapper.findAll("th").map(th => th.text());

    expect(headerText).toEqual(["Condition", "N", "Mean", "Standard deviation"]);
  });

  it("renders each row as a percentage for STANDARD/AVERAGE_ASSIGNMENT_SCORE outcome types", () => {
    const wrapper = mountComponent(DataTable, {
      props: {
        tableData: [
          { title: "Condition A", number: 12, mean: 0.5, standardDeviation: 0.1 },
          { title: "Condition B", number: 8, mean: 0.75, standardDeviation: 0.2 }
        ],
        titleHeader: "Condition",
        outcomeType: "STANDARD"
      }
    });

    const rows = wrapper.findAll("tbody tr");

    expect(rows).toHaveLength(2);
    expect(rows[0].text()).toContain("Condition A");
    expect(rows[0].text()).toContain("12");
    expect(rows[0].text()).toContain("50%");
    expect(rows[0].text()).toContain("10%");
    expect(rows[1].text()).toContain("75%");
    expect(rows[1].text()).toContain("20%");
  });

  it("renders each row using time formatting for TIME_ON_TASK outcome type", () => {
    const wrapper = mountComponent(DataTable, {
      props: {
        tableData: [
          { title: "Exposure A", number: 4, mean: 65000, standardDeviation: 5000 }
        ],
        titleHeader: "Exposure",
        outcomeType: "TIME_ON_TASK"
      }
    });

    const rows = wrapper.findAll("tbody tr");

    expect(rows[0].text()).toContain("1m 5s");
    expect(rows[0].text()).toContain("5s");
  });

  it("renders an empty state when there is no table data", () => {
    const wrapper = mountComponent(DataTable, {
      props: {
        tableData: [],
        titleHeader: "Condition",
        outcomeType: "STANDARD"
      }
    });

    expect(wrapper.findAll("tbody tr td").length).toBeGreaterThan(0);
    expect(wrapper.find("tbody").text().toLowerCase()).toContain("no data");
  });
});
