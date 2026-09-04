import { describe, expect, it, vi } from "vitest";

const pushMock = vi.fn();

vi.mock("vue-router", () => ({
  useRoute: () => ({ params: { experimentId: "1" } }),
  useRouter: () => ({ push: pushMock })
}));

import { mountComponent } from "@/test-utils/mount";
import Intro from "./Intro.vue";

describe("ExperimentDesignIntro", () => {
  it("renders the intro copy and a Continue button linking to the title step", () => {
    const wrapper = mountComponent(Intro);

    expect(wrapper.text()).toContain("Let's set up your experiment");
    expect(wrapper.text()).toContain("Continue");
  });

  it("saveExit navigates Home with the current experimentId", () => {
    const wrapper = mountComponent(Intro);

    wrapper.vm.saveExit();

    expect(pushMock).toHaveBeenCalledWith({
      name: "Home",
      params: { experimentId: "1" }
    });
  });
});
