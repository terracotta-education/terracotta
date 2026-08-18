import { participantService } from "@/services"

const state = {
  participants: [],
  participant: null,
  groups: null,
}

const actions = {
  async fetchParticipants({ commit }, payload) {
    // payload = experimentId, refresh
    return participantService
      .getAll(...payload)
      .then((data) => {
        // a failed HTTP response resolves to an error object (e.g. {status, error}) rather than
        // rejecting, so an array check is needed here to catch that case as a failure too
        if (!Array.isArray(data)) {
          console.error("fetchParticipants | non-array response", { data })
          commit("setParticipants", [])
          return
        }

        commit("setParticipants", data)
      })
      .catch((response) => {
        console.log("fetchParticipants | catch", { response })
        commit("setParticipants", [])
      })
  },

  setParticipantsGroup: ({ commit }, participantsList) => {
    commit("setParticipantsGroup", participantsList)
  },

  async updateParticipants({ state }, experimentId) {
    const requestBody = []
    state.participants.map((participant) => {
      const participantDetail = {
        participantId: participant.participantId,
        consent: participant.consent,
        dropped: participant.dropped,
        groupId: participant.groupId,
      }
      requestBody.push(participantDetail)
    })

    return participantService
      .updateParticipants(experimentId, requestBody)
      .catch((response) =>
        console.log("updateParticipants | catch", { response })
      )
  },

  // payload = experimentId, participant_data
  async updateParticipant({ commit }, payload) {
    try {
      const { experimentId, participantData } = payload;
      const response = await participantService.updateParticipant(
        experimentId,
        participantData
      );
      commit("setParticipant");
      return response;
    } catch (error) {
      console.log("updateParticipant catch", { error, state });
    }
  },
  fetchGroups: ({ commit }, experimentId) => {
    return participantService
      .getGroups(experimentId)
      .then((data) => {
        commit("setGroups", data)
      })
      .catch((response) => {
        console.log("fetchParticipants | catch", { response })
      })
  },
  resetParticipants({state}) {
    state.participants = [];
    state.participant = null;
  },
}

const mutations = {
  setParticipants(state, data) {
    state.participants = data
  },
  setParticipant(state, data) {
    state.participant = data
  },
  setParticipantsGroup(state, data) {
    state.participants = data
  },
}

const getters = {
  participants(state) {
    return state.participants
  },
}

export const participants = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters,
}
