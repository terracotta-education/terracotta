import {assessmentService} from '@/services'

const state = {
  assessment: [],
  assessments: [],
  questions: [],
}

const actions = {
  async fetchAssessment ({commit}, payload) {
    try {
      const response = await assessmentService.fetchAssessment(...payload)
      const assessment = response?.data

      commit('setAssessment', assessment)
    } catch (e) {
      console.error(e)
    }
  },
  async createAssessment ({commit}, payload) {
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
  async updateAssessment ({state}, payload) {
    // payload = experiment_id, condition_id, treatment_id, assessment_id, title, body
    // create the assessment, commit an update mutation, and return the status/data response
    try {
      const response = await assessmentService.updateAssessment(...payload)
      if (response) {
        return {
          status: response?.status,
          data: state.assessment
        }
      }
    } catch (error) {
      console.log('createAssessment catch', error)
    }
  },
  async createQuestion ({commit}, payload) {
    // payload = experiment_id, condition_id, treatment_id, assessment_id, question_order, question_type, points, body
    // create the assessment question, commit an update mutation, and return the status/data response
    try {
      const response = await assessmentService.createQuestion(...payload)
      const question = response?.data
      if (question?.questionId) {
        console.log('createQuestion try', {question})
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
  async createAnswer ({commit}, payload) {
    // payload = experiment_id, condition_id, treatment_id, assessment_id, question_id, html, correct, answerOrder
    // create the assessment answer, commit an update mutation, and return the status/data response
    try {
      const response = await assessmentService.createAnswer(...payload)
      console.log('createAnswer ', {response})
      const answer = response?.data
      if (answer?.answerId) {
        console.log('createAnswer try', {answer})
        commit('updateAnswers', answer)
        return {
          status: response?.status,
          data: answer
        }
      }
    } catch (error) {
      console.log('createAnswer catch', error)
    }
  }
}
const mutations = {
  setAssessment(state, assessment) {
    state.assessment = assessment
  },
  updateAssessments(state, assessment) {
    // check for same id and update if exists
    const foundIndex = state.assessments?.findIndex(a => a.assessmentId === assessment.assessmentId)
    if (foundIndex >= 0) {
      state.assessments[foundIndex] = assessment
    } else {
      state.assessments.push(assessment)
    }
  },
  setQuestions(state, questions) {
    // check for same id and update if exists
    state.questions = questions
  },
  updateQuestions(state, question) {
    // check for same id and update if exists
    const foundIndex = state.questions?.findIndex(q => q.questionId === question.questionId)
    if (foundIndex >= 0) {
      state.questions[foundIndex] = question
    } else {
      state.questions.push(question)
    }
  },
  updateAnswers(state, answer) {
    // check for same id and update if exists
    const foundIndex = state.answers?.findIndex(q => q.answerId === answer.answerId)
    if (foundIndex >= 0) {
      state.answers[foundIndex] = answer
    } else {
      state.answers.push(answer)
    }
  }
}
const getters = {
  assessment: (state) => {
    return state.assessment
  },
  assessments: (state) => {
    return state.assessments
  },
  questions: (state) => {
    return state.questions
  }
}

export const assessment = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters
}