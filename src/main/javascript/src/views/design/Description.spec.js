import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

const pushMock = vi.fn();

vi.mock("vue-router", () => ({
  useRouter: () => ({ push: pushMock })
}));

vi.mock("sweetalert2", () => ({
  default: {
    fire: vi.fn().mockResolvedValue({})
  }
}));

vi.mock("@/services", () => ({
  experimentService: {
    update: vi.fn()
  }
}));

import Swal from "sweetalert2";
import { experimentService } from "@/services";
import { experiment as experimentModule } from "@/store/experiment.module";
import { navigation as navigationModule } from "@/store/navigation.module";
import { mountComponent } from "@/test-utils/mount";
import Description from "./Description.vue";

describe("DesignDescription", () => {
  let pinia;
  let experimentStore;
  let navigationStore;

  beforeEach(() => {
    pinia = createPinia();
    setActivePinia(pinia);

    experimentStore = experimentModule();
    navigationStore = navigationModule();

    experimentStore.setExperiment({ experimentId: 1, description: "" });

    vi.clearAllMocks();
  });

  function mount() {
    return mountComponent(Description, {
      pinia,
      props: { experiment: experimentStore.experiment }
    });
  }

  it("renders the description textarea reflecting the current experiment description", () => {
    experimentStore.setExperiment({
      experimentId: 1,
      description: "Existing notes"
    });

    const wrapper = mount();

    expect(
      wrapper.findComponent({ name: "VTextarea" }).props("modelValue")
    ).toBe("Existing notes");
  });

  it("disables the Next button while the description is blank", () => {
    const wrapper = mount();

    expect(wrapper.findComponent({ name: "VBtn" }).props("disabled")).toBe(
      true
    );
  });

  it("enables the Next button once a description is typed", async () => {
    const wrapper = mount();

    await wrapper
      .findComponent({ name: "VTextarea" })
      .setValue("Why this matters");

    expect(wrapper.findComponent({ name: "VBtn" }).props("disabled")).toBe(
      false
    );
  });

  it("saves the experiment and navigates to conditions on submit", async () => {
    experimentService.update.mockResolvedValue({ status: 200 });
    experimentStore.setExperiment({
      experimentId: 1,
      description: "Why this matters"
    });

    const wrapper = mount();

    await wrapper.find("form").trigger("submit");
    await flushPromises();

    expect(experimentService.update).toHaveBeenCalledWith(
      expect.objectContaining({
        experimentId: 1,
        description: "Why this matters"
      })
    );
    expect(pushMock).toHaveBeenCalledWith({
      name: "ExperimentDesignConditions",
      params: { experimentId: 1 }
    });
  });

  it("shows a Swal message error when save fails with a message", async () => {
    experimentService.update.mockResolvedValue({ message: "nope" });
    experimentStore.setExperiment({
      experimentId: 1,
      description: "Why this matters"
    });

    const wrapper = mount();

    await wrapper.find("form").trigger("submit");
    await flushPromises();

    expect(Swal.fire).toHaveBeenCalledWith("Error: nope");
    expect(pushMock).not.toHaveBeenCalled();
  });

  it("saveExit routes to the edit-mode caller page when set, otherwise Home", async () => {
    experimentService.update.mockResolvedValue({ status: 200 });
    experimentStore.setExperiment({
      experimentId: 1,
      description: "Why this matters"
    });

    const wrapper = mount();

    await wrapper.vm.saveExit();
    await flushPromises();

    expect(pushMock).toHaveBeenCalledWith({
      name: "Home",
      params: { experimentId: 1 }
    });

    pushMock.mockClear();
    navigationStore.saveEditMode({ callerPage: { name: "ExperimentSummary" } });

    await wrapper.vm.saveExit();
    await flushPromises();

    expect(pushMock).toHaveBeenCalledWith({
      name: "ExperimentSummary",
      params: { experimentId: 1 }
    });
  });
});

function flushPromises() {
  return new Promise(resolve => setTimeout(resolve));
}
