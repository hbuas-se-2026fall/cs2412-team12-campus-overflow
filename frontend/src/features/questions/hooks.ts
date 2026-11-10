import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { QuestionsService } from '@/api/generated';
import type {
  CreateQuestionRequest,
  QuestionPageResponse,
  QuestionResponse,
} from '@/api/generated';
import { call } from '@/api/http';

/**
 * 问题相关的服务器状态（TanStack Query）。
 *
 * 查询键统一从这里生成，避免各页面各写一套字符串导致缓存失效不生效。
 */
export const questionKeys = {
  all: ['questions'] as const,
  list: (page: number, size: number) => [...questionKeys.all, 'list', page, size] as const,
  detail: (id: string) => [...questionKeys.all, 'detail', id] as const,
};

export function useQuestions(page: number, size: number) {
  return useQuery<QuestionPageResponse>({
    queryKey: questionKeys.list(page, size),
    queryFn: () => call(QuestionsService.listQuestions(page, size)),
  });
}

export function useQuestion(id: string | undefined) {
  return useQuery<QuestionResponse>({
    queryKey: questionKeys.detail(id ?? ''),
    queryFn: () => call(QuestionsService.getQuestion(id ?? '')),
    enabled: Boolean(id),
  });
}

export function useCreateQuestion() {
  const queryClient = useQueryClient();
  return useMutation<QuestionResponse, Error, CreateQuestionRequest>({
    mutationFn: (request) => call(QuestionsService.createQuestion(request)),
    onSuccess: async () => {
      // 新问题要出现在列表里：失效所有 questions 前缀的缓存
      await queryClient.invalidateQueries({ queryKey: questionKeys.all });
    },
  });
}
