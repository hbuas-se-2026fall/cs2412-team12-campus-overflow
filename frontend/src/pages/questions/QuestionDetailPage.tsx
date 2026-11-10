import { Link, useParams } from 'react-router-dom';
import { Alert, Button, Card, Descriptions, Skeleton, Space, Tag, Typography } from 'antd';

import { AppApiError } from '@/api/http';
import { useQuestion } from '@/features/questions/hooks';
import { formatDateTime } from '@/shared/lib/format';

export function QuestionDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { data, isPending, isError, error, refetch } = useQuestion(id);

  if (!id) {
    return <Alert type="warning" showIcon message="缺少问题 ID" />;
  }

  if (isPending) {
    return <Skeleton active paragraph={{ rows: 8 }} />;
  }

  if (isError) {
    const notFound = error instanceof AppApiError && error.status === 404;
    return (
      <Alert
        type={notFound ? 'warning' : 'error'}
        showIcon
        message={notFound ? '问题不存在或已下架' : '加载问题失败'}
        description={error.message}
        action={<Button onClick={() => void refetch()}>重试</Button>}
      />
    );
  }

  if (!data) {
    return null;
  }

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Link to="/questions">← 返回列表</Link>

      <Card>
        <Typography.Title level={3} style={{ marginTop: 0 }}>
          {data.title}
        </Typography.Title>

        <Space size="small" wrap style={{ marginBottom: 16 }}>
          {data.tags.map((tag) => (
            <Tag key={tag} color="blue">
              {tag}
            </Tag>
          ))}
        </Space>

        <div className="question-body">{data.body}</div>

        <Descriptions
          size="small"
          column={2}
          style={{ marginTop: 24 }}
          items={[
            { key: 'status', label: '状态', children: data.status },
            { key: 'votes', label: '票数', children: data.voteCount },
            { key: 'created', label: '发布时间', children: formatDateTime(data.createdAt) },
            { key: 'updated', label: '最后更新', children: formatDateTime(data.updatedAt) },
          ]}
        />
      </Card>
    </Space>
  );
}
