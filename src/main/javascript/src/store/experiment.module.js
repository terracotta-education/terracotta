import { experimentService } from '@/services'

const state = {
    experiment: null,
    experiments: null,
    newExperiment: null
};

const actions = {
    resetExperiment({ commit }) {
        commit('setExperiment', {})
    },
    createExperiment() {
        return experimentService.createExperiment()
    }
};

const mutations = {
    setExperiment(state, data) {
        state.experiment = data
    },
};

const getters = {
    pageExperiment({ commit, state }, experimentID) {
        if (!experimentID) {
            experimentID = state.experiment.experiment_id
        }

        experimentService.getById(experimentID)
            .then(data => {
                commit('setExperiment', data)
            })
            .catch(response => {
                console.log({response})
            })
    },
};

export const experiment = {
    namespaced: true,
    state,
    actions,
    mutations,
    getters
};