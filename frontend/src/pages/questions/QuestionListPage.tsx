import { useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Alert,
  Button,
  Card,
  Empty,
  List,
  Pagination,
  Select,
  Skeleton,
  Space,
  Tag,
  Typography,
} from 'antd';

import { AppApiError } from '@/api/http';
import { useQuestions, useTags } from '@/features/questions/hooks';
import { formatDateTime } from '@/shared/lib/format';

const PAGE_SIZE = 10;

export function QuestionListPage() {
  const [page, setPage] = useState(0);
  const [tag, setTag] = useState<string | undefined>(undefined);
  const { data, isPending, isError, error, refetch } = useQuestions(page, PAGE_SIZE, tag);
  const { data: tags = [] } = useTags();

  const tagOptions = tags.map((value) => ({ value, label: value }));

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

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Space style={{ width: '100%', justifyContent: 'space-between' }} wrap>
        <Typography.Title level={3} style={{ margin: 0 }}>
          全部问题
        </Typography.Title>
        <Space>
          <Typography.Text type="secondary">按标签筛选</Typography.Text>
          <Select
            allowClear
            showSearch
            placeholder="全部标签"
            style={{ width: 200 }}
            value={tag}
            options={tagOptions}
            onChange={(value) => {
              setTag(value ?? undefined);
              setPage(0);
            }}
            data-testid="tag-filter"
          />
        </Space>
      </Space>

      {items.length === 0 ? (
        <Empty description={tag ? `「${tag}」下还没有问题` : '还没有问题，来发第一条吧'}>
          <Link to="/questions/new">
            <Button type="primary">我要提问</Button>
          </Link>
        </Empty>
      ) : (
        <>
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
                    {question.tags.map((item) => (
                      <Tag
                        key={item}
                        color={item === tag ? 'geekblue' : 'blue'}
                        style={{ cursor: 'pointer' }}
                        onClick={() => {
                          setTag(item);
                          setPage(0);
                        }}
                      >
                        {item}
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
        </>
      )}
    </Space>
  );
}
