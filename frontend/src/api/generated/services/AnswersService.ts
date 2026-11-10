/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { AnswerResponse } from '../models/AnswerResponse';
import type { CreateAnswerRequest } from '../models/CreateAnswerRequest';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class AnswersService {
    /**
     * 提交回答（US-06）
     * @param requestBody
     * @returns AnswerResponse 创建成功
     * @throws ApiError
     */
    public static submitAnswer(
        requestBody: CreateAnswerRequest,
    ): CancelablePromise<AnswerResponse> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/v1/answers',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `INV-A-02（问题已下架）/ INV-A-03（60 秒限流）`,
            },
        });
    }
    /**
     * 某问题下的回答
     * @param questionId
     * @returns AnswerResponse 回答列表
     * @throws ApiError
     */
    public static listAnswers(
        questionId: string,
    ): CancelablePromise<Array<AnswerResponse>> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/v1/answers',
            query: {
                'questionId': questionId,
            },
        });
    }
}
