export interface LoginRequest {
    /** New password that will be set after successful login. */
    newPassword?: string;
    /** Password of the user which should be logged in. */
    password: string;
    /** Username of the user which should be logged in. */
    username: string;
}

/**
 * Generated login token.
 */
export interface LoginResponse {
    token: string;
}
