import { authHeader } from '@/helpers'
// import store from '@/store/index.js'

/**
 * Register methods
 */
export const experimentService = {
    getAll,
    getById,
    createExperiment,
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

    return fetch(`http://localhost:8081/api/experiments}`, requestOptions).then(handleResponse)
}

/**
 * Create Experiment
 */
function createExperiment(data) {
    console.log("createExperiment",{data})
    const requestOptions = {
        method: 'POST',
        headers: { ...authHeader(), 'Content-Type': 'application/json' },
        body: (data) ? JSON.stringify({
            deployment: (data.parameters.deployment) ? data.parameters.deployment : null,
            LMSCourseId: (data.parameters.LMSCourseId) ? data.parameters.LMSCourseId : null,
            title: (data.parameters.title) ? data.parameters.title: null,
            description: (data.parameters.description) ? data.parameters.description : null,
            exposure_type: (data.parameters.exposure_type) ? data.parameters.exposure_type : null,
            participation_type: (data.parameters.participation_type) ? data.parameters.participation_type : null,
            distributionType: (data.parameters.distributionType) ? data.parameters.distributionType : null,
            started: (data.parameters.started) ? data.parameters.started : null,
            created_at: (data.parameters.created_at) ? data.parameters.created_at : null,
            updated_at: (data.parameters.updated_at) ? data.parameters.updated_at : null,
            conditions: (data.parameters.conditions) ? data.parameters.conditions : null,
        }):null
    }

    return fetch(`http://localhost:8081/api/experiments`, requestOptions).then(handleResponse)
}

/**
 * Get individual Experiment
 */
function getById(experiment_id) {
    const requestOptions = {
        method: 'GET',
        headers: authHeader()
    }

    return fetch(`http://localhost:8081/api/experiments/${experiment_id}`, requestOptions).then(handleResponse)
}
/**
 * Update Experiment
 */
function update(experiment) {
    const requestOptions = {
        method: 'PUT',
        headers: { ...authHeader(), 'Content-Type': 'application/json' },
        body: JSON.stringify(experiment)
    }

    return fetch(`http://localhost:8081/api/experiments`, requestOptions).then(handleResponse)
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

    return fetch(`http://localhost:8081/api/experiments/${id}`, requestOptions).then(handleResponse)
}

/**
 * Handle API response
 */
function handleResponse(response) {
    return response.text().then(text => {
        const data = text && JSON.parse(text)
        if (!response || !response.ok) {
            if (response.status === 401 || response.status === 500) {
                console.log("handleResponse | 401/500",{response})
            } else if (response.status===404) {
                return null
            }

            const error = (data && data.message) || response.statusText
            return Promise.reject(error)
        }

        return data
    }).catch(response => {
        console.log("handleResponse | catch",{response})
        // TODO - Handle error response
    })
}