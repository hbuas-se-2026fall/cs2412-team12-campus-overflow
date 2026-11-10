import { useState } from 'react';
import { Button, Empty, Input, List, Space, Typography, message } from 'antd';

import { AppApiError } from '@/api/http';
import { useComments, useDeleteComment, usePostComment } from '@/features/questions/hooks';
import { formatDateTime } from '@/shared/lib/format';
import { useSessionStore } from '@/shared/store/sessionStore';

interface CommentsPanelProps {
  targetType: 'question' | 'answer';
  targetId: string;
}

/**
 * 评论区（US-09）：任意登录用户可评论，只有作者能删自己的评论。
 */
export function CommentsPanel({ targetType, targetId }: CommentsPanelProps) {
  const user = useSessionStore((state) => state.user);
  const { data, isPending } = useComments(targetType, targetId);
  const postComment = usePostComment(targetType, targetId);
  const deleteComment = useDeleteComment(targetType, targetId);
  const [draft, setDraft] = useState('');

  const comments = data ?? [];

  const handlePost = (): void => {
    const body = draft.trim();
    if (!user || body.length === 0) {
      return;
    }
    postComment.mutate(
      { authorId: user.id, body },
      {
        onSuccess: () => {
          setDraft('');
          void message.success('评论已发布');
        },
        onError: (error) => {
          void message.error(
            error instanceof AppApiError ? `${error.code}：${error.message}` : error.message,
          );
        },
      },
    );
  };

  return (
    <Space direction="vertical" size={8} style={{ width: '100%' }}>
      <Typography.Text type="secondary">评论（{comments.length}）</Typography.Text>

      {isPending ? null : comments.length === 0 ? (
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="还没有评论" />
      ) : (
        <List
          size="small"
          dataSource={comments}
          renderItem={(item) => (
            <List.Item
              key={item.id}
              actions={
                user && user.id === item.authorId
                  ? [
                      <Button
                        key="delete"
                        type="link"
                        size="small"
                        danger
                        onClick={() => {
                          if (!user) {
                            return;
                          }
                          deleteComment.mutate(
                            { commentId: item.id, operatorId: user.id },
                            {
                              onSuccess: () => void message.success('评论已删除'),
                              onError: (error) =>
                                void message.error(
                                  error instanceof AppApiError
                                    ? `${error.code}：${error.message}`
                                    : error.message,
                                ),
                            },
                          );
                        }}
                      >
                        删除
                      </Button>,
                    ]
                  : undefined
              }
            >
              <Space direction="vertical" size={0}>
                <Typography.Text>{item.body}</Typography.Text>
                <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                  {formatDateTime(item.createdAt)}
                </Typography.Text>
              </Space>
            </List.Item>
          )}
        />
      )}

      <Space.Compact style={{ width: '100%' }}>
        <Input
          value={draft}
          onChange={(event) => setDraft(event.target.value)}
          placeholder="补充说明（≤500 字）"
          maxLength={500}
          onPressEnter={handlePost}
          disabled={!user}
        />
        <Button
          type="primary"
          onClick={handlePost}
          loading={postComment.isPending}
          disabled={!user}
        >
          评论
        </Button>
      </Space.Compact>
    </Space>
  );
}
