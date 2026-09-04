import { defineStore } from "pinia";

import { submissionService } from "@/services";

export const submission = defineStore("submission", {
  state: () => ({
    submissions: [],
    submission: null,
    studentResponse: null,
    questionSubmissions: [],
    files: []
  }),

  getters: {
    hasSubmissions: state => state.submissions.length > 0,
    hasFiles: state => state.files.length > 0
  },

  actions: {
    async fetchSubmissions(payload) {
      try {
        const { data } = await submissionService.getAll(...payload);

        this.submissions = data || [];

        return data || [];
      } catch (error) {
        console.error(
          "submissions/fetchSubmissions | catch",
          error
        );

        return [];
      }
    },

    async fetchSubmission(payload) {
      try {
        const { data } =
          await submissionService.getSubmission(...payload);

        this.submission = data;

        return data;
      } catch (error) {
        console.error(
          "submissions/fetchSubmission | catch",
          error
        );

        return null;
      }
    },

    async updateSubmission(payload) {
      try {
        return await submissionService.updateSubmission(...payload);
      } catch (error) {
        console.error(
          "submissions/updateSubmission | catch",
          {
            error,
            state: this.$state
          }
        );

        return null;
      }
    },

    async updateSubmissions(payload) {
      try {
        return await submissionService.updateSubmissions(...payload);
      } catch (error) {
        console.error(
          "submissions/updateSubmissions | catch",
          {
            error,
            state: this.$state
          }
        );

        return null;
      }
    },

    async fetchStudentResponse(payload) {
      try {
        const { data } =
          await submissionService.studentResponse(...payload);

        this.studentResponse = data;

        return data;
      } catch (error) {
        console.error(
          "submissions/fetchStudentResponse | catch",
          error
        );

        return null;
      }
    },

    async fetchQuestionSubmissions(payload) {
      try {
        const { data } =
          await submissionService.getQuestionSubmissions(...payload);

        this.questionSubmissions = data || [];

        return data || [];
      } catch (error) {
        console.error(
          "submissions/fetchQuestionSubmissions | catch",
          error
        );

        return [];
      }
    },

    async createQuestionSubmissions(payload) {
      try {
        return await submissionService.createQuestionSubmissions(
          ...payload
        );
      } catch (error) {
        console.error(
          "submissions/createQuestionSubmissions | catch",
          {
            error,
            state: this.$state
          }
        );

        return null;
      }
    },

    async updateQuestionSubmissions(payload) {
      try {
        return await submissionService.updateQuestionSubmissions(
          ...payload
        );
      } catch (error) {
        console.error(
          "submissions/updateQuestionSubmissions | catch",
          {
            error,
            state: this.$state
          }
        );

        return null;
      }
    },

    async createAnswerSubmissions(payload) {
      try {
        return await submissionService.createAnswerSubmissions(
          ...payload
        );
      } catch (error) {
        console.error(
          "submissions/createAnswerSubmissions | catch",
          {
            error,
            state: this.$state
          }
        );

        return null;
      }
    },

    async updateAnswerSubmission(payload) {
      try {
        return await submissionService.updateAnswerSubmission(
          ...payload
        );
      } catch (error) {
        console.error(
          "submissions/updateAnswerSubmission | catch",
          {
            error,
            state: this.$state
          }
        );

        return null;
      }
    },

    clearQuestionSubmissions() {
      this.questionSubmissions = [];

      return Promise.resolve([]);
    },

    async downloadAnswerFileSubmission(payload) {
      try {
        return await submissionService.downloadAnswerFileSubmission(
          ...payload
        );
      } catch (error) {
        console.error(
          "submissions/downloadAnswerFileSubmission | catch",
          {
            error,
            state: this.$state
          }
        );

        return null;
      }
    },

    addFile({ file, name, questionId, submissionId }) {
      const item = { file, name, questionId, submissionId };

      const index = this.files.findIndex(
        f =>
          f.questionId === questionId &&
          f.submissionId === submissionId
      );

      if (index >= 0) {
        this.files.splice(index, 1, item);
      } else {
        this.files.push(item);
      }
    },

    clearFile({ questionId, submissionId }) {
      this.files = this.files.filter(
        f =>
          !(f.questionId === questionId && f.submissionId === submissionId)
      );
    },

    clearFiles() {
      this.files = [];
    },

    resetSubmissions() {
      this.submissions = [];
      this.submission = null;
      this.studentResponse = null;
      this.questionSubmissions = [];
      this.files = [];
    }
  }
});
