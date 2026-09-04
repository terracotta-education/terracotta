import { describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";
import { flushPromises } from "@vue/test-utils";

import { mountComponent } from "@/test-utils/mount";
import { experiment as useExperimentStore } from "@/store/experiment.module";
import { resultsDashboard as useResultsDashboardStore } from "@/store/dashboard/results.module";
import Overview from "./Overview.vue";

// Stub keys are matched against each child SFC's own filename-derived component
// identity (e.g. Conditions.vue / Assignments.vue), not the "name" passed to
// defineOptions() inside those files, and not the local import alias used here
// - confirmed empirically since findComponent/stub matching in this Vue 3 +
// vue-test-utils setup resolves via that filename identity.
const stubs = {
  PageLoading: true,
  Conditions: true,
  Assignments: true,
  ResultsOverviewSummary: true
};

const mockOverview = overview => {
  const pinia = createPinia();
  setActivePinia(pinia);

  useExperimentStore().experiment = { experimentId: 7 };

  const store = useResultsDashboardStore();
  store.getOverview = vi.fn(async experimentId => {
    store.resultsDashboard = { experimentId, overview, outcomes: null };
    return overview;
  });

  return pinia;
};

const mountOverview = async overview => {
  const pinia = mockOverview(overview);
  const wrapper = mountComponent(Overview, { pinia, global: { stubs } });

  await flushPromises();

  return wrapper;
};

describe("Overview (ResultsOverview)", () => {
  it("shows the page loading indicator before the overview data resolves", () => {
    const pinia = mockOverview({ assignments: { rows: [] }, conditions: { rows: [] } });
    const wrapper = mountComponent(Overview, { pinia, global: { stubs } });

    const loading = wrapper.findComponent({ name: "PageLoading" });

    expect(loading.props("display")).toBe(true);
    expect(wrapper.findComponent({ name: "ResultsOverviewSummary" }).exists()).toBe(false);
  });

  it("fetches the overview for the current experiment on mount", async () => {
    const pinia = mockOverview({ assignments: { rows: [] }, conditions: { rows: [] } });
    mountComponent(Overview, { pinia, global: { stubs } });

    await flushPromises();

    expect(useResultsDashboardStore().getOverview).toHaveBeenCalledWith(7);
  });

  it("renders the summary, conditions and assignments sections once loaded, passing through their data", async () => {
    const conditionsData = { rows: [{ title: "Condition A", submissionCount: 3 }] };
    const assignmentsData = { rows: [{ title: "Assignment A", open: true, submissionCount: 2 }] };

    const wrapper = await mountOverview({
      assignments: assignmentsData,
      conditions: conditionsData
    });

    expect(wrapper.findComponent({ name: "PageLoading" }).exists()).toBe(false);
    expect(wrapper.findComponent({ name: "ResultsOverviewSummary" }).exists()).toBe(true);
    expect(
      wrapper.findComponent({ name: "Conditions" }).props("conditionsData")
    ).toEqual(conditionsData);
    expect(
      wrapper.findComponent({ name: "Assignments" }).props("assignmentsData")
    ).toEqual(assignmentsData);
  });

  it("shows a not-yet-open alert when none of the assignments are open", async () => {
    const wrapper = await mountOverview({
      assignments: { rows: [{ title: "A", open: false, submissionCount: 0 }] },
      conditions: { rows: [{ title: "C", submissionCount: 0 }] }
    });

    expect(wrapper.text()).toContain(
      "These components are not yet open, and are not yet collecting submissions."
    );
  });

  it("shows a plain collecting-submissions alert when all assignments/conditions have submissions", async () => {
    const wrapper = await mountOverview({
      assignments: { rows: [{ title: "A", open: true, submissionCount: 5 }] },
      conditions: { rows: [{ title: "C", submissionCount: 5 }] }
    });

    const text = wrapper.text();

    expect(text).toContain("You are currently collecting component submissions.");
    expect(text).not.toContain("do not yet have submissions");
  });

  it("calls out components specifically when only assignment submissions are missing", async () => {
    const wrapper = await mountOverview({
      assignments: { rows: [{ title: "A", open: true, submissionCount: 0 }, { title: "B", open: true, submissionCount: 4 }] },
      conditions: { rows: [{ title: "C", submissionCount: 5 }] }
    });

    expect(wrapper.text()).toContain("Some components do not yet have submissions.");
  });

  it("hides the alert entirely when assignments are open but nothing has submissions yet", async () => {
    const wrapper = await mountOverview({
      assignments: { rows: [{ title: "A", open: true, submissionCount: 0 }] },
      conditions: { rows: [{ title: "C", submissionCount: 0 }] }
    });

    expect(wrapper.find(".alert-assignments").exists()).toBe(false);
  });
});
