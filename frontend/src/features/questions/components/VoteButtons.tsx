import { Button, Space, Typography, message } from 'antd';

import { AppApiError } from '@/api/http';
import { useVote } from '@/features/questions/hooks';
import { useSessionStore } from '@/shared/store/sessionStore';

interface VoteButtonsProps {
  targetType: 'question' | 'answer';
  targetId: string;
  voteCount: number;
  disabled?: boolean;
}

/**
 * 投票按钮（US-07）：同一用户重复点同一方向是幂等的，反向点即改票。
 */
export function VoteButtons({
  targetType,
  targetId,
  voteCount,
  disabled = false,
}: VoteButtonsProps) {
  const user = useSessionStore((state) => state.user);
  const vote = useVote();

  const handle = (value: 'up' | 'down'): void => {
    if (!user) {
      void message.warning('演示账号还没准备好，请稍候');
      return;
    }
    vote.mutate(
      { targetType, targetId, voterId: user.id, value },
      {
        onSuccess: (result) => {
          void message.success(`已投票，当前票数 ${result.targetVoteCount}`);
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
    <Space size={4} align="center">
      <Button
        size="small"
        aria-label="赞成"
        onClick={() => handle('up')}
        disabled={disabled || !user}
        loading={vote.isPending && vote.variables?.value === 'up'}
      >
        ▲
      </Button>
      <Typography.Text
        strong
        data-testid="vote-count"
        style={{ minWidth: 28, textAlign: 'center', display: 'inline-block' }}
      >
        {voteCount}
      </Typography.Text>
      <Button
        size="small"
        aria-label="反对"
        onClick={() => handle('down')}
        disabled={disabled || !user}
        loading={vote.isPending && vote.variables?.value === 'down'}
      >
        ▼
      </Button>
    </Space>
  );
}
