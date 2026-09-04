import { describe, expect, it, vi, beforeEach } from "vitest";
import { createPinia, setActivePinia } from "pinia";

const push = vi.fn();
const route = {
  params: { assignmentId: "20", exposureId: "30" }
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
  assignmentService: { fetchAssignment: vi.fn() },
  treatmentService: { create: vi.fn(), fetchTreatment: vi.fn() },
  assessmentService: { fetchAssessments: vi.fn(), createAssessment: vi.fn() }
}));

import { mountComponent } from "@/test-utils/mount";
import AssignmentTreatmentSelect from "./AssignmentTreatmentSelect.vue";
import {
  assignmentService,
  treatmentService,
  assessmentService
} from "@/services";
import { assignment as assignmentModule } from "@/store/assignment.module";
import { experiment as experimentModule } from "@/store/experiment.module";

const experiment = {
  experimentId: 10,
  conditions: [
    { conditionId: 1, name: "Control" },
    { conditionId: 2, name: "Treatment" }
  ]
};

// The condition list lives inside a v-expansion-panel that starts collapsed
// (display: none), so it must be opened before its list items are visible.
async function expandPanel(wrapper) {
  await wrapper.find(".v-expansion-panel-title").trigger("click");
  await wrapper.vm.$nextTick();
}

function mountSelect() {
  const pinia = createPinia();
  setActivePinia(pinia);

  const experimentStore = experimentModule();
  experimentStore.setExperiment(experiment);

  const wrapper = mountComponent(AssignmentTreatmentSelect, {
    pinia,
    props: { experiment }
  });

  return {
    wrapper,
    assignmentStore: assignmentModule(),
    experimentStore
  };
}

describe("AssignmentTreatmentSelect", () => {
  beforeEach(() => {
    push.mockClear();
    fire.mockClear();
    assignmentService.fetchAssignment.mockReset();
    treatmentService.create.mockReset();
    treatmentService.fetchTreatment.mockReset();
    assessmentService.fetchAssessments.mockReset();
    assessmentService.createAssessment.mockReset();
  });

  it("fetches the assignment and checks each condition for an existing treatment on mount", async () => {
    assignmentService.fetchAssignment.mockResolvedValue({
      assignmentId: 20,
      title: "Quiz"
    });
    treatmentService.fetchTreatment.mockResolvedValue({ status: 200, data: [] });

    const { wrapper } = mountSelect();
    await wrapper.vm.$nextTick();
    await new Promise(resolve => setTimeout(resolve));

    expect(assignmentService.fetchAssignment).toHaveBeenCalledWith(10, 30, 20);
    expect(treatmentService.fetchTreatment).toHaveBeenCalledWith(10, 1);
    expect(treatmentService.fetchTreatment).toHaveBeenCalledWith(10, 2);
  });

  it("renders 'no conditions' when the experiment has none", async () => {
    assignmentService.fetchAssignment.mockResolvedValue({
      assignmentId: 20,
      title: "Quiz"
    });

    const pinia = createPinia();
    setActivePinia(pinia);
    experimentModule().setExperiment({ experimentId: 10, conditions: [] });

    const wrapper = mountComponent(AssignmentTreatmentSelect, {
      pinia,
      props: { experiment: { experimentId: 10, conditions: [] } }
    });
    await wrapper.vm.$nextTick();
    await new Promise(resolve => setTimeout(resolve));

    expect(wrapper.text()).toContain("no conditions");
  });

  it("shows a Create button for a condition without a treatment yet, and an edit button once one exists", async () => {
    assignmentService.fetchAssignment.mockResolvedValue({
      assignmentId: 20,
      title: "Quiz"
    });
    treatmentService.fetchTreatment
      .mockResolvedValueOnce({
        status: 200,
        data: [{ assignmentId: 20, treatmentId: 501 }]
      })
      .mockResolvedValueOnce({ status: 200, data: [] });

    const { wrapper } = mountSelect();
    await wrapper.vm.$nextTick();
    await new Promise(resolve => setTimeout(resolve));
    await wrapper.vm.$nextTick();
    await expandPanel(wrapper);

    const listItems = wrapper.findAllComponents({ name: "VListItem" });
    expect(listItems).toHaveLength(2);

    // condition 1 already has a matching treatment -> pencil/edit icon button
    expect(listItems[0].text()).not.toContain("Create");
    // condition 2 has no treatment yet -> Create button
    expect(listItems[1].text()).toContain("Create");
  });

  it("creates a treatment and assessment then navigates to TerracottaBuilder when Create is clicked", async () => {
    assignmentService.fetchAssignment.mockResolvedValue({
      assignmentId: 20,
      title: "Quiz"
    });
    treatmentService.fetchTreatment.mockResolvedValue({ status: 200, data: [] });
    treatmentService.create.mockResolvedValue({
      status: 201,
      data: { treatmentId: 501, conditionId: 1, assignmentId: 20 }
    });
    assessmentService.fetchAssessments.mockResolvedValue({ data: [] });
    assessmentService.createAssessment.mockResolvedValue({
      status: 201,
      data: { assessmentId: 900 }
    });

    const { wrapper } = mountSelect();
    await wrapper.vm.$nextTick();
    await new Promise(resolve => setTimeout(resolve));
    await wrapper.vm.$nextTick();
    await expandPanel(wrapper);

    const createButtons = wrapper
      .findAllComponents({ name: "VBtn" })
      .filter(btn => btn.text().includes("Create"));

    await createButtons[0].trigger("click");
    await new Promise(resolve => setTimeout(resolve));

    expect(treatmentService.create).toHaveBeenCalledWith(10, 1, 20);
    expect(assessmentService.createAssessment).toHaveBeenCalledWith(10, 1, 501);
    expect(push).toHaveBeenCalledWith({
      name: "TerracottaBuilder",
      params: {
        experimentId: 10,
        exposureId: 30,
        assignmentId: 20,
        conditionId: 1,
        treatmentId: 501,
        assessmentId: 900
      }
    });
  });

  it("shows an error and does not navigate when treatment creation fails", async () => {
    assignmentService.fetchAssignment.mockResolvedValue({
      assignmentId: 20,
      title: "Quiz"
    });
    treatmentService.fetchTreatment.mockResolvedValue({ status: 200, data: [] });
    treatmentService.create.mockResolvedValue({ status: 400, data: "bad" });

    const { wrapper } = mountSelect();
    await wrapper.vm.$nextTick();
    await new Promise(resolve => setTimeout(resolve));
    await wrapper.vm.$nextTick();
    await expandPanel(wrapper);

    const createButtons = wrapper
      .findAllComponents({ name: "VBtn" })
      .filter(btn => btn.text().includes("Create"));

    await createButtons[0].trigger("click");
    await new Promise(resolve => setTimeout(resolve));

    expect(fire).toHaveBeenCalled();
    expect(push).not.toHaveBeenCalled();
  });

  it("saveExit pushes to Home", async () => {
    assignmentService.fetchAssignment.mockResolvedValue({
      assignmentId: 20,
      title: "Quiz"
    });
    treatmentService.fetchTreatment.mockResolvedValue({ status: 200, data: [] });

    const { wrapper } = mountSelect();
    await wrapper.vm.$nextTick();
    await new Promise(resolve => setTimeout(resolve));

    wrapper.vm.saveExit();

    expect(push).toHaveBeenCalledWith({ name: "Home" });
  });
});
