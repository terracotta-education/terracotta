import { afterEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";
import { flushPromises } from "@vue/test-utils";

import { mountComponent } from "@/test-utils/mount";
import { message as useMessageStore } from "@/store/messaging/message.module";
import { alert as useAlertStore } from "@/store/alert.module";
import PipedTextFileUploader from "./PipedTextFileUploader.vue";

const baseProps = {
  experimentId: 1,
  exposureId: "exposure-1",
  containerId: "container-1",
  messageId: "message-1",
  contentId: "content-1"
};

const makeFile = (name, type) => new File(["a,b,c"], name, { type });

const setupStores = () => {
  const pinia = createPinia();
  setActivePinia(pinia);

  return pinia;
};

describe("PipedTextFileUploader", () => {
  let wrapper;

  afterEach(() => {
    wrapper?.unmount();
  });

  it("renders the upload prompt when there is no selected file", () => {
    const pinia = setupStores();

    wrapper = mountComponent(PipedTextFileUploader, { props: baseProps, pinia });

    expect(wrapper.find(".drop-zone").exists()).toBe(true);
    expect(wrapper.text()).toContain("Select CSV");
    expect(wrapper.find(".drop-zone__uploaded").exists()).toBe(false);
  });

  it("emits close when the close icon is clicked", async () => {
    const pinia = setupStores();

    wrapper = mountComponent(PipedTextFileUploader, { props: baseProps, pinia });

    await wrapper.findComponent({ name: "VBtn" }).trigger("click");

    expect(wrapper.emitted("close")).toBeTruthy();
  });

  it("toggles the drop-zone--over class while dragging", async () => {
    const pinia = setupStores();

    wrapper = mountComponent(PipedTextFileUploader, { props: baseProps, pinia });

    const dropZone = wrapper.find(".drop-zone");

    await dropZone.trigger("dragenter");
    expect(dropZone.classes()).toContain("drop-zone--over");

    await dropZone.trigger("dragleave");
    expect(dropZone.classes()).not.toContain("drop-zone--over");
  });

  it("accepts a dropped CSV file and shows the selected file view", async () => {
    const pinia = setupStores();

    wrapper = mountComponent(PipedTextFileUploader, { props: baseProps, pinia });

    const file = makeFile("tags.csv", "text/csv");

    await wrapper.find(".drop-zone").trigger("drop", {
      dataTransfer: { files: [file] }
    });

    expect(wrapper.find(".drop-zone__uploaded").exists()).toBe(true);
    expect(wrapper.text()).toContain("tags.csv");
  });

  it("accepts a file dropped onto the overlaid file input (real drop target) and shows the selected file view", async () => {
    // the file <input> is absolutely positioned over the whole drop zone, so a real
    // browser drop event's target is the input, not the drop-zone div - and the
    // input's own .files is an empty (but truthy) FileList, since the @drop.prevent
    // above suppresses the browser's native "set input.files from drop" behavior.
    // dataTransfer.files (checked by length, not truthiness) must still be used.
    const pinia = setupStores();

    wrapper = mountComponent(PipedTextFileUploader, { props: baseProps, pinia });

    const file = makeFile("tags.csv", "text/csv");

    await wrapper.find("input[type='file']").trigger("drop", {
      dataTransfer: { files: [file] }
    });

    expect(wrapper.find(".drop-zone__uploaded").exists()).toBe(true);
    expect(wrapper.text()).toContain("tags.csv");
  });

  it("rejects a non-CSV file with an error status alert and does not show the selected file view", async () => {
    const pinia = setupStores();

    wrapper = mountComponent(PipedTextFileUploader, { props: baseProps, pinia });

    const file = makeFile("notes.txt", "text/plain");

    await wrapper.find(".drop-zone").trigger("drop", {
      dataTransfer: { files: [file] }
    });

    expect(wrapper.find(".drop-zone__uploaded").exists()).toBe(false);
    expect(useAlertStore().alertType).toBe("error");
    expect(useAlertStore().alertMessage).toBe("Please select a CSV file.");
  });

  it("removes the selected file and shows a success status alert", async () => {
    const pinia = setupStores();

    wrapper = mountComponent(PipedTextFileUploader, { props: baseProps, pinia });

    const file = makeFile("tags.csv", "text/csv");

    await wrapper.find(".drop-zone").trigger("drop", {
      dataTransfer: { files: [file] }
    });
    await wrapper.find(".icon-file-remove").trigger("click");

    expect(wrapper.find(".drop-zone__uploaded").exists()).toBe(false);
    expect(useAlertStore().alertType).toBe("success");
    expect(useAlertStore().alertMessage).toBe("Piped text file removed");
  });

  it("disables the Select CSV button and file input when read-only", () => {
    const pinia = setupStores();

    wrapper = mountComponent(PipedTextFileUploader, {
      props: { ...baseProps, readOnly: true },
      pinia
    });

    const selectBtn = wrapper
      .findAllComponents({ name: "VBtn" })
      .find(b => b.text() === "Select CSV");

    expect(selectBtn.props("disabled")).toBe(true);
    expect(wrapper.find("input[type='file']").attributes("disabled")).toBeDefined();
  });

  it("does not toggle drop-zone--over or accept a dropped file when read-only", async () => {
    const pinia = setupStores();

    wrapper = mountComponent(PipedTextFileUploader, {
      props: { ...baseProps, readOnly: true },
      pinia
    });

    const dropZone = wrapper.find(".drop-zone");

    await dropZone.trigger("dragenter");
    expect(dropZone.classes()).not.toContain("drop-zone--over");

    const file = makeFile("tags.csv", "text/csv");

    await dropZone.trigger("drop", { dataTransfer: { files: [file] } });

    expect(wrapper.find(".drop-zone__uploaded").exists()).toBe(false);
  });

  it("disables the remove and upload buttons for an already-selected file once read-only", async () => {
    const pinia = setupStores();

    wrapper = mountComponent(PipedTextFileUploader, { props: baseProps, pinia });

    const file = makeFile("tags.csv", "text/csv");

    await wrapper.find(".drop-zone").trigger("drop", {
      dataTransfer: { files: [file] }
    });

    await wrapper.setProps({ readOnly: true });

    const removeBtn = wrapper.find(".icon-file-remove");
    const uploadBtn = wrapper
      .findAllComponents({ name: "VBtn" })
      .find(b => b.text() === "Upload CSV");

    expect(removeBtn.attributes("disabled")).toBeDefined();
    expect(uploadBtn.props("disabled")).toBe(true);
  });

  it("uploads the selected file with the correct payload and shows a success alert", async () => {
    const pinia = setupStores();
    useMessageStore().uploadPipedText = vi.fn().mockResolvedValue({
      content: { pipedText: { items: [] } }
    });

    wrapper = mountComponent(PipedTextFileUploader, { props: baseProps, pinia });

    const file = makeFile("tags.csv", "text/csv");

    await wrapper.find(".drop-zone").trigger("drop", {
      dataTransfer: { files: [file] }
    });

    const uploadButtons = wrapper
      .findAllComponents({ name: "VBtn" })
      .filter(b => b.text() === "Upload CSV");
    await uploadButtons[0].trigger("click");
    await flushPromises();

    expect(useMessageStore().uploadPipedText).toHaveBeenCalledWith([
      baseProps.experimentId,
      baseProps.exposureId,
      baseProps.containerId,
      baseProps.messageId,
      baseProps.contentId,
      file
    ]);
    expect(useAlertStore().alertType).toBe("success");
    expect(useAlertStore().alertMessage).toBe(
      "Piped text file uploaded successfully"
    );
    expect(wrapper.text()).not.toContain("Uploading...");
  });
});
