import { describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import Conditions from "./Conditions.vue";

const conditionsData = {
  rows: [
    {
      title: "Condition A",
      submissionCount: 10,
      submissionRate: 0.8,
      averageGrade: 0.9123,
      standardDeviation: 0.045
    },
    {
      title: "Condition B",
      submissionCount: 0,
      submissionRate: 0,
      averageGrade: 0,
      standardDeviation: 0
    }
  ]
};

describe("OverviewConditionsSection", () => {
  it("renders the section heading", () => {
    const wrapper = mountComponent(Conditions, {
      props: { conditionsData }
    });

    expect(wrapper.find("h3").text()).toBe("Conditions");
  });

  it("passes condition rows through to the nested data table", () => {
    const wrapper = mountComponent(Conditions, {
      props: { conditionsData }
    });

    const dataTable = wrapper.findComponent({ name: "DataTable" });

    expect(dataTable.exists()).toBe(true);
    expect(dataTable.props("tableData")).toEqual(conditionsData.rows);
    expect(dataTable.props("titleHeader")).toBe("Condition Name");
  });

  it("renders each condition row's title in the table", () => {
    const wrapper = mountComponent(Conditions, {
      props: { conditionsData }
    });

    expect(wrapper.text()).toContain("Condition A");
    expect(wrapper.text()).toContain("Condition B");
  });

  it("falls back to an empty row set when conditionsData is not provided", () => {
    const wrapper = mountComponent(Conditions, { props: {} });

    const dataTable = wrapper.findComponent({ name: "DataTable" });

    expect(dataTable.props("tableData")).toEqual([]);
  });

  it("falls back to an empty row set when conditionsData has no rows", () => {
    const wrapper = mountComponent(Conditions, {
      props: { conditionsData: {} }
    });

    const dataTable = wrapper.findComponent({ name: "DataTable" });

    expect(dataTable.props("tableData")).toEqual([]);
  });
});
