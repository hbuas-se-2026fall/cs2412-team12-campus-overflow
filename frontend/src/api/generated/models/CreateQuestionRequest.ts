/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type CreateQuestionRequest = {
    askerId: string;
    title: string;
    body: string;
    /**
     * 必须来自共享词表 SK-01（INV-Q-02）
     */
    tags?: Array<string>;
};

