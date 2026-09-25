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
    resolve: vi.fn(),
    getCopyStatus: vi.fn(),
    acknowledgeCopyStatus: vi.fn(),
    retryCopy: vi.fn()
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
import { onBeforeRouteLeave } from "vue-router";
import { mountComponent } from "@/test-utils/mount";
import {
  experimentService,
  experimentDataExportService,
  experimentCopyCandidateService
} from "@/services";
import { configuration as configurationModule } from "@/store/configuration.module";
import { assignment as assignmentModule } from "@/store/assignment.module";
import { experiment as experimentModule } from "@/store/experiment.module";
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
    experimentCopyCandidateService.getCopyStatus.mockResolvedValue({ data: { status: "NONE", importIds: [] } });
    experimentCopyCandidateService.acknowledgeCopyStatus.mockResolvedValue({});
    experimentCopyCandidateService.retryCopy.mockResolvedValue({ data: { status: "ERROR", importIds: [] } });
  });

  // some copy-candidates tests leave the popup open (e.g. deferring, which just leaves it
  // showing PENDING candidates) - clean up its manually-appended DOM between tests so a
  // leftover mounted CopyCandidatesDialog app/element from one test can't be picked up by
  // the next test's document.getElementById("dialog-copy-candidates") lookup
  afterEach(() => {
    document.body.innerHTML = "";
  });

  // the copy-candidates selection dialog is switched off (COPY_CANDIDATE_SELECTION_ENABLED in
  // Home.vue) - kept, along with its tests, in case it's wanted again
  it.skip("fetches copy candidates when there are no experiments and automatically opens the dialog", async () => {
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

  it("never asks for copy candidates now that copied experiments are recreated automatically", async () => {
    experimentCopyCandidateService.getAll.mockResolvedValue({
      data: [{ id: "c1", experimentTitle: "Reading Study" }]
    });

    const wrapper = mountComponent(Home);

    await vi.waitFor(() => {
      expect(wrapper.findComponent({ name: "PageLoading" }).props("display")).toBe(false);
    });
    await flushPromises();

    expect(experimentCopyCandidateService.getAll).not.toHaveBeenCalled();
    expect(swalFire).not.toHaveBeenCalled();
  });

  it("tells the instructor once their experiments have been copied, then acknowledges it", async () => {
    experimentService.getAll.mockResolvedValue({ status: 200, data: [experiment] });
    experimentCopyCandidateService.getCopyStatus.mockResolvedValue({ data: { status: "COMPLETE", importIds: ["i1"] } });
    swalFire.mockResolvedValue({});

    mountComponent(Home);

    await vi.waitFor(() => {
      expect(experimentCopyCandidateService.acknowledgeCopyStatus).toHaveBeenCalled();
    });
    expect(swalFire).toHaveBeenCalledWith({
      text: "Your experiments and assignments have been copied from your previous course and are ready to use.",
      icon: "success"
    });
  });

  it("tells the instructor when their experiments could not all be copied, once a retry also fails, then acknowledges it", async () => {
    experimentCopyCandidateService.getCopyStatus.mockResolvedValue({ data: { status: "ERROR", importIds: [] } });
    swalFire.mockResolvedValue({});

    mountComponent(Home);

    await vi.waitFor(() => {
      expect(experimentCopyCandidateService.acknowledgeCopyStatus).toHaveBeenCalled();
    });
    expect(experimentCopyCandidateService.retryCopy).toHaveBeenCalledTimes(1);
    expect(swalFire).toHaveBeenCalledWith({
      text: "We couldn't copy all of your experiments and assignments from your previous course. Please contact Terracotta support for help.",
      icon: "error"
    });
  });

  it("retries a failed copy as the launching instructor and waits for it instead of reporting the failure", async () => {
    const setIntervalSpy = vi.spyOn(window, "setInterval");
    experimentCopyCandidateService.getCopyStatus.mockResolvedValue({ data: { status: "ERROR", importIds: [] } });
    experimentCopyCandidateService.retryCopy.mockResolvedValue({ data: { status: "IN_PROGRESS", importIds: [] } });

    const wrapper = mountComponent(Home);

    await vi.waitFor(() => {
      expect(wrapper.find(".copy-in-progress-alert").exists()).toBe(true);
    });
    expect(experimentCopyCandidateService.retryCopy).toHaveBeenCalledTimes(1);
    expect(swalFire).not.toHaveBeenCalled();
    expect(experimentCopyCandidateService.acknowledgeCopyStatus).not.toHaveBeenCalled();

    // the retry succeeds
    experimentCopyCandidateService.getCopyStatus.mockResolvedValue({ data: { status: "COMPLETE", importIds: [] } });
    swalFire.mockResolvedValue({});
    const pollCall = setIntervalSpy.mock.calls.find(([, delay]) => delay === 5000);

    await pollCall[0]();
    await flushPromises();

    expect(swalFire).toHaveBeenCalledWith(expect.objectContaining({ icon: "success" }));
    expect(experimentCopyCandidateService.retryCopy).toHaveBeenCalledTimes(1);

    setIntervalSpy.mockRestore();
  });

  it("shows nothing and acknowledges nothing when there was no course copy", async () => {
    const wrapper = mountComponent(Home);

    await vi.waitFor(() => {
      expect(wrapper.findComponent({ name: "PageLoading" }).props("display")).toBe(false);
    });
    await flushPromises();

    expect(swalFire).not.toHaveBeenCalled();
    expect(experimentCopyCandidateService.acknowledgeCopyStatus).not.toHaveBeenCalled();
    expect(wrapper.find(".copy-in-progress-alert").exists()).toBe(false);
  });

  it("shows a notice and disables the zero-state actions while experiments are still being copied, then reports the result once finished", async () => {
    const setIntervalSpy = vi.spyOn(window, "setInterval");
    experimentCopyCandidateService.getCopyStatus.mockResolvedValue({ data: { status: "IN_PROGRESS", importIds: [] } });
    swalFire.mockResolvedValue({});

    const wrapper = mountComponent(Home);

    await vi.waitFor(() => {
      expect(wrapper.find(".copy-in-progress-alert").exists()).toBe(true);
    });
    expect(wrapper.find(".copy-in-progress-alert").text()).toContain(
      "Your experiments and assignments are being copied from your previous course."
    );
    expect(wrapper.findComponent({ name: "ZeroState" }).props("disableActions")).toBe(true);
    expect(swalFire).not.toHaveBeenCalled();

    const pollCall = setIntervalSpy.mock.calls.find(([, delay]) => delay === 5000);
    expect(pollCall).toBeDefined();

    experimentCopyCandidateService.getCopyStatus.mockResolvedValue({ data: { status: "COMPLETE", importIds: [] } });
    experimentService.getAll.mockResolvedValue({ status: 200, data: [experiment] });
    const fetchesBefore = experimentService.getAll.mock.calls.length;

    await pollCall[0]();
    await flushPromises();

    expect(experimentService.getAll.mock.calls.length).toBeGreaterThan(fetchesBefore);
    expect(swalFire).toHaveBeenCalledWith(expect.objectContaining({ icon: "success" }));
    expect(experimentCopyCandidateService.acknowledgeCopyStatus).toHaveBeenCalled();
    expect(wrapper.find(".copy-in-progress-alert").exists()).toBe(false);

    setIntervalSpy.mockRestore();
  });

  it("leaves imports created by a course copy out of the ordinary import alerts", async () => {
    experimentService.pollImports.mockResolvedValue({
      data: [
        { id: "from-copy", status: "PROCESSING" },
        { id: "manual", status: "PROCESSING" }
      ]
    });
    experimentCopyCandidateService.getCopyStatus.mockResolvedValue({ data: { status: "IN_PROGRESS", importIds: ["from-copy"] } });

    const wrapper = mountComponent(Home);

    await vi.waitFor(() => {
      expect(wrapper.findComponent({ name: "ZeroState" }).props("experimentImportRequests")["manual"]).toBeDefined();
    });
    expect(wrapper.findComponent({ name: "ZeroState" }).props("experimentImportRequests")["from-copy"]).toBeUndefined();
  });

  it("goes straight to the experiment listing, without ever showing the copy-candidates dialog, when the course already has experiments", async () => {
    experimentService.getAll.mockResolvedValue({ status: 200, data: [experiment] });

    const wrapper = mountComponent(Home);

    await vi.waitFor(() => {
      expect(wrapper.text()).toContain("My Experiment");
    });

    // never even asks the backend for candidates - the guard is "is this course new",
    // decided before that call would happen (see also the backend's own, independent
    // "does this context already have an experiment" guard in getPendingForContext)
    expect(experimentCopyCandidateService.getAll).not.toHaveBeenCalled();
    // no copy-candidates (or any other) popup was shown
    expect(swalFire).not.toHaveBeenCalled();
    // the experiment listing table is what's shown, not the zero-state welcome screen
    expect(wrapper.find(".table-experiments").isVisible()).toBe(true);
    expect(wrapper.findComponent({ name: "ZeroState" }).isVisible()).toBe(false);
  });

  // the copy-candidates selection dialog is switched off (COPY_CANDIDATE_SELECTION_ENABLED in
  // Home.vue) - kept, along with its tests, in case it's wanted again
  it.skip("automatically opens the copy-candidates dialog when candidates exist, resolving selected candidates on confirm and registering the resulting imports as import requests", async () => {
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

  // the copy-candidates selection dialog is switched off (COPY_CANDIDATE_SELECTION_ENABLED in
  // Home.vue) - kept, along with its tests, in case it's wanted again
  it.skip("shows a preparing-imports loading screen and disables the zero-state action buttons while resolving a 'Create selected' outcome", async () => {
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

  // the copy-candidates selection dialog is switched off (COPY_CANDIDATE_SELECTION_ENABLED in
  // Home.vue) - kept, along with its tests, in case it's wanted again
  it.skip("does not resolve anything when 'I'll decide later' is chosen and confirmed", async () => {
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

  // the copy-candidates selection dialog is switched off (COPY_CANDIDATE_SELECTION_ENABLED in
  // Home.vue) - kept, along with its tests, in case it's wanted again
  it.skip("keeps the dialog open, without emitting anything, when 'Go back to selection' is chosen from an action's confirmation overlay", async () => {
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

  // the copy-candidates selection dialog is switched off (COPY_CANDIDATE_SELECTION_ENABLED in
  // Home.vue) - kept, along with its tests, in case it's wanted again
  it.skip("resolves with an empty selection when 'No thank you' is chosen and confirmed", async () => {
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

  // the copy-candidates selection dialog is switched off (COPY_CANDIDATE_SELECTION_ENABLED in
  // Home.vue) - kept, along with its tests, in case it's wanted again
  it.skip("does not decline anything when 'No thank you' is chosen but then 'Go back to selection' is picked, and creates the eventual selection instead", async () => {
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

  it("tracks import requests that were already in progress when the page loads", async () => {
    experimentService.pollImports.mockResolvedValue({
      data: [{ id: "already-running", status: "PROCESSING" }]
    });

    const wrapper = mountComponent(Home);

    await vi.waitFor(() => {
      expect(wrapper.findComponent({ name: "PageLoading" }).props("display")).toBe(false);
    });

    await vi.waitFor(() => {
      expect(
        wrapper.findComponent({ name: "ZeroState" }).props("experimentImportRequests")["already-running"]
      ).toMatchObject({ showAlert: true, polling: { active: true } });
    });
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

  it("does not prepare a data export when the final confirmation is cancelled", async () => {
    experimentService.getAll.mockResolvedValue({
      status: 200,
      data: [experiment]
    });
    experimentDataExportService.poll.mockResolvedValue(null);
    swalFire.mockResolvedValue({ isConfirmed: false });

    const wrapper = mountComponent(Home);

    await vi.waitFor(() => {
      expect(wrapper.find(".mdi-dots-horizontal").exists()).toBe(true);
    });

    const exportResultsItem = await openRowAction(wrapper, "Export Results");
    await exportResultsItem.trigger("click");

    await vi.waitFor(() => {
      expect(swalFire).toHaveBeenCalled();
    });
    expect(experimentDataExportService.prepare).not.toHaveBeenCalled();
  });

  it("shows an info message and does not prepare a new export while one is already processing or reprocessing", async () => {
    experimentService.getAll.mockResolvedValue({
      status: 200,
      data: [experiment]
    });

    const wrapper = mountComponent(Home);

    await vi.waitFor(() => {
      expect(wrapper.find(".mdi-dots-horizontal").exists()).toBe(true);
    });

    experimentDataExportService.poll.mockResolvedValueOnce({
      id: 4,
      experimentId: 11,
      experimentTitle: "My Experiment",
      status: "PROCESSING"
    });

    let exportResultsItem = await openRowAction(wrapper, "Export Results");
    await exportResultsItem.trigger("click");

    await vi.waitFor(() => {
      expect(swalFire).toHaveBeenCalledWith(expect.objectContaining({
        text: expect.stringContaining("still being processed")
      }));
    });
    expect(experimentDataExportService.prepare).not.toHaveBeenCalled();

    swalFire.mockClear();
    experimentDataExportService.poll.mockResolvedValueOnce({
      id: 4,
      experimentId: 11,
      experimentTitle: "My Experiment",
      status: "REPROCESSING"
    });

    exportResultsItem = await openRowAction(wrapper, "Export Results");
    await exportResultsItem.trigger("click");

    await vi.waitFor(() => {
      expect(swalFire).toHaveBeenCalledWith(expect.objectContaining({
        text: expect.stringContaining("New submissons have occurred")
      }));
    });
    expect(experimentDataExportService.prepare).not.toHaveBeenCalled();
  });

  it("shows a ready data export alert with a download link and finishes the download flow without further prompts", async () => {
    experimentService.getAll.mockResolvedValue({
      status: 200,
      data: [experiment]
    });
    experimentDataExportService.pollList.mockResolvedValue([
      { id: 1, experimentId: 11, experimentTitle: "My Experiment", status: "READY" }
    ]);
    experimentDataExportService.poll.mockResolvedValue({
      id: 1,
      experimentId: 11,
      experimentTitle: "My Experiment",
      status: "READY"
    });

    const wrapper = mountComponent(Home);

    await vi.waitFor(() => {
      expect(wrapper.text()).toContain('Your data export for experiment "My Experiment" is ready.');
    });

    const downloadLink = wrapper.findAll("a")
      .find(a => a.text().includes("Click here to download") && !a.text().includes("new data export"));

    expect(downloadLink).not.toBeUndefined();
    await downloadLink.trigger("click");

    await vi.waitFor(() => {
      expect(experimentDataExportService.poll).toHaveBeenCalledWith(11, true);
    });
    // the ready/retrieve short-circuit never needs to prompt the user again
    expect(swalFire).not.toHaveBeenCalled();
  });

  it("shows an outdated data export alert with a recreate link and prepares a new export when confirmed", async () => {
    experimentService.getAll.mockResolvedValue({
      status: 200,
      data: [experiment]
    });
    experimentDataExportService.pollList.mockResolvedValue([
      { id: 2, experimentId: 11, experimentTitle: "My Experiment", status: "OUTDATED" }
    ]);
    experimentDataExportService.poll.mockResolvedValue({
      id: 2,
      experimentId: 11,
      experimentTitle: "My Experiment",
      status: "OUTDATED"
    });
    experimentDataExportService.prepare.mockResolvedValue({
      id: 3,
      experimentId: 11,
      experimentTitle: "My Experiment",
      status: "PROCESSING"
    });
    swalFire.mockResolvedValue({ isConfirmed: true });

    const wrapper = mountComponent(Home);

    await vi.waitFor(() => {
      expect(wrapper.text()).toContain(
        'There have been updates since the last requested data export for experiment "My Experiment".'
      );
    });

    const recreateLink = wrapper.findAll("a").find(a => a.text().includes("new data export"));
    expect(recreateLink).not.toBeUndefined();
    await recreateLink.trigger("click");

    await vi.waitFor(() => {
      expect(experimentDataExportService.prepare).toHaveBeenCalledWith(11);
    });
    expect(swalFire).toHaveBeenCalledWith(expect.objectContaining({
      text: expect.stringContaining("Depending on its size")
    }));
  });

  it("hides a still-processing data export alert without acknowledging it when dismissed", async () => {
    experimentService.getAll.mockResolvedValue({
      status: 200,
      data: [experiment]
    });
    experimentDataExportService.pollList.mockResolvedValue([
      { id: 5, experimentId: 11, experimentTitle: "My Experiment", status: "PROCESSING" }
    ]);

    const wrapper = mountComponent(Home);

    await vi.waitFor(() => {
      expect(wrapper.text()).toContain('The data export for experiment "My Experiment" is being processed.');
    });

    const findAlert = () => wrapper.findAllComponents({ name: "VAlert" })
      .find(a => a.attributes("aria-label") === "data export request alert for experiment 11");

    expect(findAlert()).not.toBeUndefined();

    findAlert().vm.$emit("click:close");
    await nextTick();

    expect(experimentDataExportService.acknowledge).not.toHaveBeenCalled();
    expect(findAlert()).toBeUndefined();
  });

  it("acknowledges the export request when dismissing ready, outdated, or error alerts", async () => {
    const readyExperiment = { ...experiment, experimentId: 12, title: "Ready Study" };
    const outdatedExperiment = { ...experiment, experimentId: 13, title: "Outdated Study" };
    const errorExperiment = { ...experiment, experimentId: 14, title: "Error Study" };

    experimentService.getAll.mockResolvedValue({
      status: 200,
      data: [readyExperiment, outdatedExperiment, errorExperiment]
    });
    experimentDataExportService.pollList.mockResolvedValue([
      { id: 20, experimentId: 12, experimentTitle: "Ready Study", status: "READY" },
      { id: 21, experimentId: 13, experimentTitle: "Outdated Study", status: "OUTDATED" },
      { id: 22, experimentId: 14, experimentTitle: "Error Study", status: "ERROR" }
    ]);

    const wrapper = mountComponent(Home);

    await vi.waitFor(() => {
      expect(wrapper.findAllComponents({ name: "VAlert" }).length).toBeGreaterThanOrEqual(3);
    });

    const dismiss = async experimentId => {
      const alert = wrapper.findAllComponents({ name: "VAlert" })
        .find(a => a.attributes("aria-label") === `data export request alert for experiment ${experimentId}`);
      alert.vm.$emit("click:close");
      await nextTick();
    };

    await dismiss(12);
    await dismiss(13);
    await dismiss(14);

    expect(experimentDataExportService.acknowledge).toHaveBeenCalledWith(12, 20, "READY_ACKNOWLEDGED");
    expect(experimentDataExportService.acknowledge).toHaveBeenCalledWith(13, 21, "OUTDATED_ACKNOWLEDGED");
    expect(experimentDataExportService.acknowledge).toHaveBeenCalledWith(14, 22, "ERROR_ACKNOWLEDGED");
  });

  it("polls a processing data export request on an interval and clears it once no longer active", async () => {
    const setIntervalSpy = vi.spyOn(window, "setInterval").mockReturnValue(9911);
    const clearIntervalSpy = vi.spyOn(window, "clearInterval").mockImplementation(() => {});

    experimentService.getAll.mockResolvedValue({ status: 200, data: [experiment] });
    experimentDataExportService.poll.mockResolvedValue(null);
    experimentDataExportService.prepare.mockResolvedValue({
      id: 30,
      experimentId: 11,
      experimentTitle: "My Experiment",
      status: "PROCESSING"
    });
    swalFire.mockResolvedValue({ isConfirmed: true });

    const wrapper = mountComponent(Home);

    // wait for the whole onMounted chain (including its own data-export polling
    // setup) to settle first, so it can't race with - and clobber - the polling
    // state this test is about to set up via the "Export Results" action
    await vi.waitFor(() => {
      expect(wrapper.findComponent({ name: "PageLoading" }).props("display")).toBe(false);
    });
    await vi.waitFor(() => {
      expect(wrapper.find(".mdi-dots-horizontal").exists()).toBe(true);
    });

    const exportResultsItem = await openRowAction(wrapper, "Export Results");
    await exportResultsItem.trigger("click");

    await vi.waitFor(() => {
      expect(experimentDataExportService.prepare).toHaveBeenCalledWith(11);
    });
    await vi.waitFor(() => {
      expect(setIntervalSpy).toHaveBeenCalled();
    });

    const pollCallback = setIntervalSpy.mock.calls.at(-1)[0];

    experimentDataExportService.poll.mockResolvedValueOnce({
      id: 30,
      experimentId: 11,
      experimentTitle: "My Experiment",
      status: "READY"
    });
    await pollCallback();
    await flushPromises();

    expect(experimentDataExportService.poll).toHaveBeenCalledWith(11, false);
    await vi.waitFor(() => {
      expect(clearIntervalSpy).toHaveBeenCalledWith(9911);
    });

    setIntervalSpy.mockRestore();
    clearIntervalSpy.mockRestore();
  });

  it("imports an experiment from a selected file and tracks the resulting import request", async () => {
    const pinia = createPinia();
    setActivePinia(pinia);
    configurationModule().$patch({
      configurations: { experimentExportEnabled: true }
    });

    swalFire.mockResolvedValue({ value: new File(["zip"], "experiment.zip") });
    experimentService.import.mockResolvedValue({ id: "import-happy", status: "PROCESSING" });

    const wrapper = mountComponent(Home, { pinia });

    // the "Import Experiment" button is present in the DOM (just v-show hidden)
    // even before onMounted finishes, and onMounted's own trailing pollImports()
    // call would otherwise race with - and wipe out - the import request this
    // test is about to create, so wait for it to fully settle first
    await vi.waitFor(() => {
      expect(wrapper.findComponent({ name: "PageLoading" }).props("display")).toBe(false);
    });

    const importButton = wrapper.findAll("button").find(button => button.text() === "Import Experiment");
    await importButton.trigger("click");

    await vi.waitFor(() => {
      expect(experimentService.import).toHaveBeenCalled();
    });
    expect(swalFire).toHaveBeenCalledWith(expect.objectContaining({
      title: "Import experiment from file"
    }));

    const [file] = experimentService.import.mock.calls[0];
    expect(file).toBeInstanceOf(File);
  });

  it("does not import anything when the file selection dialog is cancelled", async () => {
    const pinia = createPinia();
    setActivePinia(pinia);
    configurationModule().$patch({
      configurations: { experimentExportEnabled: true }
    });

    swalFire.mockResolvedValue({ value: undefined });

    const wrapper = mountComponent(Home, { pinia });

    await vi.waitFor(() => {
      expect(wrapper.findComponent({ name: "PageLoading" }).props("display")).toBe(false);
    });

    const importButton = wrapper.findAll("button").find(button => button.text() === "Import Experiment");
    await importButton.trigger("click");

    await flushPromises();
    expect(experimentService.import).not.toHaveBeenCalled();
  });

  it("renders import request alerts for completed and failed imports once experiments are loaded", async () => {
    const pinia = createPinia();
    setActivePinia(pinia);
    configurationModule().$patch({
      configurations: { experimentExportEnabled: true }
    });

    experimentService.getAll.mockResolvedValue({ status: 200, data: [experiment] });

    const wrapper = mountComponent(Home, { pinia });

    await vi.waitFor(() => {
      expect(wrapper.findComponent({ name: "PageLoading" }).props("display")).toBe(false);
    });

    const importButton = () => wrapper.findAll("button").find(button => button.text() === "Import Experiment");

    swalFire.mockResolvedValueOnce({ value: new File(["z"], "a.zip") });
    experimentService.import.mockResolvedValueOnce({
      id: "import-complete",
      status: "COMPLETE",
      sourceTitle: "Old Name",
      importedTitle: "New Name"
    });
    await importButton().trigger("click");

    await vi.waitFor(() => {
      expect(wrapper.text()).toContain(
        'Your import of experiment "Old Name" is complete. The new title is "New Name".'
      );
    });

    swalFire.mockResolvedValueOnce({ value: new File(["z"], "b.zip") });
    experimentService.import.mockResolvedValueOnce({
      id: "import-error",
      status: "ERROR",
      sourceTitle: "Broken Study",
      errorMessages: [
        { text: "bad row 1" },
        { text: "bad row 2" },
        { text: "bad row 3" },
        { text: "bad row 4" }
      ]
    });
    await importButton().trigger("click");

    await vi.waitFor(() => {
      expect(wrapper.text()).toContain(
        'There were errors in processing the import of experiment "Broken Study"'
      );
    });

    // ZeroState renders its own (identical) copy of importRequestAlerts too, so scope
    // the search to just one of the matching alerts rather than the whole wrapper
    const errorAlert = wrapper.findAllComponents({ name: "VAlert" })
      .find(alert => alert.text().includes("Broken Study"));
    const errorItems = errorAlert.findAll("li").map(li => li.text());
    expect(errorItems).toEqual(["bad row 1", "bad row 2", "bad row 3"]);
  });

  it("updates a zero-state import alert's tracked visibility, ignoring unknown ids", async () => {
    const pinia = createPinia();
    setActivePinia(pinia);
    configurationModule().$patch({
      configurations: { experimentExportEnabled: true }
    });

    swalFire.mockResolvedValue({ value: new File(["z"], "a.zip") });
    experimentService.import.mockResolvedValue({ id: "import-vis", status: "PROCESSING" });

    const wrapper = mountComponent(Home, { pinia });

    // wait for onMounted (including its trailing pollImports() call) to fully settle
    // before importing, so it can't race with - and wipe out - the import request
    // this test is about to create
    await vi.waitFor(() => {
      expect(wrapper.findComponent({ name: "PageLoading" }).props("display")).toBe(false);
    });

    const importButton = wrapper.findAll("button").find(button => button.text() === "OR IMPORT AN EXPERIMENT");
    await importButton.trigger("click");

    const zeroState = wrapper.findComponent({ name: "ZeroState" });
    await vi.waitFor(() => {
      expect(zeroState.props("experimentImportRequests")["import-vis"]).toBeDefined();
    });

    // an id that was never tracked (e.g. already dismissed) is a no-op
    zeroState.vm.$emit("handleImportRequestAlertVisibilityChange", "unknown-id", false);
    await nextTick();
    expect(zeroState.props("experimentImportRequests")["unknown-id"]).toBeUndefined();

    zeroState.vm.$emit("handleImportRequestAlertVisibilityChange", "import-vis", false);
    await nextTick();
    expect(zeroState.props("experimentImportRequests")["import-vis"].showAlert).toBe(false);
  });

  it("does not dismiss a still-processing zero-state import alert", async () => {
    const pinia = createPinia();
    setActivePinia(pinia);
    configurationModule().$patch({
      configurations: { experimentExportEnabled: true }
    });

    const wrapper = mountComponent(Home, { pinia });

    // wait for onMounted (including its trailing pollImports() call) to fully settle
    // before importing, so it can't race with - and wipe out - the import request
    // this test is about to create
    await vi.waitFor(() => {
      expect(wrapper.findComponent({ name: "PageLoading" }).props("display")).toBe(false);
    });

    const importButton = () => wrapper.findAll("button")
      .find(button => button.text() === "OR IMPORT AN EXPERIMENT");
    const zeroState = () => wrapper.findComponent({ name: "ZeroState" });

    swalFire.mockResolvedValueOnce({ value: new File(["z"], "p.zip") });
    experimentService.import.mockResolvedValueOnce({ id: "imp-processing", status: "PROCESSING" });
    await importButton().trigger("click");
    await vi.waitFor(() => {
      expect(zeroState().props("experimentImportRequests")["imp-processing"]).toBeDefined();
    });

    zeroState().vm.$emit("handleImportRequestAlertDismiss", "imp-processing");
    await flushPromises();
    // still-processing requests can't be dismissed yet
    expect(experimentService.acknowledgeImport).not.toHaveBeenCalled();
    expect(zeroState().props("experimentImportRequests")["imp-processing"]).toBeDefined();
  });

  it("acknowledges and clears completed and errored zero-state import alerts when dismissed", async () => {
    const pinia = createPinia();
    setActivePinia(pinia);
    configurationModule().$patch({
      configurations: { experimentExportEnabled: true }
    });

    const wrapper = mountComponent(Home, { pinia });

    await vi.waitFor(() => {
      expect(wrapper.findComponent({ name: "PageLoading" }).props("display")).toBe(false);
    });

    const importButton = () => wrapper.findAll("button")
      .find(button => button.text() === "OR IMPORT AN EXPERIMENT");
    const zeroState = () => wrapper.findComponent({ name: "ZeroState" });

    swalFire.mockResolvedValueOnce({ value: new File(["z"], "c.zip") });
    experimentService.import.mockResolvedValueOnce({
      id: "imp-complete",
      status: "COMPLETE",
      sourceTitle: "A",
      importedTitle: "B"
    });
    await importButton().trigger("click");
    await vi.waitFor(() => {
      expect(zeroState().props("experimentImportRequests")["imp-complete"]).toBeDefined();
    });

    zeroState().vm.$emit("handleImportRequestAlertDismiss", "imp-complete");
    // wait for the whole dismiss handler (acknowledge, then local removal) to finish -
    // checking "acknowledge was called" alone can resolve before the removal has happened
    await vi.waitFor(() => {
      expect(zeroState().props("experimentImportRequests")["imp-complete"]).toBeUndefined();
    });
    expect(experimentService.acknowledgeImport).toHaveBeenCalledWith("imp-complete", "COMPLETE_ACKNOWLEDGED");

    swalFire.mockResolvedValueOnce({ value: new File(["z"], "e.zip") });
    experimentService.import.mockResolvedValueOnce({
      id: "imp-error",
      status: "ERROR",
      sourceTitle: "C"
    });
    await importButton().trigger("click");
    await vi.waitFor(() => {
      expect(zeroState().props("experimentImportRequests")["imp-error"]).toBeDefined();
    });

    zeroState().vm.$emit("handleImportRequestAlertDismiss", "imp-error");
    await vi.waitFor(() => {
      expect(zeroState().props("experimentImportRequests")["imp-error"]).toBeUndefined();
    });
    expect(experimentService.acknowledgeImport).toHaveBeenCalledWith("imp-error", "ERROR_ACKNOWLEDGED");
  });

  it("polls a processing import request on an interval and refetches experiments once it completes", async () => {
    const setIntervalSpy = vi.spyOn(window, "setInterval").mockReturnValue(4242);
    const clearIntervalSpy = vi.spyOn(window, "clearInterval").mockImplementation(() => {});

    const pinia = createPinia();
    setActivePinia(pinia);
    configurationModule().$patch({
      configurations: { experimentExportEnabled: true }
    });

    swalFire.mockResolvedValue({ value: new File(["z"], "a.zip") });
    experimentService.import.mockResolvedValue({ id: "imp-poll", status: "PROCESSING" });
    experimentService.pollImport.mockResolvedValue({ data: { id: "imp-poll", status: "COMPLETE" } });

    const wrapper = mountComponent(Home, { pinia });

    await vi.waitFor(() => {
      expect(wrapper.findComponent({ name: "PageLoading" }).props("display")).toBe(false);
    });

    const importButton = wrapper.findAll("button").find(button => button.text() === "OR IMPORT AN EXPERIMENT");
    await importButton.trigger("click");

    await vi.waitFor(() => {
      expect(setIntervalSpy).toHaveBeenCalled();
    });

    const pollCallback = setIntervalSpy.mock.calls.at(-1)[0];
    await pollCallback();
    await flushPromises();

    expect(experimentService.pollImport).toHaveBeenCalledWith(["imp-poll"]);
    // completing the import refetches the experiment list
    expect(experimentService.getAll).toHaveBeenCalledTimes(2);

    setIntervalSpy.mockRestore();
    clearIntervalSpy.mockRestore();
  });

  it("clears active polling intervals for in-flight import requests when navigating away", async () => {
    const setIntervalSpy = vi.spyOn(window, "setInterval").mockReturnValue(5151);
    const clearIntervalSpy = vi.spyOn(window, "clearInterval").mockImplementation(() => {});

    const pinia = createPinia();
    setActivePinia(pinia);
    configurationModule().$patch({
      configurations: { experimentExportEnabled: true }
    });

    swalFire.mockResolvedValue({ value: new File(["z"], "a.zip") });
    experimentService.import.mockResolvedValue({ id: "imp-leave", status: "PROCESSING" });

    const wrapper = mountComponent(Home, { pinia });

    await vi.waitFor(() => {
      expect(wrapper.findComponent({ name: "PageLoading" }).props("display")).toBe(false);
    });

    const importButton = wrapper.findAll("button").find(button => button.text() === "OR IMPORT AN EXPERIMENT");
    await importButton.trigger("click");

    await vi.waitFor(() => {
      expect(setIntervalSpy).toHaveBeenCalled();
    });

    const routeLeaveCallback = onBeforeRouteLeave.mock.calls.at(-1)[0];
    const next = vi.fn();
    routeLeaveCallback(null, null, next);

    expect(clearIntervalSpy).toHaveBeenCalledWith(5151);
    expect(next).toHaveBeenCalled();

    setIntervalSpy.mockRestore();
    clearIntervalSpy.mockRestore();
  });

  // the copy-candidates selection dialog is switched off (COPY_CANDIDATE_SELECTION_ENABLED in
  // Home.vue) - kept, along with its tests, in case it's wanted again
  it.skip("skips resolved copy-candidate imports that have no id", async () => {
    const pinia = createPinia();
    setActivePinia(pinia);
    const store = experimentModule();
    const upsertSpy = vi.spyOn(store, "upsertImportRequest");

    experimentCopyCandidateService.getAll.mockResolvedValue({
      data: [{ id: "c1", experimentTitle: "Reading Study" }]
    });
    experimentCopyCandidateService.resolve.mockResolvedValue({
      data: { imports: [{ status: "PROCESSING" }], declinedCandidateIds: [] }
    });
    mountCopyCandidatesDialog();

    mountComponent(Home, { pinia });

    await vi.waitFor(() => {
      expect(document.querySelector(".copy-candidate-option")).not.toBeNull();
    });

    await clickCandidateOption(0);
    await clickCopyCandidatesAction("Create selected");
    await clickCopyCandidatesOverlayButton("Got it!");

    await vi.waitFor(() => {
      expect(experimentCopyCandidateService.resolve).toHaveBeenCalledWith(["c1"]);
    });
    await flushPromises();

    expect(upsertSpy).not.toHaveBeenCalled();
  });

  it("shows an error and does not navigate when creating a new experiment fails without an experiment id", async () => {
    experimentService.create.mockResolvedValue({ status: 500, data: {} });

    const wrapper = mountComponent(Home);

    await vi.waitFor(() => {
      expect(wrapper.text()).toContain("New Experiment");
    });

    const newExperimentButton = wrapper.findAll("button").find(button => button.text() === "New Experiment");
    await newExperimentButton.trigger("click");

    await vi.waitFor(() => {
      expect(swalFire).toHaveBeenCalledWith(expect.objectContaining({
        icon: "error",
        text: expect.stringContaining("Error Status: 500")
      }));
    });
    expect(push).not.toHaveBeenCalled();
  });

  it("logs and recovers when creating a new experiment throws", async () => {
    const pinia = createPinia();
    setActivePinia(pinia);
    const store = experimentModule();
    vi.spyOn(store, "createExperiment").mockRejectedValue(new Error("boom"));
    const consoleLogSpy = vi.spyOn(console, "log").mockImplementation(() => {});

    const wrapper = mountComponent(Home, { pinia });

    await vi.waitFor(() => {
      expect(wrapper.text()).toContain("New Experiment");
    });

    const newExperimentButton = wrapper.findAll("button").find(button => button.text() === "New Experiment");
    await newExperimentButton.trigger("click");

    await vi.waitFor(() => {
      expect(consoleLogSpy).toHaveBeenCalledWith(
        "startExperiment -> createExperiment | catch",
        expect.objectContaining({ error: expect.any(Error) })
      );
    });
    expect(push).not.toHaveBeenCalled();

    consoleLogSpy.mockRestore();
  });

  it("shows an error message when deleting an experiment fails", async () => {
    const pinia = createPinia();
    setActivePinia(pinia);
    const store = experimentModule();
    vi.spyOn(store, "deleteExperiment").mockRejectedValue(new Error("boom"));

    experimentService.getAll.mockResolvedValue({
      status: 200,
      data: [experiment]
    });
    swalFire.mockResolvedValue({ isConfirmed: true });

    const wrapper = mountComponent(Home, { pinia });

    await vi.waitFor(() => {
      expect(wrapper.find(".mdi-dots-horizontal").exists()).toBe(true);
    });

    const deleteItem = await openRowAction(wrapper, "Delete");
    await deleteItem.trigger("click");

    await vi.waitFor(() => {
      expect(swalFire).toHaveBeenCalledWith(expect.objectContaining({
        text: "Could not delete experiment.",
        icon: "error"
      }));
    });
  });

  it("makes sortable experiment table column headers keyboard-activatable", async () => {
    experimentService.getAll.mockResolvedValue({
      status: 200,
      data: [experiment]
    });

    const wrapper = mountComponent(Home, { attachTo: document.body });

    await vi.waitFor(() => {
      expect(wrapper.text()).toContain("My Experiment");
    });
    await flushPromises();
    await nextTick();

    const sortableHeaderSpan = document.querySelector(
      ".table-experiments th.v-data-table__th--sortable .v-data-table-header__content > span:not(.v-icon)"
    );

    expect(sortableHeaderSpan).not.toBeNull();
    expect(sortableHeaderSpan.getAttribute("tabindex")).toBe("0");

    const clickSpy = vi.spyOn(sortableHeaderSpan, "click");

    sortableHeaderSpan.dispatchEvent(new KeyboardEvent("keyup", { key: "Enter", bubbles: true }));
    expect(clickSpy).toHaveBeenCalledTimes(1);

    // non-Enter keys are ignored
    sortableHeaderSpan.dispatchEvent(new KeyboardEvent("keyup", { key: "a", bubbles: true }));
    expect(clickSpy).toHaveBeenCalledTimes(1);

    wrapper.unmount();
  });
});
