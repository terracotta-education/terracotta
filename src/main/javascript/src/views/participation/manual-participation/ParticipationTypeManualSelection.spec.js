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
  participantService: {
    getAll: vi.fn(),
    updateParticipants: vi.fn()
  }
}));

import Swal from "sweetalert2";
import { participantService } from "@/services";
import { mountComponent } from "@/test-utils/mount";
import ParticipationTypeManualSelection from "./ParticipationTypeManualSelection.vue";
import { navigation as navigationModule } from "@/store/navigation.module";

const buildExperiment = (overrides = {}) => ({
  experimentId: 1,
  conditions: [
    { conditionId: 1, name: "Control" },
    { conditionId: 2, name: "Treatment" }
  ],
  ...overrides
});

const buildParticipant = (participantId, consent) => ({
  participantId,
  consent,
  dropped: false,
  groupId: null,
  user: { userId: `u${participantId}`, displayName: `Student ${participantId}` }
});

const mountView = async experimentOverrides => {
  const wrapper = mountComponent(ParticipationTypeManualSelection, {
    props: { experiment: buildExperiment(experimentOverrides) }
  });

  await flushPromises();
  await wrapper.vm.$nextTick();

  return wrapper;
};

describe("ParticipationTypeManualSelection", () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it("fetches participants on mount", async () => {
    participantService.getAll.mockResolvedValue([]);

    await mountView();

    expect(participantService.getAll).toHaveBeenCalledWith(1, true);
  });

  it("groups participants into participating, not participating, and unassigned", async () => {
    participantService.getAll.mockResolvedValue([
      buildParticipant(1, true),
      buildParticipant(2, false),
      buildParticipant(3, null)
    ]);

    const wrapper = await mountView();

    expect(wrapper.text()).toContain("Participating (1)");
    expect(wrapper.text()).toContain("Not participating (1)");
    expect(wrapper.text()).toContain("Unassigned (1)");
  });

  it("moves an unassigned participant into Participating through ListParticipants", async () => {
    participantService.getAll.mockResolvedValue([
      buildParticipant(1, null)
    ]);

    const wrapper = await mountView();

    // Unassigned is the third panel; expand it first
    const titles = wrapper.findAllComponents({ name: "VExpansionPanelTitle" });
    await titles[2].trigger("click");
    await wrapper.vm.$nextTick();

    const participantItem = wrapper.find(".participant-item");
    await participantItem.trigger("click");

    const moveButton = wrapper.findAllComponents({ name: "VBtn" }).find(
      button => button.text() === "MOVE TO"
    );
    await moveButton.trigger("click");
    await wrapper.vm.$nextTick();

    const participatingOption = wrapper
      .findAllComponents({ name: "VListItemTitle" })
      .find(title => title.text() === "Participating");

    await participatingOption.trigger("click");
    await wrapper.vm.$nextTick();

    expect(wrapper.text()).toContain("Participating (1)");
    expect(wrapper.text()).toContain("Unassigned (0)");
  });

  it("submits the participants and navigates to ParticipationDistribution for a multi-condition experiment", async () => {
    participantService.getAll.mockResolvedValue([
      buildParticipant(1, true)
    ]);
    participantService.updateParticipants.mockResolvedValue({ status: 200 });

    const wrapper = await mountView();

    const continueButton = wrapper
      .findAllComponents({ name: "VBtn" })
      .find(button => button.text() === "Continue");

    await continueButton.trigger("click");
    await flushPromises();

    expect(participantService.updateParticipants).toHaveBeenCalledWith(
      1,
      expect.arrayContaining([
        expect.objectContaining({ participantId: 1, consent: true })
      ])
    );
    expect(push).toHaveBeenCalledWith({
      name: "ParticipationDistribution",
      params: { experiment: 1 }
    });
  });

  it("skips straight to ParticipationSummary for a single-condition experiment", async () => {
    participantService.getAll.mockResolvedValue([
      buildParticipant(1, true)
    ]);
    participantService.updateParticipants.mockResolvedValue({ status: 200 });

    const wrapper = await mountView({
      conditions: [{ conditionId: 1, name: "Control" }]
    });

    const continueButton = wrapper
      .findAllComponents({ name: "VBtn" })
      .find(button => button.text() === "Continue");

    await continueButton.trigger("click");
    await flushPromises();

    expect(push).toHaveBeenCalledWith({
      name: "ParticipationSummary",
      params: { experiment: 1 }
    });
  });

  it("shows an error alert when submitting participants fails", async () => {
    participantService.getAll.mockResolvedValue([
      buildParticipant(1, true)
    ]);
    participantService.updateParticipants.mockResolvedValue({
      status: 400,
      error: "Could not save participants"
    });

    const wrapper = await mountView();

    const continueButton = wrapper
      .findAllComponents({ name: "VBtn" })
      .find(button => button.text() === "Continue");

    await continueButton.trigger("click");
    await flushPromises();

    expect(Swal.fire).toHaveBeenCalledWith("Could not save participants");
    expect(push).not.toHaveBeenCalled();
  });

  it("saveExit submits participants and routes to the caller page from edit mode", async () => {
    participantService.getAll.mockResolvedValue([
      buildParticipant(1, true)
    ]);
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
