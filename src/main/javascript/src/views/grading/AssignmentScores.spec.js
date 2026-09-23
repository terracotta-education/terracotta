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

// setTimeout-based flushPromises() can't resolve once vi.useFakeTimers() is active
// (its own macrotask never fires without an explicit advance). Promise microtasks
// are untouched by fake timers, so draining the microtask queue directly is the
// safe way to let pending native-Promise awaits (service mocks) settle.
function flushMicrotasks(times = 10) {
  let chain = Promise.resolve();
  for (let i = 0; i < times; i++) {
    chain = chain.then(() => Promise.resolve());
  }
  return chain;
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

const treatmentFile = {
  treatmentId: 300,
  assessmentDto: {
    title: "File Essay",
    maxPoints: 10,
    multipleSubmissionScoringScheme: "MOST_RECENT",
    questions: [{ questionId: 1, questionType: "FILE" }],
    submissions: [
      {
        participantId: 1,
        dateSubmitted: 1,
        alteredCalculatedGrade: 7,
        totalAlteredGrade: 7,
        gradeOverridden: false,
        assessmentId: 600,
        conditionId: 10,
        treatmentId: 300
      }
    ]
  }
};

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
      "1", "2", "3", true
    );
    expect(participantService.getAll).toHaveBeenCalledWith("1");

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
        experimentId: "1",
        exposureId: "2",
        assignmentId: "3",
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

  it("enables the file-retrieval button once a file submission exists", async () => {
    assignmentService.fetchAssignment.mockResolvedValue({
      assignmentId: 3,
      title: "Assignment 1",
      treatments: [treatmentFile]
    });

    const wrapper = mount();
    await flushPromises();
    await wrapper.vm.$nextTick();

    const btn = wrapper.findComponent({ name: "VBtn" });
    expect(btn.props("disabled")).toBe(false);
  });

  it("computes AVERAGE and HIGHEST scores according to each treatment's scoring scheme", async () => {
    assignmentService.fetchAssignment.mockResolvedValue({
      assignmentId: 3,
      title: "Assignment 1",
      treatments: [
        {
          treatmentId: 200,
          assessmentDto: {
            title: "Average Quiz",
            maxPoints: 10,
            multipleSubmissionScoringScheme: "AVERAGE",
            questions: [{ questionId: 1, questionType: "MC" }],
            submissions: [
              {
                participantId: 1, dateSubmitted: 1, alteredCalculatedGrade: 5,
                totalAlteredGrade: 5, gradeOverridden: false, assessmentId: 501,
                conditionId: 10, treatmentId: 200
              },
              {
                participantId: 1, dateSubmitted: 2, alteredCalculatedGrade: 8,
                totalAlteredGrade: 8, gradeOverridden: false, assessmentId: 501,
                conditionId: 10, treatmentId: 200
              }
            ]
          }
        },
        {
          treatmentId: 201,
          assessmentDto: {
            title: "Highest Quiz",
            maxPoints: 10,
            multipleSubmissionScoringScheme: "HIGHEST",
            questions: [{ questionId: 1, questionType: "MC" }],
            submissions: [
              // The most-recently-submitted attempt (dateSubmitted 2) scores lower than
              // an earlier attempt, so this only reads 9 if the HIGHEST branch (rather
              // than MOST_RECENT) is actually used.
              {
                participantId: 1, dateSubmitted: 1, alteredCalculatedGrade: 9,
                totalAlteredGrade: 9, gradeOverridden: false, assessmentId: 502,
                conditionId: 10, treatmentId: 201
              },
              {
                participantId: 1, dateSubmitted: 2, alteredCalculatedGrade: 5,
                totalAlteredGrade: 5, gradeOverridden: false, assessmentId: 502,
                conditionId: 10, treatmentId: 201
              }
            ]
          }
        }
      ]
    });

    const wrapper = mount();
    await flushPromises();
    await wrapper.vm.$nextTick();

    // (5 + 8) / 2 = 6.5, a fractional average that exercises round()'s toFixed(2) branch.
    expect(wrapper.text()).toContain("6.5");
    // Math.max(9, 5) = 9, distinct from the most-recently-submitted score of 5.
    expect(wrapper.text()).toContain("9");
  });

  it("shows a processing status and alert while a file archive is being prepared", async () => {
    assignmentService.fetchAssignment.mockResolvedValue({
      assignmentId: 3,
      title: "Assignment 1",
      treatments: [treatmentFile]
    });
    assignmentFileArchiveService.poll.mockResolvedValueOnce({ id: 1, status: "PROCESSING" });

    const wrapper = mount();
    await flushPromises();
    await wrapper.vm.$nextTick();

    expect(wrapper.find(".file-archive-status").text()).toContain("Processing");
    expect(wrapper.find(".alert-file-request").text()).toContain(
      "Your file archive is being prepared. Please wait."
    );
    expect(wrapper.find(".alert-file-request a").exists()).toBe(false);
  });

  it("shows an error status and alert when the file archive fails to prepare", async () => {
    assignmentService.fetchAssignment.mockResolvedValue({
      assignmentId: 3,
      title: "Assignment 1",
      treatments: [treatmentFile]
    });
    assignmentFileArchiveService.poll.mockResolvedValueOnce({ id: 1, status: "ERROR" });

    const wrapper = mount();
    await flushPromises();
    await wrapper.vm.$nextTick();

    expect(wrapper.find(".file-archive-status").text()).toContain("Error preparing archive");
    expect(wrapper.find(".alert-file-request").text()).toContain(
      "There was an error preparing your file archive."
    );
    expect(wrapper.find(".alert-file-request a").exists()).toBe(false);
  });

  it("downloads the archive from the alert link and dismisses the alert when ready", async () => {
    assignmentService.fetchAssignment.mockResolvedValue({
      assignmentId: 3,
      title: "Assignment 1",
      treatments: [treatmentFile]
    });
    const readyFileRequest = { id: 1, status: "READY" };
    assignmentFileArchiveService.poll.mockResolvedValueOnce(readyFileRequest);

    const wrapper = mount();
    await flushPromises();
    await wrapper.vm.$nextTick();

    expect(wrapper.find(".file-archive-status").text()).toContain("File archive ready");
    expect(wrapper.find(".alert-file-request").text()).toContain("Your file archive is ready.");

    const downloadLink = wrapper.find(".alert-file-request a");
    expect(downloadLink.exists()).toBe(true);

    await downloadLink.trigger("click");
    await flushPromises();
    await wrapper.vm.$nextTick();

    expect(assignmentFileArchiveService.retrieve).toHaveBeenCalledWith(
      "1", "2", "3", expect.objectContaining({ id: 1, ready: true })
    );
    expect(wrapper.find(".alert-file-request").exists()).toBe(false);
  });

  it("dismisses the file-request alert via the alert's close control", async () => {
    assignmentService.fetchAssignment.mockResolvedValue({
      assignmentId: 3,
      title: "Assignment 1",
      treatments: [treatmentFile]
    });
    assignmentFileArchiveService.poll.mockResolvedValueOnce({ id: 1, status: "PROCESSING" });

    const wrapper = mount();
    await flushPromises();
    await wrapper.vm.$nextTick();

    expect(wrapper.find(".alert-file-request").exists()).toBe(true);

    await wrapper.findComponent({ name: "VAlert" }).vm.$emit("click:close");
    await wrapper.vm.$nextTick();

    expect(wrapper.find(".alert-file-request").exists()).toBe(false);
  });

  it("retrieves immediately (without preparing a new archive) when handleFileRequest finds one already ready", async () => {
    assignmentService.fetchAssignment.mockResolvedValue({
      assignmentId: 3,
      title: "Assignment 1",
      treatments: [treatmentFile]
    });
    const readyFileRequest = { id: 1, status: "READY" };
    assignmentFileArchiveService.poll.mockResolvedValue(readyFileRequest);

    const wrapper = mount();
    await flushPromises();
    await wrapper.vm.$nextTick();

    await wrapper.find(".btn-download-file").trigger("click");
    await flushPromises();
    await wrapper.vm.$nextTick();

    expect(assignmentFileArchiveService.retrieve).toHaveBeenCalledWith(
      "1", "2", "3", expect.objectContaining({ id: 1, ready: true })
    );
    expect(assignmentFileArchiveService.prepare).not.toHaveBeenCalled();
  });

  it("prepares a new archive when handleFileRequest finds none ready", async () => {
    assignmentService.fetchAssignment.mockResolvedValue({
      assignmentId: 3,
      title: "Assignment 1",
      treatments: [treatmentFile]
    });
    // No archive exists yet: poll (both the initial load and handleFileRequest's own poll) resolves null.
    assignmentFileArchiveService.poll.mockResolvedValue(null);
    assignmentFileArchiveService.prepare.mockResolvedValueOnce({ id: 2, status: "PROCESSING" });

    const wrapper = mount();
    await flushPromises();
    await wrapper.vm.$nextTick();

    await wrapper.find(".btn-download-file").trigger("click");
    await flushPromises();
    await wrapper.vm.$nextTick();

    expect(assignmentFileArchiveService.prepare).toHaveBeenCalledWith("1", "2", "3");
    expect(assignmentFileArchiveService.retrieve).not.toHaveBeenCalled();
    expect(wrapper.find(".alert-file-request").text()).toContain(
      "Your file archive is being prepared. Please wait."
    );
  });

  it("polls for the file archive status every 5 seconds while processing, and stops once ready", async () => {
    assignmentService.fetchAssignment.mockResolvedValue({
      assignmentId: 3,
      title: "Assignment 1",
      treatments: [treatmentFile]
    });
    assignmentFileArchiveService.poll.mockResolvedValue(null);
    assignmentFileArchiveService.prepare.mockResolvedValueOnce({ id: 5, status: "PROCESSING" });

    const wrapper = mount();
    await flushPromises();
    await wrapper.vm.$nextTick();

    vi.useFakeTimers();

    await wrapper.find(".btn-download-file").trigger("click");
    await flushMicrotasks();
    await wrapper.vm.$nextTick();

    expect(wrapper.find(".alert-file-request").text()).toContain(
      "Your file archive is being prepared. Please wait."
    );

    // The next poll (triggered by the 5s interval) reports the archive as ready.
    assignmentFileArchiveService.poll.mockResolvedValueOnce({ id: 5, status: "READY" });

    await vi.advanceTimersByTimeAsync(5000);
    await flushMicrotasks();
    await wrapper.vm.$nextTick();

    expect(wrapper.find(".alert-file-request").text()).toContain("Your file archive is ready.");

    // Once ready, fileRequestPolling flips false and the watcher must clear the interval:
    // advancing another 5s should trigger no further poll calls.
    const callsBeforeExtraAdvance = assignmentFileArchiveService.poll.mock.calls.length;

    await vi.advanceTimersByTimeAsync(5000);
    await flushMicrotasks();

    expect(assignmentFileArchiveService.poll.mock.calls.length).toBe(callsBeforeExtraAdvance);
  });
});
