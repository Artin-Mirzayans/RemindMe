import axios from 'axios';
import { clearAuthTokens, refreshAccessToken } from './authUtils';

// Create an Axios instance
const apiClient = axios.create({
    baseURL: process.env.SERVER_API_URL, // Set your base URL from environment variables
    headers: {
        'Content-Type': 'application/json',
    },
});

// Request interceptor to add the access token to request headers
apiClient.interceptors.request.use(
    (config) => {
        const accessToken = localStorage.getItem('access_token');
        if (accessToken) {
            config.headers.Authorization = `Bearer ${accessToken}`;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// Response interceptor to handle errors, such as token expiration
apiClient.interceptors.response.use(
    (response) => {
        return response;
    },
    async (error) => {
        const originalRequest = error.config;

        // Check if the error status is 401 and we haven't already retried
        if (error.response?.status === 401 && !originalRequest._retry) {
            const refreshToken = localStorage.getItem('refresh_token');

            // No refresh token means this was an anonymous request to a protected
            // endpoint (expected for a signed-out visitor) - let the caller's own
            // gating handle it instead of treating it as an expired session.
            if (!refreshToken) {
                return Promise.reject(error);
            }

            originalRequest._retry = true; // Prevent infinite retry loop

            try {
                // Use the refreshAccessToken helper function to get a new access token
                const newAccessToken = await refreshAccessToken(refreshToken);

                if (newAccessToken) {
                    // Update the local storage and set the new access token
                    localStorage.setItem('access_token', newAccessToken);

                    // Update the original request's Authorization header with the new token
                    originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;

                    // Retry the original request
                    return await apiClient(originalRequest);
                }
            } catch {
                // fall through to session cleanup below
            }

            // The session is dead - clear it and let UserContext know, rather than
            // forcing a redirect. Whichever page is on screen re-renders into its
            // own signed-out state.
            clearAuthTokens();
            window.dispatchEvent(new Event('auth:logout'));
        }

        return Promise.reject(error);
    }
);

export default apiClient;
