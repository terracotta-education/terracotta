const state = {
  type: null,
  message: null
};

const actions = {
  info({ commit }, message) {
    commit("info", message);
  },
  success({ commit }, message) {
    commit("success", message);
  },
  warn({ commit }, message) {
    commit("warn", message);
  },
  error({ commit }, message) {
    commit("error", message);
  },
  clear({ commit }) {
    commit("clear", null);
  }
};

const getters = {
  alert(state) {
    return state;
  },
  statuses() {
    return {
      info: "info",
      success: "success",
      warn: "warn",
      error: "error"
    };
  }
};

const mutations = {
  info(state, message) {
    state.type = "info";
    state.message = message;
  },
  success(state, message) {
    state.type = "success";
    state.message = message;
  },
  warn(state, message) {
    state.type = "warn";
    state.message = message;
  },
  error(state, message) {
    state.type = "error";
    state.message = message;
  },
  clear(state) {
    state.type = null;
    state.message = null;
  }
};

export const alert = {
  namespaced: true,
  state,
  actions,
  getters,
  mutations
};