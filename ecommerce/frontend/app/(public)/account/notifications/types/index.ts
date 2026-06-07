export interface Notification {
  id: string;
  title: string;
  content: string;
  referenceType?: string;
  referenceId?: string;
  read: boolean;
  createdAt: string;
}
