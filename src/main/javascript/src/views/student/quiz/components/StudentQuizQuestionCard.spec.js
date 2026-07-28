import { describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import StudentQuizQuestionCard from "./StudentQuizQuestionCard.vue";

const baseProps = {
  questionNumber: 1,
  experimentId: "1",
  assessmentId: "2",
  conditionId: "3",
  treatmentId: "4",
  submissionId: "5"
};

const stubs = {
  FileResponseEditor: {
    name: "FileResponseEditor",
    props: ["modelValue", "selectedSubmission", "fileResponses", "selectedDownloadId", "readonly", "submissionId", "questionId"],
    emits: ["update:modelValue", "download-file-response"],
    template: "<div class=\"file-editor-stub\" />"
  }
};

describe("StudentQuizQuestionCard", () => {
  it("renders the question number and point value when not readonly", () => {
    const question = { questionId: 1, questionType: "MC", html: "<p>Pick one</p>", points: 5, answers: [] };

    const wrapper = mountComponent(StudentQuizQuestionCard, {
      props: {
        ...baseProps,
        question,
        questionValues: [{ questionId: 1, answerId: null }]
      }
    });

    expect(wrapper.text()).toContain("1");
    expect(wrapper.text()).toContain("5 Points");
    expect(wrapper.html()).toContain("Pick one");
  });

  it("shows singular 'Point' for a 1-point question", () => {
    const question = { questionId: 1, questionType: "MC", html: "q", points: 1, answers: [] };

    const wrapper = mountComponent(StudentQuizQuestionCard, {
      props: {
        ...baseProps,
        question,
        questionValues: [{ questionId: 1, answerId: null }]
      }
    });

    expect(wrapper.text()).toContain("1 Point");
    expect(wrapper.text()).not.toContain("1 Points");
  });

  it("renders a MultipleChoiceResponseEditor for MC questions and mutates the matching questionValues entry on selection", async () => {
    const question = {
      questionId: 1,
      questionType: "MC",
      html: "Pick one",
      points: 2,
      answers: [
        { answerId: 10, html: "A" },
        { answerId: 11, html: "B" }
      ]
    };
    const questionValues = [{ questionId: 1, answerId: null, response: null }];

    const wrapper = mountComponent(StudentQuizQuestionCard, {
      props: { ...baseProps, question, questionValues }
    });

    const editor = wrapper.findComponent({ name: "MultipleChoiceResponseEditor" });
    expect(editor.exists()).toBe(true);
    expect(editor.props("answers")).toEqual(question.answers);

    const radioInputs = wrapper.findAll("input[type=\"radio\"]");
    await radioInputs[1].setValue(true);

    expect(questionValues[0].answerId).toBe(11);
  });

  it("renders an EssayResponseEditor for ESSAY questions and mutates the matching questionValues entry on input", async () => {
    const question = { questionId: 2, questionType: "ESSAY", html: "Explain", points: 3 };
    const questionValues = [{ questionId: 2, answerId: null, response: null }];

    const wrapper = mountComponent(StudentQuizQuestionCard, {
      props: { ...baseProps, question, questionValues }
    });

    const editor = wrapper.findComponent({ name: "EssayResponseEditor" });
    expect(editor.exists()).toBe(true);

    await wrapper.find("textarea").setValue("My answer");

    expect(questionValues[0].response).toBe("My answer");
  });

  it("renders a FileResponseEditor (stubbed) for FILE questions and forwards download-file-response upward", async () => {
    const question = { questionId: 3, questionType: "FILE", html: "Upload", points: 4 };
    const questionValues = [{ questionId: 3, answerId: null, response: null }];

    const wrapper = mountComponent(StudentQuizQuestionCard, {
      props: {
        ...baseProps,
        question,
        questionValues,
        selectedDownloadId: 99
      },
      global: { stubs }
    });

    const editor = wrapper.findComponent({ name: "FileResponseEditor" });
    expect(editor.exists()).toBe(true);
    expect(editor.props("selectedDownloadId")).toBe(99);
    expect(editor.props("submissionId")).toBe(baseProps.submissionId);
    expect(editor.props("questionId")).toBe(3);

    const payload = { answerSubmissionId: 7 };
    await editor.vm.$emit("download-file-response", payload);

    expect(wrapper.emitted("download-file-response")).toBeTruthy();
    expect(wrapper.emitted("download-file-response").at(-1)).toEqual([payload]);
  });

  it("shows the earned score out of total points when readonly", () => {
    const question = { questionId: 1, questionType: "MC", html: "q", points: 10, answers: [] };
    const questionSubmissions = [
      { questionId: 1, questionSubmissionId: 100, alteredGrade: 7 }
    ];

    const wrapper = mountComponent(StudentQuizQuestionCard, {
      props: {
        ...baseProps,
        question,
        readonly: true,
        questionValues: [{ questionId: 1, answerId: 10 }],
        questionSubmissions
      }
    });

    expect(wrapper.text()).toContain("7 /");
    expect(wrapper.text()).toContain("10 Points");
  });

  it("falls back to calculatedPoints and then 0 when there is no altered grade or submission", () => {
    const question = { questionId: 1, questionType: "MC", html: "q", points: 10, answers: [] };

    const withCalculated = mountComponent(StudentQuizQuestionCard, {
      props: {
        ...baseProps,
        question,
        readonly: true,
        questionValues: [{ questionId: 1, answerId: 10 }],
        questionSubmissions: [{ questionId: 1, questionSubmissionId: 100, calculatedPoints: 4 }]
      }
    });
    expect(withCalculated.text()).toContain("4 /");

    const withNoSubmission = mountComponent(StudentQuizQuestionCard, {
      props: {
        ...baseProps,
        question,
        readonly: true,
        questionValues: [{ questionId: 1, answerId: 10 }],
        questionSubmissions: []
      }
    });
    expect(withNoSubmission.text()).toContain("0 /");
  });

  it("maps answers with a studentResponse marker from the question submission when readonly", () => {
    const question = {
      questionId: 1,
      questionType: "MC",
      html: "q",
      points: 5,
      answers: [
        { answerId: 10, html: "A" },
        { answerId: 11, html: "B" }
      ]
    };
    const questionSubmissions = [
      {
        questionId: 1,
        questionSubmissionId: 100,
        answerDtoList: [
          { answerId: 10, html: "A", correct: true },
          { answerId: 11, html: "B", correct: false }
        ],
        answerSubmissionDtoList: [{ answerId: 11, questionSubmissionId: 100 }]
      }
    ];

    const wrapper = mountComponent(StudentQuizQuestionCard, {
      props: {
        ...baseProps,
        question,
        readonly: true,
        questionValues: [{ questionId: 1, answerId: null }],
        questionSubmissions
      }
    });

    const editor = wrapper.findComponent({ name: "MultipleChoiceResponseEditor" });
    const answers = editor.props("answers");

    expect(answers.find(a => a.answerId === 11).studentResponse).toBe(11);
    expect(answers.find(a => a.answerId === 10).studentResponse).toBe(false);
  });

  it("passes the essay submission response to EssayResponseEditor's answer prop when readonly", () => {
    const question = { questionId: 2, questionType: "ESSAY", html: "Explain", points: 3 };
    const questionSubmissions = [
      {
        questionId: 2,
        questionSubmissionId: 200,
        answerSubmissionDtoList: [{ questionSubmissionId: 200, response: "Existing answer" }]
      }
    ];

    const wrapper = mountComponent(StudentQuizQuestionCard, {
      props: {
        ...baseProps,
        question,
        readonly: true,
        questionValues: [{ questionId: 2, answerId: null, response: null }],
        questionSubmissions
      }
    });

    const editor = wrapper.findComponent({ name: "EssayResponseEditor" });
    expect(editor.props("answer")).toEqual({ questionSubmissionId: 200, response: "Existing answer" });
  });
});
