import { groupsService } from '@/services'

const state = {
  groups: null,
}

const actions = {
  async createAndAssignGroups ({state}, experimentId) {
    return await groupsService
      .createAndAssignGroups(experimentId)
      .catch((response) => {
        console.log('createAndAssignGroups | catch', { response, state })
      });
  },
  resetGroups({state}) {
    state.groups = [];
  },
}

const mutations = {
  setGroupsService(state, data) {
    state.groups = data;
  },
};

const getters = {
  groups(state) {
    return state.groups;
  },
}

export const groups = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters,
}
