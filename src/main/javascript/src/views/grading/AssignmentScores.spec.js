import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

const pushMock = vi.fn();
const currentRoute = { value: { meta: { previousStep: "SomeStep" } } };

vi.mock("vue-router", () => ({
  useRoute: () => ({
    params: { experimentId: "1", exposureId: "2", assignmentId: "3" }
  }),
  useRouter: () => ({ push: pushMock, currentRoute })
}));

vi.mock("@/services", () => ({
  assignmentService: {
    fetchAssignment: vi.fn()
  },
  participantService: {
    getAll: vi.fn()
  },
  assignmentFileArchiveService: {
    prepare: vi.fn(),
    poll: vi.fn(),
    retrieve: vi.fn(),
    acknowledgeError: vi.fn()
  }
}));

import { assignmentService, participantService, assignmentFileArchiveService } from "@/services";
import { experiment as experimentModule } from "@/store/experiment.module";
import { mountComponent } from "@/test-utils/mount";
import AssignmentScores from "./AssignmentScores.vue";

function flushPromises() {
  return new Promise(resolve => setTimeout(resolve));
}

const RouterLinkStub = {
  name: "RouterLink",
  props: ["to"],
  template: "<a><slot /></a>"
};

const treatmentNoFile = {
  treatmentId: 100,
  assessmentDto: {
    title: "Quiz 1",
    maxPoints: 10,
    multipleSubmissionScoringScheme: "MOST_RECENT",
    questions: [{ questionId: 1, questionType: "MC" }],
    submissions: [
      {
        participantId: 1,
        dateSubmitted: 2,
        alteredCalculatedGrade: 8,
        totalAlteredGrade: 8,
        gradeOverridden: false,
        assessmentId: 500,
        conditionId: 10,
        treatmentId: 100
      },
      {
        participantId: 1,
        dateSubmitted: 1,
        alteredCalculatedGrade: 5,
        totalAlteredGrade: 5,
        gradeOverridden: false,
        assessmentId: 500,
        conditionId: 10,
        treatmentId: 100
      }
    ]
  }
};

const participants = [
  { participantId: 1, user: { displayName: "Alice Smith" } },
  { participantId: 2, user: { displayName: "Bob Jones" } }
];

describe("AssignmentScores", () => {
  let pinia;

  beforeEach(() => {
    pinia = createPinia();
    setActivePinia(pinia);

    experimentModule().setExperiment({ experimentId: 1 });

    vi.clearAllMocks();

    assignmentService.fetchAssignment.mockResolvedValue({
      assignmentId: 3,
      title: "Assignment 1",
      treatments: [treatmentNoFile]
    });
    participantService.getAll.mockResolvedValue(participants);
    assignmentFileArchiveService.poll.mockResolvedValue(null);
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  function mount() {
    return mountComponent(AssignmentScores, {
      pinia,
      global: {
        stubs: { RouterLink: RouterLinkStub }
      }
    });
  }

  it("shows a loading spinner before data resolves", () => {
    const wrapper = mount();

    const pageLoading = wrapper.findComponent({ name: "PageLoading" });
    expect(pageLoading.exists()).toBe(true);
    expect(pageLoading.props("message")).toBe(
      "Please wait while we load the submission scores."
    );
  });

  it("loads the assignment and participants on mount and renders the scores table", async () => {
    const wrapper = mount();
    await flushPromises();
    await wrapper.vm.$nextTick();

    expect(assignmentService.fetchAssignment).toHaveBeenCalledWith(
      1, 2, 3, true
    );
    expect(participantService.getAll).toHaveBeenCalledWith(1);

    expect(wrapper.text()).toContain("Assignment 1");
    expect(wrapper.text()).toContain("Quiz 1");
    expect(wrapper.text()).toContain("Alice Smith");
    // Only submitting participants get a row.
    expect(wrapper.text()).not.toContain("Bob Jones");
    // Most-recent submission (dateSubmitted 2) score is used.
    expect(wrapper.text()).toContain("8");
  });

  it("shows an error state when the assignment fails to load", async () => {
    assignmentService.fetchAssignment.mockResolvedValue(null);

    const wrapper = mount();
    await flushPromises();
    await wrapper.vm.$nextTick();

    expect(wrapper.text()).toContain("Unable to load assignment data.");
  });

  it("shows an error state when participants fail to load", async () => {
    participantService.getAll.mockRejectedValue(new Error("network error"));

    const wrapper = mount();
    await flushPromises();
    await wrapper.vm.$nextTick();

    expect(wrapper.text()).toContain("Unable to load participants.");
  });

  it("links each student's name to their submission-grading route", async () => {
    const wrapper = mount();
    await flushPromises();
    await wrapper.vm.$nextTick();

    const link = wrapper.findComponent({ name: "RouterLink" });
    expect(link.props("to")).toEqual({
      name: "StudentSubmissionGrading",
      params: {
        experimentId: 1,
        exposureId: 2,
        assignmentId: 3,
        assessmentId: 500,
        conditionId: 10,
        treatmentId: 100,
        participantId: 1
      }
    });
  });

  it("disables file retrieval when there are no file-submission questions", async () => {
    const wrapper = mount();
    await flushPromises();
    await wrapper.vm.$nextTick();

    expect(wrapper.find(".btn-download-file").exists()).toBe(false);
  });

  it("enables the file-retrieval button only once a file submission exists", async () => {
    assignmentService.fetchAssignment.mockResolvedValue({
      assignmentId: 3,
      title: "Assignment 1",
      treatments: [
        {
          treatmentId: 100,
          assessmentDto: {
            title: "Essay",
            maxPoints: 10,
            multipleSubmissionScoringScheme: "MOST_RECENT",
            questions: [{ questionId: 1, questionType: "FILE" }],
            submissions: []
          }
        }
      ]
    });

    const wrapper = mount();
    await flushPromises();
    await wrapper.vm.$nextTick();

    const btn = wrapper.findComponent({ name: "VBtn" });
    expect(btn.props("disabled")).toBe(true);
  });

  it("saveExit navigates to the route's previousStep meta", async () => {
    const wrapper = mount();
    await flushPromises();
    await wrapper.vm.$nextTick();

    wrapper.vm.saveExit();

    expect(pushMock).toHaveBeenCalledWith({ name: "SomeStep" });
  });
});
