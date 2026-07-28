import { describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import Assignments from "./Assignments.vue";

describe("Assignments (OverviewAssignmentsSection)", () => {
  it("renders the Components heading and passes rows through to the data table", () => {
    const wrapper = mountComponent(Assignments, {
      props: {
        assignmentsData: {
          rows: [
            {
              title: "Quiz 1",
              submissionCount: 10,
              submissionRate: 2,
              averageGrade: 0.85,
              standardDeviation: 0.12
            }
          ]
        }
      }
    });

    expect(wrapper.find("h3").text()).toBe("Components");

    const headerText = wrapper.findAll("th").map(th => th.text());
    expect(headerText[0]).toBe("Component name");

    const row = wrapper.findAll("tbody tr")[0];
    expect(row.text()).toContain("Quiz 1");
    expect(row.text()).toContain("10");
    expect(row.text()).toContain("85%");
    expect(row.text()).toContain("12%");
  });

  it("shows the custom no-submissions message for rows without submissions", () => {
    const wrapper = mountComponent(Assignments, {
      props: {
        assignmentsData: {
          rows: [{ title: "Quiz 2", submissionCount: 0 }]
        }
      }
    });

    const row = wrapper.findAll("tbody tr")[0];
    expect(row.text()).toContain("No submissions yet");
  });

  it("renders a tooltip on the submissions-per-participant header", () => {
    const wrapper = mountComponent(Assignments, {
      props: {
        assignmentsData: { rows: [] }
      }
    });

    const tooltip = wrapper.findComponent({ name: "ToolTip" });

    expect(tooltip.exists()).toBe(true);
    expect(wrapper.find('[aria-label="Submission rate tooltip"]').exists()).toBe(true);
  });

  it("handles missing assignmentsData gracefully", () => {
    const wrapper = mountComponent(Assignments, { props: {} });

    expect(wrapper.find("h3").text()).toBe("Components");
    expect(wrapper.findAll("tbody tr td").length).toBeGreaterThan(0);
  });
});
