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
  async createAssessment ({commit}, payload) {
    // payload = experiment_id, condition_id, treatment_id, title, body
    // create the assessment, commit an update mutation, and return the status/data response
    try {
      // check if assessment exist before creating a new one
      let response = await assessmentService.fetchAssessments(...payload)
      let assessment

      // return first assessment that matches, only one assessment per treatment
      if (response?.data?.length>0) {
        assessment = response?.data[0]
      } else {
        response = await assessmentService.createAssessment(...payload)
        assessment = response?.data
      }

      // commit changes to state
      commit('setAssessment', assessment)

      return {
        status: response?.status,
        data: assessment
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
      if (response?.status === 200) {
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
  async updateAnswer({state}, payload) {
    // payload = experiment_id, condition_id, treatment_id, assessment_id, question_id, answer_id, answer_type, html, correct, answer_order
    // update answer and return the status/data response
    try {
      const response = await assessmentService.updateAnswer(...payload)
      if (response) {
        return {
          status: response?.status,
          data: null
        }
      }
    } catch (error) {
      console.log('updateAnswer catch', {error, state})
    }
  },
  async deleteAnswer({commit}, payload) {
    const answerId = payload[5]
    // payload = experiment_id, condition_id, treatment_id, assessment_id, question_id, answer_id
    // delete answer, commit mutation, and return the status/data response
    try {
      const response = await assessmentService.deleteAnswer(...payload)
      console.log({response})
      if (response?.status === 200) {
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
    const aqid = parseInt(answer.questionId)

    // check if answer exists and update, or add answer to question
    state.assessment.questions = state.assessment.questions.map(q => {
      const qqid = parseInt(q.questionId)

      // step over question if not relevant
      if (qqid !== aqid) { return q }

      if (q.answers?.length > 0) {
        // if there are answers, check for matching answerId
        const foundIndex = q.answers.findIndex(a => parseInt(a.answerId) === parseInt(answer.answerId))

        if (foundIndex >= 0) {
          q.answers[foundIndex] = answer
        } else {
          q.answers = [...q.answers, answer]
        }
      } else if ((!q.answers || q.answers.length < 1)) {
        // create array with single answer if empty or missing answers
        q.answers = [answer]
      }

      return q
    });
  },
  deleteAnswer(state, answer_id) {
    const aid = parseInt(answer_id)
    state.assessment.questions = state.assessment.questions.map((q) => {
      return {...q, answers: q.answers.filter(a => parseInt(a.answerId) !== aid)}
    })
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