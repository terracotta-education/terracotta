import { describe, expect, it, vi, beforeEach } from "vitest";

const push = vi.fn();

vi.mock("vue-router", () => ({
  useRoute: () => ({
    params: {
      experimentId: "10",
      exposureId: "20"
    }
  }),
  useRouter: () => ({
    push
  })
}));

vi.mock("@/services", () => ({
  outcomeService: {
    getAll: vi.fn(),
    getOutcomePotentials: vi.fn(),
    create: vi.fn()
  }
}));

const swal = vi.fn();

vi.mock("sweetalert2", () => ({
  default: { fire: (...args) => swal(...args) }
}));

import { createPinia, setActivePinia } from "pinia";
import { mountComponent } from "@/test-utils/mount";
import OutcomeGradebook from "./OutcomeGradebook.vue";
import { outcomeService } from "@/services";
import { experiment as experimentModule } from "@/store/experiment.module";

const flush = () => new Promise(resolve => setTimeout(resolve));

const potentials = [
  { assignmentId: 1, name: "Quiz 1", pointsPossible: 10, type: "quiz" },
  { assignmentId: 2, name: "Quiz 2", pointsPossible: 20, type: "quiz" }
];

describe("OutcomeGradebook", () => {
  beforeEach(() => {
    push.mockClear();
    swal.mockClear();
    outcomeService.getAll.mockReset().mockResolvedValue({
      status: 200,
      data: []
    });
    outcomeService.getOutcomePotentials.mockReset().mockResolvedValue({
      status: 200,
      data: potentials
    });
    outcomeService.create.mockReset();
  });

  const mountView = () => {
    const pinia = createPinia();
    setActivePinia(pinia);

    experimentModule().experiment = { experimentId: 10 };

    return mountComponent(OutcomeGradebook, { pinia });
  };

  it("shows a loading state until the outcome potentials resolve", async () => {
    let resolvePotentials;
    outcomeService.getOutcomePotentials.mockReset().mockReturnValue(
      new Promise(resolve => {
        resolvePotentials = resolve;
      })
    );

    const wrapper = mountView();
    await flush();

    expect(wrapper.text()).toContain("Loading gradebook items");

    resolvePotentials({ status: 200, data: potentials });
    await flush();
    await wrapper.vm.$nextTick();

    expect(wrapper.text()).not.toContain("Loading gradebook items");
  });

  it("renders the fetched gradebook items in the table", async () => {
    const wrapper = mountView();
    await flush();
    await wrapper.vm.$nextTick();

    expect(wrapper.text()).toContain("Quiz 1");
    expect(wrapper.text()).toContain("Quiz 2");
    expect(wrapper.text()).toContain("10");
    expect(wrapper.text()).toContain("20");
  });

  it("shows a check icon instead of a checkbox for potentials that already have an outcome", async () => {
    outcomeService.getAll.mockResolvedValue({
      status: 200,
      data: [{ lmsOutcomeId: 1, exposureId: "20" }]
    });

    const wrapper = mountView();
    await flush();
    await wrapper.vm.$nextTick();

    const rows = wrapper.findAll("tbody tr");
    expect(rows[0].find(".mdi-check").exists()).toBe(true);
    expect(rows[0].findComponent({ name: "VCheckbox" }).exists()).toBe(false);

    expect(rows[1].find(".mdi-check").exists()).toBe(false);
    expect(rows[1].findComponent({ name: "VCheckbox" }).exists()).toBe(true);
  });

  it("select-all selects every eligible potential and deselecting clears them", async () => {
    const wrapper = mountView();
    await flush();
    await wrapper.vm.$nextTick();

    const selectAllCheckbox = wrapper.findComponent({ name: "VCheckbox" });
    await selectAllCheckbox.setValue(true);

    expect(wrapper.vm.selectedAssignmentIds).toEqual([1, 2]);

    await selectAllCheckbox.setValue(false);

    expect(wrapper.vm.selectedAssignmentIds).toEqual([]);
  });

  it("saveExit creates an outcome for each selected assignment and navigates with a success alert", async () => {
    outcomeService.create.mockResolvedValue({
      status: 201,
      data: { outcomeId: 99 }
    });

    const wrapper = mountView();
    await flush();
    await wrapper.vm.$nextTick();

    wrapper.vm.selectedAssignmentIds = [1];

    await wrapper.vm.saveExit();
    await flush();

    expect(outcomeService.create).toHaveBeenCalledWith(
      "10",
      "20",
      "Quiz 1",
      10,
      true,
      "quiz",
      1
    );
    expect(push).toHaveBeenCalledWith({
      name: "ExperimentSummary",
      params: expect.objectContaining({
        alertMessage: "Outcomes created successfully."
      })
    });
  });

  it("saveExit navigates without alert params when nothing was selected", async () => {
    const wrapper = mountView();
    await flush();
    await wrapper.vm.$nextTick();

    await wrapper.vm.saveExit();

    expect(push).toHaveBeenCalledWith({
      name: "ExperimentSummary",
      params: {}
    });
  });

  it("shows an error and does not navigate when the create call fails", async () => {
    outcomeService.create.mockRejectedValue(new Error("boom"));

    const wrapper = mountView();
    await flush();
    await wrapper.vm.$nextTick();

    wrapper.vm.selectedAssignmentIds = [1];

    await wrapper.vm.saveExit();
    await flush();

    expect(swal).toHaveBeenCalledWith(
      expect.objectContaining({
        text: "An error occurred while creating outcomes. Please try again."
      })
    );
    expect(push).not.toHaveBeenCalled();
  });

  it("saveExit navigates with a success alert message when outcomes are created successfully", async () => {
    outcomeService.create.mockResolvedValue({
      status: 201,
      data: { outcomeId: 1 }
    });

    const wrapper = mountView();
    await flush();
    await wrapper.vm.$nextTick();

    wrapper.vm.selectedAssignmentIds = [1];

    await wrapper.vm.saveExit();
    await flush();

    expect(push).toHaveBeenCalledWith({
      name: "ExperimentSummary",
      params: expect.objectContaining({
        alertMessage: "Outcomes created successfully."
      })
    });
  });

  describe("gradebook item details", () => {
    const canvasQuiz = {
      assignmentId: "5",
      name: "Week 3 Reading Quiz",
      pointsPossible: 15,
      type: "online_quiz",
      htmlUrl: "https://canvas.example.edu/courses/1/assignments/5",
      dueAt: "2026-11-20T23:59:00Z",
      description: "Read chapter 3.\n\nThen answer the questions."
    };

    const openFor = async item => {
      outcomeService.getOutcomePotentials.mockReset().mockResolvedValue({ status: 200, data: [item] });
      const wrapper = mountView();
      await flush();

      const opener = wrapper.find(".gradebook-item-name");
      await opener.trigger("click");
      await flush();

      return { wrapper, opener };
    };

    const dialogText = () => document.body.querySelector(".v-overlay__content")?.textContent || "";

    it("shows each item's name as a button that opens a dialog", async () => {
      outcomeService.getOutcomePotentials.mockReset().mockResolvedValue({ status: 200, data: [canvasQuiz] });
      const wrapper = mountView();
      await flush();

      const opener = wrapper.find("button.gradebook-item-name");

      expect(opener.text()).toBe("Week 3 Reading Quiz");
      expect(opener.attributes("aria-haspopup")).toBe("dialog");
    });

    it("shows the item's name, type, points, due date and description in the pop-up", async () => {
      const { wrapper } = await openFor(canvasQuiz);

      expect(wrapper.findComponent({ name: "VDialog" }).props("modelValue")).toBe(true);
      expect(document.getElementById("gradebook-item-details-title").textContent).toContain("Week 3 Reading Quiz");
      expect(dialogText()).toContain("Quiz");
      expect(dialogText()).toContain("15");
      expect(dialogText()).toContain("November 20, 2026");
      expect(dialogText()).toContain("Read chapter 3.");
    });

    it("links to the item in the LMS in a new tab, saying so to screen readers", async () => {
      await openFor(canvasQuiz);

      const link = [...document.body.querySelectorAll(".v-overlay__content a")]
        .find(anchor => anchor.textContent.includes("Open in"));

      expect(link.getAttribute("href")).toBe("https://canvas.example.edu/courses/1/assignments/5");
      expect(link.getAttribute("target")).toBe("_blank");
      expect(link.getAttribute("rel")).toContain("noopener");
      expect(link.querySelector(".d-sr-only").textContent).toBe("(opens in a new tab)");
      expect(link.querySelector(".mdi-open-in-new")).not.toBeNull();
    });

    it("leaves out the link when the LMS has no page for the item, or it isn't a web address", async () => {
      for (const htmlUrl of [null, "javascript:alert(1)"]) {
        document.body.innerHTML = "";
        await openFor({ ...canvasQuiz, htmlUrl });

        expect(dialogText()).not.toContain("Open in");
        expect(dialogText()).toContain("Week 3 Reading Quiz");
      }
    });

    it("leaves out the link for an LTI item, such as a Terracotta assignment", async () => {
      await openFor({ ...canvasQuiz, type: "external_tool", terracotta: true });

      expect(dialogText()).not.toContain("Open in");
      expect(dialogText()).toContain("External tool");
      expect(dialogText()).toContain("Week 3 Reading Quiz");
    });

    it("says when there's no due date or points, and calls an unknown type an assignment", async () => {
      await openFor({ ...canvasQuiz, type: "online_upload", dueAt: null, pointsPossible: null, description: null });

      expect(dialogText()).toContain("Assignment");
      expect(dialogText()).toContain("No due date");
      expect(dialogText()).toContain("None");
      expect(dialogText()).not.toContain("Description");
    });

    it("opens toward the top, level with the item clicked", async () => {
      outcomeService.getOutcomePotentials.mockReset().mockResolvedValue({ status: 200, data: [canvasQuiz] });
      const wrapper = mountView();
      await flush();

      const opener = wrapper.find(".gradebook-item-name");
      opener.element.getBoundingClientRect = () => ({ top: 300 });
      await opener.trigger("click");
      await flush();

      const content = document.body.querySelector(".v-overlay__content.gradebook-item-dialog");
      expect(content.style.marginTop).toBe("284px");
    });

    it("never opens above the top of the page", async () => {
      outcomeService.getOutcomePotentials.mockReset().mockResolvedValue({ status: 200, data: [canvasQuiz] });
      const wrapper = mountView();
      await flush();

      const opener = wrapper.find(".gradebook-item-name");
      opener.element.getBoundingClientRect = () => ({ top: 4 });
      await opener.trigger("click");
      await flush();

      expect(document.body.querySelector(".v-overlay__content.gradebook-item-dialog").style.marginTop).toBe("16px");
    });

    it("returns focus to the item's name when the pop-up closes", async () => {
      const { wrapper, opener } = await openFor(canvasQuiz);
      const focus = vi.spyOn(opener.element, "focus");

      wrapper.findComponent({ name: "VDialog" }).vm.$emit("afterLeave");
      await flush();

      expect(focus).toHaveBeenCalledTimes(1);
    });
  });
});
