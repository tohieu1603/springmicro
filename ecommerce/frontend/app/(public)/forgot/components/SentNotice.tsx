"use client";

export function SentNotice({ email }: { email: string }) {
  return (
    <div className="mt-6 rounded bg-emerald-50 border border-emerald-200 p-4 text-sm">
      Nếu địa chỉ <b>{email}</b> tồn tại trong hệ thống, bạn sẽ nhận được email trong vài phút.
    </div>
  );
}
