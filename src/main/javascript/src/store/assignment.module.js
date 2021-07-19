import {assignmentService} from '@/services'

const state = {
  assignments: [],
  assignment: {}
}

const actions = {
  async fetchAssignment({commit}, payload) {
    try {
      const response = await assignmentService.fetchAssignment(...payload)
      commit('setAssignment', response)
    } catch (e) {
      console.error(e)
    }
  },
  async fetchAssignmentsByExposure({commit}, payload) {
    try {
      const assignments = await assignmentService.fetchAssignmentsByExposure(...payload)
      commit('updateAssignments', assignments)
    } catch (e) {
      console.error(e)
    }
  },
  async deleteAssignment({commit}, payload) {
    // payload = experiment_id, exposure_id, assignment_id
    // delete assignment, commit mutation, and return the status/data response
    const aId = payload[2]
    try {
      const response = await assignmentService.deleteAssignment(...payload)
      console.log({response})

      if (response?.status === 200) {
        // send question id to the deleteQuestion mutation
        commit('deleteAssignment', aId)
        return {
          status: response?.status,
          data: null
        }
      }
    } catch (error) {
      console.log('deleteAssignment catch', {error})
    }
  },
  createAssignment: ({commit}, payload) => {
    // payload = experiment_id, exposure_id, title, order
    // create the assignment, commit an update mutation, and return the status/data response
    return assignmentService.create(...payload)
      .then((response) => {
        if (response?.assignmentId) {
          commit('setAssignment', response)
          return {
            status: 201,
            data: response
          }
        }
      })
  },
}
const mutations = {
  updateAssignments(state, assignments) {
    // check for same id and update if exists
    if (state.assignments && assignments?.length) {
      state.assignments = state.assignments
        .filter(a => !assignments.find(b => a.assignmentId === b.assignmentId))
        .concat(assignments)
    } else if (!state.assignments && assignments?.length > 0) {
      state.assignments = [...assignments]
    }
  },
  resetAssignments(state) {
    state.assignments = []
  },
  setAssignments(state, assignments) {
    state.assignments = assignments
  },
  deleteAssignment(state, aid) {
    state.assignments = [...state.assignments?.filter(a => parseInt(a.assignmentId) !== parseInt(aid))]
  },
  setAssignment(state, assignment) {
    state.assignment = assignment
  },
}
const getters = {
  assignments: (state) => {
    return state.assignments
  },
  assignment: (state) => {
    return state.assignment
  }
}

export const assignment = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters
}