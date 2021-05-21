import { conditionService } from '@/services'

const state = {};

const actions = {
    resetCondition: ({ commit }) => {
        commit('setCondition', {})
    },
    createDefaultConditions:({dispatch}, experimentId) => {
        const defaultConditions = [
            {
                name:"",
                experiment_experiment_id: experimentId
            },
            {
                name:"",
                experiment_experiment_id: experimentId
            },
        ]
        dispatch('createConditions', defaultConditions)
    },
    createConditions: ({ dispatch }, conditions) => {
        if (conditions.length > 0) {
            conditions.forEach(condition => {
                dispatch('createCondition', condition)
            })
        }
    },
    createCondition: ({commit}, condition) => {
        return conditionService.createCondition(condition)
                    .then(condition => {
                        // commit mutation from experiment module
                        commit('experiment/updateCondition', condition, {root:true})
                    })
                    .catch(response => {
                        console.log("setCondition | catch",{response})
                    })
    },
};

const mutations = {};

const getters = {};

export const condition = {
    namespaced: true,
    state,
    actions,
    mutations,
    getters
};