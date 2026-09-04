import { describe, expect, it, vi, beforeEach } from "vitest";

const push = vi.fn();

vi.mock("vue-router", () => ({
  useRouter: () => ({
    push
  })
}));

import { mountComponent } from "@/test-utils/mount";
import Intro from "./Intro.vue";
import { configuration as configurationModule } from "@/store/configuration.module";

describe("Intro", () => {
  beforeEach(() => {
    push.mockClear();
  });

  it("defaults the LMS title to 'LMS' when no configuration is loaded", () => {
    const wrapper = mountComponent(Intro, {
      props: {
        experiment: { experimentId: 1 }
      }
    });

    expect(wrapper.text()).toContain("LMS assignments");
  });

  it("uses the configured lmsTitle when available", () => {
    const wrapper = mountComponent(Intro, {
      props: {
        experiment: { experimentId: 1 }
      }
    });

    const configurationStore = configurationModule();
    configurationStore.configurations = { lmsTitle: "Canvas" };

    return wrapper.vm.$nextTick().then(() => {
      expect(wrapper.text()).toContain("Canvas assignments");
    });
  });

  it("links Continue to AssignmentExposureSets", () => {
    const wrapper = mountComponent(Intro, {
      props: {
        experiment: { experimentId: 1 }
      }
    });

    const button = wrapper.findComponent({ name: "VBtn" });
    expect(button.props("to")).toEqual({ name: "AssignmentExposureSets" });
  });

  it("saveExit pushes to Home with the current experimentId", () => {
    const wrapper = mountComponent(Intro, {
      props: {
        experiment: { experimentId: 42 }
      }
    });

    wrapper.vm.saveExit();

    expect(push).toHaveBeenCalledWith({
      name: "Home",
      params: { experiment: 42 }
    });
  });
});
