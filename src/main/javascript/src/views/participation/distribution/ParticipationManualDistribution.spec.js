import { afterEach, describe, expect, it, vi } from "vitest";
import { flushPromises } from "@vue/test-utils";

const push = vi.fn();

vi.mock("vue-router", () => ({
  useRouter: () => ({
    push
  }),
  onBeforeRouteUpdate: vi.fn()
}));

vi.mock("sweetalert2", () => ({
  default: {
    fire: vi.fn().mockResolvedValue({})
  }
}));

vi.mock("@/services", () => ({
  exposuresService: {
    getAll: vi.fn()
  },
  participantService: {
    getAll: vi.fn(),
    updateParticipants: vi.fn()
  }
}));

import Swal from "sweetalert2";
import { exposuresService, participantService } from "@/services";
import { mountComponent } from "@/test-utils/mount";
import ParticipationManualDistribution from "./ParticipationManualDistribution.vue";
import { navigation as navigationModule } from "@/store/navigation.module";

const buildExperiment = (overrides = {}) => ({
  experimentId: 1,
  conditions: [
    { conditionId: 1, name: "Control" },
    { conditionId: 2, name: "Treatment" }
  ],
  ...overrides
});

const buildExposures = () => [
  {
    exposureId: 1,
    groupConditionList: [
      { groupId: 10, conditionName: "Control" },
      { groupId: 20, conditionName: "Treatment" }
    ]
  }
];

const buildParticipant = (participantId, groupId, consent = true) => ({
  participantId,
  consent,
  dropped: false,
  groupId,
  user: { userId: `u${participantId}`, displayName: `Student ${participantId}` }
});

const mountView = async experimentOverrides => {
  const wrapper = mountComponent(ParticipationManualDistribution, {
    props: { experiment: buildExperiment(experimentOverrides) }
  });

  await flushPromises();
  await wrapper.vm.$nextTick();

  return wrapper;
};

describe("ParticipationManualDistribution", () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it("fetches exposures and participants on mount", async () => {
    exposuresService.getAll.mockResolvedValue(buildExposures());
    participantService.getAll.mockResolvedValue([]);

    await mountView();

    expect(exposuresService.getAll).toHaveBeenCalledWith(1);
    expect(participantService.getAll).toHaveBeenCalled();
  });

  it("shows an error alert when the participants fetch fails", async () => {
    exposuresService.getAll.mockResolvedValue(buildExposures());
    participantService.getAll.mockRejectedValue(new Error("network error"));

    await mountView();

    expect(Swal.fire).toHaveBeenCalledWith("Error loading participants");
  });

  it("groups consenting participants under their assigned condition and shows the counts", async () => {
    exposuresService.getAll.mockResolvedValue(buildExposures());
    participantService.getAll.mockResolvedValue([
      buildParticipant(1, 10),
      buildParticipant(2, 20),
      buildParticipant(3, null)
    ]);

    const wrapper = await mountView();

    expect(wrapper.text()).toContain("Control (1)");
    expect(wrapper.text()).toContain("Treatment (1)");
    // the unassigned list renders outside the (collapsed) expansion panels
    expect(wrapper.text()).toContain("Student 3");

    // expand the Control panel to confirm its assigned participant shows up
    const titles = wrapper.findAllComponents({ name: "VExpansionPanelTitle" });
    await titles[0].trigger("click");
    await wrapper.vm.$nextTick();

    expect(wrapper.text()).toContain("Student 1");
  });

  it("excludes non-consenting participants from any group", async () => {
    exposuresService.getAll.mockResolvedValue(buildExposures());
    participantService.getAll.mockResolvedValue([
      buildParticipant(1, 10, true),
      buildParticipant(2, 20, false)
    ]);

    const wrapper = await mountView();

    expect(wrapper.text()).toContain("Control (1)");
    expect(wrapper.text()).not.toContain("Student 2");
  });

  it("moves a participant from unassigned to a condition through ListParticipants", async () => {
    exposuresService.getAll.mockResolvedValue(buildExposures());
    participantService.getAll.mockResolvedValue([
      buildParticipant(1, null)
    ]);

    const wrapper = await mountView();

    // the unassigned list is always rendered outside the expansion panels
    const participantItem = wrapper.find(".participant-item");
    await participantItem.trigger("click");

    const moveButton = wrapper.findAllComponents({ name: "VBtn" }).find(
      button => button.text() === "MOVE TO"
    );
    await moveButton.trigger("click");
    await wrapper.vm.$nextTick();

    const controlOption = wrapper
      .findAllComponents({ name: "VListItemTitle" })
      .find(title => title.text() === "Control");

    await controlOption.trigger("click");
    await wrapper.vm.$nextTick();

    expect(wrapper.text()).toContain("Control (1)");
  });

  it("submits the distribution mapped to each condition's groupId and navigates on Continue", async () => {
    exposuresService.getAll.mockResolvedValue(buildExposures());
    participantService.getAll.mockResolvedValue([
      buildParticipant(1, 10),
      buildParticipant(2, 20),
      buildParticipant(3, null)
    ]);
    participantService.updateParticipants.mockResolvedValue({ status: 200 });

    const wrapper = await mountView();

    const continueButton = wrapper
      .findAllComponents({ name: "VBtn" })
      .find(button => button.text() === "Continue");

    await continueButton.trigger("click");
    await flushPromises();
    await wrapper.vm.$nextTick();

    expect(participantService.updateParticipants).toHaveBeenCalledWith(
      1,
      expect.arrayContaining([
        expect.objectContaining({ participantId: 1, groupId: 10 }),
        expect.objectContaining({ participantId: 2, groupId: 20 }),
        expect.objectContaining({ participantId: 3, groupId: null })
      ])
    );

    expect(push).toHaveBeenCalledWith({
      name: "ParticipationSummary",
      params: { experiment: 1 }
    });
  });

  it("shows an error alert when submitting the distribution fails", async () => {
    exposuresService.getAll.mockResolvedValue(buildExposures());
    participantService.getAll.mockResolvedValue([buildParticipant(1, 10)]);
    participantService.updateParticipants.mockResolvedValue({
      status: 400,
      error: "Could not update participants"
    });

    const wrapper = await mountView();

    const continueButton = wrapper
      .findAllComponents({ name: "VBtn" })
      .find(button => button.text() === "Continue");

    await continueButton.trigger("click");
    await flushPromises();

    expect(Swal.fire).toHaveBeenCalledWith("Could not update participants");
    expect(push).not.toHaveBeenCalled();
  });

  it("saveExit submits the distribution and routes to the caller page from edit mode", async () => {
    exposuresService.getAll.mockResolvedValue(buildExposures());
    participantService.getAll.mockResolvedValue([buildParticipant(1, 10)]);
    participantService.updateParticipants.mockResolvedValue({ status: 200 });

    const wrapper = await mountView();

    const navigationStore = navigationModule();
    navigationStore.editMode = { callerPage: { name: "ParticipationSummary" } };

    wrapper.vm.saveExit();
    await flushPromises();

    expect(participantService.updateParticipants).toHaveBeenCalled();
    expect(push).toHaveBeenCalledWith({
      name: "ParticipationSummary",
      params: { experiment: 1 }
    });
  });
});
