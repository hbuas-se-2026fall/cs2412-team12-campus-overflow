import { useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Alert,
  Button,
  Card,
  Empty,
  List,
  Pagination,
  Skeleton,
  Space,
  Tag,
  Typography,
} from 'antd';

import { AppApiError } from '@/api/http';
import { useQuestions } from '@/features/questions/hooks';
import { formatDateTime } from '@/shared/lib/format';

const PAGE_SIZE = 10;

export function QuestionListPage() {
  const [page, setPage] = useState(0);
  const { data, isPending, isError, error, refetch } = useQuestions(page, PAGE_SIZE);

  if (isPending) {
    return <Skeleton active paragraph={{ rows: 8 }} />;
  }

  if (isError) {
    return (
      <Alert
        type="error"
        showIcon
        message="加载问题列表失败"
        description={
          error instanceof AppApiError ? `${error.code}：${error.message}` : error.message
        }
        action={<Button onClick={() => void refetch()}>重试</Button>}
      />
    );
  }

  const items = data?.items ?? [];
  if (items.length === 0) {
    return (
      <Empty description="还没有问题，来发第一条吧">
        <Link to="/questions/new">
          <Button type="primary">我要提问</Button>
        </Link>
      </Empty>
    );
  }

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Typography.Title level={3} style={{ margin: 0 }}>
        全部问题
      </Typography.Title>

      <List
        dataSource={items}
        renderItem={(question) => (
          <List.Item key={question.id}>
            <Card hoverable style={{ width: '100%' }}>
              <Link to={`/questions/${question.id}`}>
                <Typography.Title level={5} style={{ marginTop: 0, marginBottom: 8 }}>
                  {question.title}
                </Typography.Title>
              </Link>

              <Space size="small" wrap style={{ marginBottom: 8 }}>
                {question.tags.map((tag) => (
                  <Tag key={tag} color="blue">
                    {tag}
                  </Tag>
                ))}
              </Space>

              <Typography.Paragraph
                type="secondary"
                ellipsis={{ rows: 2 }}
                style={{ marginBottom: 8 }}
              >
                {question.body}
              </Typography.Paragraph>

              <Typography.Text type="secondary">
                票数 {question.voteCount} · {formatDateTime(question.createdAt)}
              </Typography.Text>
            </Card>
          </List.Item>
        )}
      />

      <Pagination
        current={page + 1}
        pageSize={PAGE_SIZE}
        total={data?.total ?? 0}
        showSizeChanger={false}
        onChange={(nextPage) => setPage(nextPage - 1)}
      />
    </Space>
  );
}
