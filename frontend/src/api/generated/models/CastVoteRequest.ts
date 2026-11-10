/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type CastVoteRequest = {
    targetType: CastVoteRequest.targetType;
    targetId: string;
    voterId: string;
    value: CastVoteRequest.value;
};
export namespace CastVoteRequest {
    export enum targetType {
        QUESTION = 'question',
        ANSWER = 'answer',
    }
    export enum value {
        UP = 'up',
        DOWN = 'down',
    }
}

