import { describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import IntegrationsTokenAlert from "./IntegrationsTokenAlert.vue";

const TYPES = Object.freeze({
  initial: "initial",
  warning: "warning",
  expired: "expired"
});

function buildAlert(overrides = {}) {
  return {
    date: "August 1 at 3:00 PM",
    display: true,
    type: TYPES.initial,
    types: TYPES,
    ...overrides
  };
}

describe("IntegrationsTokenAlert", () => {
  it("shows the initial expiration alert", () => {
    const wrapper = mountComponent(IntegrationsTokenAlert, {
      props: { alert: buildAlert({ type: TYPES.initial }) }
    });

    expect(wrapper.find(".alert.initial").exists()).toBe(true);
    expect(wrapper.text()).toContain(
      "This attempt will expire on August 1 at 3:00 PM."
    );
  });

  it("shows the warning alert with the remaining time", () => {
    const wrapper = mountComponent(IntegrationsTokenAlert, {
      props: {
        alert: buildAlert({ type: TYPES.warning, date: "5 minutes" })
      }
    });

    expect(wrapper.find(".alert.warning").exists()).toBe(true);
    expect(wrapper.text()).toContain("This attempt will expire in");
    expect(wrapper.text()).toContain("5 minutes");
  });

  it("shows the expired alert", () => {
    const wrapper = mountComponent(IntegrationsTokenAlert, {
      props: { alert: buildAlert({ type: TYPES.expired }) }
    });

    expect(wrapper.find(".alert.expired").exists()).toBe(true);
    expect(wrapper.text()).toContain("This attempt has expired.");
    expect(wrapper.text()).toContain(
      "Your session ended on August 1 at 3:00 PM."
    );
  });

  it("shows only the alert matching the current type", () => {
    const wrapper = mountComponent(IntegrationsTokenAlert, {
      props: { alert: buildAlert({ type: TYPES.warning, date: "5 minutes" }) }
    });

    expect(wrapper.find(".alert.initial").exists()).toBe(false);
    expect(wrapper.find(".alert.expired").exists()).toBe(false);
  });

  it("collapses the warning body text when minimized via the close icon, without hiding the alert", async () => {
    const wrapper = mountComponent(IntegrationsTokenAlert, {
      props: { alert: buildAlert({ type: TYPES.warning, date: "5 minutes" }) }
    });

    expect(wrapper.text()).toContain(
      "begin a new timed session by closing and reopening this assignment"
    );

    const closeIcon = wrapper.find(".alert.warning .v-alert__close .v-icon");
    await closeIcon.trigger("click");

    expect(wrapper.find(".alert.warning").exists()).toBe(true);
    expect(wrapper.text()).not.toContain(
      "begin a new timed session by closing and reopening this assignment"
    );
  });

  it("re-displays the alert when the alert type changes on an already-dismissed alert", async () => {
    const wrapper = mountComponent(IntegrationsTokenAlert, {
      props: { alert: buildAlert({ type: TYPES.initial, display: false }) }
    });

    await wrapper.setProps({
      alert: buildAlert({
        type: TYPES.warning,
        display: false,
        date: "5 minutes"
      })
    });

    expect(wrapper.find(".alert.warning").exists()).toBe(true);
  });
});
