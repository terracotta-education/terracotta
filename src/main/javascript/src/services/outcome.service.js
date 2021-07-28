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
  getOutcomeScoresById,
  getScoreById,
  updateOutcomeScores,
  deleteOutcome
}

/**
 * Get all Outcomes
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
 * Get individual Outcome
 */
function getById(experiment_id, exposure_id, outcome_id) {
  const requestOptions = {
    method: 'GET',
    headers: authHeader(),
  }

  return fetch(`${store.getters['api/aud']}/api/experiments/${experiment_id}/exposures/${exposure_id}/outcomes/${outcome_id}`, requestOptions).then(handleResponse)
}

/**
 * Get Outcomes by Experiment Id
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
function create(experiment_id, exposure_id, title, max_points, external=false) {
  const requestOptions = {
    method: 'POST',
    headers: {
      ...authHeader(),
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      title,
      "maxPoints": max_points,
      external
    })
  }

  return fetch(`${store.getters['api/aud']}/api/experiments/${experiment_id}/exposures/${exposure_id}/outcomes`, requestOptions).then(handleResponse)
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
async function updateOutcomeScores(experiment_id, exposure_id, outcome_id, scores = null) {
  // scores = array || object
  if (!scores) {
    return false
  }

  const requestOptions = {
    method: 'POST',
    headers: {
      ...authHeader(),
      'Content-Type': 'application/json'
    }
  }

  if (scores.isArray()) {
    // if scores is an array
    const scorePromises = await Promise.all(scores.map(async score => {
      requestOptions.body = JSON.stringify(score)
      fetch(`${store.getters['api/aud']}/api/experiments/${experiment_id}/exposures/${exposure_id}/outcomes/${outcome_id}/outcome_scores`, requestOptions).then(handleResponse)
    }))
    console.log({scorePromises})
    return scorePromises
  } else {
    // should be an object if not array
    requestOptions.body = JSON.stringify(scores)
  }
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