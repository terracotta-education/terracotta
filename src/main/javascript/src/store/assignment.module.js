import {assignmentService} from "@/services"

const state = {
  assignments: [],
  assignment: {},
  fileRequest: null
}

const actions = {
  async updateAssignment({ commit }, payload) {
    try {
      const response = await assignmentService.updateAssignment(...payload);

      commit("setAssignment", response)
      return {
        status: 200,
        data: response
      };
    } catch (e) {
      console.error(e)
    }
  },
  async saveAssignmentOrder({ commit }, payload) {
    try {
      const response = await assignmentService.updateAssignments(...payload);
      commit("updateAssignments", response);
    } catch (e) {
      console.error(e)
    }
  },
  async fetchAssignment({commit}, payload) {
    // payload = experimentId, exposureId, assignmentId
    // get assignment by it"s assignmentId
    try {
      const response = await assignmentService.fetchAssignment(...payload)
      commit("setAssignment", response)
    } catch (e) {
      console.error(e)
    }
  },
  async fetchAssignmentsByExposure({commit}, payload) {
    // payload = experimentId, exposureId, submissions*
    // * = optional
    // get assignments by their exposureId
    try {
      const assignments = await assignmentService.fetchAssignmentsByExposure(...payload);
      commit("updateAssignments", assignments)
    } catch (e) {
      console.error(e)
    }
  },
  async deleteAssignment({commit}, payload) {
    // payload = experimentId, exposureId, assignmentId
    // delete assignment, commit mutation, and return the status/data response
    const aId = payload[2]
    try {
      const response = await assignmentService.deleteAssignment(...payload)

      if (response?.status === 200) {
        // send question id to the deleteQuestion mutation
        commit("deleteAssignment", aId)
        return {
          status: response?.status,
          data: null
        }
      }
    } catch (error) {
      console.error("deleteAssignment catch", {error})
    }
  },
  async duplicateAssignment({commit}, payload) {
    // payload = experimentId, exposureId, assignmentId
    // duplicate assignment, commit mutation, and return the status/data response
    try {
      const response = await assignmentService.duplicateAssignment(...payload);

      if (response?.assignmentId) {
        commit("setAssignment", response)
        return {
          status: 201,
          data: response
        }
      }
    } catch (error) {
      console.error("duplicateAssignment catch", {error})
    }
  },
  async createAssignment({commit}, payload) {
    // payload = experimentId, exposureId, title, order
    // create the assignment, commit an update mutation, and return the status/data response
    try {
      const response = await assignmentService.create(...payload)

      if (response?.assignmentId) {
        commit("setAssignment", response)
        return {
          status: 201,
          data: response
        }
      }
    } catch (error) {
      console.error("createAssignment catch", {error})
    }
  },
  async moveAssignment({commit}, payload) {
    const aId = payload[2];
    try {
      const response = await assignmentService.moveAssignment(...payload);
      if (response?.assignmentId) {
        commit("deleteAssignment", aId)
        commit("setAssignment", response)
        return {
          status: 201,
          data: response
        }
      }
    } catch (error) {
      console.error("updateAssignment catch", {error})
    }
  },
  async setCurrentAssignment({commit}, assignment) {
    commit("setAssignment", assignment);
  },
  async resetAssignments({commit}) {
    commit("setAssignments", []);
  },
  async resetAssignment({commit}) {
    commit("setAssignment",  null);
  }
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
  setAssignments(state, assignments) {
    state.assignments = assignments
  },
  deleteAssignment(state, aid) {
    state.assignments = [...state.assignments?.filter(a => parseInt(a.assignmentId) !== parseInt(aid))]
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
  },
  fileRequests: (state) => {
    return state.fileRequests;
  }
}

export const assignment = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters
}
