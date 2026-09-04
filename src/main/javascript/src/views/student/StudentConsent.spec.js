import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { flushPromises } from "@vue/test-utils";

vi.mock("vue-pdf-embed", () => ({
  default: {
    name: "VuePdfEmbed",
    template: "<div class=\"vue-pdf-embed-stub\" />"
  }
}));

vi.mock("sweetalert2", () => ({
  default: {
    fire: vi.fn().mockResolvedValue({})
  }
}));

vi.mock("@/services", () => ({
  consentService: {
    getConsentFile: vi.fn()
  },
  participantService: {
    updateParticipant: vi.fn()
  },
  apiService: {
    reportStep: vi.fn()
  }
}));

import Swal from "sweetalert2";
import { consentService, participantService, apiService } from "@/services";
import { mountComponent } from "@/test-utils/mount";
import StudentConsent from "./StudentConsent.vue";

const baseParticipant = {
  participantId: 1,
  source: null,
  started: false,
  consent: null,
  dateGiven: null,
  dateRevoked: null
};

async function mountAndLoad(participantOverrides = {}) {
  consentService.getConsentFile.mockResolvedValue({
    status: 200,
    base: "PDFDATA"
  });

  apiService.reportStep.mockResolvedValue({
    status: 200,
    data: { ...baseParticipant, ...participantOverrides }
  });

  const wrapper = mountComponent(StudentConsent, {
    props: {
      experimentId: "1",
      userId: "u1"
    }
  });

  await flushPromises();
  await flushPromises();

  return wrapper;
}

describe("StudentConsent", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it("loads the consent PDF and participant step on mount, then emits loaded", async () => {
    const wrapper = await mountAndLoad();

    expect(consentService.getConsentFile).toHaveBeenCalledWith("1");
    expect(apiService.reportStep).toHaveBeenCalledWith(
      "1",
      "launch_consent_assignment",
      null,
      false
    );

    expect(wrapper.emitted("loaded")).toBeTruthy();
    expect(wrapper.find(".consent-steps").attributes("style")).not.toContain("display: none");
  });

  it("shows the responded alert when the participant has already consented", async () => {
    const wrapper = await mountAndLoad({
      source: "CONSENT",
      consent: true,
      dateGiven: "2024-01-01T00:00:00.000Z"
    });

    expect(wrapper.text()).toContain("You responded \"agree to participate\"");
  });

  it("shows the responded alert with 'do not' wording when consent was declined/revoked", async () => {
    const wrapper = await mountAndLoad({
      source: "REVOKED",
      consent: false,
      dateRevoked: "2024-01-01T00:00:00.000Z"
    });

    expect(wrapper.text()).toContain("You responded \"do not agree to participate\"");
  });

  it("shows the already-accessed alert when the participant started without consenting", async () => {
    const wrapper = await mountAndLoad({
      started: true,
      consent: false
    });

    expect(wrapper.text()).toContain("no matter your response to the following question");
  });

  it("does not show either alert for a fresh, unstarted, unconsented participant", async () => {
    const wrapper = await mountAndLoad();

    expect(wrapper.text()).not.toContain("You responded");
    expect(wrapper.text()).not.toContain("no matter your response to the following question");
  });

  it("submits the selected answer and shows a success message", async () => {
    const wrapper = await mountAndLoad();

    participantService.updateParticipant.mockResolvedValue({
      ...baseParticipant,
      consent: true
    });

    const radioInputs = wrapper.findAll("input[type=\"radio\"]");
    await radioInputs[0].setValue(true);

    await wrapper.find("form").trigger("submit");
    await flushPromises();

    expect(participantService.updateParticipant).toHaveBeenCalledWith(
      "1",
      expect.objectContaining({ consent: true, participantId: 1 })
    );
    expect(Swal.fire).toHaveBeenCalledWith(
      expect.objectContaining({ text: "Successfully submitted consent", icon: "success" })
    );
  });

  it("blocks submission and warns when the participant has already accessed an assignment", async () => {
    const wrapper = await mountAndLoad({ started: true });

    const radioInputs = wrapper.findAll("input[type=\"radio\"]");
    await radioInputs[1].setValue(true);

    await wrapper.find("form").trigger("submit");
    await flushPromises();

    expect(participantService.updateParticipant).not.toHaveBeenCalled();
    expect(Swal.fire).toHaveBeenCalledWith(
      expect.objectContaining({ icon: "error" })
    );
  });

  it("shows a success message without re-submitting when the same answer is chosen again", async () => {
    const wrapper = await mountAndLoad({
      source: "CONSENT",
      consent: true,
      dateGiven: "2024-01-01T00:00:00.000Z"
    });

    const radioInputs = wrapper.findAll("input[type=\"radio\"]");
    await radioInputs[0].setValue(true);

    await wrapper.find("form").trigger("submit");
    await flushPromises();

    expect(participantService.updateParticipant).not.toHaveBeenCalled();
    expect(Swal.fire).toHaveBeenCalledWith(
      expect.objectContaining({ text: "Successfully submitted Consent", icon: "success" })
    );
  });
});
