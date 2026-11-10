/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type UserResponse = {
    id: string;
    email: string;
    nickname: string;
    status: UserResponse.status;
    createdAt: string;
};
export namespace UserResponse {
    export enum status {
        PENDING = 'pending',
        ACTIVE = 'active',
        DISABLED = 'disabled',
    }
}

