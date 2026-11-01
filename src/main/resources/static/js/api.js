// Small wrapper around fetch that adds the JWT token and turns error responses into exceptions
const Api = (() => {
    const TOKEN_KEY = "projecthub.token";
    const USER_KEY = "projecthub.user";

    function getToken() {
        return localStorage.getItem(TOKEN_KEY);
    }

    function getUser() {
        const raw = localStorage.getItem(USER_KEY);
        return raw ? JSON.parse(raw) : null;
    }

    function saveLogin(authResponse) {
        localStorage.setItem(TOKEN_KEY, authResponse.token);
        localStorage.setItem(USER_KEY, JSON.stringify(authResponse.user));
    }

    function saveUser(user) {
        localStorage.setItem(USER_KEY, JSON.stringify(user));
    }

    function logout() {
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(USER_KEY);
    }

    // The API returns either {"error": "..."} or {"field": "message", ...} for validation errors
    function errorMessage(body, status) {
        if (!body) {
            return "Request failed (" + status + ")";
        }
        if (body.error) {
            return body.error;
        }
        return Object.entries(body).map(([field, msg]) => field + ": " + msg).join(", ");
    }

    async function request(method, path, body) {
        const headers = {};
        const token = getToken();
        if (token) {
            headers["Authorization"] = "Bearer " + token;
        }
        if (body !== undefined) {
            headers["Content-Type"] = "application/json";
        }

        const response = await fetch(path, {
            method,
            headers,
            body: body !== undefined ? JSON.stringify(body) : undefined
        });

        if (response.status === 401 && token) {
            logout();
        }

        const text = await response.text();
        let data = null;
        try {
            data = text ? JSON.parse(text) : null;
        } catch (e) {
            data = null;
        }

        if (!response.ok) {
            const error = new Error(errorMessage(data, response.status));
            error.status = response.status;
            throw error;
        }
        return data;
    }

    return {
        getToken,
        getUser,
        saveLogin,
        saveUser,
        logout,
        get: (path) => request("GET", path),
        post: (path, body) => request("POST", path, body),
        put: (path, body) => request("PUT", path, body),
        del: (path) => request("DELETE", path)
    };
})();
