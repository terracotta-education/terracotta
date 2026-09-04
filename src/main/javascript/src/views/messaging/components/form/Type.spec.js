import { afterEach, describe, expect, it, vi } from "vitest";

vi.mock("sweetalert2", () => ({
  default: {
    fire: vi.fn()
  }
}));

import Swal from "sweetalert2";
import { mountComponent } from "@/test-utils/mount";
import Type from "./Type.vue";

describe("Type", () => {
  let wrapper;

  afterEach(() => {
    wrapper?.unmount();
    vi.clearAllMocks();
  });

  it("renders the label and both message type options", () => {
    wrapper = mountComponent(Type, {
      props: { type: "EMAIL", label: "How should this message be sent?" }
    });

    expect(wrapper.text()).toContain("How should this message be sent?");
    expect(wrapper.text()).toContain("Email");
    expect(wrapper.text()).toContain("Canvas message");
  });

  it("selects the radio matching the type prop", () => {
    wrapper = mountComponent(Type, {
      props: { type: "CONVERSATION" }
    });

    expect(wrapper.findComponent({ name: "VRadioGroup" }).props("modelValue")).toBe(
      "CONVERSATION"
    );
  });

  it("disables the radio group when readOnly is true", () => {
    wrapper = mountComponent(Type, {
      props: { type: "EMAIL", readOnly: true }
    });

    expect(wrapper.findComponent({ name: "VRadioGroup" }).props("disabled")).toBe(true);
  });

  it("emits updated immediately when selecting EMAIL (no confirmation needed)", async () => {
    wrapper = mountComponent(Type, {
      props: { type: "CONVERSATION" }
    });

    await wrapper.findComponent({ name: "VRadioGroup" }).setValue("EMAIL");

    expect(wrapper.emitted("updated")).toEqual([["EMAIL"]]);
    expect(Swal.fire).not.toHaveBeenCalled();
  });

  it("confirms via Swal before switching from EMAIL to CONVERSATION, and emits CONVERSATION when confirmed", async () => {
    Swal.fire.mockResolvedValue({ isConfirmed: true });

    wrapper = mountComponent(Type, {
      props: { type: "EMAIL" }
    });

    await wrapper.findComponent({ name: "VRadioGroup" }).setValue("CONVERSATION");
    await wrapper.vm.$nextTick();
    await Promise.resolve();

    expect(Swal.fire).toHaveBeenCalledWith(
      expect.objectContaining({ title: "Are you sure you want to switch?" })
    );
    expect(wrapper.emitted("updated")).toEqual([["CONVERSATION"]]);
  });

  it("reverts to EMAIL and emits EMAIL when the confirmation is cancelled", async () => {
    Swal.fire.mockResolvedValue({ isConfirmed: false });

    wrapper = mountComponent(Type, {
      props: { type: "EMAIL" }
    });

    await wrapper.findComponent({ name: "VRadioGroup" }).setValue("CONVERSATION");
    await wrapper.vm.$nextTick();
    await Promise.resolve();

    expect(wrapper.emitted("updated")).toEqual([["EMAIL"]]);
  });

  it("shows validatedErrors as error messages on the radio group", () => {
    wrapper = mountComponent(Type, {
      props: {
        type: "EMAIL",
        validatedErrors: "Message type is required."
      }
    });

    expect(
      wrapper.findComponent({ name: "VRadioGroup" }).props("errorMessages")
    ).toBe("Message type is required.");
  });
});
