import { Tag } from 'antd';

import { useReputation } from '@/features/questions/hooks';

/**
 * 声誉徽章（US-11）：按用户 ID 拉取声誉并缓存 30 秒。
 */
export function ReputationBadge({ userId }: { userId: string | undefined }) {
  const { data, isPending } = useReputation(userId);

  if (!userId) {
    return null;
  }
  if (isPending) {
    return <Tag>声誉 …</Tag>;
  }
  if (!data) {
    return null;
  }
  return <Tag color="gold">声誉 {data.score}</Tag>;
}
