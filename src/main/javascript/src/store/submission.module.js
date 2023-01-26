import { submissionService } from "@/services";

const state = {
  submissions: null,
  studentResponse: null,
  questionSubmissions: null,
};

const actions = {
  fetchSubmissions: ({ commit }, payload) => {
    // payload = experiment_id, condition_id, treatment_id, assessment_id
    return submissionService
      .getAll(...payload)
      .then(({ data }) => {
        commit("setSubmissions", data);
      })
      .catch((response) => {
        console.log("setSubmissions | catch", { response });
      });
  },
  async updateSubmission({ state }, payload) {
    // payload = experiment_id, condition_id, treatment_id, assessment_id, submission_id, alteredCalculatedGrade, totalAlteredGrade
    try {
      return await submissionService.updateSubmission(...payload);
    } catch (error) {
      console.log("updateSubmission catch", { error, state });
    }
  },
  fetchStudentResponse: ({ commit }, payload) => {
    // payload = experiment_id, condition_id, treatment_id, assessment_id, submission_id
    return submissionService
      .studentResponse(...payload)
      .then(({ data }) => {
        commit("setStudentResponse", data);
      })
      .catch((response) => {
        console.error("setStudentResponse | catch", { response });
      });
  },

  async fetchQuestionSubmissions({ commit }, payload) {
    // payload = experiment_id, condition_id, treatment_id, assessment_id, submission_id
    try {
      const { data } = await submissionService.getQuestionSubmissions(
        ...payload
      );
      commit("setQuestionSubmissions", data);
    } catch (error) {
      console.error("fetchQuestionSubmissions catch", { error, state });
    }
  },

  async createQuestionSubmissions({ state }, payload) {
    // payload = experiment_id, condition_id, treatment_id, assessment_id, submission_id, questions

    try {
      return await submissionService.createQuestionSubmissions(...payload);
    } catch (error) {
      console.error("createQuestionSubmissions catch", { error, state });
    }
  },

  async updateQuestionSubmissions({ state }, payload) {
    // payload = experiment_id, condition_id, treatment_id, assessment_id, submission_id, updatedResponseBody

    try {
      return await submissionService.updateQuestionSubmissions(...payload);
    } catch (error) {
      console.error("updateQuestionSubmissions catch", { error, state });
    }
  },

  async createAnswerSubmissions({ state }, payload) {
    // payload = experiment_id, condition_id, treatment_id, assessment_id, submission_id, answerSubmissions

    try {
      return await submissionService.createAnswerSubmissions(...payload);
    } catch (error) {
      console.error("createAnswerSubmissions catch", { error, state });
    }
  },

  async updateAnswerSubmission({ state }, payload) {
    // payload = experiment_id, condition_id, treatment_id, assessment_id, submission_id, question_submission_id, answer_submission_id, answerSubmission

    try {
      return await submissionService.updateAnswerSubmission(...payload);
    } catch (error) {
      console.error("updateAnswerSubmission catch", { error, state });
    }
  },

  async clearQuestionSubmissions({ commit }) {
    // payload = experiment_id, condition_id, treatment_id, assessment_id, submission_id, answerSubmissions

    commit("setQuestionSubmissions", []);

    return Promise.resolve([]);
  },

  downloadAnswerFileSubmission({ state }, payload) {
    // payload = experimentId, conditionId, treatmentId, assessmentId, submissionId, questionSubmissionId, answerSubmissionId, mimeType, fileName

    try {
      return submissionService.downloadAnswerFileSubmission(...payload);
    } catch (error) {
      console.error("downloadAnswerFileSubmission catch", { error, state });
    }
  },
  resetSubmissions({state}) {
    state.submissions = [];
    state.studentResponse = null;
    state.questionSubmissions = [];
  },
};

const mutations = {
  setSubmissions(state, data) {
    state.submissions = data;
  },
  setStudentResponse(state, data) {
    state.studentResponse = data;
  },
  setQuestionSubmissions(state, data) {
    state.questionSubmissions = data;
  },
};

const getters = {
  submissions(state) {
    return state.submissions;
  },
  studentResponse(state) {
    return state.studentResponse;
  },
  questionSubmissions(state) {
    return state.questionSubmissions;
  },
};

export const submissions = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters,
};
