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
