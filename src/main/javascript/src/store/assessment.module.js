import {assessmentService} from '@/services'

const state = {
  assessment: [],
  assessments: [],
}

const actions = {
  async fetchAssessment({commit}, payload) {
    try {
      const response = await assessmentService.fetchAssessment(...payload)
      const assessment = response?.data

      commit('setAssessment', assessment)
    } catch (error) {
      console.error('setAssessment catch', error)
    }
  },
  async createAssessment({commit}, payload) {
    // payload = experiment_id, condition_id, treatment_id, title, body
    // create the assessment, commit an update mutation, and return the status/data response
    try {
      const response = await assessmentService.createAssessment(...payload)
      const assessment = response?.data
      if (assessment?.assessmentId) {
        commit('updateAssessment', assessment)
        return {
          status: response?.status,
          data: assessment
        }
      }
    } catch (error) {
      console.log('createAssessment catch', error)
    }
  },
  async updateAssessment({state}, payload) {
    // payload = experiment_id, condition_id, treatment_id, assessment_id, title, body
    // update the assessment, and return the status/data response
    try {
      const response = await assessmentService.updateAssessment(...payload)
      if (response) {
        return {
          status: response?.status,
          data: state.assessment
        }
      }
    } catch (error) {
      console.log('updateAssessment catch', error)
    }
  },
  async createQuestion({commit}, payload) {
    // payload = experiment_id, condition_id, treatment_id, assessment_id, question_order, question_type, points, body
    // create the assessment question, commit an update mutation, and return the status/data response
    try {
      const response = await assessmentService.createQuestion(...payload)
      const question = response?.data
      if (question?.questionId) {
        commit('updateQuestions', question)
        return {
          status: response?.status,
          data: question
        }
      }
    } catch (error) {
      console.log('createQuestion catch', error)
    }
  },
  async updateQuestion({state}, payload) {
    // payload = experiment_id, condition_id, treatment_id, assessment_id, question_id, html, points, questionOrder, questionType
    // update question and return the status/data response
    try {
      const response = await assessmentService.updateQuestion(...payload)
      if (response) {
        return {
          status: response?.status,
          data: null
        }
      }
    } catch (error) {
      console.log('updateQuestion catch', {error, state})
    }
  },
  async deleteQuestion({commit}, payload) {
    const questionId = payload[4]
    // payload = experiment_id, condition_id, treatment_id, assessment_id, question_id
    // delete question, commit mutation, and return the status/data response
    try {
      const response = await assessmentService.deleteQuestion(...payload)
      console.log({response})
      if (response) {
        // send question id to the deleteQuestion mutation
        commit('deleteQuestion', questionId)
        return {
          status: response?.status,
          data: null
        }
      }
    } catch (error) {
      console.log('deleteQuestion catch', {error})
    }
  },
  async createAnswer({commit}, payload) {
    // payload = experiment_id, condition_id, treatment_id, assessment_id, question_id, html, correct, answerOrder
    // create the assessment answer, commit an update mutation, and return the status/data response
    try {
      const response = await assessmentService.createAnswer(...payload)
      const answer = response?.data
      if (answer?.answerId) {
        commit('updateAnswers', answer)
        return {
          status: response?.status,
          data: answer
        }
      }
    } catch (error) {
      console.log('createAnswer catch', error)
    }
  },
  async deleteAnswer({commit}, payload) {
    const answerId = ""
    // payload =
    // delete answer, commit mutation, and return the status/data response
    try {
      const response = await assessmentService.deleteAnswer(...payload)
      console.log({response})
      if (response) {
        // send answer id to the deleteAnswer mutation
        commit('deleteAnswer', answerId)
        return {
          status: response?.status,
          data: null
        }
      }
    } catch (error) {
      console.log('deleteAnswer catch', {error})
    }
  },
}
const mutations = {
  setAssessment(state, assessment) {
    state.assessment = assessment
  },
  updateAssessments(state, assessment) {
    // check for same id and update if exists
    const foundIndex = state.assessments?.findIndex(a => parseInt(a.assessmentId) === parseInt(assessment.assessmentId))
    if (foundIndex >= 0) {
      state.assessments[foundIndex] = assessment
    } else {
      state.assessments.push(assessment)
    }
  },
  setQuestions(state, questions) {
    // check for same id and update if exists
    state.assessments.questions = questions
  },
  updateQuestions(state, question) {
    // check for same id and update if exists
    const foundIndex = state.assessment.questions?.findIndex(q => parseInt(q.questionId) === parseInt(question.questionId))
    if (foundIndex >= 0) {
      state.assessment.questions[foundIndex] = question
    } else {
      state.assessment.questions.push(question)
    }
  },
  deleteQuestion(state, qid) {
    state.assessment.questions = [...state.assessment.questions?.filter(q => parseInt(q.questionId) !== parseInt(qid))]
  },
  updateAnswers(state, answer) {
    // WIP
    state.assessment.questions.map(q => {
      q.answers?.map(a => {
        console.log({a})
        if (parseInt(a.answerId) === parseInt(answer.answerId)) a = answer;
      })
    });
  },
  deleteAnswer(state, aid) {
    // WIP
    // state.assessment.questions = [...state.assessment.questions?.filter(q => parseInt(q.questionId) !== parseInt(qid))]
    console.log({state,aid})
  },
}
const getters = {
  assessment: (state) => {
    return state.assessment
  },
  questions: (state) => {
    return state.assessment.questions
  },
  assessments: (state) => {
    return state.assessments
  }
}

export const assessment = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters
}