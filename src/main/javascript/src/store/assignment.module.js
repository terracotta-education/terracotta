import {assignmentService} from '@/services'

const state = {
  assignments: []
}

const actions = {
  createAssignment: ({commit}, payload) => {
    // create the assignment, commit an update mutation, and return the status/data response
    return assignmentService.create(...payload)
      .then((response) => {
        if (response?.assignmentId) {
          commit('updateAssignments', response)
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
  }
}
const getters = {}

export const assignment = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters
}