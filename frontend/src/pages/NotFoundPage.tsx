import { Link } from 'react-router-dom';
import { Button, Result } from 'antd';

export function NotFoundPage() {
  return (
    <Result
      status="404"
      title="404"
      subTitle="这个页面不存在，可能链接写错了。"
      extra={
        <Link to="/questions">
          <Button type="primary">回到问题列表</Button>
        </Link>
      }
    />
  );
}
