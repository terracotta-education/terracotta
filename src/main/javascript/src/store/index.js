import Vue from 'vue'
import Vuex from 'vuex'
import createPersistedState from 'vuex-persistedstate'

import { api } from './api.module'
import { alert } from './alert.module'
import { experiment } from './experiment.module'
import { condition } from './condition.module'
import { consent } from './consent.module'
import { exposures } from './exposures.module';

Vue.use(Vuex)

const store = new Vuex.Store({
    plugins: [createPersistedState({
        key: 'terracotta'
    })],
    modules: {
        api,
        alert,
        experiment,
        condition,
        consent,
        exposures
    }
})

export default store