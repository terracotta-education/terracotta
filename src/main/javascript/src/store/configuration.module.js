import { configurationService } from "@/services";

const state = {
  configurations: null
};

const actions = {
  retrieve: ({commit}) => {
    return configurationService.get()
      .then(configurations => {
        if (configurations.message) {
          alert(configurations.message);
        } else {
          commit("setConfigurations", configurations);
        }
      })
      .catch(response => {
        console.log("get | catch", {response})
      })
  },
  update: ({commit}, data) => {
    commit("addConfiguration", data);
  }
}

const mutations = {
  setConfigurations(state, data) {
    state.configurations = data;
  },
  addConfiguration(state, data) {
    state.configurations = {
      ...state.configurations,
      [data.name]: data.value
    }
  }
};

const getters = {
  get(state) {
    return state.configurations;
  }
}

export const configuration = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters
}
