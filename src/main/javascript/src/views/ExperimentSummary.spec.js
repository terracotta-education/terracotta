import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("@/services", () => ({
  experimentService: {
    getById: vi.fn(),
    export: vi.fn()
  },
  exposuresService: {
    getAll: vi.fn()
  },
  assignmentService: {
    fetchAssignmentsByExposure: vi.fn()
  },
  messageContainerService: {
    getAll: vi.fn()
  },
  experimentDataExportService: {
    pollList: vi.fn(),
    poll: vi.fn(),
    prepare: vi.fn(),
    retrieve: vi.fn(),
    acknowledge: vi.fn()
  },
  consentService: {
    getConsentFile: vi.fn()
  },
  treatmentService: {
    create: vi.fn()
  },
  assessmentService: {
    fetchAssessments: vi.fn(),
    createAssessment: vi.fn()
  }
}));

const push = vi.fn();

const { routeRef, onBeforeRouteUpdateMock } = vi.hoisted(() => ({
  routeRef: {
    params: { experimentId: "8" },
    name: "ExperimentSummary"
  },
  onBeforeRouteUpdateMock: vi.fn()
}));

vi.mock("vue-router", () => ({
  useRouter: () => ({ push }),
  useRoute: () => routeRef,
  onBeforeRouteUpdate: onBeforeRouteUpdateMock
}));

const swalFire = vi.fn();

vi.mock("sweetalert2", () => ({
  default: { fire: (...args) => swalFire(...args) }
}));

import { createPinia, setActivePinia } from "pinia";
import { mountComponent } from "@/test-utils/mount";
import {
  experimentService,
  exposuresService,
  assignmentService,
  messageContainerService,
  experimentDataExportService,
  consentService,
  treatmentService,
  assessmentService
} from "@/services";
import { navigation as navigationModule } from "@/store/navigation.module";
import { alert as alertModule } from "@/store/alert.module";
import { configuration as configurationModule } from "@/store/configuration.module";
import { treatment as treatmentModule } from "@/store/treatment.module";
import { assessment as assessmentModule } from "@/store/assessment.module";
import { EventBus } from "@/helpers/event-bus";
import ExperimentSummary from "./ExperimentSummary.vue";

// setTimeout-based flushPromises() can't resolve once vi.useFakeTimers() is active
// (its own macrotask never fires without an explicit advance). Promise microtasks
// are untouched by fake timers, so draining the microtask queue directly is the
// safe way to let pending native-Promise awaits (service mocks) settle.
function flushMicrotasks(times = 10) {
  let chain = Promise.resolve();
  for (let i = 0; i < times; i++) {
    chain = chain.then(() => Promise.resolve());
  }
  return chain;
}

const experiment = {
  experimentId: 8,
  title: "My Experiment",
  description: "A description",
  conditions: [
    { conditionId: 1, name: "A", defaultCondition: true },
    { conditionId: 2, name: "B" }
  ],
  exposureType: "WITHIN",
  participationType: "CONSENT",
  consent: {
    title: "Consent Doc",
    answeredConsentCount: 1,
    expectedConsent: 2
  },
  acceptedParticipants: 5,
  potentialParticipants: 10
};

const exposure = { exposureId: 60, groupConditionList: [] };

const stubs = {
  ExperimentAssignments: true,
  ExperimentSummaryStatus: true,
  ResultsDashboard: true,
  VuePdfEmbed: true
};

// Vuetify's v-window lazily renders each v-window-item - only the active tab's
// content is actually mounted into the DOM. To inspect a non-default tab's
// content/child components, switch to it first by clicking its v-tab.
const switchTab = async (wrapper, tabKey) => {
  const tab = wrapper
    .findAllComponents({ name: "VTab" })
    .find(candidate => candidate.text() === tabKey);
  await tab.trigger("click");
  await wrapper.vm.$nextTick();
};

let pinia;

const mountSummary = (options = {}) => {
  const { global: globalOptions = {}, ...rest } = options;

  return mountComponent(ExperimentSummary, {
    ...rest,
    pinia,
    global: {
      ...globalOptions,
      stubs: { ...stubs, ...(globalOptions.stubs || {}) }
    }
  });
};

describe("ExperimentSummary", () => {
  beforeEach(() => {
    // resetAllMocks (not clearAllMocks) - clearAllMocks only wipes call history, not
    // any mockResolvedValue/mockImplementation left behind by a previous test, which
    // made later tests order-dependent on whatever an earlier test happened to leave
    // on shared service mocks like experimentDataExportService.poll/retrieve.
    vi.resetAllMocks();
    swalFire.mockReset();
    routeRef.params = { experimentId: "8" };

    pinia = createPinia();
    setActivePinia(pinia);

    experimentService.getById.mockResolvedValue({
      status: 200,
      data: experiment
    });
    exposuresService.getAll.mockResolvedValue([exposure]);
    assignmentService.fetchAssignmentsByExposure.mockResolvedValue([]);
    messageContainerService.getAll.mockResolvedValue([]);
    experimentDataExportService.pollList.mockResolvedValue([]);
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("shows 'no experiment' before the experiment has loaded", () => {
    const wrapper = mountSummary();

    expect(wrapper.text()).toContain("no experiment");
  });

  it("fetches the experiment, its exposures, per-exposure assignments/messages, and polls data export requests on mount", async () => {
    mountSummary();

    await vi.waitFor(() => {
      expect(experimentService.getById).toHaveBeenCalledWith("8");
      expect(exposuresService.getAll).toHaveBeenCalledWith(8);
      expect(assignmentService.fetchAssignmentsByExposure).toHaveBeenCalledWith(
        8,
        60,
        true
      );
      expect(messageContainerService.getAll).toHaveBeenCalledWith(8, 60);
      expect(experimentDataExportService.pollList).toHaveBeenCalledWith(
        [8],
        false
      );
    });
  });

  it("renders the experiment title and setup tabs once loaded", async () => {
    const wrapper = mountSummary();

    await vi.waitFor(() => {
      expect(wrapper.text()).toContain("My Experiment");
    });

    // The tab labels render `item.tab` (the lowercase key), not `item.title` -
    // Vuetify applies the visual uppercase transform via CSS, not markup.
    const tabTitles = wrapper
      .findAllComponents({ name: "VTab" })
      .map(tab => tab.text());

    expect(tabTitles).toEqual([
      "design",
      "participant",
      "components",
      "status",
      "results"
    ]);
  });

  it("passes the loaded experiment down to ExperimentAssignments and ExperimentSummaryStatus", async () => {
    const wrapper = mountSummary();

    await vi.waitFor(() => {
      expect(
        wrapper.findComponent({ name: "ExperimentAssignments" }).exists()
      ).toBe(true);
    });

    expect(
      wrapper.findComponent({ name: "ExperimentAssignments" }).props("experiment")
    ).toMatchObject({ experimentId: 8 });

    await switchTab(wrapper, "status");

    expect(
      wrapper.findComponent({ name: "ExperimentSummaryStatus" }).props("experiment")
    ).toMatchObject({ experimentId: 8 });
  });

  // `loaded` (computed from isLoading) waits on the full fetchExperiment/
  // fetchExposures/fetchAssignmentsByExposure/messageContainerService.getAll
  // chain, not just the experiment itself - that's real network time, and until
  // it resolves the components tab's content area was otherwise just blank
  // where the components table would appear.
  it("shows a loading spinner in place of the components table while assignments/messages are still loading", async () => {
    let resolveAssignments;
    assignmentService.fetchAssignmentsByExposure.mockReturnValue(
      new Promise(resolve => { resolveAssignments = resolve; })
    );

    const wrapper = mountSummary();

    await vi.waitFor(() => {
      expect(wrapper.text()).toContain("My Experiment");
    });

    // "components" is the default tab (see the test below), so its content is
    // already mounted without needing switchTab.
    expect(wrapper.find(".spinner-container-assignments").exists()).toBe(true);
    expect(wrapper.text()).toContain("Please wait while we load your experiment components.");
    expect(wrapper.findComponent({ name: "ExperimentAssignments" }).exists()).toBe(false);

    resolveAssignments([]);

    await vi.waitFor(() => {
      expect(wrapper.findComponent({ name: "ExperimentAssignments" }).exists()).toBe(true);
    });

    expect(wrapper.find(".spinner-container-assignments").exists()).toBe(false);
  });

  it("defaults to the components tab and exposure set 0 without a saved edit mode", async () => {
    const wrapper = mountSummary();

    await vi.waitFor(() => {
      expect(
        wrapper.findComponent({ name: "ExperimentAssignments" }).exists()
      ).toBe(true);
    });

    expect(
      wrapper.findComponent({ name: "ExperimentAssignments" }).props("activeExposureSet")
    ).toBe(0);
  });

  it("restores the tab and exposure set from a saved edit-mode caller page", async () => {
    navigationModule().saveEditMode({
      initialPage: "ExperimentSummaryStatus",
      callerPage: { name: "ExperimentSummary", tab: "status", exposureSet: 3 }
    });

    const wrapper = mountSummary();

    await vi.waitFor(() => {
      expect(wrapper.text()).toContain("My Experiment");
    });

    // The "status" tab (from the saved caller page) should already be active,
    // so its window-item's content - not "components" - is what's rendered.
    expect(
      wrapper.findComponent({ name: "ExperimentSummaryStatus" }).exists()
    ).toBe(true);
    expect(
      wrapper.findComponent({ name: "ExperimentAssignments" }).exists()
    ).toBe(false);

    // saveEditMode(null) is called on mount to clear the caller page once consumed.
    expect(navigationModule().editMode).toBeNull();

    // exposureSet was still restored to 3 even though it's only consumed by
    // ExperimentAssignments (the "components" tab) - confirm by switching to it.
    await switchTab(wrapper, "components");

    expect(
      wrapper.findComponent({ name: "ExperimentAssignments" }).props("activeExposureSet")
    ).toBe(3);
  });

  it("saves the experiment and navigates home when Save & Exit is clicked", async () => {
    const wrapper = mountSummary();

    await vi.waitFor(() => {
      expect(wrapper.text()).toContain("My Experiment");
    });

    await wrapper.find(".saveButton").trigger("click");

    expect(push).toHaveBeenCalledWith({ name: "Home" });
    expect(alertModule().alertType).toBe("success");
  });

  it("navigates to the requested design editor and saves the caller page on Edit", async () => {
    const wrapper = mountSummary();

    await vi.waitFor(() => {
      expect(wrapper.text()).toContain("Experiment Title");
    });

    const editButtons = wrapper
      .findAll(".edit-section-link")
      .filter(button => button.exists());
    await editButtons[0].trigger("click");

    expect(push).toHaveBeenCalledWith({ name: "ExperimentDesignTitle" });
    expect(navigationModule().editMode).toMatchObject({
      initialPage: "ExperimentDesignTitle",
      callerPage: { name: "ExperimentSummary", tab: "design" }
    });
  });

  it("does not show the Export Experiment button when experiment export is disabled", async () => {
    const wrapper = mountSummary();

    await vi.waitFor(() => {
      expect(wrapper.text()).toContain("My Experiment");
    });

    expect(wrapper.text()).not.toContain("Export Experiment");
  });

  it("exports the experiment when Export Experiment is enabled and clicked", async () => {
    configurationModule().$patch({
      configurations: { experimentExportEnabled: true }
    });
    experimentService.export.mockResolvedValue({ status: 200 });

    const wrapper = mountSummary();

    await vi.waitFor(() => {
      expect(wrapper.text()).toContain("Export Experiment");
    });

    const exportButton = wrapper
      .findAll("button")
      .find(button => button.text() === "Export Experiment");
    await exportButton.trigger("click");

    await vi.waitFor(() => {
      expect(experimentService.export).toHaveBeenCalledWith(8);
    });
  });

  it("never counts message containers toward balance, regardless of version", async () => {
    const exposureA = { exposureId: 60, groupConditionList: [] };
    const exposureB = { exposureId: 61, groupConditionList: [] };
    exposuresService.getAll.mockResolvedValue([exposureA, exposureB]);

    configurationModule().$patch({
      configurations: { messagingEnabled: true }
    });

    assignmentService.fetchAssignmentsByExposure.mockResolvedValue([]);

    messageContainerService.getAll.mockImplementation((experimentId, exposureId) => {
      if (exposureId === exposureA.exposureId) {
        // a lopsided number of message containers, single- and multi-version -
        // none of it should ever affect balance, which is assignments-only
        return Promise.resolve([
          { id: 200, exposureId: exposureA.exposureId, messages: [{ id: 1 }, { id: 2 }] },
          { id: 201, exposureId: exposureA.exposureId, messages: [{ id: 3 }] },
          { id: 202, exposureId: exposureA.exposureId, messages: [{ id: 4 }, { id: 5 }] }
        ]);
      }

      if (exposureId === exposureB.exposureId) {
        return Promise.resolve([]);
      }

      return Promise.resolve([]);
    });

    const wrapper = mountSummary();

    await vi.waitFor(() => {
      expect(
        wrapper.findComponent({ name: "ExperimentAssignments" }).exists()
      ).toBe(true);
    });

    expect(
      wrapper.findComponent({ name: "ExperimentAssignments" }).props("balanced")
    ).toBe(true);
  });

  it("downloads and displays the consent PDF when the consent title button is clicked", async () => {
    consentService.getConsentFile.mockResolvedValue({
      status: 200,
      base: "data:application/pdf;base64,ZmFrZQ=="
    });

    const wrapper = mountSummary();

    await vi.waitFor(() => {
      expect(wrapper.text()).toContain("My Experiment");
    });

    // The consent title/button lives under the "participant" tab, which - like
    // all v-window items - is lazily rendered only once it becomes active.
    await switchTab(wrapper, "participant");
    expect(wrapper.text()).toContain("Consent Doc");

    await wrapper.find(".pdfButton").trigger("click");

    await vi.waitFor(() => {
      expect(consentService.getConsentFile).toHaveBeenCalledWith(8);
    });
  });

  it("prepares a data export after confirmation", async () => {
    experimentDataExportService.poll.mockResolvedValue(null);
    experimentDataExportService.prepare.mockResolvedValue({
      id: 1,
      experimentId: 8,
      status: "PROCESSING"
    });
    swalFire.mockResolvedValue({ isConfirmed: true });

    const wrapper = mountSummary();

    await vi.waitFor(() => {
      expect(wrapper.text()).toContain("Export Data");
    });

    const exportDataButton = wrapper
      .findAll("button")
      .find(button => button.text() === "Export Data");
    await exportDataButton.trigger("click");

    await vi.waitFor(() => {
      expect(experimentDataExportService.prepare).toHaveBeenCalledWith(8);
    });
  });

  it("re-fetches the experiment when the route guard fires with a new experimentId", async () => {
    mountSummary();

    await vi.waitFor(() => {
      expect(experimentService.getById).toHaveBeenCalledWith("8");
    });

    experimentService.getById.mockClear();
    const next = vi.fn();

    await onBeforeRouteUpdateMock.mock.calls[0][0](
      { params: { experimentId: "12" } },
      { params: { experimentId: "8" } },
      next
    );

    expect(experimentService.getById).toHaveBeenCalledWith("12");
    expect(next).toHaveBeenCalled();
  });

  it("treats the experiment as unbalanced when it has no exposures at all", async () => {
    exposuresService.getAll.mockResolvedValue([]);

    const wrapper = mountSummary();

    await vi.waitFor(() => {
      expect(
        wrapper.findComponent({ name: "ExperimentAssignments" }).exists()
      ).toBe(true);
    });

    expect(
      wrapper.findComponent({ name: "ExperimentAssignments" }).props("balanced")
    ).toBe(false);
  });

  it("marks exposure sets unbalanced when published multi-treatment assignment counts differ across exposures, and shows the published-note banner", async () => {
    const exposureA = { exposureId: 60, groupConditionList: [] };
    const exposureB = { exposureId: 61, groupConditionList: [] };
    exposuresService.getAll.mockResolvedValue([exposureA, exposureB]);

    assignmentService.fetchAssignmentsByExposure.mockImplementation(
      (experimentId, exposureId) => {
        if (exposureId === exposureA.exposureId) {
          return Promise.resolve([
            {
              assignmentId: 1,
              exposureId: exposureA.exposureId,
              published: true,
              treatments: [{ treatmentId: 1 }, { treatmentId: 2 }]
            }
          ]);
        }

        return Promise.resolve([]);
      }
    );

    const wrapper = mountSummary();

    await vi.waitFor(() => {
      expect(
        wrapper.findComponent({ name: "ExperimentAssignments" }).exists()
      ).toBe(true);
    });

    expect(
      wrapper.findComponent({ name: "ExperimentAssignments" }).props("balanced")
    ).toBe(false);
    expect(wrapper.find(".label-unbalanced").exists()).toBe(true);
    expect(wrapper.text()).toContain(
      "You are currently collecting component submissions"
    );
  });

  it("navigates to the design editor from both 'edit' links in the exposure set explanation", async () => {
    const wrapper = mountSummary();

    await vi.waitFor(() => {
      expect(wrapper.text()).toContain("exposure sets");
    });

    const editLinks = wrapper.findAll("a").filter(a => a.text().trim() === "edit");
    expect(editLinks.length).toBe(2);

    await editLinks[0].trigger("click");
    expect(push).toHaveBeenCalledWith({ name: "ExperimentDesignConditions" });

    push.mockClear();
    await editLinks[1].trigger("click");
    expect(push).toHaveBeenCalledWith({ name: "ExperimentDesignConditions" });
  });

  it("shows a ready data-export alert and falls back to the generic message once downloaded via the alert link", async () => {
    experimentDataExportService.pollList.mockResolvedValue([
      { id: 21, experimentId: 8, status: "READY", experimentTitle: "My Experiment" }
    ]);
    experimentDataExportService.poll.mockResolvedValue({
      id: 21, experimentId: 8, status: "READY", experimentTitle: "My Experiment"
    });
    experimentDataExportService.retrieve.mockResolvedValue({
      id: 21, experimentId: 8, status: "DOWNLOADED", experimentTitle: "My Experiment"
    });

    const wrapper = mountSummary();

    await vi.waitFor(() => {
      expect(wrapper.find(".alert-data-export-request").exists()).toBe(true);
    });

    expect(wrapper.find(".alert-data-export-request").text()).toContain(
      'Your data export for experiment "My Experiment" is ready.'
    );

    const downloadLink = wrapper
      .findAll("a")
      .find(a => a.text().trim() === "Click here to download.");
    expect(downloadLink).toBeTruthy();

    await downloadLink.trigger("click");

    // handleDataExportRequest re-polls, sees the (stale, pre-retrieve) ready flag and
    // retrieves the file, but the store now holds a downloaded-only request - so the
    // alert falls through to the generic "still being processed" fallback message.
    await vi.waitFor(() => {
      expect(experimentDataExportService.retrieve).toHaveBeenCalled();
    });

    await vi.waitFor(() => {
      expect(wrapper.find(".alert-data-export-request").text()).toContain(
        "Your data export is being processed. Please do not navigate away from this page."
      );
    });
  });

  it("acknowledges a ready data-export alert as READY_ACKNOWLEDGED when dismissed", async () => {
    experimentDataExportService.pollList.mockResolvedValue([
      { id: 22, experimentId: 8, status: "READY", experimentTitle: "My Experiment" }
    ]);

    const wrapper = mountSummary();

    await vi.waitFor(() => {
      expect(wrapper.find(".alert-data-export-request").exists()).toBe(true);
    });

    const alertComponent = wrapper.findComponent({ name: "VAlert" });
    alertComponent.vm.$emit("update:model-value", false);

    await vi.waitFor(() => {
      expect(experimentDataExportService.acknowledge).toHaveBeenCalledWith(
        8, 22, "READY_ACKNOWLEDGED"
      );
    });

    await wrapper.vm.$nextTick();
    expect(wrapper.find(".alert-data-export-request").exists()).toBe(false);
  });

  it("shows an outdated data-export alert with a recreate link, and does nothing when the user declines recreating it", async () => {
    experimentDataExportService.pollList.mockResolvedValue([
      { id: 23, experimentId: 8, status: "OUTDATED", experimentTitle: "My Experiment" }
    ]);
    swalFire.mockResolvedValue({ isConfirmed: false });

    const wrapper = mountSummary();

    await vi.waitFor(() => {
      expect(wrapper.find(".alert-data-export-request").exists()).toBe(true);
    });

    expect(wrapper.find(".alert-data-export-request").text()).toContain(
      'There have been updates since the last requested data export for experiment "My Experiment".'
    );

    const recreateLink = wrapper
      .findAll("a")
      .find(a => a.text().trim() === "Click here to download a new data export.");
    expect(recreateLink).toBeTruthy();

    await recreateLink.trigger("click");

    await vi.waitFor(() => {
      expect(swalFire).toHaveBeenCalled();
    });

    expect(experimentDataExportService.prepare).not.toHaveBeenCalled();
  });

  it("acknowledges an outdated data-export alert as OUTDATED_ACKNOWLEDGED when dismissed", async () => {
    experimentDataExportService.pollList.mockResolvedValue([
      { id: 24, experimentId: 8, status: "OUTDATED", experimentTitle: "My Experiment" }
    ]);

    const wrapper = mountSummary();

    await vi.waitFor(() => {
      expect(wrapper.find(".alert-data-export-request").exists()).toBe(true);
    });

    const alertComponent = wrapper.findComponent({ name: "VAlert" });
    alertComponent.vm.$emit("update:model-value", false);

    await vi.waitFor(() => {
      expect(experimentDataExportService.acknowledge).toHaveBeenCalledWith(
        8, 24, "OUTDATED_ACKNOWLEDGED"
      );
    });
  });

  it("shows an error data-export alert and acknowledges it as ERROR_ACKNOWLEDGED when dismissed", async () => {
    experimentDataExportService.pollList.mockResolvedValue([
      { id: 25, experimentId: 8, status: "ERROR", experimentTitle: "My Experiment" }
    ]);

    const wrapper = mountSummary();

    await vi.waitFor(() => {
      expect(wrapper.find(".alert-data-export-request").exists()).toBe(true);
    });

    expect(wrapper.find(".alert-data-export-request").text()).toContain(
      'There was an error processing the requested data export for experiment "My Experiment". Please try again or contact support.'
    );

    const alertComponent = wrapper.findComponent({ name: "VAlert" });
    alertComponent.vm.$emit("update:model-value", false);

    await vi.waitFor(() => {
      expect(experimentDataExportService.acknowledge).toHaveBeenCalledWith(
        8, 25, "ERROR_ACKNOWLEDGED"
      );
    });
  });

  it("resets the alert without acknowledging when a reprocessing export is dismissed", async () => {
    experimentDataExportService.pollList.mockResolvedValue([
      { id: 26, experimentId: 8, status: "REPROCESSING", experimentTitle: "My Experiment" }
    ]);

    const wrapper = mountSummary();

    await vi.waitFor(() => {
      expect(wrapper.find(".alert-data-export-request").exists()).toBe(true);
    });

    const alertComponent = wrapper.findComponent({ name: "VAlert" });
    alertComponent.vm.$emit("update:model-value", false);

    await wrapper.vm.$nextTick();

    expect(experimentDataExportService.acknowledge).not.toHaveBeenCalled();
    expect(wrapper.find(".alert-data-export-request").exists()).toBe(false);
  });

  it("shows a reprocessing notice when a reprocessing export is requested via the Export Data button", async () => {
    experimentDataExportService.poll.mockResolvedValueOnce({
      id: 30, experimentId: 8, status: "REPROCESSING", experimentTitle: "My Experiment"
    });

    const wrapper = mountSummary();

    await vi.waitFor(() => {
      expect(wrapper.text()).toContain("Export Data");
    });

    const exportDataButton = wrapper
      .findAll("button")
      .find(button => button.text() === "Export Data");
    await exportDataButton.trigger("click");

    await vi.waitFor(() => {
      expect(swalFire).toHaveBeenCalledWith(
        expect.objectContaining({
          text: expect.stringContaining("New submissons have occurred")
        })
      );
    });

    expect(wrapper.find(".alert-data-export-request").exists()).toBe(true);
  });

  it("polls the data export status every 5 seconds while processing, and stops once resolved", async () => {
    const wrapper = mountSummary();

    await vi.waitFor(() => {
      expect(wrapper.text()).toContain("Export Data");
    });

    vi.useFakeTimers();

    experimentDataExportService.poll.mockResolvedValueOnce({
      id: 9, experimentId: 8, status: "PROCESSING", experimentTitle: "My Experiment"
    });

    const exportDataButton = wrapper
      .findAll("button")
      .find(button => button.text() === "Export Data");
    await exportDataButton.trigger("click");
    await flushMicrotasks();
    await wrapper.vm.$nextTick();

    expect(wrapper.find(".alert-data-export-request").text()).toContain(
      "is being processed"
    );

    const pollCallsBeforeInterval = experimentDataExportService.poll.mock.calls.length;

    // The next poll (triggered by the 5s interval) reports the export as ready.
    experimentDataExportService.poll.mockResolvedValueOnce({
      id: 9, experimentId: 8, status: "READY", experimentTitle: "My Experiment"
    });

    await vi.advanceTimersByTimeAsync(5000);
    await flushMicrotasks();
    await wrapper.vm.$nextTick();

    expect(experimentDataExportService.poll.mock.calls.length).toBe(
      pollCallsBeforeInterval + 1
    );
    expect(wrapper.find(".alert-data-export-request").text()).toContain(
      "is ready"
    );

    // Once ready, polling.active flips false and the watcher must clear the interval:
    // advancing another 5s should trigger no further poll calls.
    const pollCallsAfterReady = experimentDataExportService.poll.mock.calls.length;

    await vi.advanceTimersByTimeAsync(5000);
    await flushMicrotasks();

    expect(experimentDataExportService.poll.mock.calls.length).toBe(pollCallsAfterReady);
  });

  it("ignores a second click on the consent download button once the PDF is already loading or loaded", async () => {
    let resolveFile;
    consentService.getConsentFile.mockReturnValue(
      new Promise(resolve => { resolveFile = resolve; })
    );

    const wrapper = mountSummary();

    await vi.waitFor(() => {
      expect(wrapper.text()).toContain("My Experiment");
    });

    await switchTab(wrapper, "participant");

    await wrapper.find(".pdfButton").trigger("click");
    await wrapper.vm.$nextTick();

    // pdfLoading is now true, so the button is hidden while loading.
    expect(wrapper.find(".pdfButton").exists()).toBe(false);

    resolveFile({ status: 200, base: "data:application/pdf;base64,ZmFrZQ==" });

    await vi.waitFor(() => {
      expect(wrapper.find(".pdfButton").exists()).toBe(true);
    });

    const callsAfterFirstLoad = consentService.getConsentFile.mock.calls.length;

    // loadPdfFrame is now true, so a second click is a no-op guard clause.
    await wrapper.find(".pdfButton").trigger("click");

    expect(consentService.getConsentFile.mock.calls.length).toBe(callsAfterFirstLoad);
  });

  it("creates a treatment and assessment then navigates to the builder", async () => {
    treatmentService.create.mockResolvedValue({
      status: 201,
      data: { treatmentId: 55, assignmentId: 3 }
    });
    assessmentService.fetchAssessments.mockResolvedValue({ data: [] });
    assessmentService.createAssessment.mockResolvedValue({
      status: 201,
      data: { assessmentId: 77 }
    });

    const wrapper = mountSummary();

    await vi.waitFor(() => {
      expect(wrapper.text()).toContain("My Experiment");
    });

    const result = await wrapper.vm.goToBuilder(1, 3, 60);

    expect(treatmentService.create).toHaveBeenCalledWith(8, 1, 3);
    expect(assessmentService.createAssessment).toHaveBeenCalledWith(8, 1, 55);
    expect(result).not.toBe(false);
    expect(push).toHaveBeenCalledWith({
      name: "TerracottaBuilder",
      params: {
        experimentId: 8,
        exposureId: 60,
        assignmentId: 3,
        conditionId: 1,
        treatmentId: 55,
        assessmentId: 77
      }
    });
  });

  it("shows an error and does not navigate to the builder when treatment creation fails", async () => {
    treatmentService.create.mockResolvedValue({ status: 400, data: "bad treatment" });

    const wrapper = mountSummary();

    await vi.waitFor(() => {
      expect(wrapper.text()).toContain("My Experiment");
    });

    push.mockClear();
    const result = await wrapper.vm.goToBuilder(1, 3, 60);

    expect(result).toBe(false);
    expect(swalFire).toHaveBeenCalledWith(
      expect.stringContaining("There was a problem creating your treatment")
    );
    expect(push).not.toHaveBeenCalledWith(
      expect.objectContaining({ name: "TerracottaBuilder" })
    );
  });

  it("shows an error and does not navigate to the builder when assessment creation fails", async () => {
    treatmentService.create.mockResolvedValue({
      status: 201,
      data: { treatmentId: 55, assignmentId: 3 }
    });
    assessmentService.fetchAssessments.mockResolvedValue({ data: [] });
    assessmentService.createAssessment.mockResolvedValue({ status: 500, data: "bad assessment" });

    const wrapper = mountSummary();

    await vi.waitFor(() => {
      expect(wrapper.text()).toContain("My Experiment");
    });

    push.mockClear();
    const result = await wrapper.vm.goToBuilder(1, 3, 60);

    expect(result).toBe(false);
    expect(swalFire).toHaveBeenCalledWith(
      expect.stringContaining("There was a problem creating your assessment")
    );
    expect(push).not.toHaveBeenCalledWith(
      expect.objectContaining({ name: "TerracottaBuilder" })
    );
  });

  it("recovers when creating the treatment throws, showing the treatment error", async () => {
    const consoleErrorSpy = vi.spyOn(console, "error").mockImplementation(() => {});
    const store = treatmentModule();
    vi.spyOn(store, "createTreatment").mockRejectedValue(new Error("boom"));

    const wrapper = mountSummary();

    await vi.waitFor(() => {
      expect(wrapper.text()).toContain("My Experiment");
    });

    const result = await wrapper.vm.goToBuilder(1, 3, 60);

    expect(result).toBe(false);
    expect(consoleErrorSpy).toHaveBeenCalledWith(
      "handleCreateTreatment | catch",
      expect.objectContaining({ error: expect.any(Error) })
    );
    expect(swalFire).toHaveBeenCalledWith(
      expect.stringContaining("There was a problem creating your treatment")
    );

    consoleErrorSpy.mockRestore();
  });

  it("recovers when creating the assessment throws, showing the assessment error", async () => {
    treatmentService.create.mockResolvedValue({
      status: 201,
      data: { treatmentId: 55, assignmentId: 3 }
    });

    const consoleErrorSpy = vi.spyOn(console, "error").mockImplementation(() => {});
    const store = assessmentModule();
    vi.spyOn(store, "createAssessment").mockRejectedValue(new Error("kaboom"));

    const wrapper = mountSummary();

    await vi.waitFor(() => {
      expect(wrapper.text()).toContain("My Experiment");
    });

    const result = await wrapper.vm.goToBuilder(1, 3, 60);

    expect(result).toBe(false);
    expect(consoleErrorSpy).toHaveBeenCalledWith(
      "handleCreateAssessment | catch",
      expect.objectContaining({ error: expect.any(Error) })
    );
    expect(swalFire).toHaveBeenCalledWith(
      expect.stringContaining("There was a problem creating your assessment")
    );

    consoleErrorSpy.mockRestore();
  });

  it("maps group names to condition names, and safely returns an empty map for a missing list", () => {
    const wrapper = mountSummary();

    expect(
      wrapper.vm.groupNameConditionMapping([
        { groupName: "Group 1", conditionName: "Condition A" },
        { groupName: "Group 2", conditionName: "Condition B" }
      ])
    ).toEqual({ "Group 1": "Condition A", "Group 2": "Condition B" });

    expect(wrapper.vm.groupNameConditionMapping(undefined)).toEqual({});
  });

  it("sorts group names alphabetically, and safely returns undefined for a missing list", () => {
    const wrapper = mountSummary();

    expect(
      wrapper.vm.sortedGroups([
        { groupName: "Zebra" },
        { groupName: "Apple" }
      ])
    ).toEqual(["Apple", "Zebra"]);

    expect(wrapper.vm.sortedGroups(undefined)).toBeUndefined();
  });

  it("redirects to the participation selection editor when a consent-type experiment has no consent configured", async () => {
    experimentService.getById.mockResolvedValue({
      status: 200,
      data: { ...experiment, consent: undefined }
    });

    mountSummary();

    await vi.waitFor(() => {
      expect(push).toHaveBeenCalledWith({ name: "ExperimentParticipationSelectionMethod" });
    });

    expect(navigationModule().editMode).toMatchObject({
      initialPage: "ExperimentParticipationSelectionMethod",
      callerPage: { name: "ExperimentSummary", tab: "participant" }
    });
  });

  it("switches to the status tab when the statusPageNav event fires on the event bus", async () => {
    const wrapper = mountSummary();

    await vi.waitFor(() => {
      expect(
        wrapper.findComponent({ name: "ExperimentAssignments" }).exists()
      ).toBe(true);
    });

    EventBus.emit("statusPageNav");
    await wrapper.vm.$nextTick();

    expect(
      wrapper.findComponent({ name: "ExperimentSummaryStatus" }).exists()
    ).toBe(true);
  });
});
