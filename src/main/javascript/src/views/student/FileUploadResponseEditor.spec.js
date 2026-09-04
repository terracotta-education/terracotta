import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import FileUploadResponseEditor from "./FileUploadResponseEditor.vue";
import { submission as submissionModule } from "@/store/submission.module";

const makeFile = (name, size = 1024, type = "text/plain") => {
  const file = new File(["x".repeat(Math.min(size, 10))], name, { type });
  Object.defineProperty(file, "size", { value: size });
  return file;
};

const baseProps = () => ({
  submissionId: 1,
  questionId: 2
});

describe("FileUploadResponseEditor", () => {
  beforeEach(() => {
    vi.spyOn(window, "alert").mockImplementation(() => {});
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("renders the upload prompt and drag-and-drop area when idle", () => {
    const wrapper = mountComponent(FileUploadResponseEditor, {
      props: baseProps()
    });

    expect(wrapper.text()).toContain("Upload File");
    expect(wrapper.text()).toContain("or drag and drop here");
    expect(wrapper.text()).toContain("Uploaded files cannot be larger than 10MB");
  });

  it("toggles the dragover highlight class on drag events", async () => {
    const wrapper = mountComponent(FileUploadResponseEditor, {
      props: baseProps()
    });

    // index 0 is ResponseRow's own wrapping VCard; index 1 is the
    // drop-zone VCard that actually owns the drag handlers
    const dropCard = wrapper.findAllComponents({ name: "VCard" })[1];
    await dropCard.trigger("dragenter");

    expect(dropCard.classes()).toContain("bg-grey-lighten-3");

    await dropCard.trigger("dragleave");

    expect(dropCard.classes()).not.toContain("bg-grey-lighten-3");
  });

  it("accepts a file selected via the file input, adds it to the store, and emits update:modelValue", async () => {
    const wrapper = mountComponent(FileUploadResponseEditor, {
      props: baseProps()
    });

    const submissionStore = submissionModule();
    const file = makeFile("essay.pdf", 1024);

    const input = wrapper.find('input[type="file"]');
    Object.defineProperty(input.element, "files", { value: [file] });
    await input.trigger("change");

    expect(submissionStore.files).toContainEqual(
      expect.objectContaining({
        file,
        name: "essay.pdf",
        questionId: 2,
        submissionId: 1
      })
    );

    const emitted = wrapper.emitted("update:modelValue");
    expect(emitted).toBeTruthy();
    expect(emitted.at(-1)[0]).toBe(file);
    expect(wrapper.text()).toContain("essay.pdf");
  });

  it("rejects a file over 10MB, clears it, and does not emit update:modelValue", async () => {
    const wrapper = mountComponent(FileUploadResponseEditor, {
      props: baseProps()
    });

    const bigFile = makeFile("big.pdf", 11 * 1024 * 1024);

    const input = wrapper.find('input[type="file"]');
    Object.defineProperty(input.element, "files", { value: [bigFile] });
    await input.trigger("change");

    expect(window.alert).toHaveBeenCalledWith("File cannot exceed 10MB");
    expect(wrapper.emitted("update:modelValue")).toBeFalsy();
    expect(wrapper.text()).toContain("Upload File");
  });

  it("accepts a single file dropped onto the drop zone", async () => {
    const wrapper = mountComponent(FileUploadResponseEditor, {
      props: baseProps()
    });

    const file = makeFile("dropped.pdf", 2048);
    const dropCard = wrapper.findAllComponents({ name: "VCard" })[1];

    await dropCard.trigger("drop", {
      dataTransfer: { files: [file] }
    });

    expect(wrapper.emitted("update:modelValue")?.at(-1)[0]).toBe(file);
    expect(wrapper.text()).toContain("dropped.pdf");
  });

  it("alerts and does not upload when more than one file is dropped", async () => {
    const wrapper = mountComponent(FileUploadResponseEditor, {
      props: baseProps()
    });

    const dropCard = wrapper.findAllComponents({ name: "VCard" })[1];

    await dropCard.trigger("drop", {
      dataTransfer: {
        files: [makeFile("a.pdf"), makeFile("b.pdf")]
      }
    });

    expect(window.alert).toHaveBeenCalledWith(
      "Only one file may be uploaded at a time."
    );
    expect(wrapper.emitted("update:modelValue")).toBeFalsy();
  });

  it("deletes the uploaded file and emits update:modelValue(null)", async () => {
    const wrapper = mountComponent(FileUploadResponseEditor, {
      props: baseProps()
    });

    const file = makeFile("essay.pdf", 1024);
    const input = wrapper.find('input[type="file"]');
    Object.defineProperty(input.element, "files", { value: [file] });
    await input.trigger("change");

    const deleteTooltip = wrapper.findComponent({ name: "ToolTip" });
    await deleteTooltip.vm.$emit("clicked");

    const submissionStore = submissionModule();
    expect(submissionStore.files).toHaveLength(0);
    expect(wrapper.emitted("update:modelValue").at(-1)[0]).toBeNull();
    expect(wrapper.text()).toContain("Upload File");
  });

  it("renders the readonly file-submitted view listing each file response", () => {
    const fileResponses = [
      { answerSubmissionId: 1, fileName: "submitted.pdf" }
    ];

    const wrapper = mountComponent(FileUploadResponseEditor, {
      props: {
        ...baseProps(),
        readonly: true,
        fileResponses
      }
    });

    expect(wrapper.text()).toContain("File submitted:");
    expect(wrapper.text()).toContain("submitted.pdf");
    expect(wrapper.find(".upload-button").exists()).toBe(false);
  });

  it("emits download-file-response with the submission details when download is clicked", async () => {
    const fileResponses = [
      {
        answerSubmissionId: 1,
        fileName: "submitted.pdf",
        questionSubmissionId: 5,
        mimeType: "application/pdf"
      }
    ];

    const selectedSubmission = {
      conditionId: 10,
      treatmentId: 20,
      assessmentId: 30,
      submissionId: 1
    };

    const wrapper = mountComponent(FileUploadResponseEditor, {
      props: {
        ...baseProps(),
        readonly: true,
        fileResponses,
        selectedSubmission
      }
    });

    const downloadTooltip = wrapper.findComponent({ name: "ToolTip" });
    await downloadTooltip.vm.$emit("clicked");

    expect(wrapper.emitted("download-file-response")?.[0]).toEqual([
      {
        conditionId: 10,
        treatmentId: 20,
        assessmentId: 30,
        submissionId: 1,
        questionSubmissionId: 5,
        answerSubmissionId: 1,
        mimeType: "application/pdf",
        fileName: "submitted.pdf"
      }
    ]);
  });

  it("shows a spinner instead of the download tooltip while that file is downloading", () => {
    const fileResponses = [
      { answerSubmissionId: 1, fileName: "submitted.pdf" }
    ];

    const wrapper = mountComponent(FileUploadResponseEditor, {
      props: {
        ...baseProps(),
        readonly: true,
        fileResponses,
        selectedDownloadId: 1
      }
    });

    expect(wrapper.findComponent({ name: "ToolTip" }).exists()).toBe(false);
    // Spinner.vue's internal component name is "LoadingSpinner"
    expect(wrapper.findComponent({ name: "LoadingSpinner" }).exists()).toBe(true);
  });
});
