import { describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import Assignments from "./Assignments.vue";

describe("OverviewAssignmentsSummary", () => {
  it("passes the assignment count through to SummaryData with the 'Components' title", () => {
    const wrapper = mountComponent(Assignments, {
      props: { assignmentCount: 7 }
    });

    const summaryData = wrapper.findComponent({ name: "SummaryCount" });

    expect(summaryData.exists()).toBe(true);
    expect(summaryData.props("title")).toBe("Components");
    expect(summaryData.props("value")).toBe(7);
  });

  it("defaults the count to 0 when assignmentCount is not provided", () => {
    const wrapper = mountComponent(Assignments, { props: {} });

    const summaryData = wrapper.findComponent({ name: "SummaryCount" });

    expect(summaryData.props("value")).toBe(0);
  });
});
