import { Button, Card, Space, Tag, Typography, message } from 'antd';

import type { AnswerResponse } from '@/api/generated';
import { AppApiError } from '@/api/http';
import { useAcceptAnswer } from '@/features/questions/hooks';
import { formatDateTime } from '@/shared/lib/format';

import { CommentsPanel } from './CommentsPanel';
import { ReputationBadge } from './ReputationBadge';
import { VoteButtons } from './VoteButtons';

interface AnswerCardProps {
  answer: AnswerResponse;
  questionId: string;
  askerId: string;
  currentUserId: string | undefined;
}

/**
 * 单条回答：投票、采纳（仅提问者）、评论。
 */
export function AnswerCard({ answer, questionId, askerId, currentUserId }: AnswerCardProps) {
  const acceptAnswer = useAcceptAnswer(questionId);
  const canAccept = Boolean(currentUserId) && currentUserId === askerId && !answer.accepted;

  const handleAccept = (): void => {
    if (!currentUserId) {
      return;
    }
    acceptAnswer.mutate(
      { answerId: answer.id, operatorId: currentUserId },
      {
        onSuccess: () => void message.success('已采纳该回答'),
        onError: (error) => {
          void message.error(
            error instanceof AppApiError ? `${error.code}：${error.message}` : error.message,
          );
        },
      },
    );
  };

  return (
    <Card size="small" style={{ marginBottom: 12 }}>
      <Space style={{ width: '100%', justifyContent: 'space-between' }} wrap>
        <Space size={8} wrap>
          <Typography.Text strong>回答者 {answer.authorId.slice(0, 8)}…</Typography.Text>
          <ReputationBadge userId={answer.authorId} />
          {answer.accepted ? <Tag color="green">已采纳</Tag> : null}
        </Space>
        <VoteButtons targetType="answer" targetId={answer.id} voteCount={answer.voteCount} />
      </Space>

      <div className="question-body" style={{ marginTop: 8 }}>
        {answer.body}
      </div>

      <Space size={12} style={{ marginTop: 8 }} wrap>
        <Typography.Text type="secondary">{formatDateTime(answer.createdAt)}</Typography.Text>
        {canAccept ? (
          <Button
            size="small"
            type="primary"
            onClick={handleAccept}
            loading={acceptAnswer.isPending}
          >
            采纳这条回答
          </Button>
        ) : null}
      </Space>

      <div style={{ marginTop: 12 }}>
        <CommentsPanel targetType="answer" targetId={answer.id} />
      </div>
    </Card>
  );
}
