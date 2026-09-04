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
import Title from "./Title.vue";

describe("DesignTitle", () => {
  let pinia;
  let experimentStore;
  let navigationStore;

  beforeEach(() => {
    pinia = createPinia();
    setActivePinia(pinia);

    experimentStore = experimentModule();
    navigationStore = navigationModule();

    experimentStore.setExperiment({
      experimentId: 1,
      title: ""
    });

    vi.clearAllMocks();
  });

  function mount() {
    return mountComponent(Title, {
      pinia,
      props: {
        experiment: experimentStore.experiment
      }
    });
  }

  it("renders the title field reflecting the current experiment title", () => {
    experimentStore.setExperiment({ experimentId: 1, title: "My Study" });

    const wrapper = mount();

    expect(
      wrapper.findComponent({ name: "VTextField" }).props("modelValue")
    ).toBe("My Study");
  });

  it("disables the Next button when the title is blank", () => {
    const wrapper = mount();

    const nextBtn = wrapper.findComponent({ name: "VBtn" });

    expect(nextBtn.props("disabled")).toBe(true);
  });

  it("enables the Next button once a non-blank title is entered", async () => {
    const wrapper = mount();

    await wrapper
      .findComponent({ name: "VTextField" })
      .setValue("A great study");

    expect(wrapper.findComponent({ name: "VBtn" }).props("disabled")).toBe(
      false
    );
  });

  it("saves the experiment and navigates to the description step on submit", async () => {
    experimentService.update.mockResolvedValue({ status: 200 });
    experimentStore.setExperiment({ experimentId: 1, title: "A great study" });

    const wrapper = mount();

    await wrapper.find("form").trigger("submit");
    await flushPromises();

    expect(experimentService.update).toHaveBeenCalledWith(
      expect.objectContaining({ experimentId: 1, title: "A great study" })
    );
    expect(pushMock).toHaveBeenCalledWith({
      name: "ExperimentDesignDescription",
      params: { experiment: 1 }
    });
  });

  it("shows a Swal message error when the save fails with a message", async () => {
    experimentService.update.mockResolvedValue({ message: "Title taken" });
    experimentStore.setExperiment({ experimentId: 1, title: "A great study" });

    const wrapper = mount();

    await wrapper.find("form").trigger("submit");
    await flushPromises();

    expect(Swal.fire).toHaveBeenCalledWith("Error: Title taken");
    expect(pushMock).not.toHaveBeenCalled();
  });

  it("shows a generic Swal error when the save throws", async () => {
    experimentService.update.mockRejectedValue(new Error("network down"));
    experimentStore.setExperiment({ experimentId: 1, title: "A great study" });

    const wrapper = mount();

    await wrapper.find("form").trigger("submit");
    await flushPromises();

    expect(Swal.fire).toHaveBeenCalledWith(
      "There was an error saving your experiment."
    );
  });

  it("saveExit saves and routes to the edit-mode caller page when in edit mode", async () => {
    experimentService.update.mockResolvedValue({ status: 200 });
    experimentStore.setExperiment({ experimentId: 1, title: "A great study" });
    navigationStore.saveEditMode({ callerPage: { name: "ExperimentSummary" } });

    const wrapper = mount();

    await wrapper.vm.saveExit();
    await flushPromises();

    expect(pushMock).toHaveBeenCalledWith({
      name: "ExperimentSummary",
      params: { experiment: 1 }
    });
  });
});

function flushPromises() {
  return new Promise(resolve => setTimeout(resolve));
}
