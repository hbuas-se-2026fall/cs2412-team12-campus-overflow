import { create } from 'zustand';

interface UiState {
  /** 进行中的请求数：拦截器 +1 / -1，0 表示空闲。 */
  pendingRequests: number;
  beginRequest: () => void;
  endRequest: () => void;
}

/**
 * 纯客户端状态（Zustand）：全局 Loading。
 *
 * <p>与 TanStack Query 的分工：服务器数据/缓存归 Query，跨组件的 UI 状态归 Zustand。</p>
 */
export const useUiStore = create<UiState>((set) => ({
  pendingRequests: 0,
  beginRequest: () => set((state) => ({ pendingRequests: state.pendingRequests + 1 })),
  endRequest: () => set((state) => ({ pendingRequests: Math.max(0, state.pendingRequests - 1) })),
}));
