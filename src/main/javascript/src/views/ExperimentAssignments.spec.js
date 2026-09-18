import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("@/services", () => ({
  assignmentService: {
    saveAssignmentOrder: vi.fn(),
    updateAssignments: vi.fn(),
    deleteAssignment: vi.fn(),
    duplicateAssignment: vi.fn(),
    moveAssignment: vi.fn()
  },
  messageContainerService: {
    updateAll: vi.fn(),
    update: vi.fn(),
    deleteContainer: vi.fn(),
    move: vi.fn(),
    duplicate: vi.fn()
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

vi.mock("vue-router", () => ({
  useRouter: () => ({ push })
}));

const swalFire = vi.fn();

vi.mock("sweetalert2", () => ({
  default: { fire: (...args) => swalFire(...args) }
}));

import { createPinia, setActivePinia } from "pinia";
import { mountComponent } from "@/test-utils/mount";
import {
  assignmentService,
  messageContainerService,
  treatmentService,
  assessmentService
} from "@/services";
import { experiment as experimentModule } from "@/store/experiment.module";
import { exposures as exposuresModule } from "@/store/exposures.module";
import { assignment as assignmentModule } from "@/store/assignment.module";
import { configuration as configurationModule } from "@/store/configuration.module";
import { navigation as navigationModule } from "@/store/navigation.module";
import { container as messagingContainerModule } from "@/store/messaging/container.module";
import ExperimentAssignments from "./ExperimentAssignments.vue";

const experiment = { experimentId: 3, started: false };

const exposure = { exposureId: 50, exposureName: "Exposure 1" };

const assignmentRow = {
  assignmentId: 100,
  exposureId: 50,
  title: "Reading Quiz",
  assignmentOrder: 2,
  treatments: []
};

const messageContainer = {
  id: 200,
  exposureId: 50,
  messages: [],
  configuration: {
    order: 1,
    status: "PUBLISHED",
    title: "Welcome Message"
  }
};

const stubs = {
  ExposureTabs: true,
  ComponentTable: true,
  ExposureDesignCard: true,
  AddAssignmentDialog: true,
  AddMessageDialog: true
};

const seedStores = ({
  conditions = [{ conditionId: 1, defaultCondition: true }, { conditionId: 2 }],
  exposures = [exposure],
  assignments = [assignmentRow],
  messagingEnabled = false,
  messageContainers = [messageContainer]
} = {}) => {
  // experiment.module's `conditions` getter reads state.experiment.conditions
  // (nested), not a top-level `conditions` state field.
  experimentModule().$patch({ experiment: { ...experiment, conditions } });
  exposuresModule().$patch({ exposures });
  assignmentModule().$patch({ assignments });
  configurationModule().$patch({
    configurations: { messagingEnabled }
  });
  messagingContainerModule().$patch({ messageContainers });
};

// mountComponent creates a fresh pinia per call unless one is passed explicitly.
// Since seedStores() patches store state *before* mounting, we need every store
// access (both seeding and mounting) to share the same pinia instance, or the
// seeded state ends up on an orphaned instance the mounted component never sees.
let pinia;

const mountAssignments = (props = {}, options = {}) => mountComponent(ExperimentAssignments, {
  props: {
    experiment,
    balanced: true,
    activeExposureSet: 0,
    ...props
  },
  global: { stubs },
  pinia,
  ...options
});

describe("ExperimentAssignments", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    swalFire.mockReset();
    pinia = createPinia();
    setActivePinia(pinia);
    seedStores();
  });

  // the real, user-visible loading wait happens one level up, in
  // ExperimentSummary.vue - by the time this component mounts, its parent has
  // already fully resolved `experiment` and every store this reads from. This
  // component's own `loaded` ref just waits one tick for `tab` to be set from
  // activeExposureSet before rendering, so the wrong exposure tab's content
  // never flashes on mount.
  it("waits until the active tab is set before showing the exposure content", async () => {
    const wrapper = mountAssignments();

    expect(wrapper.findComponent({ name: "ExposureTabs" }).exists()).toBe(false);

    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();

    expect(wrapper.findComponent({ name: "ExposureTabs" }).exists()).toBe(true);
  });

  it("initializes the active tab from the activeExposureSet prop", async () => {
    const wrapper = mountAssignments({ activeExposureSet: 0 });
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();

    const tabs = wrapper.findComponent({ name: "ExposureTabs" });
    expect(tabs.props("modelValue")).toBe(0);
  });

  it("passes conditions, exposures, and balanced state down to ExposureTabs", async () => {
    const wrapper = mountAssignments();
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();

    const tabs = wrapper.findComponent({ name: "ExposureTabs" });
    expect(tabs.props("exposures")).toEqual([exposure]);
    expect(tabs.props("balanced")).toBe(true);
    expect(tabs.props("singleConditionExperiment")).toBe(false);
  });

  it("assembles assignment and message-container rows for the active exposure, sorted by order", async () => {
    seedStores({ messagingEnabled: true });

    const wrapper = mountAssignments();
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();

    const table = wrapper.findComponent({ name: "ComponentTable" });
    const rows = table.props("rows");

    expect(rows).toHaveLength(2);
    // messageContainer has order 1, assignmentRow has order 2 - sorted ascending
    expect(rows[0].type).toBe("message");
    expect(rows[0].title).toBe("Welcome Message");
    expect(rows[1].type).toBe("assignment");
    expect(rows[1].title).toBe("Reading Quiz");
  });

  it("excludes message-container rows when messaging is disabled", async () => {
    seedStores({ messagingEnabled: false });

    const wrapper = mountAssignments();
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();

    const rows = wrapper.findComponent({ name: "ComponentTable" }).props("rows");

    expect(rows).toHaveLength(1);
    expect(rows[0].type).toBe("assignment");
  });

  it("shows the 'no components yet' card and hides ComponentTable when the exposure has no rows", async () => {
    seedStores({ assignments: [], messageContainers: [], messagingEnabled: false });

    const wrapper = mountAssignments();
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();

    expect(wrapper.find(".no-assignments-yet").exists()).toBe(true);
    expect(wrapper.findComponent({ name: "ComponentTable" }).exists()).toBe(false);
  });

  it("marks canDeleteAssignment false once the experiment has started", async () => {
    const wrapper = mountAssignments({
      experiment: { ...experiment, started: true }
    });
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();

    expect(
      wrapper.findComponent({ name: "ComponentTable" }).props("canDeleteAssignment")
    ).toBe(false);
  });

  it("navigates to AssignmentCreateAssignment for a single-version assignment and saves edit mode", async () => {
    const wrapper = mountAssignments();
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();

    const dialog = wrapper.findComponent({ name: "AddAssignmentDialog" });
    dialog.vm.$emit("single");
    await wrapper.vm.$nextTick();

    const navigationStore = navigationModule();
    expect(navigationStore.editMode).toMatchObject({
      initialPage: "AssignmentCreateAssignment"
    });
    expect(push).toHaveBeenCalledWith({
      name: "AssignmentCreateAssignment",
      params: { exposureId: 50 },
      query: { conditionIds: JSON.stringify([1]) }
    });
  });

  it("navigates to AssignmentCreateAssignment for a multi-version assignment with all group condition ids", async () => {
    seedStores({
      exposures: [
        { ...exposure, groupConditionList: [{ conditionId: 1 }, { conditionId: 2 }] }
      ]
    });

    const wrapper = mountAssignments();
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();

    const dialog = wrapper.findComponent({ name: "AddAssignmentDialog" });
    dialog.vm.$emit("multiple");
    await wrapper.vm.$nextTick();

    expect(push).toHaveBeenCalledWith({
      name: "AssignmentCreateAssignment",
      params: { exposureId: 50 },
      query: { conditionIds: JSON.stringify([1, 2]) }
    });
  });

  it("deletes an assignment component after confirmation", async () => {
    swalFire.mockResolvedValue({ isConfirmed: true });
    assignmentService.deleteAssignment.mockResolvedValue({ status: 200 });

    const wrapper = mountAssignments();
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();

    const table = wrapper.findComponent({ name: "ComponentTable" });
    // The event payload must be one of the *computed* rows (which carry the
    // `type: "assignment"` discriminator the handler branches on) rather than
    // the raw fixture, which has no `type` field.
    const row = table.props("rows")[0];
    table.vm.$emit("delete", row);

    await vi.waitFor(() => {
      expect(assignmentService.deleteAssignment).toHaveBeenCalledWith(
        3,
        50,
        100
      );
    });
  });

  it("does not delete an assignment component when the confirmation is dismissed", async () => {
    swalFire.mockResolvedValue({ isConfirmed: false });

    const wrapper = mountAssignments();
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();

    const table = wrapper.findComponent({ name: "ComponentTable" });
    const row = table.props("rows")[0];
    table.vm.$emit("delete", row);
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();

    expect(assignmentService.deleteAssignment).not.toHaveBeenCalled();
  });

  it("publishes a message-container component", async () => {
    seedStores({ messagingEnabled: true });
    messageContainerService.update.mockResolvedValue({
      ...messageContainer,
      configuration: { ...messageContainer.configuration, status: "PUBLISHED" }
    });

    const wrapper = mountAssignments();
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();

    const messageRow = wrapper
      .findComponent({ name: "ComponentTable" })
      .props("rows")
      .find(row => row.type === "message");

    const table = wrapper.findComponent({ name: "ComponentTable" });
    table.vm.$emit("publish", messageRow);

    await vi.waitFor(() => {
      expect(messageContainerService.update).toHaveBeenCalled();
    });

    const [, , , payload] = messageContainerService.update.mock.calls[0];
    expect(payload.configuration.status).toBe("PUBLISHED");
  });

  it("saves the new component order for an exposure", async () => {
    seedStores({
      assignments: [
        { ...assignmentRow, assignmentId: 100, assignmentOrder: 1 },
        { ...assignmentRow, assignmentId: 101, assignmentOrder: 2 }
      ]
    });
    assignmentService.updateAssignments.mockResolvedValue([]);
    messageContainerService.updateAll.mockResolvedValue([]);

    const wrapper = mountAssignments();
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();

    const table = wrapper.findComponent({ name: "ComponentTable" });

    table.vm.$emit(
      "save-order",
      { oldDraggableIndex: 0, newDraggableIndex: 1 }
    );

    await vi.waitFor(() => {
      expect(assignmentService.updateAssignments).toHaveBeenCalled();
    });

    const [experimentId, exposureId, updated] =
      assignmentService.updateAssignments.mock.calls[0];
    expect(experimentId).toBe(3);
    expect(exposureId).toBe(50);
    expect(updated.map(row => row.assignmentId)).toEqual([101, 100]);
  });

  // ComponentTable.vue's own handleDragKeydown (tested in ComponentTable.spec.js)
  // is what supplies focusAssignmentId - this test covers the other half of that
  // contract: saveOrder using it to put focus back on the right row's drag handle
  // after componentTableKey's forced remount drops focus to the document body.
  // ComponentTable is stubbed in this file (see `stubs` above), so there's no real
  // drag-handle button to refocus - a standalone element with the matching
  // data-drag-handle attribute, attached to document.body, stands in for it.
  it("restores focus to the moved row's drag handle after a keyboard-triggered reorder, and announces the new position", async () => {
    seedStores({
      assignments: [
        { ...assignmentRow, assignmentId: 100, assignmentOrder: 1, title: "First" },
        { ...assignmentRow, assignmentId: 101, assignmentOrder: 2, title: "Second" }
      ]
    });
    assignmentService.updateAssignments.mockResolvedValue([]);
    messageContainerService.updateAll.mockResolvedValue([]);

    const handleStandIn = document.createElement("button");
    handleStandIn.setAttribute("data-drag-handle", "100");
    document.body.appendChild(handleStandIn);

    const wrapper = mountAssignments({}, { attachTo: document.body });
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();

    const table = wrapper.findComponent({ name: "ComponentTable" });

    table.vm.$emit(
      "save-order",
      { oldDraggableIndex: 0, newDraggableIndex: 1, focusAssignmentId: 100 }
    );

    await vi.waitFor(() => {
      expect(document.activeElement).toBe(handleStandIn);
    });

    expect(wrapper.text()).toContain("First moved to position 2 of 2.");

    wrapper.unmount();
    handleStandIn.remove();
  });

  it("opens a preview window for a treatment", async () => {
    const openSpy = vi.spyOn(window, "open").mockImplementation(() => {});

    const wrapper = mountAssignments();
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();

    const table = wrapper.findComponent({ name: "ComponentTable" });
    table.vm.$emit("preview-treatment", { conditionId: 7, treatmentId: 9 });

    expect(openSpy).toHaveBeenCalledWith(
      expect.stringContaining("/preview/experiments/3/conditions/7/treatments/9"),
      "_blank"
    );

    openSpy.mockRestore();
  });

  it("creates a treatment and assessment for a missing condition, then navigates to the builder like Edit does", async () => {
    treatmentService.create.mockResolvedValue({
      status: 201,
      data: { treatmentId: 55, conditionId: 2, assignmentId: 100 }
    });
    assessmentService.fetchAssessments.mockResolvedValue({ data: [] });
    assessmentService.createAssessment.mockResolvedValue({
      status: 201,
      data: { assessmentId: 77 }
    });

    const wrapper = mountAssignments();
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();

    const table = wrapper.findComponent({ name: "ComponentTable" });
    const row = table.props("rows").find(item => item.type === "assignment");

    table.vm.$emit("add-treatment", { row, condition: { conditionId: 2 } });

    await vi.waitFor(() => {
      expect(treatmentService.create).toHaveBeenCalledWith(3, 2, 100);
    });

    await vi.waitFor(() => {
      expect(assessmentService.createAssessment).toHaveBeenCalledWith(3, 2, 55);
    });

    await vi.waitFor(() => {
      expect(push).toHaveBeenCalledWith(
        expect.objectContaining({
          name: "TerracottaBuilder",
          params: expect.objectContaining({
            conditionId: 2,
            treatmentId: 55,
            assessmentId: 77
          })
        })
      );
    });
  });

  it("shows an error alert and does not navigate when creating the treatment fails", async () => {
    treatmentService.create.mockResolvedValue({ status: 500 });

    const wrapper = mountAssignments();
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();

    const table = wrapper.findComponent({ name: "ComponentTable" });
    const row = table.props("rows").find(item => item.type === "assignment");

    table.vm.$emit("add-treatment", { row, condition: { conditionId: 2 } });

    await vi.waitFor(() => {
      expect(treatmentService.create).toHaveBeenCalled();
    });

    expect(assessmentService.createAssessment).not.toHaveBeenCalled();
    expect(push).not.toHaveBeenCalledWith(
      expect.objectContaining({ name: "TerracottaBuilder" })
    );
  });
});
