import axios from "axios";

export const validateAccessToken = async (
    token: string
): Promise<{ isValid: boolean; email?: string }> => {
    const response = await fetch(
        "https://oauth2.googleapis.com/tokeninfo?access_token=" + token
    );
    if (response.ok) {
        const data = await response.json();
        return { isValid: true, email: data.email };
    } else {
        return { isValid: false };
    }
};

export const refreshAccessToken = async (refreshToken: string) => {
    const response = await axios.post(
        `${process.env.SERVER_API_URL}/refresh-token`,
        null,
        {
            params: { refresh_token: refreshToken },
            headers: {
                "Content-Type": "application/x-www-form-urlencoded",
            },
        }
    );

    const responseData = response.data;
    const newAccessToken = responseData;

    if (newAccessToken) {
        return newAccessToken;
    }
};

export const clearAuthTokens = () => {
    localStorage.removeItem("refresh_token");
    localStorage.removeItem("access_token");
};


