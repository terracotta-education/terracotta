import { afterEach, describe, expect, it, vi } from "vitest";

vi.mock("sweetalert2", () => ({
  default: {
    fire: vi.fn().mockResolvedValue({})
  }
}));

import Swal from "sweetalert2";
import { mountComponent } from "@/test-utils/mount";
import ReplyTo from "./ReplyTo.vue";

const setSearch = (wrapper, value) => {
  wrapper.findComponent({ name: "VCombobox" }).vm.$emit("update:search", value);
};

const setSelection = (wrapper, value) => {
  wrapper.findComponent({ name: "VCombobox" }).vm.$emit("update:modelValue", value);
};

const mountReplyTo = async props => {
  const wrapper = mountComponent(ReplyTo, { props });

  // isLoaded flips to true inside onMounted, which only takes effect on the
  // DOM after a reactivity flush.
  await wrapper.vm.$nextTick();

  return wrapper;
};

describe("ReplyTo", () => {
  let wrapper;

  afterEach(() => {
    wrapper?.unmount();
    vi.clearAllMocks();
  });

  it("renders the header and pre-populates chips from the replyTos prop", async () => {
    wrapper = await mountReplyTo({
      replyTos: [
        { id: 1, email: "a@example.com" },
        { id: 2, email: "b@example.com" }
      ]
    });

    expect(wrapper.text()).toContain("Reply-to addresses");
    expect(wrapper.text()).toContain("a@example.com");
    expect(wrapper.text()).toContain("b@example.com");
  });

  it("does not render a closable chip when there is only one reply-to address", async () => {
    wrapper = await mountReplyTo({
      replyTos: [{ id: 1, email: "only@example.com" }]
    });

    expect(wrapper.findAll(".v-chip__close").length).toBe(0);
  });

  it("renders closable chips when there is more than one reply-to address", async () => {
    wrapper = await mountReplyTo({
      replyTos: [
        { id: 1, email: "a@example.com" },
        { id: 2, email: "b@example.com" }
      ]
    });

    expect(wrapper.findAll(".v-chip__close").length).toBe(2);
  });

  it("requires a reply-to email and emits an empty list with an alert when none is provided", async () => {
    wrapper = await mountReplyTo({
      replyTos: [{ id: 1, email: "a@example.com" }],
      required: true
    });

    setSelection(wrapper, []);
    await wrapper.vm.updateReplyTo();

    expect(Swal.fire).toHaveBeenCalledWith(
      expect.objectContaining({ title: "Reply-to email is required" })
    );
    expect(wrapper.emitted("updated").at(-1)).toEqual([[]]);
    expect(wrapper.vm.isValid()).toBe(false);
  });

  it("rejects an invalid email address via an alert and does not add it", async () => {
    wrapper = await mountReplyTo({
      replyTos: [{ id: 1, email: "a@example.com" }]
    });

    setSelection(wrapper, [{ id: 1, email: "a@example.com", order: 0 }]);
    setSearch(wrapper, "not-an-email");
    await wrapper.vm.updateReplyTo();

    expect(Swal.fire).toHaveBeenCalledWith(
      expect.objectContaining({ title: "Invalid email" })
    );
    expect(wrapper.vm.isValid()).toBe(false);

    const updated = wrapper.emitted("updated").at(-1)[0];
    expect(updated.map(r => r.email)).toEqual(["a@example.com"]);
  });

  it("adds a valid new email address and emits the updated list", async () => {
    wrapper = await mountReplyTo({
      replyTos: [{ id: 1, email: "a@example.com" }]
    });

    setSelection(wrapper, [{ id: 1, email: "a@example.com", order: 0 }]);
    setSearch(wrapper, "new@example.com");
    await wrapper.vm.updateReplyTo();

    const updated = wrapper.emitted("updated").at(-1)[0];
    expect(updated.map(r => r.email)).toEqual([
      "a@example.com",
      "new@example.com"
    ]);
    expect(wrapper.vm.isValid()).toBe(true);
  });

  it("disables the combobox when read-only, matching the other message form components (Type/ToConsentedOnly/Scheduler all use :disabled)", async () => {
    wrapper = await mountReplyTo({
      replyTos: [{ id: 1, email: "a@example.com" }],
      readOnly: true
    });

    expect(wrapper.findComponent({ name: "VCombobox" }).props("disabled")).toBe(true);
  });

  it("does not render closable chips when read-only, even with multiple reply-to addresses", async () => {
    wrapper = await mountReplyTo({
      replyTos: [
        { id: 1, email: "a@example.com" },
        { id: 2, email: "b@example.com" }
      ],
      readOnly: true
    });

    expect(wrapper.findAll(".v-chip__close").length).toBe(0);
  });

  it("removes the chip from the combobox on close-button click and emits the updated list", async () => {
    wrapper = await mountReplyTo({
      replyTos: [
        { id: 1, email: "a@example.com" },
        { id: 2, email: "b@example.com" }
      ]
    });

    await wrapper.findAll(".v-chip__close")[0].trigger("click");

    const remaining = wrapper.findComponent({ name: "VCombobox" }).props("modelValue");
    expect(remaining.map(r => r.email)).toEqual(["b@example.com"]);

    const updated = wrapper.emitted("updated").at(-1)[0];
    expect(updated.map(r => r.email)).toEqual(["b@example.com"]);
  });
});
