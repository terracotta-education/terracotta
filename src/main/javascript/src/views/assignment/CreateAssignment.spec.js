import { describe, expect, it, vi, beforeEach } from "vitest";
import { createPinia, setActivePinia } from "pinia";

const push = vi.fn();
const route = {
  params: { experimentId: "10", exposureId: "30" },
  query: { conditionIds: JSON.stringify([1, 2]) }
};

vi.mock("vue-router", () => ({
  useRoute: () => route,
  useRouter: () => ({ push })
}));

const fire = vi.fn().mockResolvedValue({ isConfirmed: true });
vi.mock("sweetalert2", () => ({
  default: { fire: (...args) => fire(...args) }
}));

vi.mock("@/services", () => ({
  assignmentService: { create: vi.fn() },
  treatmentService: { create: vi.fn() },
  assessmentService: { fetchAssessments: vi.fn(), createAssessment: vi.fn() }
}));

import { mountComponent } from "@/test-utils/mount";
import CreateAssignment from "./CreateAssignment.vue";
import {
  assignmentService,
  treatmentService,
  assessmentService
} from "@/services";
import { assignment as assignmentModule } from "@/store/assignment.module";

const AssignmentSettingsStub = {
  name: "AssignmentSettings",
  template: "<div class=\"assignment-settings-stub\" />"
};

function mountCreate() {
  const pinia = createPinia();
  setActivePinia(pinia);

  const wrapper = mountComponent(CreateAssignment, {
    pinia,
    global: {
      stubs: { AssignmentSettings: AssignmentSettingsStub }
    }
  });

  return { wrapper, assignmentStore: assignmentModule() };
}

describe("CreateAssignment", () => {
  beforeEach(() => {
    push.mockClear();
    fire.mockClear();
    assignmentService.create.mockReset();
    treatmentService.create.mockReset();
    assessmentService.fetchAssessments.mockReset();
    assessmentService.createAssessment.mockReset();
  });

  it("resets numOfSubmissions to null on mount so multiple attempts default off", () => {
    const { assignmentStore } = mountCreate();

    expect(assignmentStore.assignment).toEqual({ numOfSubmissions: null });
  });

  it("binds the title field to the assignment store", async () => {
    const { wrapper, assignmentStore } = mountCreate();

    const titleField = wrapper.findComponent({ name: "VTextField" });
    await titleField.find("input").setValue("My new assignment");

    expect(assignmentStore.assignment.title).toBe("My new assignment");
  });

  it("creates the assignment, a treatment and assessment per condition, then navigates to ExperimentSummary", async () => {
    assignmentService.create.mockResolvedValue({
      assignmentId: 99,
      title: "My new assignment"
    });
    treatmentService.create
      .mockResolvedValueOnce({
        status: 201,
        data: { treatmentId: 501, conditionId: 1 }
      })
      .mockResolvedValueOnce({
        status: 201,
        data: { treatmentId: 502, conditionId: 2 }
      });
    assessmentService.fetchAssessments.mockResolvedValue({ data: [] });
    assessmentService.createAssessment.mockResolvedValue({
      status: 201,
      data: { assessmentId: 900 }
    });

    const { wrapper, assignmentStore } = mountCreate();
    assignmentStore.setAssignment({ title: "My new assignment" });

    await wrapper.vm.saveExit();
    await new Promise(resolve => setTimeout(resolve));

    expect(assignmentService.create).toHaveBeenCalledWith(
      10,
      30,
      expect.objectContaining({ title: "My new assignment" }),
      1
    );

    expect(treatmentService.create).toHaveBeenCalledTimes(2);
    expect(treatmentService.create).toHaveBeenCalledWith(10, 1, 99);
    expect(treatmentService.create).toHaveBeenCalledWith(10, 2, 99);

    expect(assessmentService.createAssessment).toHaveBeenCalledTimes(2);

    expect(push).toHaveBeenCalledWith({
      name: "ExperimentSummary",
      params: { experimentId: 10 }
    });
  });

  it("shows an error and does not navigate when assignment creation fails", async () => {
    assignmentService.create.mockResolvedValue({ status: 400, data: "bad" });

    const { wrapper, assignmentStore } = mountCreate();
    assignmentStore.setAssignment({ title: "My new assignment" });

    await wrapper.vm.saveExit();
    await new Promise(resolve => setTimeout(resolve));

    expect(fire).toHaveBeenCalled();
    expect(push).not.toHaveBeenCalled();
    expect(treatmentService.create).not.toHaveBeenCalled();
  });

  it("shows a clear error and does not navigate when the assignment store swallows a service rejection", async () => {
    // assignmentStore.createAssignment catches its own rejections and
    // resolves to null, so CreateAssignment falls into the
    // `response?.status !== 201` branch instead of the outer catch.
    assignmentService.create.mockRejectedValue(new Error("network down"));

    const { wrapper } = mountCreate();

    await wrapper.vm.saveExit();
    await new Promise(resolve => setTimeout(resolve));

    expect(fire).toHaveBeenCalledWith(
      "There was an error creating the assignment. Please try again."
    );
    expect(push).not.toHaveBeenCalled();
  });
});
