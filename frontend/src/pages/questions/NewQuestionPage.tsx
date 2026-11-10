import { useNavigate } from 'react-router-dom';
import { Button, Card, Form, Input, Select, Space, Typography, message } from 'antd';

import { AppApiError } from '@/api/http';
import { useCreateQuestion, useTags } from '@/features/questions/hooks';
import { TAG_OPTIONS } from '@/shared/constants/tags';
import { useSessionStore } from '@/shared/store/sessionStore';

interface QuestionFormValues {
  title: string;
  body: string;
  tags?: string[];
}

export function NewQuestionPage() {
  const navigate = useNavigate();
  const [form] = Form.useForm<QuestionFormValues>();
  const user = useSessionStore((state) => state.user);
  const ensuring = useSessionStore((state) => state.ensuring);
  const createQuestion = useCreateQuestion();
  // 标签从后端词表拉取（US-10）；拉取失败时退回内置常量，保证离线也能演示
  const { data: apiTags = [] } = useTags();
  const tagOptions = (apiTags.length > 0 ? apiTags : TAG_OPTIONS).map((value) => ({
    value,
    label: value,
  }));

  const canSubmit = Boolean(user) && !ensuring;

  const handleFinish = (values: QuestionFormValues): void => {
    if (!user) {
      void message.warning('演示账号还没准备好，请稍候再试');
      return;
    }
    createQuestion.mutate(
      {
        askerId: user.id,
        title: values.title,
        body: values.body,
        tags: values.tags ?? [],
      },
      {
        onSuccess: (created) => {
          void message.success('发布成功');
          navigate(`/questions/${created.id}`);
        },
        onError: (error) => {
          // 后端不变式错误码直接展示，便于课堂演示 INV-Q-02 / INV-U-02
          void message.error(
            error instanceof AppApiError ? `${error.code}：${error.message}` : error.message,
          );
        },
      },
    );
  };

  return (
    <Card title="发布问题" style={{ maxWidth: 880 }}>
      <Typography.Paragraph type="secondary">
        标题 5–150 字；标签来自共享词表 SK-01（最多 5 个）。当前以
        <Typography.Text strong> {user ? user.nickname : '演示账号'} </Typography.Text>
        的身份提问。
      </Typography.Paragraph>

      <Form
        form={form}
        layout="vertical"
        onFinish={handleFinish}
        disabled={!canSubmit}
        initialValues={{ tags: [] }}
      >
        <Form.Item
          name="title"
          label="标题"
          rules={[
            { required: true, message: '请输入标题' },
            { min: 5, max: 150, message: '标题长度需在 5–150 字之间' },
          ]}
        >
          <Input placeholder="用一句话说清你的问题" maxLength={150} showCount />
        </Form.Item>

        <Form.Item
          name="body"
          label="正文（Markdown）"
          rules={[
            { required: true, message: '请输入正文' },
            { max: 20000, message: '正文不能超过 20000 字' },
          ]}
        >
          <Input.TextArea
            rows={10}
            placeholder="补充环境、报错信息、已经尝试过的方案"
            maxLength={20000}
            showCount
          />
        </Form.Item>

        <Form.Item name="tags" label="标签（最多 5 个，来自共享词表）">
          <Select mode="multiple" maxCount={5} placeholder="选择标签" options={tagOptions} />
        </Form.Item>

        <Form.Item>
          <Space>
            <Button type="primary" htmlType="submit" loading={createQuestion.isPending}>
              发布
            </Button>
            <Button onClick={() => navigate(-1)}>取消</Button>
          </Space>
        </Form.Item>
      </Form>
    </Card>
  );
}
