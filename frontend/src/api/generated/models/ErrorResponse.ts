/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { ErrorDetail } from './ErrorDetail';
export type ErrorResponse = {
    /**
     * 稳定错误码（不变式编号，如 INV-Q-02）
     */
    code: string;
    message: string;
    traceId?: string | null;
    timestamp: string;
    path: string;
    details?: Array<ErrorDetail>;
};

