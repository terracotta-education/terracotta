import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";
import { flushPromises } from "@vue/test-utils";

const { routeParams, routerPush } = vi.hoisted(() => ({
  routeParams: { exposureId: "0" },
  routerPush: vi.fn()
}));

vi.mock("vue-router", () => ({
  useRoute: () => ({ params: routeParams }),
  useRouter: () => ({ push: routerPush })
}));

const swalFire = vi.fn(() => Promise.resolve({ isConfirmed: true }));
vi.mock("sweetalert2", () => ({
  default: {
    fire: (...args) => swalFire(...args)
  }
}));

vi.mock("@/services", () => ({
  exposuresService: {
    getAll: vi.fn(),
    createExposures: vi.fn()
  },
  assignmentService: {
    fetchAssignmentsByExposure: vi.fn(),
    deleteAssignment: vi.fn(() => Promise.resolve({ status: 200 }))
  }
}));

import { mountComponent } from "@/test-utils/mount";
import YourAssignments from "./YourAssignments.vue";
import { assignment as assignmentModule } from "@/store/assignment.module";
import { experiment as experimentModule } from "@/store/experiment.module";
import { exposures as exposuresModule } from "@/store/exposures.module";

let pinia;
let experimentStore;

const exposureA = { exposureId: 1, title: "Exposure A" };
const exposureB = { exposureId: 2, title: "Exposure B" };

const assignmentFor = (exposureId, id, treatmentCount) => ({
  assignmentId: id,
  exposureId,
  title: `Assignment ${id}`,
  treatments: Array.from({ length: treatmentCount }, (_, i) => ({ treatmentId: i }))
});

const expandAllPanels = async wrapper => {
  const titles = wrapper.findAllComponents({ name: "VExpansionPanelTitle" });

  for (const title of titles) {
    await title.trigger("click");
  }

  await flushPromises();
};

const mountView = async () => {
  const wrapper = mountComponent(YourAssignments, {
    pinia,
    props: {
      experiment: { experimentId: 5 }
    }
  });

  await flushPromises();

  return wrapper;
};

describe("YourAssignments", () => {
  beforeEach(async () => {
    pinia = createPinia();
    setActivePinia(pinia);
    assignmentModule();
    experimentStore = experimentModule();
    exposuresModule();

    routeParams.exposureId = "0";
    vi.clearAllMocks();
    swalFire.mockImplementation(() => Promise.resolve({ isConfirmed: true }));

    experimentStore.setExperiment({
      experimentId: 5,
      conditions: [{ conditionId: 1 }, { conditionId: 2 }]
    });

    const { exposuresService, assignmentService } = await import("@/services");
    exposuresService.getAll.mockResolvedValue([exposureA, exposureB]);
    assignmentService.fetchAssignmentsByExposure.mockImplementation((experimentId, exposureId) => {
      if (exposureId === exposureA.exposureId) {
        return Promise.resolve([assignmentFor(1, 100, 2)]);
      }

      if (exposureId === exposureB.exposureId) {
        return Promise.resolve([assignmentFor(2, 200, 2)]);
      }

      return Promise.resolve([]);
    });
  });

  it("resets assignments and fetches exposures/assignments for the experiment on mount", async () => {
    await mountView();

    const { exposuresService, assignmentService } = await import("@/services");

    expect(exposuresService.getAll).toHaveBeenCalledWith(5);
    expect(assignmentService.fetchAssignmentsByExposure).toHaveBeenCalledWith(5, 1);
    expect(assignmentService.fetchAssignmentsByExposure).toHaveBeenCalledWith(5, 2);
  });

  it("renders nothing when the experiment has no exposures", async () => {
    const { exposuresService } = await import("@/services");
    exposuresService.getAll.mockResolvedValue([]);

    const wrapper = await mountView();

    expect(wrapper.find("h1").exists()).toBe(false);
  });

  it("renders a panel per exposure with a complete/total treatment count", async () => {
    const wrapper = await mountView();

    expect(wrapper.find("h1").text()).toBe("Your Components");

    const panelTitles = wrapper.findAllComponents({ name: "VExpansionPanelTitle" });
    expect(panelTitles.length).toBe(2);
    expect(panelTitles[0].text()).toContain("Exposure A");
    expect(panelTitles[0].text()).toContain("(1/1)");
    expect(panelTitles[1].text()).toContain("(1/1)");
  });

  it("flags an exposure as unbalanced when it has fewer components than others", async () => {
    const { assignmentService } = await import("@/services");
    assignmentService.fetchAssignmentsByExposure.mockImplementation((experimentId, exposureId) => {
      if (exposureId === exposureA.exposureId) {
        return Promise.resolve([
          assignmentFor(1, 100, 2),
          assignmentFor(1, 101, 2)
        ]);
      }

      if (exposureId === exposureB.exposureId) {
        return Promise.resolve([assignmentFor(2, 200, 2)]);
      }

      return Promise.resolve([]);
    });

    const wrapper = await mountView();
    await expandAllPanels(wrapper);

    expect(wrapper.text()).toContain("Add a component to balance the experiment");
  });

  it("flags an exposure as incomplete when a component is missing a treatment for every condition", async () => {
    const { assignmentService } = await import("@/services");
    assignmentService.fetchAssignmentsByExposure.mockImplementation((experimentId, exposureId) => {
      if (exposureId === exposureA.exposureId) {
        return Promise.resolve([assignmentFor(1, 100, 1)]);
      }

      if (exposureId === exposureB.exposureId) {
        return Promise.resolve([assignmentFor(2, 200, 1)]);
      }

      return Promise.resolve([]);
    });

    const wrapper = await mountView();
    await expandAllPanels(wrapper);

    expect(wrapper.text()).toContain("Create a treatment for all conditions");
  });

  it("disables the Finish button while any assignment is missing treatments", async () => {
    const { assignmentService } = await import("@/services");
    assignmentService.fetchAssignmentsByExposure.mockImplementation((experimentId, exposureId) => {
      if (exposureId === exposureA.exposureId) {
        return Promise.resolve([assignmentFor(1, 100, 1)]);
      }

      return Promise.resolve([assignmentFor(2, 200, 2)]);
    });

    const wrapper = await mountView();

    const finishButtons = wrapper.findAllComponents({ name: "VBtn" })
      .filter(btn => btn.text() === "Finish");

    expect(finishButtons[0].props("disabled")).toBe(true);
  });

  it("enables the Finish button once every exposure is balanced and complete", async () => {
    const wrapper = await mountView();

    const finishButtons = wrapper.findAllComponents({ name: "VBtn" })
      .filter(btn => btn.text() === "Finish");

    expect(finishButtons[0].props("disabled")).toBe(false);
  });

  it("redirects to AssignmentCreateAssignment when the routed exposure has no assignments yet", async () => {
    routeParams.exposureId = "2";

    const { assignmentService } = await import("@/services");
    assignmentService.fetchAssignmentsByExposure.mockImplementation((experimentId, exposureId) => {
      if (exposureId === exposureA.exposureId) {
        return Promise.resolve([assignmentFor(1, 100, 2)]);
      }

      // exposure B (the routed one) has no assignments yet
      return Promise.resolve([]);
    });

    const wrapper = await mountView();

    expect(routerPush).toHaveBeenCalledWith({
      name: "AssignmentCreateAssignment",
      params: { exposureId: 2 }
    });
    expect(wrapper.find("h1").exists()).toBe(false);
  });

  it("does not redirect when the routed exposure already has assignments", async () => {
    routeParams.exposureId = "1";

    const wrapper = await mountView();

    expect(routerPush).not.toHaveBeenCalled();
    expect(wrapper.find("h1").exists()).toBe(true);
  });

  it("deletes an assignment after the user confirms the Swal dialog", async () => {
    const wrapper = await mountView();
    await expandAllPanels(wrapper);

    const deleteButtons = wrapper.findAllComponents({ name: "VBtn" })
      .filter(btn => btn.props("icon") === "mdi-delete");

    await deleteButtons[0].trigger("click");
    await flushPromises();

    const { assignmentService } = await import("@/services");
    expect(swalFire).toHaveBeenCalled();
    expect(assignmentService.deleteAssignment).toHaveBeenCalledWith(5, 1, 100);
  });

  it("does not delete an assignment when the user cancels the Swal dialog", async () => {
    swalFire.mockImplementation(() => Promise.resolve({ isConfirmed: false }));

    const wrapper = await mountView();
    await expandAllPanels(wrapper);

    const deleteButtons = wrapper.findAllComponents({ name: "VBtn" })
      .filter(btn => btn.props("icon") === "mdi-delete");

    await deleteButtons[0].trigger("click");
    await flushPromises();

    const { assignmentService } = await import("@/services");
    expect(assignmentService.deleteAssignment).not.toHaveBeenCalled();
  });

  it("exposes saveExit, which navigates back Home", async () => {
    const wrapper = await mountView();

    wrapper.vm.saveExit();

    expect(routerPush).toHaveBeenCalledWith({ name: "Home" });
  });
});
