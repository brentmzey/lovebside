config.resolve = config.resolve || {};
config.resolve.fallback = config.resolve.fallback || {};

// Add Node.js polyfills for browser
config.resolve.fallback = {
    ...config.resolve.fallback,
    "fs": false,
    "path": false,
    "crypto": false,
};

// Ignore warnings about optional dependencies
config.ignoreWarnings = [
    /Failed to parse source map/,
    /Critical dependency: the request of a dependency is an expression/
];

// Disable performance hints for large bundles
config.performance = {
    hints: false,
    maxEntrypointSize: 512000,
    maxAssetSize: 512000
};
