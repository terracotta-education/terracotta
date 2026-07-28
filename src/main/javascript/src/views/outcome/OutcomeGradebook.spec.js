import { describe, expect, it, vi, beforeEach } from "vitest";

const push = vi.fn();

vi.mock("vue-router", () => ({
  useRoute: () => ({
    params: {
      experimentId: "10",
      exposureId: "20"
    }
  }),
  useRouter: () => ({
    push
  })
}));

vi.mock("@/services", () => ({
  outcomeService: {
    getAll: vi.fn(),
    getOutcomePotentials: vi.fn(),
    create: vi.fn()
  }
}));

const swal = vi.fn();

vi.mock("sweetalert2", () => ({
  default: { fire: (...args) => swal(...args) }
}));

import { createPinia, setActivePinia } from "pinia";
import { mountComponent } from "@/test-utils/mount";
import OutcomeGradebook from "./OutcomeGradebook.vue";
import { outcomeService } from "@/services";
import { experiment as experimentModule } from "@/store/experiment.module";

const flush = () => new Promise(resolve => setTimeout(resolve));

const potentials = [
  { assignmentId: 1, name: "Quiz 1", pointsPossible: 10, type: "quiz" },
  { assignmentId: 2, name: "Quiz 2", pointsPossible: 20, type: "quiz" }
];

describe("OutcomeGradebook", () => {
  beforeEach(() => {
    push.mockClear();
    swal.mockClear();
    outcomeService.getAll.mockReset().mockResolvedValue({
      status: 200,
      data: []
    });
    outcomeService.getOutcomePotentials.mockReset().mockResolvedValue({
      status: 200,
      data: potentials
    });
    outcomeService.create.mockReset();
  });

  const mountView = () => {
    const pinia = createPinia();
    setActivePinia(pinia);

    experimentModule().experiment = { experimentId: 10 };

    return mountComponent(OutcomeGradebook, { pinia });
  };

  it("shows a loading state until the outcome potentials resolve", async () => {
    let resolvePotentials;
    outcomeService.getOutcomePotentials.mockReset().mockReturnValue(
      new Promise(resolve => {
        resolvePotentials = resolve;
      })
    );

    const wrapper = mountView();
    await flush();

    expect(wrapper.text()).toContain("Loading gradebook items");

    resolvePotentials({ status: 200, data: potentials });
    await flush();
    await wrapper.vm.$nextTick();

    expect(wrapper.text()).not.toContain("Loading gradebook items");
  });

  it("renders the fetched gradebook items in the table", async () => {
    const wrapper = mountView();
    await flush();
    await wrapper.vm.$nextTick();

    expect(wrapper.text()).toContain("Quiz 1");
    expect(wrapper.text()).toContain("Quiz 2");
    expect(wrapper.text()).toContain("10");
    expect(wrapper.text()).toContain("20");
  });

  it("shows a check icon instead of a checkbox for potentials that already have an outcome", async () => {
    outcomeService.getAll.mockResolvedValue({
      status: 200,
      data: [{ lmsOutcomeId: 1, exposureId: 20 }]
    });

    const wrapper = mountView();
    await flush();
    await wrapper.vm.$nextTick();

    const rows = wrapper.findAll("tbody tr");
    expect(rows[0].find(".mdi-check").exists()).toBe(true);
    expect(rows[0].findComponent({ name: "VCheckbox" }).exists()).toBe(false);

    expect(rows[1].find(".mdi-check").exists()).toBe(false);
    expect(rows[1].findComponent({ name: "VCheckbox" }).exists()).toBe(true);
  });

  it("select-all selects every eligible potential and deselecting clears them", async () => {
    const wrapper = mountView();
    await flush();
    await wrapper.vm.$nextTick();

    const selectAllCheckbox = wrapper.findComponent({ name: "VCheckbox" });
    await selectAllCheckbox.setValue(true);

    expect(wrapper.vm.selectedAssignmentIds).toEqual([1, 2]);

    await selectAllCheckbox.setValue(false);

    expect(wrapper.vm.selectedAssignmentIds).toEqual([]);
  });

  it("saveExit creates an outcome for each selected assignment and navigates with a success alert", async () => {
    outcomeService.create.mockResolvedValue({
      status: 201,
      data: { outcomeId: 99 }
    });

    const wrapper = mountView();
    await flush();
    await wrapper.vm.$nextTick();

    wrapper.vm.selectedAssignmentIds = [1];

    await wrapper.vm.saveExit();
    await flush();

    expect(outcomeService.create).toHaveBeenCalledWith(
      10,
      20,
      "Quiz 1",
      10,
      true,
      "quiz",
      1
    );
    expect(push).toHaveBeenCalledWith({
      name: "ExperimentSummary",
      params: expect.objectContaining({
        alertMessage: "Outcomes created successfully."
      })
    });
  });

  it("saveExit navigates without alert params when nothing was selected", async () => {
    const wrapper = mountView();
    await flush();
    await wrapper.vm.$nextTick();

    await wrapper.vm.saveExit();

    expect(push).toHaveBeenCalledWith({
      name: "ExperimentSummary",
      params: {}
    });
  });

  it("shows an error and does not navigate when the create call fails", async () => {
    outcomeService.create.mockRejectedValue(new Error("boom"));

    const wrapper = mountView();
    await flush();
    await wrapper.vm.$nextTick();

    wrapper.vm.selectedAssignmentIds = [1];

    await wrapper.vm.saveExit();
    await flush();

    expect(swal).toHaveBeenCalledWith(
      expect.objectContaining({
        text: "An error occurred while creating outcomes. Please try again."
      })
    );
    expect(push).not.toHaveBeenCalled();
  });

  it("saveExit navigates with a success alert message when outcomes are created successfully", async () => {
    outcomeService.create.mockResolvedValue({
      status: 201,
      data: { outcomeId: 1 }
    });

    const wrapper = mountView();
    await flush();
    await wrapper.vm.$nextTick();

    wrapper.vm.selectedAssignmentIds = [1];

    await wrapper.vm.saveExit();
    await flush();

    expect(push).toHaveBeenCalledWith({
      name: "ExperimentSummary",
      params: expect.objectContaining({
        alertMessage: "Outcomes created successfully."
      })
    });
  });
});
