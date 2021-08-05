import {authHeader, isJson} from '@/helpers'
import store from '@/store/index.js'

/**
 * Register methods
 */
export const outcomeService = {
  getAll,
  getById,
  getByExperimentId,
  create,
  updateOutcome,
  deleteOutcome,
  getOutcomeScoresById,
  getScoreById,
  createOutcomeScores,
  updateOutcomeScores,
  getOutcomePotentials
}

/**
 * Get all Outcomes by Experiment and Exposure Id
 */
function getAll(experiment_id, exposure_id) {
  const requestOptions = {
    method: 'GET',
    headers: authHeader(),
  }

  return fetch(
    `${store.getters['api/aud']}/api/experiments/${experiment_id}/exposures/${exposure_id}/outcomes`,
    requestOptions
  ).then(handleResponse)
}

/**
 * Get Outcome by Experiment, Exposure, and Outcome Id
 */
function getById(experiment_id, exposure_id, outcome_id) {
  const requestOptions = {
    method: 'GET',
    headers: authHeader(),
  }

  return fetch(`${store.getters['api/aud']}/api/experiments/${experiment_id}/exposures/${exposure_id}/outcomes/${outcome_id}`, requestOptions).then(handleResponse)
}

/**
 * Get Outcomes by Experiment Id and a list of exposures
 */
async function getByExperimentId(experiment_id, exposures = []) {
  const requestOptions = {
    method: 'GET',
    headers: authHeader()
  }

  if (exposures.length>1) {
    return await Promise.all(exposures.map(async exposure_id =>
      fetch(`${store.getters['api/aud']}/api/experiments/${experiment_id}/exposures/${exposure_id}/outcomes`, requestOptions).then(handleResponse)
    ))
  }
}

/**
 * Create Outcome
 */
function create(experiment_id, exposure_id, title, max_points, external=false, lms_type='NONE', lms_outcome_id = null) {
  const requestOptions = {
    method: 'POST',
    headers: {
      ...authHeader(),
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      title,
      "maxPoints": max_points,
      external,
      "lmsType": lms_type,
      "lmsOutcomeId": lms_outcome_id
    })
  }

  return fetch(`${store.getters['api/aud']}/api/experiments/${experiment_id}/exposures/${exposure_id}/outcomes`, requestOptions).then(handleResponse)
}

/**
 * Update Outcome by Outcome Id
 */
function updateOutcome(experiment_id, exposure_id, outcome) {
  const requestOptions = {
    method: 'PUT',
    headers: {
      ...authHeader(),
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      "title":outcome.title,
      "maxPoints": outcome.maxPoints,
      "external":outcome.external
    })
  }

  return fetch(
    `${store.getters['api/aud']}/api/experiments/${experiment_id}/exposures/${exposure_id}/outcomes/${outcome.outcomeId}`,
    requestOptions
  ).then(handleResponse)
}

/**
 * Delete Outcome by Outcome Id
 */
function deleteOutcome(experiment_id, exposure_id, outcome_id) {
  const requestOptions = {
    method: 'DELETE',
    headers: authHeader(),
  }

  return fetch(
    `${store.getters['api/aud']}/api/experiments/${experiment_id}/exposures/${exposure_id}/outcomes/${outcome_id}`,
    requestOptions
  ).then(handleResponse)
}

/**
 * Get Outcome Scores by Outcome Id
 */
function getOutcomeScoresById(experiment_id, exposure_id, outcome_id) {
    console.log("outcome.service.js getOutcomeScoresById: ", experiment_id, exposure_id, outcome_id)
  const requestOptions = {
    method: 'GET',
    headers: authHeader(),
  }

  return fetch(
    `${store.getters['api/aud']}/api/experiments/${experiment_id}/exposures/${exposure_id}/outcomes/${outcome_id}/outcome_scores`,
    requestOptions
  ).then(handleResponse)
}

/**
 * Get Outcome Score by Outcome Score Id
 */
function getScoreById(experiment_id, exposure_id, outcome_id, outcome_score_id) {
    console.log("getOutcomeScore by outcome score id:", outcome_id, outcome_score_id)
  const requestOptions = {
    method: 'GET',
    headers: authHeader(),
  }

  return fetch(
    `${store.getters['api/aud']}/api/experiments/${experiment_id}/exposures/${exposure_id}/outcomes/${outcome_id}/outcome_scores/${outcome_score_id}`,
    requestOptions
  ).then(handleResponse)
}

/**
 * POST Outcome Scores
 */
async function createOutcomeScores(experiment_id, exposure_id, outcome_id, scores = null) {
  // scores = array || object
  if (!Array.isArray(scores) || typeof scores !== 'object' && scores.participantId) {
    // if scores is not an array or object with the participantId key
    return false
  }

  const requestOptions = {
    method: 'POST',
    headers: {
      ...authHeader(),
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(scores)
  }

  return fetch(`${store.getters['api/aud']}/api/experiments/${experiment_id}/exposures/${exposure_id}/outcomes/${outcome_id}/outcome_scores`, requestOptions).then(handleResponse)
}

/**
 * POST/PUT Outcome Scores
 */
async function updateOutcomeScores(experiment_id, exposure_id, outcome_id, scores = null) {
    console.log("outcome.service.js updateOutcomeScores: ", experiment_id, exposure_id, outcome_id, scores)
  if (
    !scores ||
    // scores exists and is not an array
    !Array.isArray(scores) ||
    // scores is an array and includes participantId's
    (Array.isArray(scores) && !scores.find(o=>o.participantId)) ||
    // score is an object with participantId
    (typeof scores === 'object' && scores.participantId)
  ) {
    return false
  }

  const requestOptions = {
    headers: {
      ...authHeader(),
      'Content-Type': 'application/json'
    }
  }

  if(Array.isArray(scores) && scores.find(o=>o.outcomeScoreId)) {
    requestOptions.method = 'PUT'
    requestOptions.body = JSON.stringify(scores)
    console.log('Request Body 1', requestOptions)
    return await Promise.all(scores.map(async score => {
      requestOptions.body = JSON.stringify(score)
      await fetch(`${store.getters['api/aud']}/api/experiments/${experiment_id}/exposures/${exposure_id}/outcomes/${outcome_id}/outcome_scores/${score.outcomeScoreId}`, requestOptions).then(handleResponse)
    }))

  } else if (typeof scores === 'object' && scores.outcomeScoreId) {
    requestOptions.method = 'PUT'
    requestOptions.body = JSON.stringify(scores)
    console.log('Request Body 2', requestOptions)

    return await fetch(`${store.getters['api/aud']}/api/experiments/${experiment_id}/exposures/${exposure_id}/outcomes/${outcome_id}/outcome_scores/${scores.outcomeScoreId}`, requestOptions).then(handleResponse)
  } else {
    requestOptions.method = 'POST'
    requestOptions.body = JSON.stringify(scores)
    console.log('Request Body 3', requestOptions)

    return await Promise.all(scores.map(async score => {
      requestOptions.body = JSON.stringify(score)
      await fetch(`${store.getters['api/aud']}/api/experiments/${experiment_id}/exposures/${exposure_id}/outcomes/${outcome_id}/outcome_scores`, requestOptions).then(handleResponse)
    }))
  }
}


/**
 * Get Outcome Potentials by Experiment Id
 */
function getOutcomePotentials(experiment_id) {
  const requestOptions = {
    method: 'GET',
    headers: authHeader(),
  }

  return fetch(`${store.getters['api/aud']}/api/experiments/${experiment_id}/outcome_potentials`, requestOptions).then(handleResponse)
}


/**
 * Handle API response
 */
function handleResponse(response) {
  return response.text()
    .then((text) => {
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
      console.log('handleResponse | catch', {text})
    })
}