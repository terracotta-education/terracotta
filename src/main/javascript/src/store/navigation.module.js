const state = {
  /*
    Enables the appropriate navigation when editing a section
    editMode: {
        initialPage: first edit page,
        callerPage: { page to return to
            name: page name,
            tab: active tab on page (if available),
            exposureSet: index of active exposure set
    }
  */
  editMode: null
}

const actions = {
  saveEditMode: ({commit}, editMode) => {
    commit('setEditMode', editMode);
  },
  deleteEditMode: ({commit}) => {
    commit('resetEditMode');
  }
}

const mutations = {
  setEditMode(state, editMode) {
    state.editMode = editMode;
  },
  resetEditMode(state) {
    state.editMode = null;
  }
}

const getters = {
  editMode(state) {
    return state.editMode;
  }
}

export const navigation = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters
}
