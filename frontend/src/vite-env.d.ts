/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** 后端地址前缀；默认空串，走 Vite 的 /api 代理 */
  readonly VITE_API_BASE?: string;
  /** 开发代理目标，默认 http://localhost:8080 */
  readonly VITE_PROXY_TARGET?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
