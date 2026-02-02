// ===== USER MODELS (BR-AUTH-01, BR-AUTH-02, BR-AUTH-03) =====

export type UserRole = 'USER' | 'EMPLOYEE' | 'ADMIN';

export interface User {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  role?: UserRole;
  address?: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}

// ===== CONVERSATION / MESSAGING MODELS (BR-SUP-02) =====

export type ConversationStatus = 'OPEN' | 'CLOSED' | 'PENDING';

export interface Conversation {
  id: number;
  subject: string;
  customerId: number;
  customerName: string;
  employeeId?: number;
  employeeName?: string;
  status: ConversationStatus;
  createdAt: string;
  updatedAt: string;
  unreadCount: number;
  messages?: Message[];
}

export interface Message {
  id: number;
  conversationId: number;
  senderId: number;
  senderName: string;
  content: string;
  sentAt: string;
  isRead: boolean;
}

export interface CreateConversationRequest {
  subject: string;
}

export interface SendMessageRequest {
  conversationId: number;
  content: string;
}
