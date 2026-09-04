import { describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import Participants from "./Participants.vue";

describe("OverviewParticipantsSummary", () => {
  it("displays the participant count and converts the consent rate to a percent bar value", () => {
    const wrapper = mountComponent(Participants, {
      props: {
        participantsData: { count: 42, consentRate: 0.756 }
      }
    });

    const summaryData = wrapper.findComponent({ name: "SummaryCount" });
    const percentBar = wrapper.findComponent({ name: "PercentBar" });

    expect(summaryData.props("value")).toBe(42);
    expect(summaryData.props("showTooltip")).toBe(true);
    // percent(0.756) => Math.round(75.6) => 76
    expect(percentBar.props("value")).toBe(76);
  });

  it("defaults count and consent rate to 0 when participantsData is not provided", () => {
    const wrapper = mountComponent(Participants, { props: {} });

    const summaryData = wrapper.findComponent({ name: "SummaryCount" });
    const percentBar = wrapper.findComponent({ name: "PercentBar" });

    expect(summaryData.props("value")).toBe(0);
    expect(percentBar.props("value")).toBe(0);
  });

  it("defaults count and consent rate to 0 when participantsData has no matching fields", () => {
    const wrapper = mountComponent(Participants, {
      props: { participantsData: {} }
    });

    const summaryData = wrapper.findComponent({ name: "SummaryCount" });
    const percentBar = wrapper.findComponent({ name: "PercentBar" });

    expect(summaryData.props("value")).toBe(0);
    expect(percentBar.props("value")).toBe(0);
  });

  it("uses the 'Participants' title", () => {
    const wrapper = mountComponent(Participants, {
      props: { participantsData: { count: 1, consentRate: 1 } }
    });

    const summaryData = wrapper.findComponent({ name: "SummaryCount" });

    expect(summaryData.props("title")).toBe("Participants");
  });
});
