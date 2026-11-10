/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type CommentResponse = {
    id: string;
    targetType: CommentResponse.targetType;
    targetId: string;
    authorId: string;
    body: string;
    createdAt: string;
};
export namespace CommentResponse {
    export enum targetType {
        QUESTION = 'question',
        ANSWER = 'answer',
    }
}

