import { participantService } from "@/services";

const state = {
  participants: null,
  groups: null,
};

const actions = {
  fetchParticipants: ({ commit }, experimentId) => {
    return participantService
      .getAll(experimentId)
      .then((data) => {
        commit("setParticipants", data);
      })
      .catch((response) => {
        console.log("fetchParticipants | catch", { response });
      });
  },

  setParticipantsGroup: ({ commit }, participantsList) => {
    commit("setParticipantsGroup", participantsList);
  },

  updateParticipants: ({ state }, experimentId) => {
    const requestBody = [];
    state.participants.map((participant) => {
      const participantDetail = {
        participantId: participant.participantId,
        consent: participant.consent,
        dropped: participant.dropped,
        groupId: participant.groupId,
      };
      requestBody.push(participantDetail);
    });

    return participantService
      .updateParticipants(experimentId, requestBody)
      .catch((response) =>
        console.log("updateParticipants | catch", { response })
      );
  },

  fetchGroups: ({ commit }, experimentId) => {
    return participantService
      .getGroups(experimentId)
      .then((data) => {
        commit("setGroups", data);
      })
      .catch((response) => {
        console.log("fetchParticipants | catch", { response });
      });
  },
};

const mutations = {
  setParticipants(state, data) {
    state.participants = data;
  },

  setParticipantsGroup(state, data) {
    state.participants = data;
  },
};

const getters = {
  participants(state) {
    return state.participants;
  },
};

export const participants = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters,
};
