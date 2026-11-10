/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type AnswerResponse = {
    id: string;
    questionId: string;
    authorId: string;
    body: string;
    status: AnswerResponse.status;
    accepted: boolean;
    voteCount: number;
    createdAt: string;
    updatedAt: string;
};
export namespace AnswerResponse {
    export enum status {
        NORMAL = 'normal',
        HIDDEN = 'hidden',
    }
}

