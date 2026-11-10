/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { ReputationResponse } from '../models/ReputationResponse';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class ReputationService {
    /**
     * 用户声誉与明细（US-11）
     * @param id
     * @returns ReputationResponse 声誉
     * @throws ApiError
     */
    public static getReputation(
        id: string,
    ): CancelablePromise<ReputationResponse> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/v1/users/{id}/reputation',
            path: {
                'id': id,
            },
        });
    }
}
