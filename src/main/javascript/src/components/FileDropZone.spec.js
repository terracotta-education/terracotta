import { afterEach, describe, expect, it, vi } from "vitest";

vi.mock("sweetalert2", () => ({
  default: {
    fire: vi.fn().mockResolvedValue({})
  }
}));

import Swal from "sweetalert2";
import { mountComponent } from "@/test-utils/mount";
import FileDropZone from "./FileDropZone.vue";

const makeFile = (name, type, size = 1024) => {
  const file = new File(["x".repeat(size)], name, { type });
  return file;
};

describe("FileDropZone", () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it("renders the upload prompt when there is no file", () => {
    const wrapper = mountComponent(FileDropZone);

    expect(wrapper.find(".drop-zone").exists()).toBe(true);
    expect(wrapper.text()).toContain("Upload PDF");
    expect(wrapper.text()).toContain("or drag and drop here");
    expect(wrapper.find(".drop-zone__uploaded").exists()).toBe(false);
  });

  it("renders the uploaded file view when an existingFile prop is passed", () => {
    const wrapper = mountComponent(FileDropZone, {
      props: { existingFile: "existing.pdf" }
    });

    expect(wrapper.find(".drop-zone__uploaded").exists()).toBe(true);
    expect(wrapper.find(".drop-zone").exists()).toBe(false);
    expect(wrapper.text()).toContain("Selected file:");
  });

  it("toggles the drop-zone--over class while dragging", async () => {
    const wrapper = mountComponent(FileDropZone);

    const dropZone = wrapper.find(".drop-zone");

    await dropZone.trigger("dragenter");
    expect(dropZone.classes()).toContain("drop-zone--over");

    await dropZone.trigger("dragleave");
    expect(dropZone.classes()).not.toContain("drop-zone--over");
  });

  it("accepts a valid pdf dropped file and emits update/newUpload", async () => {
    const wrapper = mountComponent(FileDropZone);

    const file = makeFile("consent.pdf", "application/pdf");

    await wrapper.find(".drop-zone").trigger("drop", {
      dataTransfer: { files: [file] }
    });

    await wrapper.vm.$nextTick();
    await new Promise(resolve => setTimeout(resolve));

    expect(wrapper.emitted("update")?.[0]).toEqual([file]);
    expect(wrapper.emitted("newUpload")?.[0]).toEqual([true]);
    expect(wrapper.find(".drop-zone__uploaded").exists()).toBe(true);
  });

  it("accepts a file dropped onto the overlaid file input (real drop target) and emits update", async () => {
    // the file <input> is absolutely positioned over the whole drop zone, so a real
    // browser drop event's target is the input, not the drop-zone div - and the
    // input's own .files is an empty (but truthy) FileList, since the @drop.prevent
    // above suppresses the browser's native "set input.files from drop" behavior.
    // dataTransfer.files (checked by length, not truthiness) must still be used.
    const wrapper = mountComponent(FileDropZone);

    const file = makeFile("consent.pdf", "application/pdf");

    await wrapper.find("input[type='file']").trigger("drop", {
      dataTransfer: { files: [file] }
    });

    await wrapper.vm.$nextTick();
    await new Promise(resolve => setTimeout(resolve));

    expect(wrapper.emitted("update")?.[0]).toEqual([file]);
    expect(wrapper.emitted("newUpload")?.[0]).toEqual([true]);
    expect(wrapper.find(".drop-zone__uploaded").exists()).toBe(true);
  });

  it("rejects a non-pdf file via Swal and does not emit update", async () => {
    const wrapper = mountComponent(FileDropZone);

    const file = makeFile("notes.txt", "text/plain");

    await wrapper.find(".drop-zone").trigger("drop", {
      dataTransfer: { files: [file] }
    });

    await wrapper.vm.$nextTick();
    await new Promise(resolve => setTimeout(resolve));

    expect(Swal.fire).toHaveBeenCalledWith("Please select a pdf file.");
    expect(wrapper.emitted("update")).toBeFalsy();
  });

  it("rejects a pdf file over 10MB via Swal and does not emit update", async () => {
    const wrapper = mountComponent(FileDropZone);

    const bigFile = makeFile("big.pdf", "application/pdf", 1);
    Object.defineProperty(bigFile, "size", { value: 11 * 1024 * 1024 });

    await wrapper.find(".drop-zone").trigger("drop", {
      dataTransfer: { files: [bigFile] }
    });

    await wrapper.vm.$nextTick();
    await new Promise(resolve => setTimeout(resolve));

    expect(Swal.fire).toHaveBeenCalledWith(
      "Please check file size is not over 10 MB."
    );
    expect(wrapper.emitted("update")).toBeFalsy();
  });

  it("removes the file and emits update(null)/newUpload/displayFile(false) on remove click", async () => {
    const wrapper = mountComponent(FileDropZone, {
      props: { existingFile: "existing.pdf" }
    });

    await wrapper.find(".icon-file-remove").trigger("click");

    expect(wrapper.emitted("update")?.[0]).toEqual([null]);
    expect(wrapper.emitted("newUpload")?.[0]).toEqual([true]);
    expect(wrapper.emitted("displayFile")?.[0]).toEqual([false]);
    expect(wrapper.find(".drop-zone").exists()).toBe(true);
  });

  it("emits displayFile(true) when the view button is clicked", async () => {
    const wrapper = mountComponent(FileDropZone, {
      props: { existingFile: "existing.pdf" }
    });

    await wrapper.find(".icon-file-view").trigger("click");

    expect(wrapper.emitted("displayFile")?.[0]).toEqual([true]);
  });
});
