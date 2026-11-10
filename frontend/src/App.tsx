import { Navigate, Route, Routes } from 'react-router-dom';

import { AppShell } from '@/shared/layout/AppShell';
import { NotFoundPage } from '@/pages/NotFoundPage';
import { NewQuestionPage } from '@/pages/questions/NewQuestionPage';
import { QuestionDetailPage } from '@/pages/questions/QuestionDetailPage';
import { QuestionListPage } from '@/pages/questions/QuestionListPage';

/**
 * 路由表（Lab 5 Step 4）：
 *  - /questions       问题列表
 *  - /questions/new   发布问题
 *  - /questions/:id   问题详情
 */
export default function App() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route index element={<Navigate to="/questions" replace />} />
        <Route path="/questions" element={<QuestionListPage />} />
        <Route path="/questions/new" element={<NewQuestionPage />} />
        <Route path="/questions/:id" element={<QuestionDetailPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
