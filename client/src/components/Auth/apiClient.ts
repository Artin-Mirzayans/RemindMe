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
            originalRequest._retry = true; // Prevent infinite retry loop

            const refreshToken = localStorage.getItem('refresh_token');
            if (refreshToken) {
                try {
                    // Use the refreshAccessToken helper function to get a new access token
                    const newAccessToken = await refreshAccessToken(refreshToken);

                    if (newAccessToken) {
                        // Update the local storage and set the new access token
                        localStorage.setItem('access_token', newAccessToken);

                        // Update the original request's Authorization header with the new token
                        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;

                        try {
                            // Retry the original request
                            return await apiClient(originalRequest);
                        } catch (retryError) {
                            clearAuthTokens();
                            window.location.href = '/login';
                            return Promise.reject(retryError);
                        }
                    }
                } catch (refreshError) {
                    clearAuthTokens();
                    window.location.href = '/login';
                    return Promise.reject(refreshError);
                }
            }

            // If the refresh fails or there's no refresh token, clear tokens and redirect
            clearAuthTokens();
            window.location.href = '/login';
        }

        return Promise.reject(error);
    }
);

export default apiClient;
