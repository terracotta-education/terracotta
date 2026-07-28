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
  conditionService: {
    updateAll: vi.fn()
  }
}));

import Swal from "sweetalert2";
import { conditionService } from "@/services";
import { mountComponent } from "@/test-utils/mount";
import ParticipationCustomDistribution from "./ParticipationCustomDistribution.vue";
import { navigation as navigationModule } from "@/store/navigation.module";

const buildExperiment = (overrides = {}) => ({
  experimentId: 1,
  conditions: [
    { conditionId: 1, name: "Control", distributionPct: 50 },
    { conditionId: 2, name: "Treatment", distributionPct: 50 }
  ],
  ...overrides
});

describe("ParticipationCustomDistribution", () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it("renders a distribution field per condition pre-filled with its current percentage", () => {
    const wrapper = mountComponent(ParticipationCustomDistribution, {
      props: { experiment: buildExperiment() }
    });

    expect(wrapper.text()).toContain("Control will receive");
    expect(wrapper.text()).toContain("Treatment will receive");

    const fields = wrapper.findAllComponents({ name: "VTextField" });
    expect(fields).toHaveLength(2);
    expect(fields[0].props("modelValue")).toBe(50);
    expect(fields[1].props("modelValue")).toBe(50);
  });

  it("disables Continue, but only shows the error message once a field has been touched", async () => {
    const wrapper = mountComponent(ParticipationCustomDistribution, {
      props: {
        experiment: buildExperiment({
          conditions: [
            { conditionId: 1, name: "Control", distributionPct: 50 },
            { conditionId: 2, name: "Treatment", distributionPct: 40 }
          ]
        })
      }
    });

    expect(wrapper.findComponent({ name: "VBtn" }).props("disabled")).toBe(true);
    expect(wrapper.text()).not.toContain("Enter a percentage of zero or greater");

    await wrapper.findAllComponents({ name: "VTextField" })[0]
      .find("input")
      .trigger("input");

    expect(wrapper.text()).toContain(
      "Enter a percentage of zero or greater for each condition"
    );
    expect(wrapper.text()).toContain("they currently add up to 90%");
  });

  it("treats a blank field as invalid rather than silently coercing it to 0", async () => {
    const wrapper = mountComponent(ParticipationCustomDistribution, {
      props: {
        experiment: buildExperiment({
          conditions: [
            { conditionId: 1, name: "Control", distributionPct: 100 },
            { conditionId: 2, name: "Treatment", distributionPct: "" }
          ]
        })
      }
    });

    expect(wrapper.findComponent({ name: "VBtn" }).props("disabled")).toBe(true);
  });

  it("disables Continue when a distribution value is negative", () => {
    const wrapper = mountComponent(ParticipationCustomDistribution, {
      props: {
        experiment: buildExperiment({
          conditions: [
            { conditionId: 1, name: "Control", distributionPct: -10 },
            { conditionId: 2, name: "Treatment", distributionPct: 110 }
          ]
        })
      }
    });

    expect(wrapper.findComponent({ name: "VBtn" }).props("disabled")).toBe(true);
  });

  it("enables Continue once the values total exactly 100", async () => {
    const wrapper = mountComponent(ParticipationCustomDistribution, {
      props: {
        experiment: buildExperiment({
          conditions: [
            { conditionId: 1, name: "Control", distributionPct: 30 },
            { conditionId: 2, name: "Treatment", distributionPct: 70 }
          ]
        })
      }
    });

    expect(wrapper.findComponent({ name: "VBtn" }).props("disabled")).toBe(false);
  });

  it("updates a distribution value and re-enables Continue when the total becomes 100", async () => {
    const wrapper = mountComponent(ParticipationCustomDistribution, {
      props: { experiment: buildExperiment() }
    });

    const fields = wrapper.findAllComponents({ name: "VTextField" });
    await fields[0].find("input").setValue("60");
    await fields[1].find("input").setValue("40");

    expect(wrapper.findComponent({ name: "VBtn" }).props("disabled")).toBe(false);
  });

  it("saves the updated distributions and navigates to ParticipationSummary on Continue", async () => {
    conditionService.updateAll.mockResolvedValue({ status: 200 });

    const wrapper = mountComponent(ParticipationCustomDistribution, {
      props: { experiment: buildExperiment() }
    });

    await wrapper.findComponent({ name: "VBtn" }).trigger("click");
    await flushPromisesAndTicks(wrapper);

    expect(conditionService.updateAll).toHaveBeenCalledWith([
      expect.objectContaining({
        conditionId: 1,
        distributionPct: 50,
        experimentId: 1
      }),
      expect.objectContaining({
        conditionId: 2,
        distributionPct: 50,
        experimentId: 1
      })
    ]);

    expect(push).toHaveBeenCalledWith({
      name: "ParticipationSummary",
      params: { experiment: 1 }
    });
  });

  it("shows an error alert when saving the distribution fails", async () => {
    conditionService.updateAll.mockResolvedValue({
      status: 400,
      error: "Could not save"
    });

    const wrapper = mountComponent(ParticipationCustomDistribution, {
      props: { experiment: buildExperiment() }
    });

    await wrapper.findComponent({ name: "VBtn" }).trigger("click");
    await flushPromisesAndTicks(wrapper);

    expect(Swal.fire).toHaveBeenCalledWith("Could not save");
    expect(push).not.toHaveBeenCalled();
  });

  it("saveExit skips saving and exits immediately when the distribution is invalid", () => {
    const wrapper = mountComponent(ParticipationCustomDistribution, {
      props: {
        experiment: buildExperiment({
          conditions: [
            { conditionId: 1, name: "Control", distributionPct: 10 },
            { conditionId: 2, name: "Treatment", distributionPct: 10 }
          ]
        })
      }
    });

    wrapper.vm.saveExit();

    expect(conditionService.updateAll).not.toHaveBeenCalled();
    expect(push).toHaveBeenCalledWith({
      name: "Home",
      params: { experiment: 1 }
    });
  });

  it("saveExit saves and routes to the caller page from edit mode when the distribution is valid", async () => {
    conditionService.updateAll.mockResolvedValue({ status: 200 });

    const wrapper = mountComponent(ParticipationCustomDistribution, {
      props: { experiment: buildExperiment() }
    });

    const navigationStore = navigationModule();
    navigationStore.editMode = { callerPage: { name: "ParticipationSummary" } };

    wrapper.vm.saveExit();
    await flushPromisesAndTicks(wrapper);

    expect(conditionService.updateAll).toHaveBeenCalled();
    expect(push).toHaveBeenCalledWith({
      name: "ParticipationSummary",
      params: { experiment: 1 }
    });
  });
});

async function flushPromisesAndTicks(wrapper) {
  await wrapper.vm.$nextTick();
  await new Promise(resolve => setTimeout(resolve));
  await wrapper.vm.$nextTick();
}
