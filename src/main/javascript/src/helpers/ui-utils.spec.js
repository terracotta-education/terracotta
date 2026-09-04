import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { createPinia, setActivePinia } from "pinia";

import {
  widenContainer,
  shrinkContainer,
  adjustBodyTopPadding,
  getColor,
  deleteAttributesFromObservedElement,
  addAttributesToObservedElement,
  deleteAttributesFromElement,
  addAttributesToElement,
  getAttributeFromElement,
  handleTooltipOpening,
  statusAlert,
  createStatusAlert,
  showSkipLink
} from "./ui-utils";
import { alert as alertStore } from "@/store/alert.module";
import { configuration as configurationStore } from "@/store/configuration.module";

function flushMutations() {
  return new Promise((resolve) => setTimeout(resolve, 0));
}

beforeEach(() => {
  document.body.innerHTML = "";
  document.documentElement.style.cssText = "";
});

describe("widenContainer", () => {
  it("swaps the default col-md-6 class for col-md-10", () => {
    document.body.innerHTML =
      '<div class="steps-container-col col-md-6"></div>';

    widenContainer();

    const el = document.getElementsByClassName("steps-container-col")[0];
    expect(el.classList.contains("col-md-6")).toBe(false);
    expect(el.classList.contains("col-md-10")).toBe(true);
  });

  it("supports custom from/to class names", () => {
    document.body.innerHTML =
      '<div class="steps-container-col custom-a"></div>';

    widenContainer("custom-a", "custom-b");

    const el = document.getElementsByClassName("steps-container-col")[0];
    expect(el.classList.contains("custom-a")).toBe(false);
    expect(el.classList.contains("custom-b")).toBe(true);
  });
});

describe("shrinkContainer", () => {
  it("swaps the default col-md-10 class back to col-md-6", () => {
    document.body.innerHTML =
      '<div class="steps-container-col col-md-10"></div>';

    shrinkContainer();

    const el = document.getElementsByClassName("steps-container-col")[0];
    expect(el.classList.contains("col-md-10")).toBe(false);
    expect(el.classList.contains("col-md-6")).toBe(true);
  });
});

describe("adjustBodyTopPadding", () => {
  it("uses defaults to remove and re-add pt-4", () => {
    document.body.innerHTML =
      '<div class="experiment-steps__body pt-4"></div>';

    adjustBodyTopPadding();

    const el = document.getElementsByClassName("experiment-steps__body")[0];
    expect(el.classList.contains("pt-4")).toBe(true);
  });

  it("removes the 'from' class and adds the 'to' class when both given", () => {
    document.body.innerHTML =
      '<div class="experiment-steps__body pt-0"></div>';

    adjustBodyTopPadding("pt-8", "pt-0");

    const el = document.getElementsByClassName("experiment-steps__body")[0];
    expect(el.classList.contains("pt-0")).toBe(false);
    expect(el.classList.contains("pt-8")).toBe(true);
  });

  it("does not add a class when 'to' is falsy", () => {
    document.body.innerHTML =
      '<div class="experiment-steps__body pt-4"></div>';

    adjustBodyTopPadding(null, "pt-4");

    const el = document.getElementsByClassName("experiment-steps__body")[0];
    expect(el.classList.contains("pt-4")).toBe(false);
    expect(el.className.trim()).toBe("experiment-steps__body");
  });
});

describe("getColor", () => {
  it("reads a CSS custom property set on the document element's inline style", () => {
    document.documentElement.style.setProperty("--my-color", "#ff0000");

    expect(getColor("--my-color")).toBe("#ff0000");
  });

  // this is how every real color custom property is actually defined (variables.scss's
  // :root {} block) - a naive documentElement.style.getPropertyValue() read (rather than
  // getComputedStyle) would silently return "" here, which was the actual bug: Vuetify 3's
  // v-alert :color prop doesn't fall back to a type-derived color for an empty string the
  // way v2's did, so the alert rendered with no color at all instead of falling back visibly.
  it("reads a CSS custom property defined via a stylesheet rule, not just inline style", () => {
    const style = document.createElement("style");
    style.textContent = ":root { --stylesheet-color: #00ff00; }";
    document.head.appendChild(style);

    expect(getColor("--stylesheet-color")).toBe("#00ff00");

    document.head.removeChild(style);
  });

  it("returns an empty string for a property that has not been set", () => {
    expect(getColor("--not-set")).toBe("");
  });
});

describe("observed element attribute helpers", () => {
  let warnSpy;

  beforeEach(() => {
    warnSpy = vi.spyOn(console, "warn").mockImplementation(() => {});
  });

  afterEach(() => {
    warnSpy.mockRestore();
  });

  it("addAttributesToObservedElement adds attributes to matching descendants of newly added nodes", async () => {
    document.body.innerHTML = '<div class="parent"></div>';

    addAttributesToObservedElement(".parent", "child-node", ".target", [
      { name: "aria-hidden", value: "true" }
    ]);

    const wrapper = document.createElement("div");
    wrapper.classList.add("child-node");
    wrapper.innerHTML = '<span class="target"></span>';
    document.querySelector(".parent").appendChild(wrapper);

    await flushMutations();

    expect(
      wrapper.querySelector(".target").getAttribute("aria-hidden")
    ).toBe("true");
  });

  it("addAttributesToObservedElement falls back to the node itself when no matching descendants exist", async () => {
    document.body.innerHTML = '<div class="parent2"></div>';

    addAttributesToObservedElement(".parent2", "child-node2", ".nope", [
      { name: "data-foo", value: "bar" }
    ]);

    const wrapper = document.createElement("div");
    wrapper.classList.add("child-node2");
    document.querySelector(".parent2").appendChild(wrapper);

    await flushMutations();

    expect(wrapper.getAttribute("data-foo")).toBe("bar");
  });

  it("deleteAttributesFromObservedElement removes attributes from matching descendants of newly added nodes", async () => {
    document.body.innerHTML = '<div class="parent3"></div>';

    deleteAttributesFromObservedElement(".parent3", "child-node3", ".target", [
      "aria-hidden"
    ]);

    const wrapper = document.createElement("div");
    wrapper.classList.add("child-node3");
    wrapper.innerHTML = '<span class="target" aria-hidden="true"></span>';
    document.querySelector(".parent3").appendChild(wrapper);

    await flushMutations();

    expect(
      wrapper.querySelector(".target").hasAttribute("aria-hidden")
    ).toBe(false);
  });

  it("warns and does not throw when the parent element does not exist (add)", () => {
    addAttributesToObservedElement(".missing-parent", "x", ".y", []);

    expect(warnSpy).toHaveBeenCalledWith(
      expect.stringContaining("Parent element with class .missing-parent not found")
    );
  });

  it("warns and does not throw when the parent element does not exist (delete)", () => {
    deleteAttributesFromObservedElement(".missing-parent", "x", ".y", []);

    expect(warnSpy).toHaveBeenCalledWith(
      expect.stringContaining("Parent element with class .missing-parent not found")
    );
  });
});

describe("deleteAttributesFromElement", () => {
  it("removes the given attributes from every matching element", () => {
    document.body.innerHTML =
      '<span class="target" data-a="1" data-b="2"></span>' +
      '<span class="target" data-a="3" data-b="4"></span>';

    deleteAttributesFromElement(".target", ["data-a", "data-b"]);

    document.querySelectorAll(".target").forEach((el) => {
      expect(el.hasAttribute("data-a")).toBe(false);
      expect(el.hasAttribute("data-b")).toBe(false);
    });
  });

  it("does nothing when there are no matching elements", () => {
    expect(() =>
      deleteAttributesFromElement(".missing", ["data-a"])
    ).not.toThrow();
  });
});

describe("addAttributesToElement", () => {
  it("adds the given attributes to every matching element", () => {
    document.body.innerHTML =
      '<span class="target"></span><span class="target"></span>';

    addAttributesToElement(".target", [{ name: "data-x", value: "yes" }]);

    document.querySelectorAll(".target").forEach((el) => {
      expect(el.getAttribute("data-x")).toBe("yes");
    });
  });
});

describe("getAttributeFromElement", () => {
  it("returns the attribute value from the first matching element", () => {
    document.body.innerHTML = '<span class="target" data-x="found"></span>';

    expect(getAttributeFromElement(".target", "data-x")).toBe("found");
  });

  it("returns null when no element matches", () => {
    expect(getAttributeFromElement(".missing", "data-x")).toBeNull();
  });
});

describe("handleTooltipOpening", () => {
  it("closes all tooltip refs except the one passed in", () => {
    const closeA = vi.fn();
    const closeB = vi.fn();
    const fakeThis = {
      $refs: {
        tooltipA: [{ close: closeA }],
        tooltipB: [{ close: closeB }]
      }
    };

    handleTooltipOpening.call(fakeThis, "tooltipA");

    expect(closeA).not.toHaveBeenCalled();
    expect(closeB).toHaveBeenCalledTimes(1);
  });

  it("does not throw when a ref is missing or empty", () => {
    const fakeThis = {
      $refs: {
        tooltipA: [],
        tooltipB: null
      }
    };

    expect(() => handleTooltipOpening.call(fakeThis, "tooltipC")).not.toThrow();
  });
});

describe("statusAlert", () => {
  it("builds a plain alert descriptor object", () => {
    expect(statusAlert("error", "Something broke")).toEqual({
      alertType: "error",
      alertMessage: "Something broke"
    });
  });
});

describe("createStatusAlert", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  it("dispatches to the matching alert store action", () => {
    createStatusAlert({ alertType: "success", alertMessage: "Yay" });

    const store = alertStore();
    expect(store.alertType).toBe("success");
    expect(store.alertMessage).toBe("Yay");
  });

  it("defaults to the info action when no alertType is given", () => {
    createStatusAlert({ alertMessage: "Default message" });

    const store = alertStore();
    expect(store.alertType).toBe("info");
    expect(store.alertMessage).toBe("Default message");
  });
});

describe("showSkipLink", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  it("stores the showSkipLink configuration value", () => {
    showSkipLink(true);

    const store = configurationStore();
    expect(store.configurations.showSkipLink).toBe(true);
  });

  it("updates showSkipLink to false", () => {
    showSkipLink(false);

    const store = configurationStore();
    expect(store.configurations.showSkipLink).toBe(false);
  });
});
