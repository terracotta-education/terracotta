import { afterEach, describe, expect, it, vi } from "vitest";

const push = vi.fn();

vi.mock("vue-router", () => ({
  useRouter: () => ({
    push
  })
}));

vi.mock("sweetalert2", () => ({
  default: {
    fire: vi.fn().mockResolvedValue({})
  }
}));

vi.mock("@/services", () => ({
  experimentService: {
    update: vi.fn()
  },
  apiService: {
    reportStep: vi.fn(),
    getStepStatus: vi.fn()
  }
}));

import Swal from "sweetalert2";
import { experimentService, apiService } from "@/services";
import { mountComponent } from "@/test-utils/mount";
import SelectionMethod from "./SelectionMethod.vue";
import { navigation as navigationModule } from "@/store/navigation.module";

const buildExperiment = (overrides = {}) => ({
  experimentId: 1,
  participationType: null,
  ...overrides
});

const BATCH_ID = "batch-1";

describe("SelectionMethod", () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it("renders all three participation panels", () => {
    const wrapper = mountComponent(SelectionMethod, {
      props: { experiment: buildExperiment() }
    });

    expect(wrapper.text()).toContain("Students will be invited to consent");
    expect(wrapper.text()).toContain("Teacher will manually decide");
    expect(wrapper.text()).toContain("Automatically include all students");
  });

  it("does not disable any panel when no participation type has been selected yet", () => {
    const wrapper = mountComponent(SelectionMethod, {
      props: { experiment: buildExperiment() }
    });

    const panels = wrapper.findAllComponents({ name: "VExpansionPanel" });
    panels.forEach(panel => {
      expect(panel.props("disabled")).toBeFalsy();
    });
  });

  it("disables the other panels once a participation type has been selected", async () => {
    const wrapper = mountComponent(SelectionMethod, {
      props: { experiment: buildExperiment({ participationType: "MANUAL" }) }
    });

    await wrapper.vm.$nextTick();

    const panels = wrapper.findAllComponents({ name: "VExpansionPanel" });

    // order: CONSENT, MANUAL, AUTO
    expect(panels[0].props("disabled")).toBe(true);
    expect(panels[1].props("disabled")).toBeFalsy();
    expect(panels[2].props("disabled")).toBe(true);
  });

  it("selecting CONSENT saves the experiment, reports the step, polls until completed, and routes to the consent overview", async () => {
    vi.useFakeTimers();

    try {
      experimentService.update.mockResolvedValue({ status: 200 });
      apiService.reportStep.mockResolvedValue({
        status: 200,
        data: { batchId: BATCH_ID, status: "IN_PROGRESS" }
      });
      apiService.getStepStatus.mockResolvedValue({
        status: 200,
        data: { batchId: BATCH_ID, status: "COMPLETED" }
      });

      const wrapper = mountComponent(SelectionMethod, {
        props: { experiment: buildExperiment() }
      });

      const selectButtons = wrapper.findAllComponents({ name: "VBtn" });
      await selectButtons[0].trigger("click");
      await flushWithFakeTimers(wrapper);

      expect(experimentService.update).toHaveBeenCalledWith(
        expect.objectContaining({ participationType: "CONSENT" })
      );
      expect(apiService.reportStep).toHaveBeenCalledWith(
        1,
        "participation_type",
        null,
        false
      );

      // not yet - still waiting on the first poll tick
      expect(push).not.toHaveBeenCalled();

      await vi.advanceTimersByTimeAsync(5000);

      expect(apiService.getStepStatus).toHaveBeenCalledWith(1, BATCH_ID);
      expect(push).toHaveBeenCalledWith({
        name: "ParticipationTypeConsentOverview",
        params: { experiment: 1 }
      });

      wrapper.unmount();
    } finally {
      vi.useRealTimers();
    }
  });

  it("selecting MANUAL routes to ParticipationTypeManual once the poll completes", async () => {
    vi.useFakeTimers();

    try {
      experimentService.update.mockResolvedValue({ status: 200 });
      apiService.reportStep.mockResolvedValue({
        status: 200,
        data: { batchId: BATCH_ID, status: "IN_PROGRESS" }
      });
      apiService.getStepStatus.mockResolvedValue({
        status: 200,
        data: { batchId: BATCH_ID, status: "COMPLETED" }
      });

      const wrapper = mountComponent(SelectionMethod, {
        props: { experiment: buildExperiment() }
      });

      const selectButtons = wrapper.findAllComponents({ name: "VBtn" });
      await selectButtons[1].trigger("click");
      await flushWithFakeTimers(wrapper);
      await vi.advanceTimersByTimeAsync(5000);

      expect(push).toHaveBeenCalledWith({
        name: "ParticipationTypeManual",
        params: { experiment: 1 }
      });

      wrapper.unmount();
    } finally {
      vi.useRealTimers();
    }
  });

  it("selecting AUTO routes to ParticipationTypeAutoConfirm once the poll completes", async () => {
    vi.useFakeTimers();

    try {
      experimentService.update.mockResolvedValue({ status: 200 });
      apiService.reportStep.mockResolvedValue({
        status: 200,
        data: { batchId: BATCH_ID, status: "IN_PROGRESS" }
      });
      apiService.getStepStatus.mockResolvedValue({
        status: 200,
        data: { batchId: BATCH_ID, status: "PROCESSED" }
      });

      const wrapper = mountComponent(SelectionMethod, {
        props: { experiment: buildExperiment() }
      });

      const selectButtons = wrapper.findAllComponents({ name: "VBtn" });
      await selectButtons[2].trigger("click");
      await flushWithFakeTimers(wrapper);
      await vi.advanceTimersByTimeAsync(5000);

      expect(push).toHaveBeenCalledWith({
        name: "ParticipationTypeAutoConfirm",
        params: { experiment: 1 }
      });

      wrapper.unmount();
    } finally {
      vi.useRealTimers();
    }
  });

  it("keeps polling every 5 seconds while the status is still IN_PROGRESS", async () => {
    vi.useFakeTimers();

    try {
      experimentService.update.mockResolvedValue({ status: 200 });
      apiService.reportStep.mockResolvedValue({
        status: 200,
        data: { batchId: BATCH_ID, status: "IN_PROGRESS" }
      });
      apiService.getStepStatus
        .mockResolvedValueOnce({ status: 200, data: { batchId: BATCH_ID, status: "IN_PROGRESS" } })
        .mockResolvedValueOnce({ status: 200, data: { batchId: BATCH_ID, status: "IN_PROGRESS" } })
        .mockResolvedValueOnce({ status: 200, data: { batchId: BATCH_ID, status: "COMPLETED" } });

      const wrapper = mountComponent(SelectionMethod, {
        props: { experiment: buildExperiment() }
      });

      const selectButtons = wrapper.findAllComponents({ name: "VBtn" });
      await selectButtons[0].trigger("click");
      await flushWithFakeTimers(wrapper);

      await vi.advanceTimersByTimeAsync(5000);
      expect(push).not.toHaveBeenCalled();

      await vi.advanceTimersByTimeAsync(5000);
      expect(push).not.toHaveBeenCalled();

      await vi.advanceTimersByTimeAsync(5000);
      expect(apiService.getStepStatus).toHaveBeenCalledTimes(3);
      expect(push).toHaveBeenCalledWith({
        name: "ParticipationTypeConsentOverview",
        params: { experiment: 1 }
      });

      // polling must stop once a terminal status is reached - no further calls on later ticks
      await vi.advanceTimersByTimeAsync(5000);
      expect(apiService.getStepStatus).toHaveBeenCalledTimes(3);

      wrapper.unmount();
    } finally {
      vi.useRealTimers();
    }
  });

  it("shows an error alert and does not navigate when the poll reports a FAILED status", async () => {
    vi.useFakeTimers();

    try {
      experimentService.update.mockResolvedValue({ status: 200 });
      apiService.reportStep.mockResolvedValue({
        status: 200,
        data: { batchId: BATCH_ID, status: "IN_PROGRESS" }
      });
      apiService.getStepStatus.mockResolvedValue({
        status: 200,
        data: { batchId: BATCH_ID, status: "FAILED", message: "canvas error" }
      });

      const wrapper = mountComponent(SelectionMethod, {
        props: { experiment: buildExperiment() }
      });

      const selectButtons = wrapper.findAllComponents({ name: "VBtn" });
      await selectButtons[0].trigger("click");
      await flushWithFakeTimers(wrapper);
      await vi.advanceTimersByTimeAsync(5000);

      expect(Swal.fire).toHaveBeenCalledWith("Error: canvas error");
      expect(push).not.toHaveBeenCalled();

      // polling must stop after the FAILED status - no further calls on later ticks
      await vi.advanceTimersByTimeAsync(5000);
      expect(apiService.getStepStatus).toHaveBeenCalledTimes(1);

      wrapper.unmount();
    } finally {
      vi.useRealTimers();
    }
  });

  it("shows a generic error alert when the poll reports FAILED with no message", async () => {
    vi.useFakeTimers();

    try {
      experimentService.update.mockResolvedValue({ status: 200 });
      apiService.reportStep.mockResolvedValue({
        status: 200,
        data: { batchId: BATCH_ID, status: "IN_PROGRESS" }
      });
      apiService.getStepStatus.mockResolvedValue({
        status: 200,
        data: { batchId: BATCH_ID, status: "FAILED" }
      });

      const wrapper = mountComponent(SelectionMethod, {
        props: { experiment: buildExperiment() }
      });

      const selectButtons = wrapper.findAllComponents({ name: "VBtn" });
      await selectButtons[0].trigger("click");
      await flushWithFakeTimers(wrapper);
      await vi.advanceTimersByTimeAsync(5000);

      expect(Swal.fire).toHaveBeenCalledWith(
        "There was an error preparing participants for this experiment."
      );
      expect(push).not.toHaveBeenCalled();

      wrapper.unmount();
    } finally {
      vi.useRealTimers();
    }
  });

  it("shows an error alert and does not navigate when the save fails", async () => {
    experimentService.update.mockResolvedValue({
      status: 400,
      message: "Something went wrong"
    });

    const wrapper = mountComponent(SelectionMethod, {
      props: { experiment: buildExperiment() }
    });

    const selectButtons = wrapper.findAllComponents({ name: "VBtn" });
    await selectButtons[0].trigger("click");
    await flushPromisesAndTicks(wrapper);

    expect(Swal.fire).toHaveBeenCalledWith("Error: Something went wrong");
    expect(push).not.toHaveBeenCalled();
    expect(apiService.getStepStatus).not.toHaveBeenCalled();
  });

  it("shows an error alert and does not navigate when reportStep fails", async () => {
    experimentService.update.mockResolvedValue({ status: 200 });
    apiService.reportStep.mockResolvedValue({ status: 500, message: "boom" });

    const wrapper = mountComponent(SelectionMethod, {
      props: { experiment: buildExperiment() }
    });

    const selectButtons = wrapper.findAllComponents({ name: "VBtn" });
    await selectButtons[0].trigger("click");
    await flushPromisesAndTicks(wrapper);

    expect(Swal.fire).toHaveBeenCalledWith("Error: boom");
    expect(push).not.toHaveBeenCalled();
    expect(apiService.getStepStatus).not.toHaveBeenCalled();
  });

  it("shows a generic error alert and does not navigate when reportStep resolves with no data (e.g. a network failure)", async () => {
    experimentService.update.mockResolvedValue({ status: 200 });
    apiService.reportStep.mockResolvedValue(null);

    const wrapper = mountComponent(SelectionMethod, {
      props: { experiment: buildExperiment() }
    });

    const selectButtons = wrapper.findAllComponents({ name: "VBtn" });
    await selectButtons[0].trigger("click");
    await flushPromisesAndTicks(wrapper);

    expect(Swal.fire).toHaveBeenCalledWith(
      "There was an error preparing participants for this experiment."
    );
    expect(push).not.toHaveBeenCalled();
    expect(apiService.getStepStatus).not.toHaveBeenCalled();
  });

  it("shows a generic error alert and does not poll when reportStep succeeds but returns no batchId", async () => {
    experimentService.update.mockResolvedValue({ status: 200 });
    apiService.reportStep.mockResolvedValue({ status: 200, data: {} });

    const wrapper = mountComponent(SelectionMethod, {
      props: { experiment: buildExperiment() }
    });

    const selectButtons = wrapper.findAllComponents({ name: "VBtn" });
    await selectButtons[0].trigger("click");
    await flushPromisesAndTicks(wrapper);

    expect(Swal.fire).toHaveBeenCalledWith(
      "There was an error preparing participants for this experiment."
    );
    expect(push).not.toHaveBeenCalled();
    expect(apiService.getStepStatus).not.toHaveBeenCalled();
  });

  it("shows the consent-file-missing alert only in edit mode for a CONSENT experiment without a consent file", async () => {
    const wrapper = mountComponent(SelectionMethod, {
      props: {
        experiment: buildExperiment({
          participationType: "CONSENT",
          consent: null
        })
      }
    });

    const navigationStore = navigationModule();
    navigationStore.editMode = { callerPage: { name: "Home" } };

    await wrapper.vm.$nextTick();

    expect(wrapper.text()).toContain(
      "Please complete the participation section"
    );
  });

  it("saveExit navigates to the caller page from edit mode, defaulting to Home", () => {
    const wrapper = mountComponent(SelectionMethod, {
      props: { experiment: buildExperiment({ experimentId: 7 }) }
    });

    wrapper.vm.saveExit();

    expect(push).toHaveBeenCalledWith({
      name: "Home",
      params: { experiment: 7 }
    });
  });
});

async function flushPromisesAndTicks(wrapper) {
  await wrapper.vm.$nextTick();
  await new Promise(resolve => setTimeout(resolve));
  await wrapper.vm.$nextTick();
}

// flushPromisesAndTicks' bare setTimeout becomes a fake timer once vi.useFakeTimers() is
// active, and never resolves without an explicit advance - use this instead in those tests.
async function flushWithFakeTimers(wrapper) {
  await wrapper.vm.$nextTick();
  await vi.advanceTimersByTimeAsync(0);
  await wrapper.vm.$nextTick();
}
