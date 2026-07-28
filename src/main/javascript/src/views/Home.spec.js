import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("@/services", () => ({
  experimentService: {
    getAll: vi.fn(),
    create: vi.fn(),
    delete: vi.fn(),
    export: vi.fn(),
    import: vi.fn(),
    pollImport: vi.fn(),
    pollImports: vi.fn(),
    acknowledgeImport: vi.fn()
  },
  experimentDataExportService: {
    prepare: vi.fn(),
    poll: vi.fn(),
    pollList: vi.fn(),
    retrieve: vi.fn(),
    acknowledge: vi.fn()
  }
}));

const push = vi.fn();

vi.mock("vue-router", () => ({
  useRouter: () => ({ push }),
  onBeforeRouteLeave: vi.fn()
}));

const swalFire = vi.fn();

vi.mock("sweetalert2", () => ({
  default: { fire: (...args) => swalFire(...args) }
}));

import { createPinia, setActivePinia } from "pinia";
import { mountComponent } from "@/test-utils/mount";
import {
  experimentService,
  experimentDataExportService
} from "@/services";
import { configuration as configurationModule } from "@/store/configuration.module";
import { assignment as assignmentModule } from "@/store/assignment.module";
import Home from "./Home.vue";

const experiment = {
  experimentId: 11,
  title: "My Experiment",
  createdAt: "2024-01-01T00:00:00Z",
  exposureType: "BETWEEN",
  participationType: "CONSENT",
  distributionType: "CUSTOM",
  started: false
};

// Opens the row-level "..." actions menu for the given row index (0-based) and
// returns the matching VListItem for the given visible title text.
const openRowAction = async (wrapper, itemTitle, rowIndex = 0) => {
  const icons = wrapper.findAll(".mdi-dots-horizontal");
  await icons[rowIndex].trigger("click");
  await wrapper.vm.$nextTick();

  return wrapper
    .findAllComponents({ name: "VListItem" })
    .find(item => item.text().includes(itemTitle));
};

describe("Home", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    swalFire.mockReset();
    experimentService.getAll.mockResolvedValue({ status: 200, data: [] });
    experimentService.pollImports.mockResolvedValue({ data: [] });
    experimentDataExportService.pollList.mockResolvedValue([]);
  });

  it("shows the zero state and hides the table when there are no experiments", async () => {
    const wrapper = mountComponent(Home);

    await vi.waitFor(() => {
      expect(wrapper.findComponent({ name: "PageLoading" }).props("display")).toBe(false);
    });

    expect(wrapper.findComponent({ name: "ZeroState" }).isVisible()).toBe(true);
    expect(wrapper.find(".table-experiments").isVisible()).toBe(false);
  });

  it("resets prior experiment-related store state (e.g. leftover assignments) on mount", async () => {
    const pinia = createPinia();
    setActivePinia(pinia);
    assignmentModule().assignments = [{ assignmentId: 1 }];

    const wrapper = mountComponent(Home, { pinia });

    await vi.waitFor(() => {
      expect(wrapper.findComponent({ name: "PageLoading" }).props("display")).toBe(false);
    });

    expect(assignmentModule().assignments).toEqual([]);
  });

  it("renders the experiments table once experiments load", async () => {
    experimentService.getAll.mockResolvedValue({
      status: 200,
      data: [experiment]
    });

    const wrapper = mountComponent(Home);

    await vi.waitFor(() => {
      expect(wrapper.text()).toContain("My Experiment");
    });

    expect(wrapper.find(".table-experiments").isVisible()).toBe(true);
    expect(wrapper.findComponent({ name: "ZeroState" }).isVisible()).toBe(false);
  });

  it("navigates to ExperimentSummary when a fully-configured experiment's title is clicked", async () => {
    experimentService.getAll.mockResolvedValue({
      status: 200,
      data: [experiment]
    });

    const wrapper = mountComponent(Home);

    await vi.waitFor(() => {
      expect(wrapper.find(".v-data-table__link").exists()).toBe(true);
    });

    await wrapper.find(".v-data-table__link").trigger("click");

    expect(push).toHaveBeenCalledWith({
      name: "ExperimentSummary",
      params: { experimentId: 11 }
    });
  });

  it("navigates to ExperimentDesignIntro when the experiment is not fully configured", async () => {
    experimentService.getAll.mockResolvedValue({
      status: 200,
      data: [{ ...experiment, exposureType: "NOSET" }]
    });

    const wrapper = mountComponent(Home);

    await vi.waitFor(() => {
      expect(wrapper.find(".v-data-table__link").exists()).toBe(true);
    });

    await wrapper.find(".v-data-table__link").trigger("click");

    expect(push).toHaveBeenCalledWith({
      name: "ExperimentDesignIntro",
      params: { experimentId: 11 }
    });
  });

  it("creates a new experiment and navigates to its design intro", async () => {
    experimentService.create.mockResolvedValue({
      data: { experimentId: 42 }
    });

    const wrapper = mountComponent(Home);

    await vi.waitFor(() => {
      expect(wrapper.text()).toContain("New Experiment");
    });

    const newExperimentButton = wrapper
      .findAll("button")
      .find(button => button.text() === "New Experiment");

    await newExperimentButton.trigger("click");
    await vi.waitFor(() => {
      expect(push).toHaveBeenCalledWith({
        name: "ExperimentDesignIntro",
        params: { experimentId: 42 }
      });
    });
  });

  it("deletes an experiment after confirmation", async () => {
    experimentService.getAll.mockResolvedValue({
      status: 200,
      data: [experiment]
    });
    experimentService.delete.mockResolvedValue({ status: 200 });
    swalFire.mockResolvedValue({ isConfirmed: true });

    const wrapper = mountComponent(Home);

    await vi.waitFor(() => {
      expect(wrapper.find(".mdi-dots-horizontal").exists()).toBe(true);
    });

    const deleteItem = await openRowAction(wrapper, "Delete");
    await deleteItem.trigger("click");

    await vi.waitFor(() => {
      expect(experimentService.delete).toHaveBeenCalledWith(11);
    });
  });

  it("does not delete an experiment when the confirmation is dismissed", async () => {
    experimentService.getAll.mockResolvedValue({
      status: 200,
      data: [experiment]
    });
    swalFire.mockResolvedValue({ isConfirmed: false });

    const wrapper = mountComponent(Home);

    await vi.waitFor(() => {
      expect(wrapper.find(".mdi-dots-horizontal").exists()).toBe(true);
    });

    const deleteItem = await openRowAction(wrapper, "Delete");
    await deleteItem.trigger("click");

    expect(experimentService.delete).not.toHaveBeenCalled();
  });

  it("exports the experiment definition when experiment export is enabled", async () => {
    const pinia = createPinia();
    setActivePinia(pinia);
    configurationModule().$patch({
      configurations: { experimentExportEnabled: true }
    });

    experimentService.getAll.mockResolvedValue({
      status: 200,
      data: [experiment]
    });
    experimentService.export.mockResolvedValue({ status: 200 });

    const wrapper = mountComponent(Home, { pinia });

    await vi.waitFor(() => {
      expect(wrapper.find(".mdi-dots-horizontal").exists()).toBe(true);
    });

    const exportItem = await openRowAction(wrapper, "Export Experiment");
    await exportItem.trigger("click");

    await vi.waitFor(() => {
      expect(experimentService.export).toHaveBeenCalledWith(11);
    });
  });

  it("does not offer an Export Experiment action when experiment export is disabled", async () => {
    experimentService.getAll.mockResolvedValue({
      status: 200,
      data: [experiment]
    });

    const wrapper = mountComponent(Home);

    await vi.waitFor(() => {
      expect(wrapper.find(".mdi-dots-horizontal").exists()).toBe(true);
    });

    await wrapper.find(".mdi-dots-horizontal").trigger("click");
    await wrapper.vm.$nextTick();

    const exportItem = wrapper
      .findAllComponents({ name: "VListItem" })
      .find(item => item.text().includes("Export Experiment"));

    expect(exportItem).toBeUndefined();
  });

  it("prepares a data export after confirming the request", async () => {
    experimentService.getAll.mockResolvedValue({
      status: 200,
      data: [experiment]
    });
    experimentDataExportService.poll.mockResolvedValue(null);
    experimentDataExportService.prepare.mockResolvedValue({
      id: 1,
      experimentId: 11,
      status: "PROCESSING"
    });
    swalFire.mockResolvedValue({ isConfirmed: true });

    const wrapper = mountComponent(Home);

    await vi.waitFor(() => {
      expect(wrapper.find(".mdi-dots-horizontal").exists()).toBe(true);
    });

    const exportResultsItem = await openRowAction(wrapper, "Export Results");
    await exportResultsItem.trigger("click");

    await vi.waitFor(() => {
      expect(experimentDataExportService.prepare).toHaveBeenCalledWith(11);
    });
  });
});
