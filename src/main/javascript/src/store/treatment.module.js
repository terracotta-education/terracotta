import {treatmentService} from '@/services'

const state = {
  treatments: [],
  treatment: {}
}

const actions = {
  async createTreatment ({commit}, payload) {
    // payload = experiment_id, condition_id, assignment_id
    // create the treatment, commit an update mutation, and return the status/data response
    try {
      const response = await treatmentService.create(...payload)
      const treatment = response?.data
      if (treatment.assignmentId) {
        commit('updateTreatment', treatment)
        commit('updateTreatments', treatment)
        return {
          status: response?.status,
          data: treatment
        }
      }
    } catch (error) {
      console.log('createTreatment catch', error)
    }
  },
}
const mutations = {
  updateTreatment(state, treatment) {
    state.treatment = treatment
  },
  updateTreatments(state, treatment) {
    // check for same id and update if exists
    const foundIndex = state.treatments?.findIndex(t => t.treatmentId === treatment.treatmentId)
    if (foundIndex >= 0) {
      state.treatments[foundIndex] = treatment
    } else {
      state.treatments.push(treatment)
    }
  }
}
const getters = {
  treatment: (state) => {
    return state.treatment
  },
  treatments: (state) => {
    return state.treatments
  }
}

export const treatment = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters
}