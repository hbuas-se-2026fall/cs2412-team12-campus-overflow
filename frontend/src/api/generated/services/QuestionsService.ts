/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { AcceptAnswerRequest } from '../models/AcceptAnswerRequest';
import type { CreateQuestionRequest } from '../models/CreateQuestionRequest';
import type { QuestionPageResponse } from '../models/QuestionPageResponse';
import type { QuestionResponse } from '../models/QuestionResponse';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class QuestionsService {
    /**
     * 问题列表（按创建时间倒序分页）
     * @param page 页码，从 0 开始
     * @param size 每页条数（1–100）
     * @returns QuestionPageResponse 分页结果
     * @throws ApiError
     */
    public static listQuestions(
        page?: number,
        size: number = 20,
    ): CancelablePromise<QuestionPageResponse> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/v1/questions',
            query: {
                'page': page,
                'size': size,
            },
        });
    }
    /**
     * 发布问题
     * @param requestBody
     * @returns QuestionResponse 创建成功
     * @throws ApiError
     */
    public static createQuestion(
        requestBody: CreateQuestionRequest,
    ): CancelablePromise<QuestionResponse> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/v1/questions',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `请求校验失败 / 不变式违反（INV-Q-01、INV-Q-02、INV-U-02）`,
                404: `资源不存在`,
            },
        });
    }
    /**
     * 问题详情
     * @param id
     * @returns QuestionResponse 问题详情
     * @throws ApiError
     */
    public static getQuestion(
        id: string,
    ): CancelablePromise<QuestionResponse> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/v1/questions/{id}',
            path: {
                'id': id,
            },
            errors: {
                404: `资源不存在`,
            },
        });
    }
    /**
     * 采纳答案（仅提问者）
     * @param id
     * @param requestBody
     * @returns QuestionResponse 采纳后的问題
     * @throws ApiError
     */
    public static acceptAnswer(
        id: string,
        requestBody: AcceptAnswerRequest,
    ): CancelablePromise<QuestionResponse> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/v1/questions/{id}/acceptance',
            path: {
                'id': id,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `不变式违反（INV-Q-03 / INV-Q-04 / INV-Q-05）`,
                404: `资源不存在`,
            },
        });
    }
}
