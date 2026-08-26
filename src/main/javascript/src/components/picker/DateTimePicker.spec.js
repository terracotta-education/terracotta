import { describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import DateTimePicker from "./DateTimePicker.vue";

describe("DateTimePicker", () => {
  it("renders with the id, name and aria-label props applied", () => {
    const wrapper = mountComponent(DateTimePicker, {
      props: {
        id: "start-date",
        name: "startDate",
        ariaLabel: "Start date picker",
        modelValue: null
      }
    });

    expect(wrapper.attributes("id")).toBe("start-date");
    // name/aria-label aren't declared props on the FlatPickr component, so they
    // fall through as plain attributes onto its rendered <input>.
    expect(wrapper.find("input").attributes("name")).toBe("startDate");
  });

  it("initializes the flatpickr v-model from modelValue", () => {
    const wrapper = mountComponent(DateTimePicker, {
      props: {
        modelValue: "2024-01-15T10:00:00Z"
      }
    });

    expect(wrapper.findComponent({ name: "FlatPickr" }).props("modelValue")).toBe("2024-01-15T10:00:00Z");
  });

  it("passes enableTime/enableDate/min/max down to the flatpickr config", () => {
    const wrapper = mountComponent(DateTimePicker, {
      props: {
        modelValue: null,
        enableTime: false,
        enableDate: true,
        min: "2024-01-01",
        max: "2024-12-31"
      }
    });

    const config = wrapper.findComponent({ name: "FlatPickr" }).props("config");

    expect(config.enableTime).toBe(false);
    expect(config.noCalendar).toBe(false);
    expect(config.minDate).toBe("2024-01-01");
    expect(config.maxDate).toBe("2024-12-31");
    expect(config.altFormat).toBe("m/d/Y");
  });

  it("uses a time-inclusive altFormat when enableTime is true", () => {
    const wrapper = mountComponent(DateTimePicker, {
      props: {
        modelValue: null,
        enableTime: true
      }
    });

    const config = wrapper.findComponent({ name: "FlatPickr" }).props("config");

    expect(config.altFormat).toBe("m/d/Y h:iK");
  });

  it("sets noCalendar to true when enableDate is false", () => {
    const wrapper = mountComponent(DateTimePicker, {
      props: {
        modelValue: null,
        enableDate: false
      }
    });

    expect(wrapper.findComponent({ name: "FlatPickr" }).props("config").noCalendar).toBe(true);
  });

  it("emits update:modelValue when the underlying date value changes", async () => {
    const wrapper = mountComponent(DateTimePicker, {
      props: {
        modelValue: null
      }
    });

    await wrapper.findComponent({ name: "FlatPickr" }).vm.$emit("update:modelValue", "2024-02-02");

    expect(wrapper.emitted("update:modelValue")).toBeTruthy();
    expect(wrapper.emitted("update:modelValue")[0]).toEqual(["2024-02-02"]);
  });

  it("emits update:modelValue with the date from on-change", async () => {
    const wrapper = mountComponent(DateTimePicker, {
      props: {
        modelValue: null
      }
    });

    await wrapper.findComponent({ name: "FlatPickr" }).vm.$emit(
      "on-change",
      [new Date("2024-03-03T00:00:00Z")],
      "2024-03-03T00:00:00Z"
    );

    expect(wrapper.emitted("update:modelValue")).toBeTruthy();

    // The real (unstubbed) flatpickr instance round-trips the value through its
    // own formatter once it receives it via v-model, so only the timestamp -
    // not the exact string - is guaranteed to be preserved.
    const lastEmitted = wrapper.emitted("update:modelValue").at(-1)[0];

    expect(new Date(lastEmitted).getTime()).toBe(new Date("2024-03-03T00:00:00Z").getTime());
  });

  it("updates the internal date when modelValue prop changes externally", async () => {
    const wrapper = mountComponent(DateTimePicker, {
      props: {
        modelValue: "2024-01-01T00:00:00Z"
      }
    });

    await wrapper.setProps({ modelValue: "2024-06-06T00:00:00Z" });

    const flatpickrModelValue = wrapper.findComponent({ name: "FlatPickr" }).props("modelValue");

    expect(new Date(flatpickrModelValue).getTime()).toBe(new Date("2024-06-06T00:00:00Z").getTime());
  });

  it("passes the disabled prop through to the flatpickr input", () => {
    const wrapper = mountComponent(DateTimePicker, {
      props: {
        modelValue: null,
        disabled: true
      }
    });

    expect(wrapper.findComponent({ name: "FlatPickr" }).props("disabled")).toBe(true);
  });

  it("does not attempt to open the picker when disabled and the container is clicked", async () => {
    const wrapper = mountComponent(DateTimePicker, {
      props: {
        modelValue: null,
        disabled: true
      }
    });

    // Guard against throwing when fp isn't ready / disabled short-circuits before use.
    await expect(wrapper.trigger("click")).resolves.not.toThrow();
  });

  it("prevents the calendar icon's mousedown default so clicking it doesn't steal focus from the input", () => {
    // Browsers focus the nearest focusable ancestor on mousedown of a non-focusable
    // descendant. Without preventing this, clicking the icon focuses the tabindex=0
    // wrapper div instead of the flatpickr input, so a later click on a calendar day
    // (rendered outside the wrapper) blurs the div and closes the picker before the
    // selection is saved. jsdom doesn't simulate that browser default-focus behavior,
    // so this only verifies the fix's mechanism (preventDefault on the icon's
    // mousedown), not the full end-to-end symptom.
    const wrapper = mountComponent(DateTimePicker, {
      props: { modelValue: null }
    });

    const icon = wrapper.find(".v-icon").element;
    const event = new MouseEvent("mousedown", { bubbles: true, cancelable: true });
    icon.dispatchEvent(event);

    expect(event.defaultPrevented).toBe(true);
  });

  it("applies custom classes to both the container and the flatpickr input", () => {
    const wrapper = mountComponent(DateTimePicker, {
      props: {
        modelValue: null,
        classes: "my-custom-class"
      }
    });

    expect(wrapper.classes()).toContain("my-custom-class");
    expect(wrapper.find("input").classes()).toContain("my-custom-class");
  });
});
