import { describe, expect, it } from "vitest";
import { createPinia, setActivePinia } from "pinia";

import { mountComponent } from "@/test-utils/mount";
import { resultsDashboard as useResultsDashboardStore } from "@/store/dashboard/results.module";
import Summary from "./Summary.vue";

function mountWithOverview(overview) {
  const pinia = createPinia();
  setActivePinia(pinia);
  useResultsDashboardStore().resultsDashboard = {
    experimentId: 1,
    overview,
    outcomes: null
  };

  return mountComponent(Summary, { pinia });
}

describe("ResultsOverviewSummary", () => {
  it("passes participants, assignment count, condition count, and exposure type to the child summaries", () => {
    const wrapper = mountWithOverview({
      participants: { count: 25, consentRate: 0.5, assignmentCount: 4 },
      conditions: {
        exposureType: "BETWEEN",
        rows: [
          { title: "Condition A" },
          { title: "Condition B" },
          { title: "Components with only one version" }
        ]
      }
    });

    expect(wrapper.findComponent({ name: "OverviewParticipantsSummary" }).props("participantsData")).toEqual({
      count: 25,
      consentRate: 0.5,
      assignmentCount: 4
    });
    expect(wrapper.findComponent({ name: "OverviewAssignmentsSummary" }).props("assignmentCount")).toBe(4);
    expect(wrapper.findComponent({ name: "OverviewExposuresSummary" }).props("exposureType")).toBe("BETWEEN");
  });

  it("excludes the 'Components with only one version' row from the condition count", () => {
    const wrapper = mountWithOverview({
      participants: {},
      conditions: {
        rows: [
          { title: "Condition A" },
          { title: "Condition B" },
          { title: "Components with only one version" }
        ]
      }
    });

    expect(wrapper.findComponent({ name: "OverviewConditionsSummary" }).props("conditionCount")).toBe(2);
  });

  it("defaults all values to safe empty states when there is no overview data", () => {
    const wrapper = mountWithOverview(null);

    expect(wrapper.findComponent({ name: "OverviewParticipantsSummary" }).props("participantsData")).toEqual({});
    expect(wrapper.findComponent({ name: "OverviewAssignmentsSummary" }).props("assignmentCount")).toBe(0);
    expect(wrapper.findComponent({ name: "OverviewConditionsSummary" }).props("conditionCount")).toBe(0);
    expect(wrapper.findComponent({ name: "OverviewExposuresSummary" }).props("exposureType")).toBeUndefined();
  });
});
