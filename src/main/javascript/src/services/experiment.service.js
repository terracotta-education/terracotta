import { authHeader } from '@/helpers'
// import store from '@/store/index.js'

const base_url = "http://localhost:8081"

/**
 * Register methods
 */
export const experimentService = {
    getAll,
    getById,
    create,
    update,
    delete: _delete
}

/**
 * Get all Experiments
 */
function getAll() {
    const requestOptions = {
        method: 'GET',
        headers: authHeader()
    }

    return fetch(`${base_url}/api/experiments`, requestOptions).then(handleResponse)
}

/**
 * Create Experiment
 */
function create() {
    const requestOptions = {
        method: 'POST',
        headers: { ...authHeader(), 'Content-Type': 'application/json' },
        body: JSON.stringify({})
    }

    return fetch(`${base_url}/api/experiments`, requestOptions).then(handleResponse)
}

/**
 * Get individual Experiment
 */
function getById(experiment_id) {
    const requestOptions = {
        method: 'GET',
        headers: { ...authHeader() },
    }

    return fetch(`${base_url}/api/experiments/${experiment_id}?conditions=true`, requestOptions).then(handleResponse)
}
/**
 * Update Experiment
 */
function update(experiment) {
    const requestOptions = {
        method: 'PUT',
        headers: { ...authHeader() },
        body: JSON.stringify(experiment)
    }

    return fetch(`${base_url}/api/experiments/${experiment.experimentId}`, requestOptions).then(handleResponse)
}

/**
 * Delete Experiment
 *
 * (Prefixed function name with underscore because delete is a reserved word in javascript)
 */
function _delete(id) {
    const requestOptions = {
        method: 'DELETE',
        headers: authHeader()
    }

    return fetch(`${base_url}/api/experiments/${id}`, requestOptions).then(handleResponse)
}

/**
 * Handle API response
 */
function handleResponse(response) {
    return response.text()
        .then(text => {
            const data = text && JSON.parse(text)

            if (!response || !response.ok) {
                if (response.status === 401 || response.status === 402 || response.status === 500) {
                    console.log("handleResponse | 401/402/500",{response})
                } else if (response.status===404) {
                    console.log("handleResponse | 404",{response})
                }

                return response
            }

            console.log("handleResponse | then",{text,data,response})
            return data || response
        }).catch(text => {
            console.log("handleResponse | catch",{text})
        })
}