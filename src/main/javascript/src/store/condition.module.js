import {conditionService} from '@/services'

const state = {}

const actions = {
  resetCondition: ({commit}) => {
    commit('setCondition', {})
  },
  createDefaultConditions: ({dispatch}, experimentId) => {
    const defaultConditions = [
      {
        name: '',
        experiment_experiment_id: experimentId
      },
      {
        name: '',
        experiment_experiment_id: experimentId
      },
    ]
    dispatch('createConditions', defaultConditions)
  },
  createConditions: ({dispatch}, conditions) => {
    if (conditions.length > 0) {
      conditions.forEach(condition => {
        dispatch('createCondition', condition)
      })
    }
  },
  createCondition: ({commit}, condition) => {
    return conditionService.create(condition)
            .then(condition => {
              // commit mutation from experiment module
              if (condition.message) {
                alert(condition.message)
              } else {
                commit('experiment/setCondition', condition, {root: true})
              }
            })
            .catch(response => {
              console.log('createCondition | catch', {response})
            })
  },
  async updateConditions({dispatch}, conditions) {
    if (conditions.length > 0) {
      return Promise.all(
        conditions.map(async (condition) => {
          if (condition?.conditionId) {
            return dispatch('updateCondition', condition)
          }
        })
      )
    }
  },
  updateCondition: ({commit}, condition) => {
    return conditionService.update(condition)
            .then((response) => {
              if (response.status === 200) {
                // commit mutation from experiment module
                commit('experiment/setCondition', condition, {root: true})
              }
              return response
            })
            .catch(response => {
              console.log('setCondition | catch', {response})
            })
  },
  setDefaultCondition({dispatch}, payload) {
    if (!payload || !payload.conditions || !payload.defaultConditionId) {
      return false
    }

    return Promise.all(
      payload.conditions.map(async (condition) => {
        return new Promise((resolve) => {
          condition.defaultCondition = (condition.conditionId === payload.defaultConditionId) ? 1 : 0
          dispatch('updateCondition', condition)
          resolve(condition)
        });
      })
    )
  },
  deleteCondition: ({commit}, condition) => {
    return conditionService.delete(condition)
            .then(() => {
              // commit mutation from experiment module
              commit('experiment/deleteCondition', condition, {root: true})
            })
            .catch(response => {
              console.log('setCondition | catch', {response})
            })
  }
}

const mutations = {}

const getters = {}

export const condition = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters
}