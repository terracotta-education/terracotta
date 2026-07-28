import { describe, expect, it, vi, beforeEach } from "vitest";

const push = vi.fn();
const currentRoute = {
  value: {
    meta: { previousStep: "ExperimentOutcomesOverview" }
  }
};

vi.mock("vue-router", () => ({
  useRoute: () => ({
    params: {
      experimentId: "10",
      exposureId: "20",
      outcomeId: "30"
    }
  }),
  useRouter: () => ({
    push,
    currentRoute
  })
}));

vi.mock("@/services", () => ({
  outcomeService: {
    getById: vi.fn(),
    getOutcomeScoresById: vi.fn(),
    updateOutcome: vi.fn(),
    updateOutcomeScores: vi.fn()
  },
  participantService: {
    getAll: vi.fn()
  }
}));

const swal = vi.fn();

vi.mock("sweetalert2", () => ({
  default: { fire: (...args) => swal(...args) }
}));

import { createPinia, setActivePinia } from "pinia";
import { mountComponent } from "@/test-utils/mount";
import OutcomeScoring from "./OutcomeScoring.vue";
import { outcomeService, participantService } from "@/services";
import { experiment as experimentModule } from "@/store/experiment.module";
import { exposures as exposuresModule } from "@/store/exposures.module";

const flush = () => new Promise(resolve => setTimeout(resolve));

const outcomeData = {
  outcomeId: 30,
  title: "Reading comprehension",
  maxPoints: 100
};

const participants = [
  {
    participantId: 1,
    user: { displayName: "Bob Long" }
  },
  {
    participantId: 2,
    user: { displayName: "Alice Adams" }
  }
];

describe("OutcomeScoring", () => {
  beforeEach(() => {
    push.mockClear();
    swal.mockClear();

    outcomeService.getById.mockReset().mockResolvedValue({
      status: 200,
      data: { ...outcomeData }
    });
    outcomeService.getOutcomeScoresById.mockReset().mockResolvedValue({
      status: 200,
      data: []
    });
    outcomeService.updateOutcome.mockReset().mockResolvedValue({
      status: 200
    });
    outcomeService.updateOutcomeScores.mockReset().mockResolvedValue({
      status: 200
    });
    participantService.getAll.mockReset().mockResolvedValue(participants);
  });

  const mountView = () => {
    const pinia = createPinia();
    setActivePinia(pinia);

    experimentModule().experiment = { experimentId: 10 };
    exposuresModule().exposures = [
      { exposureId: 20, title: "Exposure One" }
    ];

    return mountComponent(OutcomeScoring, { pinia });
  };

  it("renders nothing until the outcome has loaded (loading state)", () => {
    outcomeService.getById.mockReturnValue(new Promise(() => {}));

    const wrapper = mountView();

    expect(wrapper.find("form").exists()).toBe(false);
  });

  it("renders the exposure title and outcome fields once loaded", async () => {
    const wrapper = mountView();
    await flush();
    await wrapper.vm.$nextTick();

    expect(wrapper.text()).toContain("Exposure One");

    const titleField = wrapper.findComponent({ name: "VTextField" });
    expect(titleField.props("modelValue")).toBe("Reading comprehension");
  });

  it("sorts participants by last name and renders a score field for each", async () => {
    outcomeService.getOutcomeScoresById.mockResolvedValue({
      status: 200,
      data: [
        { outcomeId: 30, participantId: 1, participantScoreId: 1, scoreNumeric: 80 }
      ]
    });

    const wrapper = mountView();
    await flush();
    await wrapper.vm.$nextTick();

    const rows = wrapper.findAll("tbody tr");
    expect(rows).toHaveLength(2);
    // Alice Adams should sort before Bob Long by last name.
    expect(rows[0].text()).toContain("Alice Adams");
    expect(rows[1].text()).toContain("Bob Long");
  });

  it("reacts to a fetch error by not rendering the form (outcome stays unset)", async () => {
    outcomeService.getById.mockResolvedValue({ status: 404 });

    const wrapper = mountView();
    await flush();
    await wrapper.vm.$nextTick();

    expect(wrapper.find("form").exists()).toBe(false);
  });

  it("blocks save and shows an error dialog when the title is empty", async () => {
    const wrapper = mountView();
    await flush();
    await wrapper.vm.$nextTick();

    const titleField = wrapper.findComponent({ name: "VTextField" });
    await titleField.setValue("");

    await wrapper.vm.saveExit();

    expect(swal).toHaveBeenCalledWith(
      expect.objectContaining({
        text: "Could not update outcome due to entered data.",
        icon: "error"
      })
    );
    expect(outcomeService.updateOutcome).not.toHaveBeenCalled();
    expect(push).not.toHaveBeenCalled();
  });

  it("blocks save when a score exceeds the outcome's max points", async () => {
    outcomeService.getOutcomeScoresById.mockResolvedValue({
      status: 200,
      data: [
        { outcomeId: 30, participantId: 1, outcomeScoreId: 1, scoreNumeric: 150 }
      ]
    });

    const wrapper = mountView();
    await flush();
    await wrapper.vm.$nextTick();

    await wrapper.vm.saveExit();

    expect(swal).toHaveBeenCalled();
    expect(outcomeService.updateOutcome).not.toHaveBeenCalled();
  });

  it("saves the outcome and scores then navigates to the previous step on success", async () => {
    const wrapper = mountView();
    await flush();
    await wrapper.vm.$nextTick();

    await wrapper.vm.saveExit();
    await flush();

    expect(outcomeService.updateOutcome).toHaveBeenCalledWith(
      10,
      20,
      expect.objectContaining({ outcomeId: 30 })
    );
    expect(outcomeService.updateOutcomeScores).toHaveBeenCalledWith(
      10,
      20,
      30,
      expect.any(Array)
    );
    expect(push).toHaveBeenCalledWith({
      name: "ExperimentOutcomesOverview",
      params: expect.objectContaining({
        alertMessage: "Outcome and scores updated successfully."
      })
    });
  });
});
