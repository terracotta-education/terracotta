import { authHeader } from '../helpers';

/**
 * Register methods
 */
export const userService = {
    login,
    getAll,
    getById,
    update,
    delete: _delete
};

/**
 * Log in User
 */
function login(username, password) {
    const requestOptions = {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        // TODO - pass correct fields
        body: JSON.stringify({ username, password })
    };

    return fetch(`/api/users/authenticate`, requestOptions)
    .then(handleResponse)
    .then(response => {
        if (response  && response.user && response.user.api_token) {
            return response
        } else {
            return {
                status: 'failure',
                message: (response && response.message) ? response.message : 'Unknown error, please contact support.'
            }
        }
    }).catch(error => {
        return {
            status: 'failure',
            error: error
        }
    })
}

/**
 * Get all Users
 */
function getAll() {
    const requestOptions = {
        method: 'GET',
        headers: authHeader()
    };

    return fetch(`/api/users`, requestOptions).then(handleResponse);
}

/**
 * Get single User
 */
function getById(id) {
    const requestOptions = {
        method: 'GET',
        headers: authHeader()
    };

    return fetch(`/api/users/${id}`, requestOptions).then(handleResponse);
}

/**
 * Update User
 */
function update(user) {
    const requestOptions = {
        method: 'PUT',
        headers: { ...authHeader(), 'Content-Type': 'application/json' },
        body: JSON.stringify(user)
    };

    return fetch(`/api/users/${user.id}`, requestOptions).then(handleResponse);
}

/**
 * Delete User
 *
 * (Prefixed function name with underscore because delete is a reserved word in javascript)
 */
function _delete(id) {
    const requestOptions = {
        method: 'DELETE',
        headers: authHeader()
    };

    return fetch(`/api/users/${id}`, requestOptions).then(handleResponse);
}

/**
 * Handle API response
 */
function handleResponse(response) {
    return response.text().then(text => {
        const data = text && JSON.parse(text);
        if (!response || !response.ok) {
            if (response.status === 401) {
                return {
                    status: 'failure',
                    message: 'Invalid Credentials'
                }
            }

            const error = (data && data.message) || response.statusText;
            return Promise.reject(error);
        }

        return data;
    }).catch(response => {
        // logout();
        console.log(response)
    });
}