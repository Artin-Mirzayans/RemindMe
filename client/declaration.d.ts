declare module '*.png' {
    const value: string;
    export default value;
}

declare module '*.jpg' {
    const value: string;
    export default value;
}

declare const process: {
    env: {
        SERVER_API_URL: string,
        OAUTH_REDIRECT_URI: string
    }
}