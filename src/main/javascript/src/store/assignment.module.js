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
  async fetchAssignments({commit}, payload) {
    try {
      const assignments = await assignmentService.fetchAssignments(...payload)
      commit('setAssignments', assignments)
    } catch (e) {
      console.error(e)
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
            status:201,
            data: response
          }
        }
      })
  },
}
const mutations = {
  updateAssignments(state, assignment) {
    // check for same id or title and update if exists
    const foundIndex = state.assignments?.findIndex(a => a.assignmentId === assignment.assignmentId || a.title === assignment.title)
    if (foundIndex >= 0) {
      state.assignments[foundIndex] = assignment
    } else {
      state.assignments.push(assignment)
    }
  },
  setAssignments(state, assignments) {
    console.log({assignments})
    state.assignments = assignments
  },
  setAssignment(state, assignment) {
    state.assignment = assignment
  }
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