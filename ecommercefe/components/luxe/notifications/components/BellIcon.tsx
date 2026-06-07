"use client";

interface BellIconProps {
  unread: number;
  onClick: () => void;
}

export function BellIcon({ unread, onClick }: BellIconProps) {
  return (
    <button type="button" className="lux-bell" onClick={onClick} aria-label="Thông báo">
      <svg viewBox="0 0 24 24" width={22} height={22} stroke="currentColor" fill="none" strokeWidth={1.4}>
        <path d="M6 8a6 6 0 1112 0c0 6 3 7 3 7H3s3-1 3-7" />
        <path d="M10 21a2 2 0 004 0" />
      </svg>
      {unread > 0 && (
        <span className="lux-bell-badge">{unread > 99 ? "99+" : unread}</span>
      )}
    </button>
  );
}
