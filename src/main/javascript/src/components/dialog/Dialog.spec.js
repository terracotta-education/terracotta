import { describe, expect, it, vi } from "vitest";
import { flushPromises } from "@vue/test-utils";

import Swal from "sweetalert2";

import { mountComponent } from "@/test-utils/mount";
import Dialog from "./Dialog.vue";

vi.mock("sweetalert2", () => ({
  default: {
    fire: vi.fn(),
    isLoading: vi.fn(() => false)
  }
}));

describe("Dialog (ConfirmationDialog)", () => {
  it("calls Swal.fire with the provided props on mount", () => {
    Swal.fire.mockResolvedValue({ isConfirmed: true });

    mountComponent(Dialog, {
      props: {
        title: "Are you sure?",
        body: "This cannot be undone.",
        showCancelButton: true,
        confirmButtonText: "Yes",
        cancelButtonText: "No",
        reverseButtons: true
      }
    });

    expect(Swal.fire).toHaveBeenCalledWith(
      expect.objectContaining({
        title: "Are you sure?",
        html: "This cannot be undone.",
        showCancelButton: true,
        confirmButtonText: "Yes",
        cancelButtonText: "No",
        reverseButtons: true
      })
    );
  });

  it("emits confirmed with true when the user confirms", async () => {
    Swal.fire.mockResolvedValue({ isConfirmed: true });

    const wrapper = mountComponent(Dialog, {
      props: {
        title: "Are you sure?"
      }
    });

    await flushPromises();

    expect(wrapper.emitted("confirmed")).toEqual([[true]]);
  });

  it("emits confirmed with false when the user cancels", async () => {
    Swal.fire.mockResolvedValue({ isConfirmed: false });

    const wrapper = mountComponent(Dialog, {
      props: {
        title: "Are you sure?"
      }
    });

    await flushPromises();

    expect(wrapper.emitted("confirmed")).toEqual([[false]]);
  });

  it("applies default prop values when none are passed", () => {
    Swal.fire.mockResolvedValue({ isConfirmed: true });

    mountComponent(Dialog);

    expect(Swal.fire).toHaveBeenCalledWith(
      expect.objectContaining({
        title: "",
        html: "",
        showCancelButton: false,
        confirmButtonText: "OK",
        cancelButtonText: "CANCEL",
        reverseButtons: false
      })
    );
  });

  it("exposes doDisplay so it can be re-triggered imperatively", async () => {
    Swal.fire.mockResolvedValue({ isConfirmed: true });

    const wrapper = mountComponent(Dialog, {
      props: {
        title: "Are you sure?"
      }
    });

    await flushPromises();

    expect(typeof wrapper.vm.doDisplay).toBe("function");

    Swal.fire.mockClear();
    await wrapper.vm.doDisplay();

    expect(Swal.fire).toHaveBeenCalledTimes(1);
  });
});
