import { QueryClient } from '@tanstack/react-query';

/**
 * TanStack Query 全局配置（Lab 5 Step 3）。
 *
 * 服务器状态只走这里：组件里不再用 useState 保存"从后端取来的数据"，
 * 避免 prop drilling（见 Lab 5 常见陷阱第一条）。
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      staleTime: 30_000,
      refetchOnWindowFocus: false,
    },
    mutations: {
      retry: 0,
    },
  },
});
