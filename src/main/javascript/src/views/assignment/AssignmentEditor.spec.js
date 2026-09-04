import { describe, expect, it, vi, beforeEach } from "vitest";
import { createPinia, setActivePinia } from "pinia";

const push = vi.fn();
const routeParams = {
  experimentId: "10",
  assignmentId: "20",
  exposureId: "30"
};

vi.mock("vue-router", () => ({
  useRoute: () => ({ params: routeParams }),
  useRouter: () => ({ push })
}));

const fire = vi.fn().mockResolvedValue({ isConfirmed: true });
vi.mock("sweetalert2", () => ({
  default: { fire: (...args) => fire(...args) }
}));

vi.mock("@/services", () => ({
  assignmentService: {
    fetchAssignment: vi.fn(),
    updateAssignment: vi.fn()
  }
}));

import { mountComponent } from "@/test-utils/mount";
import AssignmentEditor from "./AssignmentEditor.vue";
import { assignmentService } from "@/services";
import { assignment as assignmentModule } from "@/store/assignment.module";
import { navigation as navigationModule } from "@/store/navigation.module";

const AssignmentSettingsStub = {
  name: "AssignmentSettings",
  template: "<div class=\"assignment-settings-stub\" />"
};

// VTabs renders its own internal VBtn per tab, so a plain
// findComponent({ name: "VBtn" }) can match the tab instead of the actual
// "Continue" action button. Find it by its visible text instead.
function findContinueButton(wrapper) {
  return wrapper
    .findAllComponents({ name: "VBtn" })
    .find(button => button.text().includes("Continue"));
}

function mountEditor(initialAssignment = { assignmentId: 20, title: "" }) {
  const pinia = createPinia();
  setActivePinia(pinia);

  const assignmentStore = assignmentModule();

  if (initialAssignment) {
    assignmentStore.setAssignment(initialAssignment);
  }

  const wrapper = mountComponent(AssignmentEditor, {
    pinia,
    props: {
      experiment: { experimentId: 10 }
    },
    global: {
      stubs: { AssignmentSettings: AssignmentSettingsStub }
    }
  });

  return {
    wrapper,
    assignmentStore,
    navigationStore: navigationModule()
  };
}

describe("AssignmentEditor", () => {
  beforeEach(() => {
    push.mockClear();
    fire.mockClear();
    assignmentService.fetchAssignment.mockReset();
    assignmentService.updateAssignment.mockReset();
  });

  it("mounts without throwing when the assignment store starts out null", async () => {
    assignmentService.fetchAssignment.mockResolvedValue({
      assignmentId: 20,
      title: "Fetched title"
    });

    const { wrapper } = mountEditor(null);

    expect(wrapper.findComponent({ name: "VTextField" }).props("modelValue")).toBe("");

    await vi.waitFor(() => {
      expect(wrapper.findComponent({ name: "VTextField" }).props("modelValue")).toBe(
        "Fetched title"
      );
    });
  });

  it("typing in the title field writes through to the assignment store", async () => {
    assignmentService.fetchAssignment.mockResolvedValue({
      assignmentId: 20,
      title: "My assignment"
    });

    const { wrapper, assignmentStore } = mountEditor({ assignmentId: 20, title: "My assignment" });
    await wrapper.vm.$nextTick();

    await wrapper.findComponent({ name: "VTextField" }).setValue("Updated title");

    expect(assignmentStore.assignment.title).toBe("Updated title");
  });

  it("fetches the assignment on mount using the route params", async () => {
    assignmentService.fetchAssignment.mockResolvedValue({
      assignmentId: 20,
      title: "My assignment"
    });

    const { wrapper } = mountEditor();
    await wrapper.vm.$nextTick();
    await new Promise(resolve => setTimeout(resolve));

    expect(assignmentService.fetchAssignment).toHaveBeenCalledWith(10, 30, 20);
  });

  it("disables the Continue button while the title is blank", async () => {
    assignmentService.fetchAssignment.mockResolvedValue({
      assignmentId: 20,
      title: ""
    });

    const { wrapper } = mountEditor();
    await wrapper.vm.$nextTick();
    await new Promise(resolve => setTimeout(resolve));
    await wrapper.vm.$nextTick();

    const button = findContinueButton(wrapper);
    expect(button.props("disabled")).toBe(true);
  });

  it("enables Continue once a title is present and saves on click", async () => {
    assignmentService.fetchAssignment.mockResolvedValue({
      assignmentId: 20,
      title: "My assignment"
    });
    assignmentService.updateAssignment.mockResolvedValue({
      assignmentId: 20,
      title: "My assignment",
      status: 200
    });

    const { wrapper } = mountEditor({ assignmentId: 20, title: "My assignment" });
    await wrapper.vm.$nextTick();
    await new Promise(resolve => setTimeout(resolve));
    await wrapper.vm.$nextTick();

    const button = findContinueButton(wrapper);
    expect(button.props("disabled")).toBe(false);

    await button.trigger("click");
    await new Promise(resolve => setTimeout(resolve));

    expect(assignmentService.updateAssignment).toHaveBeenCalledWith(
      10,
      30,
      20,
      expect.objectContaining({ title: "My assignment" })
    );

    expect(push).toHaveBeenCalledWith({
      name: "AssignmentYourAssignments",
      params: { experiment: 10, exposureId: 30 }
    });
  });

  it("shows an error via Swal and does not navigate when saving fails with a 400", async () => {
    assignmentService.fetchAssignment.mockResolvedValue({
      assignmentId: 20,
      title: "My assignment"
    });
    assignmentService.updateAssignment.mockResolvedValue({
      status: 400,
      data: "Bad title"
    });

    const { wrapper } = mountEditor({ assignmentId: 20, title: "My assignment" });
    await wrapper.vm.$nextTick();
    await new Promise(resolve => setTimeout(resolve));
    await wrapper.vm.$nextTick();

    const button = findContinueButton(wrapper);
    await button.trigger("click");
    await new Promise(resolve => setTimeout(resolve));

    expect(fire).toHaveBeenCalledWith({ status: 400, data: "Bad title" });
    expect(push).not.toHaveBeenCalled();
  });

  it("hides the Continue button while in edit mode", async () => {
    assignmentService.fetchAssignment.mockResolvedValue({
      assignmentId: 20,
      title: "My assignment"
    });

    const { wrapper, navigationStore } = mountEditor({ assignmentId: 20, title: "My assignment" });
    navigationStore.saveEditMode({ callerPage: { name: "ExperimentSummary" } });
    await wrapper.vm.$nextTick();

    expect(findContinueButton(wrapper)).toBeUndefined();
  });

  it("exposes saveExit which saves and routes to the caller page from edit mode", async () => {
    assignmentService.fetchAssignment.mockResolvedValue({
      assignmentId: 20,
      title: "My assignment"
    });
    assignmentService.updateAssignment.mockResolvedValue({
      assignmentId: 20,
      title: "My assignment",
      status: 200
    });

    const { wrapper, navigationStore } = mountEditor({ assignmentId: 20, title: "My assignment" });
    navigationStore.saveEditMode({ callerPage: { name: "ExperimentSummary" } });
    await wrapper.vm.$nextTick();
    await new Promise(resolve => setTimeout(resolve));

    await wrapper.vm.saveExit();
    await new Promise(resolve => setTimeout(resolve));

    expect(push).toHaveBeenCalledWith({
      name: "ExperimentSummary",
      params: { experiment: 10 }
    });
  });

  it("saveExit is a no-op when the title is blank", async () => {
    assignmentService.fetchAssignment.mockResolvedValue({
      assignmentId: 20,
      title: ""
    });

    const { wrapper } = mountEditor();
    await wrapper.vm.$nextTick();
    await new Promise(resolve => setTimeout(resolve));

    await wrapper.vm.saveExit();

    expect(assignmentService.updateAssignment).not.toHaveBeenCalled();
    expect(push).not.toHaveBeenCalled();
  });
});
