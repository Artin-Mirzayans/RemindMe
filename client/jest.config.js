// Pin the timezone so date/time formatting assertions are deterministic no matter how jest is
// invoked (npm test sets this too, but a bare `jest` / IDE run wouldn't).
process.env.TZ = "UTC";

module.exports = {
    testEnvironment: "jsdom",
    setupFilesAfterEnv: ["<rootDir>/jest.setup.js"],
    moduleNameMapper: {
        "\\.(css|less|scss)$": "identity-obj-proxy",
        "\\.(png|jpe?g|gif|svg|ico)$": "<rootDir>/__mocks__/fileMock.js",
    },
    testMatch: ["<rootDir>/src/**/*.test.{js,jsx,ts,tsx}"],
    clearMocks: true,
};
