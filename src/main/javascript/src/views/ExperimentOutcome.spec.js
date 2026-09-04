import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("@/services", () => ({
  experimentService: {
    getById: vi.fn()
  }
}));

const { routeRef, saveExitSpy, onBeforeRouteUpdateMock } = vi.hoisted(() => ({
  routeRef: {
    params: { experimentId: "5" },
    meta: { previousStep: "ExperimentDesignIntro", stepActionText: null },
    fullPath: "/experiments/5/design/outcome"
  },
  saveExitSpy: vi.fn(),
  onBeforeRouteUpdateMock: vi.fn()
}));

vi.mock("vue-router", async () => {
  const { h } = await import("vue");

  const ChildStub = {
    name: "ChildStub",
    setup(_, { expose }) {
      expose({ saveExit: saveExitSpy });

      return () => h("div", { class: "child-stub" }, "child content");
    }
  };

  return {
    useRoute: () => routeRef,
    onBeforeRouteUpdate: onBeforeRouteUpdateMock,
    RouterLink: {
      name: "RouterLink",
      props: ["to"],
      render() {
        return h(
          "a",
          { class: "router-link-stub" },
          this.$slots.default ? this.$slots.default() : []
        );
      }
    },
    RouterView: {
      name: "RouterView",
      render() {
        return this.$slots.default
          ? this.$slots.default({ Component: ChildStub })
          : null;
      }
    }
  };
});

import { mountComponent } from "@/test-utils/mount";
import { experimentService } from "@/services";
import ExperimentOutcome from "./ExperimentOutcome.vue";

describe("ExperimentOutcome", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    saveExitSpy.mockReset();
    routeRef.meta = {
      previousStep: "ExperimentDesignIntro",
      stepActionText: null
    };
    experimentService.getById.mockResolvedValue({
      status: 200,
      data: { experimentId: 5 }
    });
  });

  it("fetches the experiment by the route's experimentId on mount", async () => {
    mountComponent(ExperimentOutcome);

    await vi.waitFor(() => {
      expect(experimentService.getById).toHaveBeenCalledWith("5");
    });
  });

  it("renders the Back link when a previousStep is set in route meta", () => {
    const wrapper = mountComponent(ExperimentOutcome);

    const backLink = wrapper.findComponent({ name: "RouterLink" });
    expect(backLink.exists()).toBe(true);
    expect(wrapper.text()).toContain("Back");
  });

  it("hides the Back link when there is no previousStep", () => {
    routeRef.meta = { previousStep: null, stepActionText: null };

    const wrapper = mountComponent(ExperimentOutcome);

    expect(wrapper.findComponent({ name: "RouterLink" }).exists()).toBe(false);
  });

  it("defaults the save button text to SAVE & EXIT", () => {
    const wrapper = mountComponent(ExperimentOutcome);

    expect(wrapper.find(".save-button").text()).toBe("SAVE & EXIT");
  });

  it("uses the route meta's stepActionText for the save button when provided", () => {
    routeRef.meta = {
      previousStep: "ExperimentDesignIntro",
      stepActionText: "CONTINUE"
    };

    const wrapper = mountComponent(ExperimentOutcome);

    expect(wrapper.find(".save-button").text()).toBe("CONTINUE");
  });

  it("renders the routed child component", () => {
    const wrapper = mountComponent(ExperimentOutcome);

    expect(wrapper.find(".child-stub").exists()).toBe(true);
  });

  it("calls the child component's saveExit when the save button is clicked, toggling isSaving around the call", async () => {
    let resolveSaveExit;
    saveExitSpy.mockImplementation(
      () => new Promise(resolve => { resolveSaveExit = resolve; })
    );

    const wrapper = mountComponent(ExperimentOutcome);

    const clickPromise = wrapper.find(".save-button").trigger("click");
    await Promise.resolve();

    expect(wrapper.find(".save-button").attributes("disabled")).toBeDefined();
    expect(
      wrapper.findComponent({ name: "RouterLink" }).attributes("aria-disabled")
    ).toBe("true");

    resolveSaveExit();
    await clickPromise;

    await vi.waitFor(() => {
      expect(
        wrapper.find(".save-button").classes()
      ).not.toContain("v-btn--disabled");
    });

    expect(saveExitSpy).toHaveBeenCalled();
  });

  it("re-fetches the experiment when the registered route guard fires with a new experimentId", async () => {
    mountComponent(ExperimentOutcome);

    await vi.waitFor(() => {
      expect(experimentService.getById).toHaveBeenCalledWith("5");
    });

    experimentService.getById.mockClear();

    const guard = onBeforeRouteUpdateMock.mock.calls[0][0];
    await guard({ params: { experimentId: "9" } });

    expect(experimentService.getById).toHaveBeenCalledWith("9");
  });
});
