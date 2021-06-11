import { authHeader } from '@/helpers'
import axios from 'axios';
// import store from '@/store/index.js'

const base_url = "http://localhost:8081"

/**
 * Register methods
 */
export const consentService = {
    create,
    update,
    delete: _delete
}

/**
 * Create Assignment
 */
function create(experiment_id, consent) {
    const requestOptions = {
        headers: {
            'Content-Type': 'multipart/form-data',
            ...authHeader()
        }
    }

    let formData = new FormData();
    formData.append('consent', consent.file);

    // Axios was required for correct formData boundary
    return axios.post(`${base_url}/api/experiments/${experiment_id}/consent?title=${consent.title}`, formData, requestOptions).then(handleResponse)
}

/**
 * Update Assignment
 */
function update(experiment_id) {
    const requestOptions = {
        method: 'PUT',
        headers: { ...authHeader(), 'Content-Type': 'application/json' },
        // body: JSON.stringify(assignment)
    }

    return fetch(`${base_url}/api/experiments/${experiment_id}/consent`, requestOptions).then(handleResponse)
}

/**
 * Delete Assignment
 *
 * (Prefixed function name with underscore because delete is a reserved word in javascript)
 */
function _delete(experiment_id) {
    const requestOptions = {
        method: 'DELETE',
        headers: { ...authHeader(), 'Content-Type': 'application/json' },
    }

    return fetch(`${base_url}/api/experiments/${experiment_id}/consent`, requestOptions).then(handleResponse)
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