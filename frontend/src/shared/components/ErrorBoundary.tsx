import { Component } from 'react';
import type { ErrorInfo, ReactNode } from 'react';
import { Button, Result, Typography } from 'antd';

interface ErrorBoundaryProps {
  children: ReactNode;
}

interface ErrorBoundaryState {
  error: Error | null;
}

/**
 * 全局 ErrorBoundary（Lab 5 Step 3）：渲染期异常不再白屏，给出可恢复的出口。
 *
 * <p>注意：它只能捕获"渲染阶段"的错误；异步请求的错误由 TanStack Query 的 error 状态处理。</p>
 */
export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  override state: ErrorBoundaryState = { error: null };

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { error };
  }

  override componentDidCatch(error: Error, info: ErrorInfo): void {
    console.error('[ErrorBoundary]', error, info.componentStack);
  }

  private readonly handleReload = (): void => {
    this.setState({ error: null });
    window.location.reload();
  };

  override render(): ReactNode {
    const { error } = this.state;
    if (error) {
      return (
        <Result
          status="error"
          title="页面渲染出错了"
          subTitle="已经记录到控制台，可以重新加载再试一次。"
          extra={
            <Button type="primary" onClick={this.handleReload}>
              重新加载
            </Button>
          }
        >
          <Typography.Paragraph type="secondary" code>
            {error.message}
          </Typography.Paragraph>
        </Result>
      );
    }
    return this.props.children;
  }
}
