import { experimentService } from '@/services'

const state = {
    experiment: null,
    experiments: null
};

const actions = {
    resetExperiment: ({ commit }) => {
        commit('setExperiment', {})
    },
    createExperiment: () => {
        return experimentService.createExperiment()
    },
    fetchExperimentById: ({commit}, experimentId) => {
        return experimentService.getById(experimentId)
            .then(data => {
                commit('setExperiment', data)
            })
            .catch(response => {
                console.log("fetchExperimentById | catch",{response})
            })
    },
    updateExperiment: ({commit}, experiment) => {
        return experimentService.update(experiment)
                .then(commit('setExperiment', experiment))
                .catch(response => {
                    console.log("updateExperiment | catch",{response})
                })
    },
};

const mutations = {
    setExperiment(state, data) {
        state.experiment = data
    },
};

const getters = {
};

export const experiment = {
    namespaced: true,
    state,
    actions,
    mutations,
    getters
};