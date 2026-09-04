import { beforeEach, describe, expect, it, vi } from "vitest";

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
  consentService
} from "@/services";
import { navigation as navigationModule } from "@/store/navigation.module";
import { alert as alertModule } from "@/store/alert.module";
import { configuration as configurationModule } from "@/store/configuration.module";
import ExperimentSummary from "./ExperimentSummary.vue";

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
    vi.clearAllMocks();
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
});
