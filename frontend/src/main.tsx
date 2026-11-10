import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { QueryClientProvider } from '@tanstack/react-query';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { BrowserRouter } from 'react-router-dom';

import App from '@/App';
import { configureHttp } from '@/api/http';
import { ErrorBoundary } from '@/shared/components/ErrorBoundary';
import { queryClient } from '@/shared/lib/queryClient';

import 'antd/dist/reset.css';
import '@/index.css';

// 在渲染之前装配 axios / OpenAPI：拦截器只注册一次
configureHttp();

const rootElement = document.getElementById('root');
if (!rootElement) {
  throw new Error('找不到 #root 挂载点，index.html 可能被改坏了');
}

createRoot(rootElement).render(
  <StrictMode>
    <ConfigProvider locale={zhCN}>
      <ErrorBoundary>
        <QueryClientProvider client={queryClient}>
          <BrowserRouter>
            <App />
          </BrowserRouter>
        </QueryClientProvider>
      </ErrorBoundary>
    </ConfigProvider>
  </StrictMode>,
);
