"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.failure = exports.success = void 0;
const success = (data, message = "Success") => ({
    status: "success",
    data,
    message,
    code: 200
});
exports.success = success;
const failure = (message, code = 400, error) => ({
    status: "error",
    message,
    code,
    error
});
exports.failure = failure;
