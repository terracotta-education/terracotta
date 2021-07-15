import {authHeader, isJson} from '@/helpers'
import store from '@/store/index.js'

/**
 * Register methods
 */
export const assessmentService = {
  fetchAssessment,
  createAssessment,
  updateAssessment,
  createQuestion,
  updateQuestion,
  deleteQuestion,
  createAnswer,
  deleteAnswer,
}

/**
 * Fetch Assessment
 */
async function fetchAssessment(experiment_id, condition_id, treatment_id, assessment_id) {
  const requestOptions = {
    method: 'GET',
    headers: {...authHeader()}
  }

  return fetch(`${store.getters['api/aud']}/api/experiments/${experiment_id}/conditions/${condition_id}/treatments/${treatment_id}/assessments/${assessment_id}?submissions=true&questions=true`, requestOptions).then(handleResponse)
}

/**
 * Create Assessment
 */
async function createAssessment(experiment_id, condition_id, treatment_id, title, body) {
  const requestOptions = {
    method: 'POST',
    headers: {...authHeader(), 'Content-Type': 'application/json'},
    body: JSON.stringify({
      title,
      "html": body
    })
  }

  return fetch(`${store.getters['api/aud']}/api/experiments/${experiment_id}/conditions/${condition_id}/treatments/${treatment_id}/assessments`, requestOptions).then(handleResponse)
}

/**
 * Update Assessment
 */
async function updateAssessment(
  experiment_id,
  condition_id,
  treatment_id,
  assessment_id,
  title,
  body
) {
  const requestOptions = {
    method: 'PUT',
    headers: {...authHeader(), 'Content-Type': 'application/json'},
    body: JSON.stringify({
      title,
      "html": body
    })
  }

  return fetch(`${store.getters['api/aud']}/api/experiments/${experiment_id}/conditions/${condition_id}/treatments/${treatment_id}/assessments/${assessment_id}`, requestOptions).then(handleResponse)
}

/**
 * Create Question
 */
async function createQuestion(
  experiment_id,
  condition_id,
  treatment_id,
  assessment_id,
  question_order,
  question_type,
  points,
  html
) {
  const requestOptions = {
    method: 'POST',
    headers: {...authHeader(), 'Content-Type': 'application/json'},
    body: JSON.stringify({
      questionOrder: question_order,
      questionType: question_type,
      points,
      html
    })
  }

  return fetch(`${store.getters['api/aud']}/api/experiments/${experiment_id}/conditions/${condition_id}/treatments/${treatment_id}/assessments/${assessment_id}/questions`, requestOptions).then(handleResponse)
}

/**
 * Update Question
 */
async function updateQuestion(
  experiment_id,
  condition_id,
  treatment_id,
  assessment_id,
  question_id,
  html,
  points,
  questionOrder,
  questionType
) {
  const requestOptions = {
    method: 'PUT',
    headers: {...authHeader(), 'Content-Type': 'application/json'},
    body: JSON.stringify({
      html,
      points,
      questionOrder,
      questionType
    })
  }

  return fetch(`${store.getters['api/aud']}/api/experiments/${experiment_id}/conditions/${condition_id}/treatments/${treatment_id}/assessments/${assessment_id}/questions/${question_id}`, requestOptions).then(handleResponse)
}

/**
 * Delete Question
 */
async function deleteQuestion(
  experiment_id,
  condition_id,
  treatment_id,
  assessment_id,
  question_id
) {
  const requestOptions = {
    method: 'DELETE',
    headers: {...authHeader()}
  }

  return fetch(`${store.getters['api/aud']}/api/experiments/${experiment_id}/conditions/${condition_id}/treatments/${treatment_id}/assessments/${assessment_id}/questions/${question_id}`, requestOptions).then(handleResponse)
}

/**
 * Create Answer
 */
async function createAnswer(
  experiment_id,
  condition_id,
  treatment_id,
  assessment_id,
  question_id,
  html,
  correct,
  answerOrder
) {
  const requestOptions = {
    method: 'POST',
    headers: {...authHeader(), 'Content-Type': 'application/json'},
    body: JSON.stringify({
      html,
      correct,
      answerOrder
    })
  }

  return fetch(`${store.getters['api/aud']}/api/experiments/${experiment_id}/conditions/${condition_id}/treatments/${treatment_id}/assessments/${assessment_id}/questions/${question_id}/answers`, requestOptions).then(handleResponse)
}

/**
 * Delete Answer
 */
async function deleteAnswer() {
  const requestOptions = {
    method: 'DELETE',
    headers: {...authHeader()}
  }

  return fetch(`${store.getters['api/aud']}/`, requestOptions).then(handleResponse)
}


/**
 * Handle API response
 */
function handleResponse(response) {
  return response.text()
  .then(text => {
    const data = (text && isJson(text)) ? JSON.parse(text) : text

    if (
      !response ||
      response.status === 401 ||
      response.status === 402 ||
      response.status === 500 ||
      response.status === 404
    ) {
      console.log('handleResponse | 401/402/500', {response})
    } else if (response.status === 409) {
      return {
        message: data
      }
    } else if (response.status === 204) {
      console.log('handleResponse | 204', {text, data, response})
      return []
    }

    const dataResponse = (data) ? {
      data,
      status: response.status
    } : null

    return dataResponse || response
  }).catch(text => {
    console.error('handleResponse | catch', {text})
  })
}