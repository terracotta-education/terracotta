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
    reportStep: vi.fn()
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

  it("selecting CONSENT saves the experiment, reports the step, and routes to the consent overview", async () => {
    experimentService.update.mockResolvedValue({ status: 200 });
    apiService.reportStep.mockResolvedValue({ status: 200 });

    const wrapper = mountComponent(SelectionMethod, {
      props: { experiment: buildExperiment() }
    });

    const selectButtons = wrapper.findAllComponents({ name: "VBtn" });
    await selectButtons[0].trigger("click");
    await flushPromisesAndTicks(wrapper);

    expect(experimentService.update).toHaveBeenCalledWith(
      expect.objectContaining({ participationType: "CONSENT" })
    );
    expect(apiService.reportStep).toHaveBeenCalledWith(
      1,
      "participation_type",
      null,
      false
    );
    expect(push).toHaveBeenCalledWith({
      name: "ParticipationTypeConsentOverview",
      params: { experiment: 1 }
    });
  });

  it("selecting MANUAL routes to ParticipationTypeManual", async () => {
    experimentService.update.mockResolvedValue({ status: 200 });
    apiService.reportStep.mockResolvedValue({ status: 200 });

    const wrapper = mountComponent(SelectionMethod, {
      props: { experiment: buildExperiment() }
    });

    const selectButtons = wrapper.findAllComponents({ name: "VBtn" });
    await selectButtons[1].trigger("click");
    await flushPromisesAndTicks(wrapper);

    expect(push).toHaveBeenCalledWith({
      name: "ParticipationTypeManual",
      params: { experiment: 1 }
    });
  });

  it("selecting AUTO routes to ParticipationTypeAutoConfirm", async () => {
    experimentService.update.mockResolvedValue({ status: 200 });
    apiService.reportStep.mockResolvedValue({ status: 200 });

    const wrapper = mountComponent(SelectionMethod, {
      props: { experiment: buildExperiment() }
    });

    const selectButtons = wrapper.findAllComponents({ name: "VBtn" });
    await selectButtons[2].trigger("click");
    await flushPromisesAndTicks(wrapper);

    expect(push).toHaveBeenCalledWith({
      name: "ParticipationTypeAutoConfirm",
      params: { experiment: 1 }
    });
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
