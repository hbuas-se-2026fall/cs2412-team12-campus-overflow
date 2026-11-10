/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { CastVoteRequest } from '../models/CastVoteRequest';
import type { VoteResponse } from '../models/VoteResponse';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class VotesService {
    /**
     * 投票 / 改票（US-07，幂等）
     * @param requestBody
     * @returns VoteResponse 投票结果（含目标最新票数）
     * @throws ApiError
     */
    public static castVote(
        requestBody: CastVoteRequest,
    ): CancelablePromise<VoteResponse> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/v1/votes',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `INV-V-02（不能自投）/ INV-V-04（已下架）`,
            },
        });
    }
    /**
     * 撤票（幂等）
     * @param targetType
     * @param targetId
     * @param voterId
     * @returns void
     * @throws ApiError
     */
    public static retractVote(
        targetType: 'question' | 'answer',
        targetId: string,
        voterId: string,
    ): CancelablePromise<void> {
        return __request(OpenAPI, {
            method: 'DELETE',
            url: '/api/v1/votes',
            query: {
                'targetType': targetType,
                'targetId': targetId,
                'voterId': voterId,
            },
        });
    }
}
