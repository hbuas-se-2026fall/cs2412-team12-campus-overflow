/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type CreateCommentRequest = {
    targetType: CreateCommentRequest.targetType;
    targetId: string;
    authorId: string;
    body: string;
};
export namespace CreateCommentRequest {
    export enum targetType {
        QUESTION = 'question',
        ANSWER = 'answer',
    }
}

