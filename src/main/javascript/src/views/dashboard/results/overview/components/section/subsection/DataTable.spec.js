import { describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import DataTable from "./DataTable.vue";

const baseRow = {
  title: "Component 1",
  submissionCount: 12,
  submissionRate: 1.2345,
  averageGrade: 0.8765,
  standardDeviation: 0.1234
};

describe("DataTable", () => {
  it("renders the custom title header text", () => {
    const wrapper = mountComponent(DataTable, {
      props: {
        tableData: [baseRow],
        titleHeader: "Condition Name"
      }
    });

    expect(wrapper.text()).toContain("Condition Name");
  });

  it("renders row values, using round() and percent() for rate/grade/sd", () => {
    const wrapper = mountComponent(DataTable, {
      props: { tableData: [baseRow] }
    });

    const text = wrapper.text();

    // round(1.2345) => 1.23
    expect(text).toContain("1.23");
    // percent(0.8765) => 88
    expect(text).toContain("88%");
    // percent(0.1234) => 12
    expect(text).toContain("12%");
    expect(text).toContain("12"); // submissionCount
  });

  it("shows an em dash and tooltip instead of a percentage when averageGrade is negative", () => {
    const wrapper = mountComponent(DataTable, {
      props: {
        tableData: [
          {
            ...baseRow,
            averageGrade: -1
          }
        ]
      }
    });

    expect(wrapper.text()).toContain("—");
    expect(wrapper.findComponent({ name: "ToolTip" }).exists()).toBe(true);
  });

  it("shows the no-submissions message and hides data cells when submissionCount is 0", () => {
    const wrapper = mountComponent(DataTable, {
      props: {
        tableData: [
          {
            title: "Empty component",
            submissionCount: 0
          }
        ],
        noSubmissionsMessage: "Nothing to see here"
      }
    });

    expect(wrapper.text()).toContain("Nothing to see here");
    expect(wrapper.text()).not.toContain("undefined");
  });

  it("falls back to N/A for the no-submissions message when none is provided", () => {
    const wrapper = mountComponent(DataTable, {
      props: {
        tableData: [
          {
            title: "Empty component",
            submissionCount: 0
          }
        ]
      }
    });

    expect(wrapper.text()).toContain("N/A");
  });

  it("shows a 'Only One Version' chip when a row has exactly one treatment", () => {
    const wrapper = mountComponent(DataTable, {
      props: {
        tableData: [
          {
            ...baseRow,
            treatments: { rows: [{ id: 1 }] }
          }
        ]
      }
    });

    expect(wrapper.text()).toContain("Only One Version");
  });

  it("does not show the 'Only One Version' chip when there are multiple treatments", () => {
    const wrapper = mountComponent(DataTable, {
      props: {
        tableData: [
          {
            ...baseRow,
            treatments: { rows: [{ id: 1 }, { id: 2 }] }
          }
        ]
      }
    });

    expect(wrapper.text()).not.toContain("Only One Version");
  });

  it("renders an expand toggle button when showExpand is true and there are multiple treatments", () => {
    const wrapper = mountComponent(DataTable, {
      props: {
        tableData: [
          {
            ...baseRow,
            treatments: { rows: [{ id: 1 }, { id: 2 }] }
          }
        ],
        showExpand: true
      }
    });

    expect(wrapper.find(".v-data-table__expand-icon").exists()).toBe(true);
  });

  it("does not render an expand toggle button for a single treatment even with showExpand", () => {
    const wrapper = mountComponent(DataTable, {
      props: {
        tableData: [
          {
            ...baseRow,
            treatments: { rows: [{ id: 1 }] }
          }
        ],
        showExpand: true
      }
    });

    expect(wrapper.find(".v-data-table__expand-icon").exists()).toBe(false);
  });

  it("renders the note about consenting students only when includeNote is true", () => {
    const withNote = mountComponent(DataTable, {
      props: { tableData: [baseRow], includeNote: true }
    });
    const withoutNote = mountComponent(DataTable, {
      props: { tableData: [baseRow], includeNote: false }
    });

    expect(withNote.text()).toContain("Only includes data from consenting students");
    expect(withoutNote.text()).not.toContain("Only includes data from consenting students");
  });

  it("renders a submissionRate tooltip in the header when a matching tooltip config is supplied", () => {
    const wrapper = mountComponent(DataTable, {
      props: {
        tableData: [baseRow],
        tooltips: [
          {
            id: "submissionRate",
            header: "Custom header",
            message: "Custom message"
          }
        ]
      }
    });

    const tooltip = wrapper.findComponent({ name: "ToolTip" });

    expect(tooltip.exists()).toBe(true);
    expect(tooltip.props("header")).toBe("Custom header");
    expect(tooltip.props("content")).toBe("Custom message");
    expect(tooltip.props("size")).toBe("compact");
  });

  it("defaults to an empty table when no tableData is provided", () => {
    const wrapper = mountComponent(DataTable, { props: {} });

    expect(wrapper.findComponent({ name: "VDataTable" }).exists()).toBe(true);
  });
});
