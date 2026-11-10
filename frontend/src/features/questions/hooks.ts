import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import {
  AnswersService,
  CastVoteRequest,
  CommentsService,
  CreateCommentRequest,
  QuestionsService,
  ReputationService,
  TagsService,
  VotesService,
} from '@/api/generated';
import type {
  AnswerResponse,
  CommentResponse,
  CreateQuestionRequest,
  QuestionPageResponse,
  QuestionResponse,
  ReputationResponse,
  VoteResponse,
} from '@/api/generated';
import { call } from '@/api/http';

/**
 * 问题相关的服务器状态（TanStack Query）。
 *
 * 查询键统一从这里生成，避免各页面各写一套字符串导致缓存失效不生效。
 */
export const questionKeys = {
  all: ['questions'] as const,
  list: (page: number, size: number, tag?: string) =>
    [...questionKeys.all, 'list', page, size, tag ?? ''] as const,
  detail: (id: string) => [...questionKeys.all, 'detail', id] as const,
};

export const answerKeys = {
  all: ['answers'] as const,
  byQuestion: (questionId: string) => [...answerKeys.all, questionId] as const,
};

export const commentKeys = {
  all: ['comments'] as const,
  byTarget: (targetType: string, targetId: string) =>
    [...commentKeys.all, targetType, targetId] as const,
};

export const reputationKeys = {
  byUser: (userId: string) => ['reputation', userId] as const,
};

export function useQuestions(page: number, size: number, tag?: string) {
  return useQuery<QuestionPageResponse>({
    queryKey: questionKeys.list(page, size, tag),
    queryFn: () => call(QuestionsService.listQuestions(page, size, tag)),
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
      await queryClient.invalidateQueries({ queryKey: questionKeys.all });
      await queryClient.invalidateQueries({ queryKey: ['reputation'] });
    },
  });
}

export function useAnswers(questionId: string | undefined) {
  return useQuery<AnswerResponse[]>({
    queryKey: answerKeys.byQuestion(questionId ?? ''),
    queryFn: () => call(AnswersService.listAnswers(questionId ?? '')),
    enabled: Boolean(questionId),
  });
}

export function useSubmitAnswer(questionId: string) {
  const queryClient = useQueryClient();
  return useMutation<AnswerResponse, Error, { authorId: string; body: string }>({
    mutationFn: ({ authorId, body }) =>
      call(AnswersService.submitAnswer({ questionId, authorId, body })),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: answerKeys.byQuestion(questionId) });
      await queryClient.invalidateQueries({ queryKey: questionKeys.all });
      await queryClient.invalidateQueries({ queryKey: ['reputation'] });
    },
  });
}

export function useAcceptAnswer(questionId: string) {
  const queryClient = useQueryClient();
  return useMutation<QuestionResponse, Error, { answerId: string; operatorId: string }>({
    mutationFn: ({ answerId, operatorId }) =>
      call(QuestionsService.acceptAnswer(questionId, { answerId, operatorId })),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: questionKeys.detail(questionId) });
      await queryClient.invalidateQueries({ queryKey: answerKeys.byQuestion(questionId) });
      await queryClient.invalidateQueries({ queryKey: ['reputation'] });
    },
  });
}

export function useVote() {
  const queryClient = useQueryClient();
  return useMutation<
    VoteResponse,
    Error,
    { targetType: 'question' | 'answer'; targetId: string; voterId: string; value: 'up' | 'down' }
  >({
    mutationFn: ({ targetType, targetId, voterId, value }) =>
      call(
        VotesService.castVote({
          targetType:
            targetType === 'question'
              ? CastVoteRequest.targetType.QUESTION
              : CastVoteRequest.targetType.ANSWER,
          targetId,
          voterId,
          value: value === 'up' ? CastVoteRequest.value.UP : CastVoteRequest.value.DOWN,
        }),
      ),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: questionKeys.all });
      await queryClient.invalidateQueries({ queryKey: answerKeys.all });
      await queryClient.invalidateQueries({ queryKey: ['reputation'] });
    },
  });
}

export function useComments(targetType: 'question' | 'answer', targetId: string | undefined) {
  return useQuery<CommentResponse[]>({
    queryKey: commentKeys.byTarget(targetType, targetId ?? ''),
    queryFn: () => call(CommentsService.listComments(targetType, targetId ?? '')),
    enabled: Boolean(targetId),
  });
}

export function usePostComment(targetType: 'question' | 'answer', targetId: string) {
  const queryClient = useQueryClient();
  return useMutation<CommentResponse, Error, { authorId: string; body: string }>({
    mutationFn: ({ authorId, body }) =>
      call(
        CommentsService.postComment({
          targetType:
            targetType === 'question'
              ? CreateCommentRequest.targetType.QUESTION
              : CreateCommentRequest.targetType.ANSWER,
          targetId,
          authorId,
          body,
        }),
      ),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: commentKeys.byTarget(targetType, targetId) });
    },
  });
}

export function useDeleteComment(targetType: 'question' | 'answer', targetId: string) {
  const queryClient = useQueryClient();
  return useMutation<void, Error, { commentId: string; operatorId: string }>({
    mutationFn: ({ commentId, operatorId }) =>
      call(CommentsService.deleteComment(commentId, operatorId)),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: commentKeys.byTarget(targetType, targetId) });
    },
  });
}

export function useTags() {
  return useQuery<string[]>({
    queryKey: ['tags'],
    queryFn: () => call(TagsService.listTags()),
    staleTime: 5 * 60_000,
  });
}

export function useReputation(userId: string | undefined) {
  return useQuery<ReputationResponse>({
    queryKey: reputationKeys.byUser(userId ?? ''),
    queryFn: () => call(ReputationService.getReputation(userId ?? '')),
    enabled: Boolean(userId),
    staleTime: 30_000,
  });
}
