export interface NotificationItem {
  id: string;
  userId: string;
  type: string;
  channel: string;
  title: string;
  content: string;
  status: string;
  isRead: boolean;
  errorMessage?: string;
  referenceType?: string;
  referenceId?: string;
  createdAt: string;
  sentAt?: string;
  readAt?: string;
}

export interface NotificationsPage {
  content: NotificationItem[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
