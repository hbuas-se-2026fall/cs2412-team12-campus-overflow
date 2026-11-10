import { create } from 'zustand';
import { persist } from 'zustand/middleware';

import { UsersService } from '@/api/generated';
import { call } from '@/api/http';

export interface SessionUser {
  id: string;
  nickname: string;
  email: string;
}

interface SessionState {
  user: SessionUser | null;
  ensuring: boolean;
  error: string | null;
  /** 没有登录态时，自动注册并验证一个演示账号（Sprint 2 还没有 US-02 登录）。 */
  ensureSession: () => Promise<SessionUser | null>;
  clear: () => void;
}

function randomSuffix(): string {
  return Math.random().toString(36).slice(2, 8);
}

export const useSessionStore = create<SessionState>()(
  persist(
    (set, get) => ({
      user: null,
      ensuring: false,
      error: null,
      ensureSession: async () => {
        const existing = get().user;
        if (existing) {
          return existing;
        }
        if (get().ensuring) {
          return null;
        }
        set({ ensuring: true, error: null });
        try {
          const suffix = randomSuffix();
          const created = await call(
            UsersService.registerUser({
              email: `demo-${suffix}@hbuas.edu.cn`,
              nickname: `演示同学-${suffix}`,
              password: 'abc12345',
            }),
          );
          const verified = await call(UsersService.verifyEmail(created.id));
          const user: SessionUser = {
            id: verified.id,
            nickname: verified.nickname,
            email: verified.email,
          };
          set({ user, ensuring: false });
          return user;
        } catch (error) {
          const text = error instanceof Error ? error.message : '创建演示账号失败';
          set({ ensuring: false, error: text });
          return null;
        }
      },
      clear: () => set({ user: null }),
    }),
    {
      name: 'campus-overflow-session',
      partialize: (state) => ({ user: state.user }),
    },
  ),
);
