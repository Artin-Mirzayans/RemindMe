import { clearAuthTokens, refreshAccessToken, validateAccessToken, fetchUserData } from "./authUtils";

const CLIENT_ID = "344473433718-a1vsjaaules6d3i934gmnbjs72nep4va.apps.googleusercontent.com";
const REDIRECT_URI = process.env.OAUTH_REDIRECT_URI;
const authUrl = `https://accounts.google.com/o/oauth2/v2/auth?client_id=${CLIENT_ID}&redirect_uri=${REDIRECT_URI}&response_type=code&scope=email&access_type=offline&prompt=consent`;

// eslint-disable-next-line @typescript-eslint/no-unsafe-function-type
export const handleLogin = async (setUser: Function, navigate: Function) => {
    const accessToken = localStorage.getItem("access_token");
    const refreshToken = localStorage.getItem("refresh_token");

    try {
        if (accessToken) {
            const { isValid, email } = await validateAccessToken(accessToken);
            if (isValid && email) {
                const userData = await fetchUserData(email);
                setUser(userData);
                navigate("/");
            } else {
                const newAccessToken = await refreshAccessToken(refreshToken);
                if (newAccessToken) {
                    const { isValid, email } = await validateAccessToken(newAccessToken);
                    if (isValid && email) {
                        const userData = await fetchUserData(email);
                        setUser(userData);
                        localStorage.setItem("access_token", newAccessToken);
                        navigate("/");
                    } else {
                        clearAuthTokens();
                        window.location.href = authUrl;
                    }
                } else {
                    clearAuthTokens();
                    window.location.href = authUrl;
                }
            }
        } else if (refreshToken) {
            const newAccessToken = await refreshAccessToken(refreshToken);
            if (newAccessToken) {
                const { isValid, email } = await validateAccessToken(newAccessToken);
                if (isValid && email) {
                    const userData = await fetchUserData(email);
                    setUser(userData);
                    localStorage.setItem("access_token", newAccessToken);
                    navigate("/");
                } else {
                    clearAuthTokens();
                    window.location.href = authUrl;
                }
            } else {
                clearAuthTokens();
                window.location.href = authUrl;
            }
        } else {
            window.location.href = authUrl;
        }
    } catch (error) {
        console.error("Error during login process:", error);
        clearAuthTokens();
        window.location.href = authUrl;
    }
};
