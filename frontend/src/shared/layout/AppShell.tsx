import { useEffect } from 'react';
import { Link, Outlet, useLocation } from 'react-router-dom';
import { Alert, Button, Layout, Menu, Space, Spin, Tag, Typography } from 'antd';

import { useSessionStore } from '@/shared/store/sessionStore';
import { useUiStore } from '@/shared/store/uiStore';
import { ReputationBadge } from '@/features/questions/components/ReputationBadge';

const { Header, Sider, Content } = Layout;

/**
 * 应用外壳（Lab 5 Step 4）：Header + Sidebar + Outlet。
 *
 * <p>负责两件"全局"的事：</p>
 * <ol>
 *   <li>首次进入时准备演示账号（Sprint 2 还没有登录故事）；</li>
 *   <li>展示全局 Loading（请求计数 &gt; 0 时出现）。</li>
 * </ol>
 */
export function AppShell() {
  const location = useLocation();
  const user = useSessionStore((state) => state.user);
  const ensuring = useSessionStore((state) => state.ensuring);
  const sessionError = useSessionStore((state) => state.error);
  const ensureSession = useSessionStore((state) => state.ensureSession);
  const pendingRequests = useUiStore((state) => state.pendingRequests);

  useEffect(() => {
    void ensureSession();
  }, [ensureSession]);

  const selectedKey = location.pathname === '/questions/new' ? '/questions/new' : '/questions';

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          background: '#fff',
          paddingInline: 24,
          borderBottom: '1px solid #f0f0f0',
        }}
      >
        <Space size="middle">
          <Typography.Title level={4} style={{ margin: 0 }}>
            CampusOverflow
          </Typography.Title>
          <Tag color="blue">Lab 6 · Sprint 3</Tag>
        </Space>

        <Space size="middle">
          {pendingRequests > 0 ? <Spin size="small" /> : null}
          {user ? (
            <>
              <Typography.Text type="secondary">当前用户：{user.nickname}</Typography.Text>
              <ReputationBadge userId={user.id} />
            </>
          ) : ensuring ? (
            <Typography.Text type="secondary">正在准备演示账号…</Typography.Text>
          ) : null}
          <Link to="/questions/new">
            <Button type="primary">我要提问</Button>
          </Link>
        </Space>
      </Header>

      <Layout>
        <Sider width={200} theme="light">
          <Menu
            mode="inline"
            selectedKeys={[selectedKey]}
            style={{ height: '100%', borderInlineEnd: 0 }}
            items={[
              { key: '/questions', label: <Link to="/questions">全部问题</Link> },
              { key: '/questions/new', label: <Link to="/questions/new">我要提问</Link> },
            ]}
          />
        </Sider>

        <Content style={{ padding: 24 }}>
          {sessionError ? (
            <Alert
              type="warning"
              showIcon
              style={{ marginBottom: 16 }}
              message="演示账号准备失败"
              description={sessionError}
            />
          ) : null}
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
