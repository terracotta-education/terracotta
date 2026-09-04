import { describe, expect, it } from "vitest";
import { createPinia, setActivePinia } from "pinia";

import { mountComponent } from "@/test-utils/mount";
import { assignment as useAssignmentStore } from "@/store/assignment.module";
import { condition as useConditionStore } from "@/store/condition.module";
import { experiment as useExperimentStore } from "@/store/experiment.module";
import { exposures as useExposuresStore } from "@/store/exposures.module";
import DataTableTreatment from "./DataTableTreatment.vue";

const headers = [
  { title: "Title", key: "title" },
  { title: "Submissions", key: "submissionCount" },
  { title: "Rate", key: "submissionRate" },
  { title: "Grade", key: "averageGrade" },
  { title: "SD", key: "standardDeviation" }
];

function setupStores({ conditions, exposures } = {}) {
  const pinia = createPinia();
  setActivePinia(pinia);

  useExperimentStore().experiment = { conditions: conditions || [] };
  useExposuresStore().exposures = exposures || [];

  return pinia;
}

describe("DataTableTreatment", () => {
  it("renders each treatment's title and formatted values", () => {
    const pinia = setupStores({
      conditions: [{ conditionId: 1 }, { conditionId: 2 }]
    });

    useAssignmentStore().assignments = [];

    const item = {
      treatments: {
        rows: [
          {
            id: 1,
            title: "Treatment 1",
            submissionCount: 5,
            submissionRate: 0.5,
            averageGrade: 0.72,
            standardDeviation: 0.03
          }
        ]
      }
    };

    const wrapper = mountComponent(DataTableTreatment, {
      props: { headers, item },
      pinia
    });

    const text = wrapper.text();

    expect(text).toContain("Treatment 1");
    expect(text).toContain("5");
    // percent(0.72) => 72
    expect(text).toContain("72%");
  });

  it("falls back to 'Treatment' as the title when none is set", () => {
    const pinia = setupStores();

    useAssignmentStore().assignments = [];

    const item = {
      treatments: {
        rows: [{ id: 1, submissionCount: 1, submissionRate: 1, averageGrade: 0.5, standardDeviation: 0 }]
      }
    };

    const wrapper = mountComponent(DataTableTreatment, {
      props: { headers, item },
      pinia
    });

    expect(wrapper.text()).toContain("Treatment");
  });

  it("shows an em dash and tooltip when averageGrade is negative", () => {
    const pinia = setupStores();

    useAssignmentStore().assignments = [];

    const item = {
      treatments: {
        rows: [{ id: 1, title: "T1", submissionCount: 1, submissionRate: 1, averageGrade: -1, standardDeviation: 0 }]
      }
    };

    const wrapper = mountComponent(DataTableTreatment, {
      props: { headers, item },
      pinia
    });

    expect(wrapper.text()).toContain("—");
    expect(wrapper.findComponent({ name: "ToolTip" }).exists()).toBe(true);
  });

  it("does not show a condition chip in a single-condition experiment", () => {
    const pinia = setupStores({
      conditions: [{ conditionId: 1 }]
    });

    useAssignmentStore().assignments = [
      { assignmentId: 10, exposureId: 100, treatments: [{ conditionId: 1 }] }
    ];

    const item = {
      treatments: {
        rows: [
          {
            id: 1,
            title: "T1",
            assignmentId: 10,
            conditionId: 1,
            submissionCount: 1,
            submissionRate: 1,
            averageGrade: 0.5,
            standardDeviation: 0
          }
        ]
      }
    };

    const wrapper = mountComponent(DataTableTreatment, {
      props: { headers, item },
      pinia
    });

    expect(wrapper.find(".v-chip").exists()).toBe(false);
  });

  it("shows a condition chip with the mapped color/name in a multi-condition experiment", () => {
    const pinia = setupStores({
      conditions: [{ conditionId: 1 }, { conditionId: 2 }]
    });

    useExposuresStore().exposures = [
      {
        exposureId: 100,
        groupConditionList: [
          { conditionId: 1, conditionName: "Condition A" },
          { conditionId: 2, conditionName: "Condition B" }
        ]
      }
    ];

    useAssignmentStore().assignments = [
      {
        assignmentId: 10,
        exposureId: 100,
        treatments: [{ conditionId: 1 }, { conditionId: 2 }]
      }
    ];

    const item = {
      treatments: {
        rows: [
          {
            id: 1,
            title: "T1",
            assignmentId: 10,
            conditionId: 1,
            submissionCount: 1,
            submissionRate: 1,
            averageGrade: 0.5,
            standardDeviation: 0
          }
        ]
      }
    };

    const wrapper = mountComponent(DataTableTreatment, {
      props: { headers, item },
      pinia
    });

    const chip = wrapper.find(".v-chip");

    expect(chip.exists()).toBe(true);
    expect(chip.text()).toBe("Condition A");
    expect(useConditionStore().conditionColorMapping["Condition A"]).toBe("#FFCCBC");
  });
});
