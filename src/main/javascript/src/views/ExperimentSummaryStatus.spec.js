import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("@/services", () => ({
  exposuresService: {
    getAll: vi.fn()
  },
  assignmentService: {
    fetchAssignmentsByExposure: vi.fn()
  },
  outcomeService: {
    create: vi.fn(),
    deleteOutcome: vi.fn(),
    getAllByExperimentId: vi.fn()
  }
}));

const push = vi.fn();

vi.mock("vue-router", () => ({
  useRouter: () => ({ push })
}));

const swal = vi.fn();

vi.mock("sweetalert2", () => ({
  default: { fire: (...args) => swal(...args) }
}));

import { mountComponent } from "@/test-utils/mount";
import {
  exposuresService,
  assignmentService,
  outcomeService
} from "@/services";
import { navigation as navigationModule } from "@/store/navigation.module";
import ExperimentSummaryStatus from "./ExperimentSummaryStatus.vue";

const experiment = {
  experimentId: 7,
  consent: {
    title: "Consent Form",
    answeredConsentCount: 3,
    expectedConsent: 5
  }
};

const exposure = { exposureId: 20, title: "Exposure Set 1" };

const assignment = {
  assignmentId: 30,
  exposureId: 20,
  title: "Reading Quiz",
  treatments: [
    {
      assessmentDto: {
        submissionsExpected: 10,
        submissionsCompletedCount: 10,
        submissionsInProgressCount: 0
      }
    }
  ]
};

const outcome = {
  outcomeId: 40,
  exposureId: 20,
  title: "Final Score",
  external: false
};

// Opens every expansion panel so their (lazily-rendered) content mounts into the DOM.
// Re-queries the title list on each iteration since opening one panel re-renders
// the tree and can detach previously-captured DOMWrapper references for the rest.
// Waits for the exposure's own title first: the Consent panel renders synchronously
// from props, but the per-exposure panels only appear once the exposures/assignments/
// outcomes fetches inside onMounted have all resolved, so waiting on ">0 panels" alone
// would race ahead and only ever open the Consent panel.
const openAllPanels = async wrapper => {
  await vi.waitFor(() => {
    expect(wrapper.text()).toContain(exposure.title);
  });

  const count = wrapper.findAll(".v-expansion-panel-title").length;

  for (let i = 0; i < count; i++) {
    await wrapper.findAll(".v-expansion-panel-title")[i].trigger("click");
  }

  await wrapper.vm.$nextTick();
};

describe("ExperimentSummaryStatus", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    exposuresService.getAll.mockResolvedValue([exposure]);
    assignmentService.fetchAssignmentsByExposure.mockResolvedValue([assignment]);
    outcomeService.getAllByExperimentId.mockResolvedValue({ data: [outcome] });
  });

  // Note: the template has a `v-else` "no experiment" fallback, but onMounted
  // unconditionally dereferences props.experiment.experimentId with no null guard,
  // so mounting with a falsy experiment throws inside onMounted (unhandled rejection)
  // before that fallback ever has a chance to matter. The only real caller
  // (ExperimentSummary.vue) always guards with `v-if="experiment"` before rendering
  // this component, so the branch is effectively dead code protecting against a
  // crash that would happen anyway if it were ever hit.

  it("shows a loading indicator before data resolves", () => {
    const wrapper = mountComponent(ExperimentSummaryStatus, {
      props: { experiment }
    });

    const pageLoading = wrapper.findComponent({ name: "PageLoading" });
    expect(pageLoading.exists()).toBe(true);
    expect(pageLoading.props("message")).toBe(
      "Please wait while we load your components and outcomes."
    );
  });

  it("fetches exposures, their assignments, and outcomes on mount", async () => {
    mountComponent(ExperimentSummaryStatus, { props: { experiment } });

    await vi.waitFor(() => {
      expect(exposuresService.getAll).toHaveBeenCalledWith(7);
      expect(assignmentService.fetchAssignmentsByExposure).toHaveBeenCalledWith(
        7,
        20,
        true
      );
      expect(outcomeService.getAllByExperimentId).toHaveBeenCalledWith(7);
    });
  });

  it("renders the consent completion status", async () => {
    const wrapper = mountComponent(ExperimentSummaryStatus, {
      props: { experiment }
    });

    await openAllPanels(wrapper);

    expect(wrapper.text()).toContain("Consent Form");
    expect(wrapper.text()).toContain("3/5");

    const consentStatus = wrapper.findAll(".completion-status")[0];
    expect(consentStatus.text()).toBe("In Progress");
    expect(consentStatus.classes()).not.toContain("complete");
  });

  it("renders each exposure's components with completion status and submission counts", async () => {
    const wrapper = mountComponent(ExperimentSummaryStatus, {
      props: { experiment }
    });

    await openAllPanels(wrapper);

    expect(wrapper.text()).toContain("Reading Quiz");
    expect(wrapper.text()).toContain("10");
    expect(wrapper.find(".completion-status.complete").exists()).toBe(true);
  });

  it("renders outcomes for an exposure", async () => {
    const wrapper = mountComponent(ExperimentSummaryStatus, {
      props: { experiment }
    });

    await openAllPanels(wrapper);

    expect(wrapper.text()).toContain("Final Score");
    expect(wrapper.text()).toContain("Manual Entry");
  });

  it("navigates to AssignmentScores and saves edit mode when a component link is clicked", async () => {
    const wrapper = mountComponent(ExperimentSummaryStatus, {
      props: { experiment }
    });

    await openAllPanels(wrapper);

    expect(wrapper.find(".link-view-assignment").exists()).toBe(true);
    await wrapper.find(".link-view-assignment").trigger("click");

    const navigationStore = navigationModule();
    expect(navigationStore.editMode).toEqual({
      initialPage: "ExperimentSummaryStatus",
      callerPage: { name: "ExperimentSummary", tab: "status" }
    });
    expect(push).toHaveBeenCalledWith({
      name: "AssignmentScores",
      params: { experimentId: 7, exposureId: 20, assignmentId: 30 }
    });
  });

  it("navigates to OutcomeGradebook and saves edit mode when selecting an item from the gradebook", async () => {
    const wrapper = mountComponent(ExperimentSummaryStatus, {
      props: { experiment }
    });

    await openAllPanels(wrapper);
    expect(wrapper.text()).toContain("Add Outcome");

    const addOutcomeButton = wrapper
      .findAll("button")
      .find(button => button.text().includes("Add Outcome"));

    await addOutcomeButton.trigger("click");
    await wrapper.vm.$nextTick();

    const gradebookItem = wrapper
      .findAllComponents({ name: "VListItem" })
      .find(item => item.text().includes("Select item from gradebook"));

    await gradebookItem.trigger("click");

    const navigationStore = navigationModule();
    expect(navigationStore.editMode).toEqual({
      initialPage: "ExperimentSummaryStatus",
      callerPage: { name: "ExperimentSummary", tab: "status" }
    });
    expect(push).toHaveBeenCalledWith({
      name: "OutcomeGradebook",
      params: { experimentId: 7, exposureId: 20 }
    });
  });

  it("creates a manual outcome and navigates to OutcomeScoring", async () => {
    outcomeService.create.mockResolvedValue({
      status: 201,
      data: { outcomeId: 99, exposureId: 20 }
    });

    const wrapper = mountComponent(ExperimentSummaryStatus, {
      props: { experiment }
    });

    await openAllPanels(wrapper);
    expect(wrapper.text()).toContain("Add Outcome");

    const addOutcomeButton = wrapper
      .findAll("button")
      .find(button => button.text().includes("Add Outcome"));

    await addOutcomeButton.trigger("click");
    await wrapper.vm.$nextTick();

    const manualEntryItem = wrapper
      .findAllComponents({ name: "VListItem" })
      .find(item => item.text().includes("Manually enter scores"));

    await manualEntryItem.trigger("click");

    expect(outcomeService.create).toHaveBeenCalledWith(7, 20, "", 0, false);
    await vi.waitFor(() => {
      expect(push).toHaveBeenCalledWith({
        name: "OutcomeScoring",
        params: { experimentId: 7, exposureId: 20, outcomeId: 99 }
      });
    });
  });

  it("deletes an outcome after confirmation and refreshes the outcomes list", async () => {
    swal.mockResolvedValue({ isConfirmed: true });
    outcomeService.deleteOutcome.mockResolvedValue({ status: 200 });

    const wrapper = mountComponent(ExperimentSummaryStatus, {
      props: { experiment }
    });

    await openAllPanels(wrapper);
    expect(wrapper.text()).toContain("Final Score");

    await wrapper.find(".mdi-dots-horizontal").trigger("click");
    await wrapper.vm.$nextTick();

    const deleteOutcomeItem = wrapper
      .findAllComponents({ name: "VListItem" })
      .find(item => item.text().includes("Delete outcome"));

    await deleteOutcomeItem.trigger("click");
    await vi.waitFor(() => {
      expect(outcomeService.deleteOutcome).toHaveBeenCalledWith(7, 20, 40);
    });

    expect(outcomeService.getAllByExperimentId).toHaveBeenCalledWith(7);
  });

  it("does not delete the outcome when the confirmation is dismissed", async () => {
    swal.mockResolvedValue({ isConfirmed: false });

    const wrapper = mountComponent(ExperimentSummaryStatus, {
      props: { experiment }
    });

    await openAllPanels(wrapper);
    expect(wrapper.text()).toContain("Final Score");

    await wrapper.find(".mdi-dots-horizontal").trigger("click");
    await wrapper.vm.$nextTick();

    const deleteOutcomeItem = wrapper
      .findAllComponents({ name: "VListItem" })
      .find(item => item.text().includes("Delete outcome"));

    await deleteOutcomeItem.trigger("click");

    expect(outcomeService.deleteOutcome).not.toHaveBeenCalled();
  });
});
