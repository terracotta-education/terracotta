import { describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

const push = vi.fn();

vi.mock("vue-router", () => ({
  useRouter: () => ({
    push
  })
}));

import { mountComponent } from "@/test-utils/mount";
import ParticipationTypeManual from "./ParticipationTypeManual.vue";
import { configuration as configurationModule } from "@/store/configuration.module";

const RouterLinkStub = {
  name: "RouterLink",
  props: ["to"],
  template: "<a><slot /></a>"
};

const buildExperiment = (overrides = {}) => ({
  experimentId: 1,
  conditions: [{ conditionId: 1, name: "Control" }],
  ...overrides
});

// NOTE: ParticipationTypeManual.vue reads
// `configurations.value.parentalPermissionTemplateUrl` without optional
// chaining, while the configuration store defaults `configurations` to
// `null`. That throws on mount whenever the store hasn't been populated
// yet (e.g. before Intro/configuration.retrieve() resolves in the real
// app). Pre-seeding the store here works around it for these tests, but
// this looks like a real latent bug worth flagging.
const mountView = (experimentOverrides, configurationOverrides = {}) => {
  const pinia = createPinia();
  setActivePinia(pinia);

  const configurationStore = configurationModule();
  configurationStore.configurations = {
    parentalPermissionTemplateUrl: "https://example.com/template.docx",
    ...configurationOverrides
  };

  return mountComponent(ParticipationTypeManual, {
    pinia,
    props: { experiment: buildExperiment(experimentOverrides) },
    global: { components: { RouterLink: RouterLinkStub } }
  });
};

describe("ParticipationTypeManual", () => {
  it("links the parental permission template download to the configured URL", () => {
    const wrapper = mountView();

    const link = wrapper.find("a.link-download-template");
    expect(link.attributes("href")).toBe("https://example.com/template.docx");
  });

  it("links Continue to ParticipationTypeManualSelection", () => {
    const wrapper = mountView();

    const button = wrapper.findComponent({ name: "VBtn" });
    expect(button.props("to")).toEqual({
      name: "ParticipationTypeManualSelection"
    });
  });

  it("skip link goes to ParticipationDistribution for a multi-condition experiment", () => {
    const wrapper = mountView({
      conditions: [
        { conditionId: 1, name: "Control" },
        { conditionId: 2, name: "Treatment" }
      ]
    });

    const skipLink = wrapper.findComponent({ name: "RouterLink" });
    expect(skipLink.props("to")).toEqual({
      name: "ParticipationDistribution"
    });
  });

  it("skip link goes straight to ParticipationSummary for a single-condition experiment", () => {
    const wrapper = mountView({
      conditions: [{ conditionId: 1, name: "Control" }]
    });

    const skipLink = wrapper.findComponent({ name: "RouterLink" });
    expect(skipLink.props("to")).toEqual({ name: "ParticipationSummary" });
  });

  it("saveExit navigates to the caller page from edit mode, defaulting to Home", () => {
    const wrapper = mountView({ experimentId: 5 });

    wrapper.vm.saveExit();

    expect(push).toHaveBeenCalledWith({
      name: "Home",
      params: { experiment: 5 }
    });
  });
});
