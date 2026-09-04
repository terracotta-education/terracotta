import { describe, expect, it, vi } from "vitest";

const push = vi.fn();

vi.mock("vue-router", () => ({
  useRoute: () => ({
    params: {
      numberOfExperimentSets: "3",
      exposureId: "7"
    }
  }),
  useRouter: () => ({
    push
  })
}));

import { mountComponent } from "@/test-utils/mount";
import ExposureSetsIntro from "./ExposureSetsIntro.vue";

describe("ExposureSetsIntro", () => {
  it("renders the number of exposure sets from the route params", () => {
    const wrapper = mountComponent(ExposureSetsIntro, {
      props: {
        experiment: { experimentId: 1 }
      }
    });

    expect(wrapper.text()).toContain("3 exposure sets");
    expect(wrapper.text()).toContain("3 assignments");
  });

  it("links the Continue button to AssignmentYourAssignments with the exposureId", () => {
    const wrapper = mountComponent(ExposureSetsIntro, {
      props: {
        experiment: { experimentId: 1 }
      }
    });

    const button = wrapper.findComponent({ name: "VBtn" });
    expect(button.props("to")).toEqual({
      name: "AssignmentYourAssignments",
      params: { exposureId: "7" }
    });
  });

  it("saveExit pushes to Home", () => {
    push.mockClear();

    const wrapper = mountComponent(ExposureSetsIntro, {
      props: {
        experiment: { experimentId: 1 }
      }
    });

    wrapper.vm.saveExit();

    expect(push).toHaveBeenCalledWith({ name: "Home" });
  });
});
