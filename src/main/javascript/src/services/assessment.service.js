import {authHeader, isJson} from '@/helpers'
import store from '@/store/index.js'

/**
 * Register methods
 */
export const assessmentService = {
  createAssessment,
  createQuestion,
  createAnswer,
}

/**
 * Create Assessment
 */
async function createAssessment(experiment_id, condition_id, treatment_id, title, body) {
  const requestOptions = {
    method: 'POST',
    headers: { ...authHeader(), 'Content-Type': 'application/json' },
    body: JSON.stringify({
      title,
      "html": body
    })
  }

  return fetch(`${store.getters['api/aud']}/api/experiments/${experiment_id}/conditions/${condition_id}/treatments/${treatment_id}/assessments`, requestOptions).then(handleResponse)
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
  body
) {
  const requestOptions = {
    method: 'POST',
    headers: { ...authHeader(), 'Content-Type': 'application/json' },
    body: JSON.stringify({
      questionOrder: question_order,
      questionType: question_type,
      points,
      "html": body
    })
  }

  return fetch(`${store.getters['api/aud']}/api/experiments/${experiment_id}/conditions/${condition_id}/treatments/${treatment_id}/assessments/${assessment_id}/questions`, requestOptions).then(handleResponse)
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
  body,
  correct,
  answerOrder
) {
  const requestOptions = {
    method: 'POST',
    headers: { ...authHeader(), 'Content-Type': 'application/json' },
    body: JSON.stringify({
      "html": body,
      correct,
      answerOrder
    })
  }

  return fetch(`${store.getters['api/aud']}/api/experiments/${experiment_id}/conditions/${condition_id}/treatments/${treatment_id}/assessments/${assessment_id}/questions/${question_id}/answers`, requestOptions).then(handleResponse)
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
    console.error('handleResponse | catch',{text})
  })
}