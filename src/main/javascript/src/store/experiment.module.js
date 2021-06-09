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
        return experimentService.create()
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
    }
};

const mutations = {
    setExperiment(state, data) {
        state.experiment = data
    },
    createConsent(state) {
        state.experiment.consent = state.experiment.consent || {
            title: "",
            file: null
        }
    },
    setConsent(state, consent) {
        state.experiment.consent = consent
    },
    setConsentTitle(state, title) {
        state.experiment.consent.title = title
    },
    setConditions(state, conditions) {
        state.experiment.conditions = conditions
    },
    updateConditions(state, conditions) {
        state.experiment.conditions = conditions
    },
    updateCondition(state, condition) {
        const foundIndex = state.experiment.conditions.findIndex(c => c.conditionId === condition.conditionId)
        if (foundIndex >= 0) {
            state.experiment.conditions[foundIndex] = condition
        } else {
            state.experiment.conditions.push(condition)
        }
    },
    deleteCondition(state, condition) {
        state.experiment.conditions = state.experiment.conditions.filter(function(item) {
            return item.conditionId !== condition.conditionId
        })
    }
};

const getters = {
    consent(state) {
        return state.experiment.consent
    },
    conditions(state) {
        return state.experiment.conditions
    }
};

export const experiment = {
    namespaced: true,
    state,
    actions,
    mutations,
    getters
};