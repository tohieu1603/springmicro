"use client";

import { Result, Button } from "antd";

export default function AdminError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <Result
      status="error"
      title="Có lỗi xảy ra"
      subTitle={error.digest ? `Mã sự cố: ${error.digest}` : error.message}
      extra={[
        <Button key="retry" type="primary" onClick={reset}>
          Thử lại
        </Button>,
        <Button key="home" href="/admin">Về Dashboard</Button>,
      ]}
    />
  );
}
