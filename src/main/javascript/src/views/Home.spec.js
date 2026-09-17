import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { nextTick } from "vue";

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
  },
  experimentCopyCandidateService: {
    getAll: vi.fn(),
    resolve: vi.fn()
  }
}));

const push = vi.fn();

vi.mock("vue-router", () => ({
  useRouter: () => ({ push }),
  onBeforeRouteLeave: vi.fn()
}));

const swalFire = vi.fn();
const swalClose = vi.fn();

vi.mock("sweetalert2", () => ({
  default: {
    fire: (...args) => swalFire(...args),
    close: (...args) => swalClose(...args)
  }
}));

// The copy-candidates dialog is a single Swal.fire call that stays open for the whole
// interaction (create/decline/defer are all confirmed via an overlay inside the mounted
// CopyCandidatesDialog component, not via separate Swal.fire calls - see Home.vue). To
// exercise that for real, this mounts the dialog's actual `html` + `didOpen` into the
// document (SweetAlert2 itself is mocked out, so nothing does this automatically), and
// wires Swal.close() to resolve the pending Swal.fire() promise, matching real behavior.
const mountCopyCandidatesDialog = () => {
  swalFire.mockImplementation(options => {
    const container = document.createElement("div");
    container.innerHTML = options.html;
    document.body.appendChild(container);
    options.didOpen?.();

    return new Promise(resolve => {
      swalClose.mockImplementation(() => {
        options.willClose?.();
        container.remove();
        resolve({ isDismissed: true });
      });
    });
  });
};

const clickCandidateOption = async (index = 0) => {
  document.querySelectorAll(".copy-candidate-option")[index]
    .dispatchEvent(new MouseEvent("click", { bubbles: true }));
  await nextTick();
};

const clickCopyCandidatesAction = async label => {
  const button = [...document.querySelectorAll(".copy-candidates-content .copy-candidates-btn")]
    .find(candidate => candidate.textContent.trim() === label);
  button.dispatchEvent(new MouseEvent("click", { bubbles: true }));
  await nextTick();
};

const clickCopyCandidatesOverlayButton = async label => {
  const button = [...document.querySelectorAll(".copy-candidates-confirm-buttons .copy-candidates-btn")]
    .find(candidate => candidate.textContent.trim() === label);
  button.dispatchEvent(new MouseEvent("click", { bubbles: true }));
  await nextTick();
};

import { createPinia, setActivePinia } from "pinia";
import { flushPromises } from "@vue/test-utils";
import { mountComponent } from "@/test-utils/mount";
import {
  experimentService,
  experimentDataExportService,
  experimentCopyCandidateService
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
    swalClose.mockReset();
    experimentService.getAll.mockResolvedValue({ status: 200, data: [] });
    experimentService.pollImports.mockResolvedValue({ data: [] });
    experimentDataExportService.pollList.mockResolvedValue([]);
    experimentCopyCandidateService.getAll.mockResolvedValue({ data: [] });
  });

  // some copy-candidates tests leave the popup open (e.g. deferring, which just leaves it
  // showing PENDING candidates) - clean up its manually-appended DOM between tests so a
  // leftover mounted CopyCandidatesDialog app/element from one test can't be picked up by
  // the next test's document.getElementById("dialog-copy-candidates") lookup
  afterEach(() => {
    document.body.innerHTML = "";
  });

  it("fetches copy candidates when there are no experiments and automatically opens the dialog", async () => {
    experimentCopyCandidateService.getAll.mockResolvedValue({
      data: [{ id: "c1", experimentTitle: "Reading Study" }]
    });
    mountCopyCandidatesDialog();

    const wrapper = mountComponent(Home);

    await vi.waitFor(() => {
      expect(wrapper.findComponent({ name: "PageLoading" }).props("display")).toBe(false);
    });

    expect(experimentCopyCandidateService.getAll).toHaveBeenCalled();
    await vi.waitFor(() => {
      expect(swalFire).toHaveBeenCalled();
    });
  });

  it("does not fetch copy candidates when experiments already exist", async () => {
    experimentService.getAll.mockResolvedValue({ status: 200, data: [experiment] });

    const wrapper = mountComponent(Home);

    await vi.waitFor(() => {
      expect(wrapper.text()).toContain("My Experiment");
    });

    expect(experimentCopyCandidateService.getAll).not.toHaveBeenCalled();
  });

  it("automatically opens the copy-candidates dialog when candidates exist, resolving selected candidates on confirm and registering the resulting imports as import requests", async () => {
    experimentCopyCandidateService.getAll.mockResolvedValue({
      data: [{ id: "c1", experimentTitle: "Reading Study" }]
    });
    experimentCopyCandidateService.resolve.mockResolvedValue({
      data: { imports: [{ id: "import-1", status: "PROCESSING" }], declinedCandidateIds: [] }
    });
    mountCopyCandidatesDialog();

    const wrapper = mountComponent(Home);

    await vi.waitFor(() => {
      expect(document.querySelector(".copy-candidate-option")).not.toBeNull();
    });

    await clickCandidateOption(0);
    await clickCopyCandidatesAction("Create selected");
    await clickCopyCandidatesOverlayButton("Got it!");

    await vi.waitFor(() => {
      expect(experimentCopyCandidateService.resolve).toHaveBeenCalledWith(["c1"]);
    });
    // resolving the outcome is what closes the single, still-open popup
    expect(swalFire).toHaveBeenCalledTimes(1);

    await vi.waitFor(() => {
      expect(
        wrapper.findComponent({ name: "ZeroState" }).props("experimentImportRequests")["import-1"]
      ).toMatchObject({ showAlert: true });
    });
  });

  it("shows a preparing-imports loading screen and disables the zero-state action buttons while resolving a 'Create selected' outcome", async () => {
    experimentCopyCandidateService.getAll.mockResolvedValue({
      data: [{ id: "c1", experimentTitle: "Reading Study" }]
    });
    mountCopyCandidatesDialog();

    let resolveImport;
    experimentCopyCandidateService.resolve.mockReturnValue(
      new Promise(resolve => { resolveImport = resolve; })
    );

    const wrapper = mountComponent(Home);
    const findPreparingLoading = () => wrapper.findAllComponents({ name: "PageLoading" })
      .find(candidate => candidate.props("message") === "We are preparing to import the selected experiments. Please wait.");

    await vi.waitFor(() => {
      expect(document.querySelector(".copy-candidate-option")).not.toBeNull();
    });

    expect(findPreparingLoading().props("display")).toBe(false);
    expect(wrapper.findComponent({ name: "ZeroState" }).props("disableActions")).toBe(false);

    await clickCandidateOption(0);
    await clickCopyCandidatesAction("Create selected");
    await clickCopyCandidatesOverlayButton("Got it!");

    await vi.waitFor(() => {
      expect(experimentCopyCandidateService.resolve).toHaveBeenCalledWith(["c1"]);
    });
    expect(findPreparingLoading().props("display")).toBe(true);
    expect(wrapper.findComponent({ name: "ZeroState" }).props("disableActions")).toBe(true);

    resolveImport({ data: { imports: [], declinedCandidateIds: [] } });
    await flushPromises();

    expect(findPreparingLoading().props("display")).toBe(false);
    expect(wrapper.findComponent({ name: "ZeroState" }).props("disableActions")).toBe(false);
  });

  it("does not resolve anything when 'I'll decide later' is chosen and confirmed", async () => {
    experimentCopyCandidateService.getAll.mockResolvedValue({
      data: [{ id: "c1", experimentTitle: "Reading Study" }]
    });
    mountCopyCandidatesDialog();

    mountComponent(Home);

    await vi.waitFor(() => {
      expect(document.querySelector(".copy-candidate-option")).not.toBeNull();
    });

    await clickCopyCandidatesAction("I'll decide later");
    await clickCopyCandidatesOverlayButton("Got it!");

    await flushPromises();
    expect(experimentCopyCandidateService.resolve).not.toHaveBeenCalled();
    // deferring leaves everything PENDING - there's nothing more for this popup to do,
    // so it just closes without a second, separate acknowledgement popup
    expect(swalFire).toHaveBeenCalledTimes(1);
  });

  it("keeps the dialog open, without emitting anything, when 'Go back to selection' is chosen from an action's confirmation overlay", async () => {
    experimentCopyCandidateService.getAll.mockResolvedValue({
      data: [{ id: "c1", experimentTitle: "Reading Study" }]
    });
    experimentCopyCandidateService.resolve.mockResolvedValue({
      data: { imports: [], declinedCandidateIds: ["c1"] }
    });
    mountCopyCandidatesDialog();

    mountComponent(Home);

    await vi.waitFor(() => {
      expect(document.querySelector(".copy-candidate-option")).not.toBeNull();
    });

    // starts down the "I'll decide later" path, then backs out of it - the overlay
    // covers the grid rather than replacing the dialog, so no second Swal.fire happens
    await clickCopyCandidatesAction("I'll decide later");
    await clickCopyCandidatesOverlayButton("Go back to selection");
    expect(document.querySelector(".copy-candidates-confirm-overlay")).toBeNull();

    // the same, still-open dialog can now be used to pick a different action instead
    await clickCopyCandidatesAction("No thank you");
    await clickCopyCandidatesOverlayButton("Got it!");

    await vi.waitFor(() => {
      expect(experimentCopyCandidateService.resolve).toHaveBeenCalledWith([]);
    });
    expect(swalFire).toHaveBeenCalledTimes(1);
  });

  it("resolves with an empty selection when 'No thank you' is chosen and confirmed", async () => {
    experimentCopyCandidateService.getAll.mockResolvedValue({
      data: [
        { id: "c1", experimentTitle: "Reading Study" },
        { id: "c2", experimentTitle: "Writing Study" }
      ]
    });
    experimentCopyCandidateService.resolve.mockResolvedValue({
      data: { imports: [], declinedCandidateIds: ["c1", "c2"] }
    });
    mountCopyCandidatesDialog();

    mountComponent(Home);

    await vi.waitFor(() => {
      expect(document.querySelector(".copy-candidate-option")).not.toBeNull();
    });

    await clickCopyCandidatesAction("No thank you");
    await clickCopyCandidatesOverlayButton("Got it!");

    await vi.waitFor(() => {
      expect(experimentCopyCandidateService.resolve).toHaveBeenCalledWith([]);
    });
  });

  it("does not decline anything when 'No thank you' is chosen but then 'Go back to selection' is picked, and creates the eventual selection instead", async () => {
    experimentCopyCandidateService.getAll.mockResolvedValue({
      data: [{ id: "c1", experimentTitle: "Reading Study" }]
    });
    experimentCopyCandidateService.resolve.mockResolvedValue({
      data: { imports: [{ id: "import-1", status: "PROCESSING" }], declinedCandidateIds: [] }
    });
    mountCopyCandidatesDialog();

    mountComponent(Home);

    await vi.waitFor(() => {
      expect(document.querySelector(".copy-candidate-option")).not.toBeNull();
    });

    await clickCopyCandidatesAction("No thank you");
    await clickCopyCandidatesOverlayButton("Go back to selection");

    await clickCandidateOption(0);
    await clickCopyCandidatesAction("Create selected");
    await clickCopyCandidatesOverlayButton("Got it!");

    await vi.waitFor(() => {
      expect(experimentCopyCandidateService.resolve).toHaveBeenCalledWith(["c1"]);
    });
    // proves the initial "No thank you" was aborted rather than also going through -
    // resolve was only ever called once, with the eventual selection
    expect(experimentCopyCandidateService.resolve).toHaveBeenCalledTimes(1);
    expect(swalFire).toHaveBeenCalledTimes(1);
  });

  it("does not automatically open the copy-candidates dialog when there are no candidates", async () => {
    const wrapper = mountComponent(Home);

    await vi.waitFor(() => {
      expect(wrapper.findComponent({ name: "PageLoading" }).props("display")).toBe(false);
    });

    expect(swalFire).not.toHaveBeenCalled();
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
