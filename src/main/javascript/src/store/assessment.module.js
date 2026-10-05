import { defineStore } from "pinia";

import { assessmentService } from "@/services";

// Shared by every action below that writes this.assessment (fetchAssessment,
// fetchAssessmentForSubmission, createAssessment) so a stale response from any of
// them can't clobber a newer one. Navigating quickly between two instructor grading
// pages for the same assessment (e.g. Back, then straight into another student)
// fires a new fetch before the previous one has resolved; without this, whichever
// response happens to land last wins, even if it's the older request -
// intermittently leaving the just-mounted page showing the wrong (or no)
// submissions, or a slow fetch clobbering an assessment createAssessment just made.
let assessmentRequestId = 0;

// handleResponse returns [] for a 204, the raw Response or { data, status } for any
// other 2xx, and { data, status, error } for a failure - all truthy, so callers have
// to look at the status to tell success from failure
function isSuccessfulResponse(response) {
  if (Array.isArray(response)) {
    return true;
  }

  return response?.status >= 200 && response?.status < 300;
}

function buildQuestionFromPayload(payload) {
  const [
    ,
    ,
    ,
    ,
    questionId,
    html,
    points,
    questionOrder,
    questionType,
    randomizeAnswers,
    answers,
    integration
  ] = payload;

  return {
    questionId,
    html,
    points,
    questionOrder,
    questionType,
    randomizeAnswers,
    answers,
    integration
  };
}

export const assessment = defineStore("assessment", {
  state: () => ({
    assessment: null,
    assessments: []
  }),

  getters: {
    questions: state =>
      [...(state?.assessment?.questions || [])].toSorted(
        (a, b) => (a?.questionOrder || 0) - (b?.questionOrder || 0)
      ),

    answerableQuestions() {
      return this.questions.filter(
        question => question.questionType !== "PAGE_BREAK"
      );
    },

    questionPages(state) {
      if (!state.assessment?.questions?.length) {
        return [];
      }

      const pages = [{
        key: 0,
        pageBreakAfter: false,
        questions: [],
        questionStartIndex: 0
      }];

      const sorted = this.questions;
      const lastQuestionId = sorted.at(-1)?.questionId;

      for (const question of sorted) {
        const currentPage = pages[pages.length - 1];

        if (question.questionType === "PAGE_BREAK") {
          currentPage.pageBreakAfter = true;

          if (question.questionId !== lastQuestionId) {
            pages.push({
              key: pages.length,
              pageBreakAfter: false,
              questions: [],
              questionStartIndex:
                currentPage.questionStartIndex +
                currentPage.questions.length
            });
          }
        } else {
          currentPage.questions.push(question);
        }
      }

      return pages;
    }
  },

  actions: {
    setAssessment(assessmentData) {
      this.assessment = assessmentData;
    },

    updateQuestions(question) {
      this.upsertQuestion(question);
    },

    updateAnswers(answer) {
      this.upsertAnswer(answer);
    },

    async fetchAssessment(payload) {
      const requestId = ++assessmentRequestId;

      try {
        const response =
          await assessmentService.fetchAssessment(...payload);

        if (requestId !== assessmentRequestId) {
          return this.assessment;
        }

        this.assessment = response?.data ?? null;

        return this.assessment;
      } catch (error) {
        console.error(
          "assessment/fetchAssessment | catch",
          error
        );

        return null;
      }
    },

    async fetchAssessmentForSubmission(payload) {
      const requestId = ++assessmentRequestId;

      try {
        const response =
          await assessmentService.fetchAssessmentForSubmission(
            ...payload
          );

        if (requestId !== assessmentRequestId) {
          return this.assessment;
        }

        this.assessment = response?.data ?? null;

        return this.assessment;
      } catch (error) {
        console.error(
          "assessment/fetchAssessmentForSubmission | catch",
          error
        );

        return null;
      }
    },

    async createAssessment(payload) {
      const requestId = ++assessmentRequestId;

      try {
        let response =
          await assessmentService.fetchAssessments(...payload);

        let assessmentData = null;

        if (response?.data?.length > 0) {
          assessmentData = response.data[0];
        } else {
          response =
            await assessmentService.createAssessment(...payload);

          if (response?.status !== 201) {
            return response;
          }

          assessmentData = response?.data;
        }

        if (requestId === assessmentRequestId) {
          this.assessment = assessmentData;
        }

        this.upsertAssessment(assessmentData);

        return {
          status: response?.status,
          data: assessmentData
        };
      } catch (error) {
        console.error(
          "assessment/createAssessment | catch",
          error
        );

        return null;
      }
    },

    async regradeQuestions(payload) {
      try {
        return await assessmentService.regradeQuestions(...payload);
      } catch (error) {
        console.error(
          "assessment/regradeQuestions | catch",
          error
        );

        return null;
      }
    },

    async updateAssessment(payload) {
      try {
        const response =
          await assessmentService.updateAssessment(...payload);

        return response
          ? {
              status: response.status,
              data: response.data
            }
          : null;
      } catch (error) {
        console.error(
          "assessment/updateAssessment | catch",
          error
        );

        return null;
      }
    },

    async createQuestion(payload) {
      return this.createQuestionAtIndex({ payload });
    },

    async createQuestionAtIndex({ payload, questionIndex = -1 }) {
      try {
        const response =
          await assessmentService.createQuestion(...payload);

        const question = response?.data;

        if (question?.questionId) {
          if (questionIndex >= 0) {
            this.assessment.questions.splice(
              questionIndex,
              0,
              question
            );
          } else {
            this.upsertQuestion(question);
          }

          return {
            status: response?.status,
            data: question
          };
        }

        return null;
      } catch (error) {
        console.error(
          "assessment/createQuestion | catch",
          error
        );

        return null;
      }
    },

    async updateQuestion(payload) {
      try {
        const response =
          await assessmentService.updateQuestion(...payload);

        if (response) {
          this.upsertQuestion(buildQuestionFromPayload(payload));

          return {
            status: response?.status,
            data: null
          };
        }

        return null;
      } catch (error) {
        console.error(
          "assessment/updateQuestion | catch",
          error
        );

        return null;
      }
    },

    async updateQuestionsBatch(payload) {
      try {
        const [, , , , questionList] = payload;

        const response =
          await assessmentService.updateQuestions(...payload);

        // handleResponse hands back an { status, error } object (truthy) for a failed
        // request, so a plain truthiness check here reported failures as success
        if (isSuccessfulResponse(response)) {
          questionList.forEach(question => this.upsertQuestion(question));

          return {
            status: response?.status,
            data: null
          };
        }

        console.error("assessment/updateQuestionsBatch | failed", response);

        return null;
      } catch (error) {
        console.error(
          "assessment/updateQuestionsBatch | catch",
          error
        );

        return null;
      }
    },

    async deleteQuestion(payload) {
      try {
        const questionId = payload[4];

        const response =
          await assessmentService.deleteQuestion(...payload);

        if (response?.status === 200) {
          this.assessment = {
            ...this.assessment,
            questions:
              this.assessment.questions?.filter(
                q => q.questionId !== questionId
              ) || []
          };

          return {
            status: response.status,
            data: this.assessment?.questions
          };
        }

        return null;
      } catch (error) {
        console.error(
          "assessment/deleteQuestion | catch",
          error
        );

        return null;
      }
    },

    async deleteQuestions(payload) {
      try {
        const response =
          await assessmentService.deleteQuestions(...payload);

        if (response?.status === 200) {
          this.assessment = {
            ...this.assessment,
            questions: []
          };

          return {
            status: response.status,
            data: null
          };
        }

        return null;
      } catch (error) {
        console.error(
          "assessment/deleteQuestions | catch",
          error
        );

        return null;
      }
    },

    async createAnswer(payload) {
      try {
        const response =
          await assessmentService.createAnswer(...payload);

        const answer = response?.data;

        if (answer?.answerId) {
          this.upsertAnswer(answer);

          return {
            status: response?.status,
            data: answer
          };
        }

        return null;
      } catch (error) {
        console.error(
          "assessment/createAnswer | catch",
          error
        );

        return null;
      }
    },

    async updateAnswer(payload) {
      try {
        const response =
          await assessmentService.updateAnswer(...payload);

        return response
          ? {
              status: response?.status,
              data: null
            }
          : null;
      } catch (error) {
        console.error(
          "assessment/updateAnswer | catch",
          error
        );

        return null;
      }
    },

    async updateAnswersBatch(payload) {
      try {
        const [, , , , , answerList] = payload;

        const response =
          await assessmentService.updateAnswers(...payload);

        if (isSuccessfulResponse(response)) {
          answerList.forEach(answer => this.upsertAnswer(answer));

          return {
            status: response?.status,
            data: null
          };
        }

        console.error("assessment/updateAnswersBatch | failed", response);

        return null;
      } catch (error) {
        console.error(
          "assessment/updateAnswersBatch | catch",
          error
        );

        return null;
      }
    },

    async deleteAnswer(payload) {
      try {
        const answerId = payload[5];

        const response =
          await assessmentService.deleteAnswer(...payload);

        if (response?.status === 200) {
          this.assessment.questions =
            this.assessment.questions.map(question => ({
              ...question,
              answers:
                question.answers?.filter(
                  answer =>
                    answer.answerId !== answerId
                ) || []
            }));

          return {
            status: response.status,
            data: null
          };
        }

        return null;
      } catch (error) {
        console.error(
          "assessment/deleteAnswer | catch",
          error
        );

        return null;
      }
    },

    resetAssessments() {
      this.assessment = null;
      this.assessments = [];
    },

    upsertAssessment(assessmentData) {
      if (!assessmentData) {
        return;
      }

      const index = this.assessments.findIndex(
        item =>
          item.assessmentId === assessmentData.assessmentId
      );

      if (index >= 0) {
        this.assessments.splice(index, 1, assessmentData);
      } else {
        this.assessments.push(assessmentData);
      }
    },

    upsertQuestion(question) {
      const index = this.assessment?.questions?.findIndex(
        item =>
          item.questionId === question.questionId
      );

      if (index >= 0) {
        this.assessment.questions.splice(index, 1, question);
      } else {
        this.assessment.questions.push(question);
      }
    },

    upsertAnswer(answer) {
      const answerQuestionId = answer.questionId;
      const question = this.assessment.questions.find(
        q => q.questionId === answerQuestionId
      );

      if (!question) return;

      if (!question.answers) question.answers = [];

      const index = question.answers.findIndex(
        item => item.answerId === answer.answerId
      );

      if (index >= 0) {
        question.answers.splice(index, 1, answer);
      } else {
        question.answers.push(answer);
      }
    }
  }
});
