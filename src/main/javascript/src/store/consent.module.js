import { consentService } from '@/services'

const state = {};

const actions = {
    resetConsent: ({ commit }) => {
        commit('experiment/setConsent', {}, {root:true})
    },
    createConsent: ({commit}, {experiment_id, consent}) => {
        return consentService.create(experiment_id, consent)
                .then(response => {
                    // commit mutation from experiment module
                    console.log({response, commit})
                    // commit('experiment/setConsent', consent, {root:true})
                })
                .catch(response => {
                    console.log("setCondition | catch",{response})
                })
    },
};

const mutations = {};

const getters = {};

export const consent = {
    namespaced: true,
    state,
    actions,
    mutations,
    getters
};