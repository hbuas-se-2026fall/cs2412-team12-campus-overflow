/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type VoteResponse = {
    targetType: VoteResponse.targetType;
    targetId: string;
    voterId: string;
    value: VoteResponse.value;
    targetVoteCount: number;
};
export namespace VoteResponse {
    export enum targetType {
        QUESTION = 'question',
        ANSWER = 'answer',
    }
    export enum value {
        UP = 'up',
        DOWN = 'down',
    }
}

