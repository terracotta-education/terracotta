import { submissionService } from "@/services";

const state = {
  submissions: null,
};

const actions = {
  fetchSubmissions: ({ commit }, payload) => {
    // payload = experiment_id, condition_id, treatment_id, assessment_id
    return submissionService
      .getAll(...payload)
      .then((data) => {
        commit("setSubmissions", data);
      })
      .catch((response) => {
        console.log("setSubmissions | catch", { response });
      });
  },
  async updateSubmission({ state }, payload) {
    // payload = experiment_id, condition_id, treatment_id, assessment_id, submission_id, alteredCalculatedGrade, totalAlteredGrade
  
    try {
      const response = await submissionService.updateSubmission(...payload);
      if (response) {
        return {
          status: response?.status,
          data: null,
        };
      }
    } catch (error) {
      console.log("updateSubmission catch", { error, state });
    }
  },
};

const mutations = {
  setSubmissions(state, data) {
    state.submissions = data;
  },
};

const getters = {
  submissions(state) {
    return state.submissions;
  },
};

export const submissions = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters,
};
