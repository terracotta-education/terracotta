import { describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import SummaryData from "./SummaryData.vue";

describe("SummaryCount (SummaryData.vue)", () => {
  it("renders the title, value, and icon from props", () => {
    const wrapper = mountComponent(SummaryData, {
      props: {
        title: "Participants",
        value: 42,
        icon: "/assets/participants.svg",
        iconBgColor: "#fef8e6"
      }
    });

    expect(wrapper.find("h3").text()).toBe("Participants");
    expect(wrapper.find(".summary-count").text()).toBe("42");
    expect(wrapper.find(".header-icon").exists()).toBe(true);
    expect(wrapper.find(".header-icon").attributes("style")).toContain(
      "--header-icon-bg-color: #fef8e6"
    );
  });

  it("does not render an icon image when none is provided", () => {
    const wrapper = mountComponent(SummaryData, {
      props: { title: "Foo", value: 1 }
    });

    expect(wrapper.find(".header-icon").exists()).toBe(false);
  });

  it("defaults title to N/A and value to 0 when not provided", () => {
    const wrapper = mountComponent(SummaryData, { props: {} });

    expect(wrapper.find("h3").text()).toBe("N/A");
    expect(wrapper.find(".summary-count").text()).toBe("0");
  });

  it("does not render a tooltip by default", () => {
    const wrapper = mountComponent(SummaryData, {
      props: { title: "Foo", value: 1 }
    });

    expect(wrapper.findComponent({ name: "ToolTip" }).exists()).toBe(false);
  });

  it("renders a tooltip with the message and icon-based activator when showTooltip is true", () => {
    const wrapper = mountComponent(SummaryData, {
      props: {
        title: "Participants",
        value: 1,
        message: "Helpful explanation",
        showTooltip: true
      }
    });

    const tooltip = wrapper.findComponent({ name: "ToolTip" });

    expect(tooltip.exists()).toBe(true);
    expect(tooltip.props("header")).toBe("Participants");
    expect(tooltip.props("content")).toBe("Helpful explanation");
    expect(tooltip.props("activatorType")).toBe("icon");
    expect(tooltip.props("icon")).toBe("mdi-information-outline");
  });

  it("falls back to N/A for the tooltip message when none is provided", () => {
    const wrapper = mountComponent(SummaryData, {
      props: { title: "Foo", value: 1, showTooltip: true }
    });

    expect(wrapper.findComponent({ name: "ToolTip" }).props("content")).toBe("N/A");
  });

  it("applies a custom value font size when provided", () => {
    const wrapper = mountComponent(SummaryData, {
      props: { title: "Foo", value: 1, valueFontSize: "1em" }
    });

    expect(wrapper.find(".summary-count").attributes("style")).toContain("font-size: 1em");
  });

  it("defaults the value font size to 2em when not provided", () => {
    const wrapper = mountComponent(SummaryData, {
      props: { title: "Foo", value: 1 }
    });

    expect(wrapper.find(".summary-count").attributes("style")).toContain("font-size: 2em");
  });
});
