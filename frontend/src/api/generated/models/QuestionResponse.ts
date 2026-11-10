/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type QuestionResponse = {
    id: string;
    askerId: string;
    title: string;
    body: string;
    tags: Array<string>;
    status: QuestionResponse.status;
    acceptedAnswerId?: string | null;
    voteCount: number;
    createdAt: string;
    updatedAt: string;
};
export namespace QuestionResponse {
    export enum status {
        OPEN = 'open',
        SOLVED = 'solved',
        HIDDEN = 'hidden',
    }
}

