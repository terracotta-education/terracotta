import { describe, expect, it, vi, beforeEach } from "vitest";

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
  consentService: {
    create: vi.fn(),
    getConsentFile: vi.fn()
  }
}));

import { createPinia, setActivePinia } from "pinia";
import Swal from "sweetalert2";
import { mountComponent } from "@/test-utils/mount";
import ParticipationTypeConsentFile from "./ParticipationTypeConsentFile.vue";
import { consentService } from "@/services";
import { navigation as navigationModule } from "@/store/navigation.module";
import { configuration as configurationModule } from "@/store/configuration.module";

const makeFile = (name = "consent.pdf", type = "application/pdf") =>
  new File(["x".repeat(10)], name, { type });

const flush = () => new Promise(resolve => setTimeout(resolve));

// The view nests FileDropZone, which renders its own "Upload PDF" VBtn, so
// findComponent({ name: "VBtn" }) would grab that one instead of "Next".
const findNextButton = wrapper =>
  wrapper
    .findAllComponents({ name: "VBtn" })
    .find(button => button.text().trim() === "Next");

describe("ParticipationTypeConsentFile", () => {
  beforeEach(() => {
    push.mockClear();
    Swal.fire.mockClear();
    consentService.create.mockReset();
    consentService.getConsentFile.mockReset();
  });

  const mountView = (experimentOverrides = {}, configureStores) => {
    const pinia = createPinia();
    setActivePinia(pinia);

    // NOTE: icsFileUrl reads configurations.value.icsTemplateUrl with no
    // optional chaining, so it throws while configurations is still null
    // (the store's default state before configuration.retrieve() resolves).
    // Seed an empty object so the component can render in tests; this is a
    // real bug in the source, flagged separately, not fixed here.
    configurationModule().configurations = {};

    // Any store state needed before mount must be set against this same
    // pinia instance (not one left active by a previous test).
    if (configureStores) {
      configureStores();
    }

    return mountComponent(
      ParticipationTypeConsentFile,
      {
        pinia,
        props: {
          experiment: {
            experimentId: 1,
            conditions: [],
            ...experimentOverrides
          }
        },
        global: {
          stubs: {
            VuePdfEmbed: true
          }
        }
      }
    );
  };

  it("disables Next when no file has been uploaded", () => {
    const wrapper = mountView();

    const button = findNextButton(wrapper);
    expect(button.props("disabled")).toBe(true);
  });

  it("enables Next once a pdf is dropped", async () => {
    const wrapper = mountView();

    const file = makeFile();
    await wrapper.find(".drop-zone").trigger("drop", {
      dataTransfer: { files: [file] }
    });
    await flush();

    const button = findNextButton(wrapper);
    expect(button.props("disabled")).toBe(false);
  });

  it("saves the consent file and navigates to the single-condition summary page", async () => {
    consentService.create.mockResolvedValue({ status: 200 });

    const wrapper = mountView({ conditions: [{ conditionId: 1 }] });

    const file = makeFile();
    await wrapper.find(".drop-zone").trigger("drop", {
      dataTransfer: { files: [file] }
    });
    await flush();

    const button = findNextButton(wrapper);
    await button.trigger("click");
    await flush();

    expect(consentService.create).toHaveBeenCalledWith(
      1,
      file,
      expect.any(String)
    );
    expect(push).toHaveBeenCalledWith({
      name: "ParticipationSummary",
      params: { experiment: 1 }
    });
  });

  it("navigates to ParticipationDistribution for multi-condition experiments", async () => {
    consentService.create.mockResolvedValue({ status: 200 });

    const wrapper = mountView({
      conditions: [{ conditionId: 1 }, { conditionId: 2 }]
    });

    const file = makeFile();
    await wrapper.find(".drop-zone").trigger("drop", {
      dataTransfer: { files: [file] }
    });
    await flush();

    await findNextButton(wrapper).trigger("click");
    await flush();

    expect(push).toHaveBeenCalledWith({
      name: "ParticipationDistribution",
      params: { experiment: 1 }
    });
  });

  it("shows an error dialog and does not navigate when the upload fails", async () => {
    consentService.create.mockRejectedValue(new Error("upload failed"));

    const wrapper = mountView();

    const file = makeFile();
    await wrapper.find(".drop-zone").trigger("drop", {
      dataTransfer: { files: [file] }
    });
    await flush();

    await findNextButton(wrapper).trigger("click");
    await flush();

    expect(Swal.fire).toHaveBeenCalledWith(
      expect.objectContaining({
        text: expect.stringContaining("upload failed"),
        icon: "error"
      })
    );
    expect(push).not.toHaveBeenCalled();
  });

  it("downloads and displays the existing consent file when in edit mode", async () => {
    consentService.getConsentFile.mockResolvedValue({
      status: 200,
      base: "base64data"
    });

    const wrapper = mountView({}, () => {
      navigationModule().editMode = {
        callerPage: { name: "ExperimentSummary" }
      };
    });
    await flush();
    await wrapper.vm.$nextTick();

    expect(consentService.getConsentFile).toHaveBeenCalledWith(1);
    expect(wrapper.find(".drop-zone__uploaded").exists()).toBe(true);
  });

  it("does not download an existing file for a brand-new experiment", () => {
    mountView();

    expect(consentService.getConsentFile).not.toHaveBeenCalled();
  });

  it("renders the ics template download link from configuration", () => {
    const wrapper = mountView();

    const configurationStore = configurationModule();
    configurationStore.configurations = {
      icsTemplateUrl: "https://example.com/template.docx"
    };

    return wrapper.vm.$nextTick().then(() => {
      const link = wrapper.find("a[download]");
      expect(link.attributes("href")).toBe(
        "https://example.com/template.docx"
      );
    });
  });

  it("saveExit saves the consent file and pushes to the save/exit page", async () => {
    consentService.create.mockResolvedValue({ status: 200 });

    const wrapper = mountView({}, () => {
      navigationModule().editMode = {
        callerPage: { name: "ExperimentSummary" }
      };
    });

    const file = makeFile();
    await wrapper.find(".drop-zone").trigger("drop", {
      dataTransfer: { files: [file] }
    });
    await flush();

    wrapper.vm.saveExit();
    await flush();

    expect(push).toHaveBeenCalledWith({
      name: "ExperimentSummary",
      params: { experiment: 1 }
    });
  });
});
