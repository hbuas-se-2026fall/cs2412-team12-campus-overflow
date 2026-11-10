/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type RegisterUserRequest = {
    email: string;
    nickname: string;
    /**
     * 至少 8 位且同时包含字母与数字（INV-U-03）
     */
    password: string;
};

