import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("@/services", () => ({
  experimentService: {
    getById: vi.fn()
  }
}));

const { routeRef, saveExitSpy } = vi.hoisted(() => ({
  routeRef: {
    name: "ExperimentDesignConditions",
    params: { experimentId: "5" },
    meta: {
      currentSection: "design",
      currentStep: "conditions",
      previousStep: "ExperimentDesignIntro",
      previousStepSingleCondition: null,
      stepActionText: null
    },
    fullPath: "/experiments/5/design/conditions"
  },
  saveExitSpy: vi.fn()
}));

let capturedRouteGuard;

vi.mock("vue-router", () => ({
  useRoute: () => routeRef,
  onBeforeRouteUpdate: vi.fn(guard => {
    capturedRouteGuard = guard;
  })
}));

import { h } from "vue";
import { createPinia, setActivePinia } from "pinia";
import { mountComponent } from "@/test-utils/mount";
import { experimentService } from "@/services";
import { navigation as navigationModule } from "@/store/navigation.module";
import ExperimentSteps from "./ExperimentSteps.vue";

// ExperimentSteps.vue doesn't import RouterView/RouterLink itself - it relies on
// them being globally registered by the real router plugin (installed in main.js).
// Since we don't install a real router here, register lightweight stand-ins as
// global components so `<router-view>`/`<router-link>` resolve at runtime.
const ChildStub = {
  name: "ChildStub",
  props: ["experiment"],
  setup(_, { expose }) {
    expose({ saveExit: saveExitSpy });

    return () => h("div", { class: "child-stub" }, "child content");
  }
};

const RouterViewStub = {
  name: "RouterView",
  render() {
    return this.$slots.default
      ? this.$slots.default({ Component: ChildStub })
      : null;
  }
};

const RouterLinkStub = {
  name: "RouterLink",
  props: ["to"],
  render() {
    return h(
      "a",
      { class: "router-link-stub" },
      this.$slots.default ? this.$slots.default() : []
    );
  }
};

const mountSteps = (options = {}) => {
  const { global: globalOptions = {}, ...rest } = options;

  return mountComponent(ExperimentSteps, {
    ...rest,
    global: {
      ...globalOptions,
      components: {
        RouterView: RouterViewStub,
        RouterLink: RouterLinkStub,
        ...(globalOptions.components || {})
      },
      stubs: {
        Steps: true,
        Help: true,
        ...(globalOptions.stubs || {})
      }
    }
  });
};

describe("ExperimentSteps", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    saveExitSpy.mockReset();
    capturedRouteGuard = undefined;

    routeRef.name = "ExperimentDesignConditions";
    routeRef.params = { experimentId: "5" };
    routeRef.meta = {
      currentSection: "design",
      currentStep: "conditions",
      previousStep: "ExperimentDesignIntro",
      previousStepSingleCondition: null,
      stepActionText: null
    };

    experimentService.getById.mockResolvedValue({
      status: 200,
      data: {
        experimentId: 5,
        participationType: "CONSENT",
        conditions: [{ conditionId: 1 }, { conditionId: 2 }]
      }
    });
  });

  it("fetches the experiment by the route's experimentId on mount", async () => {
    mountSteps();

    await vi.waitFor(() => {
      expect(experimentService.getById).toHaveBeenCalledWith("5");
    });
  });

  it("shows the 'Experiment not found' alert when no experiment has loaded", () => {
    const wrapper = mountSteps();

    expect(wrapper.text()).toContain("Experiment not found");
    expect(wrapper.find(".child-stub").exists()).toBe(false);
  });

  it("renders the sidebar with the current section/step/participation type once the experiment loads", async () => {
    const wrapper = mountSteps();

    await vi.waitFor(() => {
      expect(wrapper.findComponent({ name: "Steps" }).exists()).toBe(true);
    });

    const steps = wrapper.findComponent({ name: "Steps" });
    expect(steps.props("currentSection")).toBe("design");
    expect(steps.props("currentStep")).toBe("conditions");
    expect(steps.props("participationType")).toBe("CONSENT");
  });

  it("hides the sidebar for no-sidebar routes like TerracottaBuilder", async () => {
    routeRef.name = "TerracottaBuilder";

    const wrapper = mountSteps();

    await vi.waitFor(() => {
      expect(wrapper.find(".child-stub").exists()).toBe(true);
    });

    expect(wrapper.find(".experiment-steps__sidebar").exists()).toBe(false);
  });

  it("renders the routed child component with the loaded experiment", async () => {
    const wrapper = mountSteps();

    await vi.waitFor(() => {
      expect(wrapper.findComponent({ name: "ChildStub" }).exists()).toBe(true);
    });

    expect(
      wrapper.findComponent({ name: "ChildStub" }).props("experiment")
    ).toMatchObject({ experimentId: 5 });
  });

  it("defaults the save button text to SAVE & EXIT outside of edit mode", async () => {
    const wrapper = mountSteps();

    await vi.waitFor(() => {
      expect(wrapper.find(".save-button").exists()).toBe(true);
    });

    expect(wrapper.find(".save-button").text()).toBe("SAVE & EXIT");
  });

  it("uses SAVE & CLOSE when navigation is in edit mode for this route", async () => {
    // Seed the same pinia instance the mounted component will use - mountComponent
    // otherwise creates a fresh pinia per call, so a store patched beforehand
    // against the "currently active" instance can end up orphaned.
    const pinia = createPinia();
    setActivePinia(pinia);
    navigationModule().saveEditMode({
      initialPage: "ExperimentDesignConditions",
      callerPage: { name: "ExperimentSummary", tab: "design" }
    });

    const wrapper = mountSteps({ pinia });

    await vi.waitFor(() => {
      expect(wrapper.find(".save-button").exists()).toBe(true);
    });

    expect(wrapper.find(".save-button").text()).toBe("SAVE & CLOSE");
  });

  it("prefers route meta's stepActionText over the edit-mode default", async () => {
    routeRef.meta = { ...routeRef.meta, stepActionText: "CONTINUE" };

    const wrapper = mountSteps();

    await vi.waitFor(() => {
      expect(wrapper.find(".save-button").exists()).toBe(true);
    });

    expect(wrapper.find(".save-button").text()).toBe("CONTINUE");
  });

  it("hides the save button on the ExperimentDesignIntro route", async () => {
    routeRef.name = "ExperimentDesignIntro";

    const wrapper = mountSteps();

    await vi.waitFor(() => {
      expect(wrapper.find(".child-stub").exists()).toBe(true);
    });

    expect(wrapper.find(".save-button").isVisible()).toBe(false);
  });

  it("calls the child component's saveExit when the save button is clicked", async () => {
    saveExitSpy.mockResolvedValue();

    const wrapper = mountSteps();

    await vi.waitFor(() => {
      expect(wrapper.find(".save-button").exists()).toBe(true);
    });

    await wrapper.find(".save-button").trigger("click");

    expect(saveExitSpy).toHaveBeenCalled();
  });

  it("re-fetches the experiment when the route guard fires with a new experimentId", async () => {
    mountSteps();

    await vi.waitFor(() => {
      expect(experimentService.getById).toHaveBeenCalledWith("5");
    });

    experimentService.getById.mockClear();
    const next = vi.fn();

    await capturedRouteGuard(
      { name: "ExperimentDesignParticipation", params: { experimentId: "9" } },
      { name: "ExperimentDesignConditions" },
      next
    );

    expect(experimentService.getById).toHaveBeenCalledWith("9");
    expect(next).toHaveBeenCalled();
  });

  it("skips re-fetching when moving from the consent title step to the consent file step", async () => {
    mountSteps();

    await vi.waitFor(() => {
      expect(experimentService.getById).toHaveBeenCalledWith("5");
    });

    experimentService.getById.mockClear();
    const next = vi.fn();

    await capturedRouteGuard(
      { name: "ParticipationTypeConsentFile", params: { experimentId: "5" } },
      { name: "ParticipationTypeConsentTitle" },
      next
    );

    expect(experimentService.getById).not.toHaveBeenCalled();
    expect(next).toHaveBeenCalled();
  });
});
