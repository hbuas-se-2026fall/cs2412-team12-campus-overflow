import axios from 'axios';
import { message } from 'antd';

import { ApiError as GeneratedApiError, OpenAPI } from './generated';
import type { CancelablePromise, ErrorDetail, ErrorResponse } from './generated';
import { useUiStore } from '@/shared/store/uiStore';

/**
 * 应用统一异常：把后端的 `{ code, message, traceId, details }` 规整成一个类型，
 * 页面按 `code` 分支（例如 INV-U-02 提示先验证邮箱），不再解析原始 axios 错误。
 */
export class AppApiError extends Error {
  readonly code: string;
  readonly status: number;
  readonly traceId: string | undefined;
  readonly details: ReadonlyArray<ErrorDetail>;

  constructor(args: {
    code: string;
    message: string;
    status: number;
    traceId?: string | undefined;
    details?: ReadonlyArray<ErrorDetail> | undefined;
  }) {
    super(args.message);
    this.name = 'AppApiError';
    this.code = args.code;
    this.status = args.status;
    this.traceId = args.traceId;
    this.details = args.details ?? [];
  }
}

const TOKEN_KEY = 'co.token';

function readToken(): string {
  try {
    return window.localStorage.getItem(TOKEN_KEY) ?? '';
  } catch {
    return '';
  }
}

let configured = false;

/**
 * 装配 axios 拦截器（Lab 5 Step 3）：
 *  - 请求：全局 Loading +1，注入 Token
 *  - 响应：全局 Loading -1；网络层失败统一提示
 *
 * <p>生成的客户端内部使用 axios 默认实例，所以这里挂在默认实例上即可生效。</p>
 */
export function configureHttp(): void {
  if (configured) {
    return;
  }
  configured = true;

  OpenAPI.BASE = import.meta.env.VITE_API_BASE ?? '';
  OpenAPI.TOKEN = () => Promise.resolve(readToken());

  axios.interceptors.request.use((config) => {
    useUiStore.getState().beginRequest();
    const token = readToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  });

  axios.interceptors.response.use(
    (response) => {
      useUiStore.getState().endRequest();
      return response;
    },
    (error: unknown) => {
      useUiStore.getState().endRequest();
      // 有响应体的业务错误交给页面处理；连不上后端才弹全局提示
      if (axios.isAxiosError(error) && !error.response) {
        void message.error('网络异常：请确认后端已启动（默认 http://localhost:8080）');
      }
      return Promise.reject(error);
    },
  );
}

export function setAuthToken(token: string): void {
  window.localStorage.setItem(TOKEN_KEY, token);
}

export function clearAuthToken(): void {
  window.localStorage.removeItem(TOKEN_KEY);
}

/**
 * 调用生成的服务函数，并把生成的 {@link GeneratedApiError} 翻译成 {@link AppApiError}。
 */
export async function call<T>(promise: CancelablePromise<T>): Promise<T> {
  try {
    return await promise;
  } catch (error) {
    if (error instanceof GeneratedApiError) {
      const body = error.body as ErrorResponse | undefined;
      throw new AppApiError({
        code: body?.code ?? `HTTP_${error.status}`,
        message: body?.message ?? error.message,
        status: error.status,
        traceId: body?.traceId ?? undefined,
        details: body?.details,
      });
    }
    throw error instanceof Error ? error : new Error(String(error));
  }
}
