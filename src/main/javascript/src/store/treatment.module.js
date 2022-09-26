import {treatmentService} from '@/services'

const state = {
  treatments: [],
  treatment: {}
}

const actions = {
  async createTreatment ({commit}, payload) {
    const assignment_id = parseInt(payload[2])
    // payload = experiment_id, condition_id, assignment_id
    // create the treatment, commit an update mutation, and return the status/data response
    try {
      // check if treatments exist before creating a new one
      let response = await treatmentService.fetchTreatment(...payload)
      let treatment

      // return first treatment that matches, only one treatment per condition
      if (response?.data?.length>0 && response.data.find(t=>t.assignmentId===assignment_id)) {
        treatment = response.data.find(t=>t.assignmentId===assignment_id)
      } else {
        response = await treatmentService.create(...payload);
        if (response.status !== 201) {
          return response;
        }
        treatment = response?.data
      }

      // commit changes to state
      commit('setTreatment', treatment)
      commit('updateTreatments', treatment)

      return {
        status: response?.status,
        data: treatment
      }
    } catch (error) {
      console.log('createTreatment catch', error)
    }
  },
  async duplicateTreatment ({commit}, payload) {
    // payload = experiment_id, condition_id, assignment_id
    // create the treatment, commit an update mutation, and return the status/data response
    try {

      // return first treatment that matches, only one treatment per condition
      const response = await treatmentService.update(...payload);
      if (response.status !== 201) {
        return response;
      }
      const treatment = response?.data

      // commit changes to state
      commit('setTreatment', treatment)
      commit('updateTreatments', treatment)

      return {
        status: response?.status,
        data: treatment
      }
    } catch (error) {
      console.log('createTreatment catch', error)
    }
  },
  async checkTreatment({state}, payload) {
    // payload = experiment_id, condition_id, assignment_id
    try {
      const response = await treatmentService.fetchTreatment(...payload)
      if (response) {
        return {
          status: response.status,
          data: response.data
        }
      }
    } catch (error) {
      console.error('checkTreatment catch', {error, state})
    }
  },
}
const mutations = {
  setTreatment(state, treatment) {
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
