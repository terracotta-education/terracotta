import { describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import FileUploadQuestionEditor from "./FileUploadQuestionEditor.vue";

const QuestionEditorStub = {
  name: "QuestionEditor",
  props: ["question", "isMC"],
  template: '<div class="question-editor-stub"><slot /></div>'
};

describe("FileUploadQuestionEditor", () => {
  it("passes the question prop through to QuestionEditor", () => {
    const question = { questionId: 5, html: "<p>Upload a file</p>" };

    const wrapper = mountComponent(FileUploadQuestionEditor, {
      props: { question },
      global: {
        stubs: { QuestionEditor: QuestionEditorStub }
      }
    });

    const questionEditor = wrapper.findComponent(QuestionEditorStub);
    expect(questionEditor.exists()).toBe(true);
    expect(questionEditor.props("question")).toEqual(question);
  });

  it("renders the 10MB file size note inside QuestionEditor's default slot", () => {
    const wrapper = mountComponent(FileUploadQuestionEditor, {
      props: { question: { questionId: 5 } },
      global: {
        stubs: { QuestionEditor: QuestionEditorStub }
      }
    });

    expect(wrapper.text()).toContain("Note: Files must be smaller than 10MB");
  });
});
