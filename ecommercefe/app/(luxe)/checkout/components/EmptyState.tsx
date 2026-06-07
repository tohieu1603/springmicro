"use client";

interface EmptyStateProps {
  phase: "loading" | "auth" | "empty";
}

export function EmptyState({ phase }: EmptyStateProps) {
  if (phase === "loading") {
    return <div style={{ padding: 40, color: "#666" }}>Đang tải giỏ hàng…</div>;
  }
  if (phase === "auth") {
    return (
      <div style={{ padding: 40, color: "#666" }}>
        Vui lòng <a href="/login" style={{ textDecoration: "underline" }}>đăng nhập</a> để xem giỏ hàng.
      </div>
    );
  }
  return (
    <div style={{ padding: 40, color: "#666" }}>
      Giỏ hàng trống. <a href="/shop" style={{ textDecoration: "underline" }}>Khám phá bộ sưu tập</a>.
    </div>
  );
}
