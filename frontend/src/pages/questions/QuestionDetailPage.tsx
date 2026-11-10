import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Alert, Button, Card, Input, Skeleton, Space, Tag, Typography, message } from 'antd';

import { AppApiError } from '@/api/http';
import { AnswerCard } from '@/features/questions/components/AnswerCard';
import { CommentsPanel } from '@/features/questions/components/CommentsPanel';
import { ReputationBadge } from '@/features/questions/components/ReputationBadge';
import { VoteButtons } from '@/features/questions/components/VoteButtons';
import { useAnswers, useQuestion, useSubmitAnswer } from '@/features/questions/hooks';
import { formatDateTime } from '@/shared/lib/format';
import { useSessionStore } from '@/shared/store/sessionStore';

export function QuestionDetailPage() {
  const { id } = useParams<{ id: string }>();
  const user = useSessionStore((state) => state.user);
  const questionQuery = useQuestion(id);
  const answersQuery = useAnswers(id);
  const submitAnswer = useSubmitAnswer(id ?? '');
  const [draft, setDraft] = useState('');

  if (!id) {
    return <Alert type="warning" showIcon message="缺少问题 ID" />;
  }

  if (questionQuery.isPending) {
    return <Skeleton active paragraph={{ rows: 8 }} />;
  }

  if (questionQuery.isError) {
    const notFound =
      questionQuery.error instanceof AppApiError && questionQuery.error.status === 404;
    return (
      <Alert
        type={notFound ? 'warning' : 'error'}
        showIcon
        message={notFound ? '问题不存在或已下架' : '加载问题失败'}
        description={questionQuery.error.message}
        action={<Button onClick={() => void questionQuery.refetch()}>重试</Button>}
      />
    );
  }

  const question = questionQuery.data;
  if (!question) {
    return null;
  }
  const answers = answersQuery.data ?? [];

  const handleSubmit = (): void => {
    const body = draft.trim();
    if (!user || body.length === 0) {
      return;
    }
    submitAnswer.mutate(
      { authorId: user.id, body },
      {
        onSuccess: () => {
          setDraft('');
          void message.success('回答已发布');
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
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Link to="/questions">← 返回列表</Link>

      <Card>
        <Space style={{ width: '100%', justifyContent: 'space-between' }} wrap>
          <Typography.Title level={3} style={{ margin: 0 }}>
            {question.title}
          </Typography.Title>
          <VoteButtons
            targetType="question"
            targetId={question.id}
            voteCount={question.voteCount}
          />
        </Space>

        <Space size="small" wrap style={{ margin: '8px 0 16px' }}>
          {question.tags.map((tag) => (
            <Tag key={tag} color="blue">
              {tag}
            </Tag>
          ))}
          <ReputationBadge userId={question.askerId} />
          <Tag color={question.status === 'open' ? 'default' : 'green'}>
            {question.status === 'open'
              ? '待解决'
              : question.status === 'solved'
                ? '已解决'
                : '已下架'}
          </Tag>
        </Space>

        <div className="question-body">{question.body}</div>

        <Typography.Text type="secondary" style={{ display: 'block', marginTop: 12 }}>
          提问者 {question.askerId.slice(0, 8)}… · {formatDateTime(question.createdAt)}
        </Typography.Text>

        <div style={{ marginTop: 16 }}>
          <CommentsPanel targetType="question" targetId={question.id} />
        </div>
      </Card>

      <Card title={`回答（${answers.length}）`}>
        {answers.length === 0 ? (
          <Typography.Text type="secondary">还没有回答，来写第一条。</Typography.Text>
        ) : (
          answers.map((answer) => (
            <AnswerCard
              key={answer.id}
              answer={answer}
              questionId={question.id}
              askerId={question.askerId}
              currentUserId={user?.id}
            />
          ))
        )}

        <Space direction="vertical" size={8} style={{ width: '100%', marginTop: 16 }}>
          <Typography.Text type="secondary">写回答（Markdown，≤10000 字）</Typography.Text>
          <Input.TextArea
            rows={5}
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
            placeholder="写下你的回答（Markdown）"
            maxLength={10000}
            showCount
            disabled={!user}
          />
          <Button
            type="primary"
            onClick={handleSubmit}
            loading={submitAnswer.isPending}
            disabled={!user}
          >
            提交回答
          </Button>
        </Space>
      </Card>
    </Space>
  );
}
