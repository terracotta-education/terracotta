import {submissionService} from '@/services'

const state = {
  submissions: null,
  studentResponse: null,
  questionSubmissions: null,
}

const actions = {
  fetchSubmissions: ({commit}, payload) => {
    // payload = experiment_id, condition_id, treatment_id, assessment_id
    return submissionService
      .getAll(...payload)
      .then((data) => {
        commit('setSubmissions', data)
      })
      .catch((response) => {
        console.log('setSubmissions | catch', {response})
      })
  },
  async updateSubmission({state}, payload) {
    // payload = experiment_id, condition_id, treatment_id, assessment_id, submission_id, alteredCalculatedGrade, totalAlteredGrade
    try {
      const response = await submissionService.updateSubmission(...payload)
      if (response) {
        return {
          status: response?.status,
          data: null,
        }
      }
    } catch (error) {
      console.log('updateSubmission catch', {error, state})
    }
  },
  fetchStudentResponse: ({commit}, payload) => {
    // payload = experiment_id, condition_id, treatment_id, assessment_id, submission_id
    return submissionService
      .studentResponse(...payload)
      .then((data) => {
        commit('setStudentResponse', data)
      })
      .catch((response) => {
        console.error('setStudentResponse | catch', {response})
      })
  },

  async fetchQuestionSubmissions({commit}, payload) {
    // payload = experiment_id, condition_id, treatment_id, assessment_id, submission_id
    try {
      const data = await submissionService.getQuestionSubmissions(...payload);
      commit('setQuestionSubmissions', data)
    } catch (error) {
      console.error('fetchQuestionSubmissions catch', {error, state})
    }
  },

  async createQuestionSubmissions({state}, payload) {
    // payload = experiment_id, condition_id, treatment_id, assessment_id, submission_id, questions

    try {
      const response = await submissionService.createQuestionSubmissions(...payload)
      if (response) {
        return {
          data: response
        }
      }
    } catch (error) {
      console.error('createQuestionSubmissions catch', {error, state})
    }
  },

  async updateQuestionSubmissions({state}, payload) {
    // payload = experiment_id, condition_id, treatment_id, assessment_id, submission_id, updatedResponseBody

    try {
      const response = await submissionService.updateQuestionSubmissions(...payload)
      if (response) {
        return {
          status: response?.status,
          data: null,
        }
      }
    } catch (error) {
      console.error('updateQuestionSubmissions catch', {error, state})
    }
  },

  async createAnswerSubmission({state}, payload) {
    // payload = experiment_id, condition_id, treatment_id, assessment_id, submission_id, question_submission_id, answerSubmission

    try {
      const response = await submissionService.createAnswerSubmission(...payload)
      if (response) {
        return {
          data: response
        }
      }
    } catch (error) {
      console.error('createAnswerSubmission catch', {error, state})
    }
  },
}

const mutations = {
  setSubmissions(state, data) {
    state.submissions = data
  },
  setStudentResponse(state, data) {
    state.studentResponse = data
  },
  setQuestionSubmissions(state, data) {
    state.questionSubmissions = data;
  }
}

const getters = {
  submissions(state) {
    return state.submissions
  },
  studentResponse(state) {
    return state.studentResponse
  },
  questionSubmissions(state) {
    return state.questionSubmissions;
  }
}

export const submissions = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters,
}
