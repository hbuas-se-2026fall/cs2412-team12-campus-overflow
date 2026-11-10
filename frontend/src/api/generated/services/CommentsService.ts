/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { CommentResponse } from '../models/CommentResponse';
import type { CreateCommentRequest } from '../models/CreateCommentRequest';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class CommentsService {
    /**
     * 发表评论（US-09）
     * @param requestBody
     * @returns CommentResponse 评论
     * @throws ApiError
     */
    public static postComment(
        requestBody: CreateCommentRequest,
    ): CancelablePromise<CommentResponse> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/v1/comments',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * 某目标下的评论
     * @param targetType
     * @param targetId
     * @returns CommentResponse 评论列表
     * @throws ApiError
     */
    public static listComments(
        targetType: 'question' | 'answer',
        targetId: string,
    ): CancelablePromise<Array<CommentResponse>> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/v1/comments',
            query: {
                'targetType': targetType,
                'targetId': targetId,
            },
        });
    }
    /**
     * 删除自己的评论（INV-C-03）
     * @param id
     * @param operatorId
     * @returns void
     * @throws ApiError
     */
    public static deleteComment(
        id: string,
        operatorId: string,
    ): CancelablePromise<void> {
        return __request(OpenAPI, {
            method: 'DELETE',
            url: '/api/v1/comments/{id}',
            path: {
                'id': id,
            },
            query: {
                'operatorId': operatorId,
            },
            errors: {
                403: `不是作者`,
            },
        });
    }
}
