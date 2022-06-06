import {submissionService} from '@/services'

const state = {
  submissions: null,
  studentResponse: null,
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

  async createQuestionSubmission({state}, payload) {
    // payload = experiment_id, condition_id, treatment_id, assessment_id, submission_id, questions

    try {
      const response = await submissionService.createQuestionSubmission(...payload)
      if (response) {
        return {
          data: response
        }
      }
    } catch (error) {
      console.error('createQuestionSubmission catch', {error, state})
    }
  },

  async updateQuestionSubmission({state}, payload) {
    // payload = experiment_id, condition_id, treatment_id, assessment_id, submission_id, updatedResponseBody

    try {
      const response = await submissionService.updateQuestionSubmission(...payload)
      if (response) {
        return {
          status: response?.status,
          data: null,
        }
      }
    } catch (error) {
      console.error('updateQuestionSubmission catch', {error, state})
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
}

const getters = {
  submissions(state) {
    return state.submissions
  },
  studentResponse(state) {
    return state.studentResponse
  },
}

export const submissions = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters,
}
