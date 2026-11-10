/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { RegisterUserRequest } from '../models/RegisterUserRequest';
import type { UserResponse } from '../models/UserResponse';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class UsersService {
    /**
     * 注册账号（初始状态 pending）
     * @param requestBody
     * @returns UserResponse 注册成功
     * @throws ApiError
     */
    public static registerUser(
        requestBody: RegisterUserRequest,
    ): CancelablePromise<UserResponse> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/v1/users',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                409: `邮箱已注册`,
            },
        });
    }
    /**
     * 模拟点击邮件验证链接（pending → active）
     * @param id
     * @returns UserResponse 验证成功
     * @throws ApiError
     */
    public static verifyEmail(
        id: string,
    ): CancelablePromise<UserResponse> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/v1/users/{id}/email-verification',
            path: {
                'id': id,
            },
            errors: {
                404: `资源不存在`,
            },
        });
    }
    /**
     * 查询账号
     * @param id
     * @returns UserResponse 账号信息
     * @throws ApiError
     */
    public static getUser(
        id: string,
    ): CancelablePromise<UserResponse> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/v1/users/{id}',
            path: {
                'id': id,
            },
            errors: {
                404: `资源不存在`,
            },
        });
    }
}
