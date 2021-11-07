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

const CONDITION_COLORS = [
  '#FFCCBC',
  '#FFECB3',
  '#F0F4C3',
  '#C8E6C9',
  '#B2EBF2',
  '#BBDEFB',
  '#D1C4E9',
  '#F8BBD0',
  '#D7CCC8',
  '#FFE0B2',
  '#FFF9C4',
  '#DCEDC8',
  '#B2DFDB',
  '#B3E5FC',
  '#C5CAE9',
  '#E1BEE7',
  '#E1BEE7',
  '#CFD8DC',
];
const getters = {
  conditionColorMapping(_state, _getters, _rootState, rootGetters) {
    const exposures = rootGetters['exposures/exposures'];
    // Base the color mapping on the order of conditions in the first exposure
    const groupConditionList = exposures?.length > 0 ? exposures[0].groupConditionList : [];
    const conditionColorMap = {};
    for (let index = 0; index < groupConditionList.length; index++) {
      const groupCondition = groupConditionList[index];
      conditionColorMap[groupCondition.conditionName] = CONDITION_COLORS[index % CONDITION_COLORS.length]
    }
    return conditionColorMap;
  }
}

export const condition = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters
}
