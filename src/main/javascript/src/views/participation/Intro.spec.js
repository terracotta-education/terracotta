import { describe, expect, it, vi, beforeEach } from "vitest";

const push = vi.fn();

vi.mock("vue-router", () => ({
  useRouter: () => ({
    push
  })
}));

import { mountComponent } from "@/test-utils/mount";
import Intro from "./Intro.vue";
import { navigation as navigationModule } from "@/store/navigation.module";

describe("ParticipationIntro", () => {
  beforeEach(() => {
    push.mockClear();
  });

  it("renders the intro copy", () => {
    const wrapper = mountComponent(Intro, {
      props: { experiment: { experimentId: 1 } }
    });

    expect(wrapper.text()).toContain(
      "how students in your class become"
    );
    expect(wrapper.text()).toContain(
      "provide consent to participate"
    );
  });

  it("links Continue to ExperimentParticipationSelectionMethod", () => {
    const wrapper = mountComponent(Intro, {
      props: { experiment: { experimentId: 1 } }
    });

    const button = wrapper.findComponent({ name: "VBtn" });
    expect(button.props("to")).toEqual({
      name: "ExperimentParticipationSelectionMethod"
    });
  });

  it("saveExit pushes to Home with the current experimentId by default", () => {
    const wrapper = mountComponent(Intro, {
      props: { experiment: { experimentId: 42 } }
    });

    wrapper.vm.saveExit();

    expect(push).toHaveBeenCalledWith({
      name: "Home",
      params: { experiment: 42 }
    });
  });

  it("saveExit pushes to the edit mode's caller page when set", () => {
    const wrapper = mountComponent(Intro, {
      props: { experiment: { experimentId: 42 } }
    });

    const navigationStore = navigationModule();
    navigationStore.editMode = { callerPage: { name: "ExperimentSummary" } };

    wrapper.vm.saveExit();

    expect(push).toHaveBeenCalledWith({
      name: "ExperimentSummary",
      params: { experiment: 42 }
    });
  });
});
