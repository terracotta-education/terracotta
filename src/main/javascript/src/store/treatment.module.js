import {treatmentService} from '@/services'

const state = {
  treatments: []
}

const actions = {
  createTreatment: ({commit}, payload) => {
    // payload = experiment_id, condition_id, assignment_id
    // create the treatment, commit an update mutation, and return the status/data response
    return treatmentService.create(...payload)
      .then((response) => {
        console.log({response, commit})
      })
  },
}
const mutations = {
}
const getters = {
  treatments: (state) => {
    return state.treatments
  },
  treatmentById: ({state}, tid) => {
    return state.treatments.find(t=>t.treatmentId===tid)
  }
}

export const treatment = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters
}