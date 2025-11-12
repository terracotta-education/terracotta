import { integrationsService } from "@/services";

const state = {
  validation: {
    iframeUrlValid: false
  }
}

const actions = {
  async validateIframeUrl({ commit }, url) {
    try {
      const response = await integrationsService.validateIframeUrl(url);
      commit("setIframeValid", response);
    } catch (e) {
      console.error("validateIframeUrl catch", {e});
    }
  }
}

const mutations = {
  setIframeValid(state, iframeUrlValid) {
    state.validation = {
        ...state.validation,
        iframeUrlValid: iframeUrlValid
    }
  }
}

const getters = {
  get: (state) => {
    return state.validation;
  },
  isIframeUrlValid: (state) => {
      return state.validation?.iframeUrlValid || false;
  }
}

export const integrations = {
  namespaced: true,
  state,
  actions,
  getters,
  mutations
}
