export interface ApiResult<T> {
    status: "success" | "error";
    data?: T;
    message?: string;
    code?: number;
    error?: any;
}

export const success = <T>(data: T, message: string = "Success"): ApiResult<T> => ({
    status: "success",
    data,
    message,
    code: 200
});

export const failure = (message: string, code: number = 400, error?: any): ApiResult<null> => ({
    status: "error",
    message,
    code,
    error
});
