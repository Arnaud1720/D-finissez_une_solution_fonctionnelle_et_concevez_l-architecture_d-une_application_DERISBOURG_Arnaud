import { HttpClient } from '@angular/common/http';
import { Injectable, signal, computed } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  Conversation,
  Message,
  CreateConversationRequest,
  SendMessageRequest
} from '../models';

@Injectable({
  providedIn: 'root'
})
export class ConversationService {
  private apiUrl = `${environment.apiUrl}/conversations`;
  private messagesUrl = `${environment.apiUrl}/messages`;

  private conversationsSignal = signal<Conversation[]>([]);
  private currentConversationSignal = signal<Conversation | null>(null);
  private messagesSignal = signal<Message[]>([]);
  private loadingSignal = signal<boolean>(false);

  conversations = this.conversationsSignal.asReadonly();
  currentConversation = this.currentConversationSignal.asReadonly();
  messages = this.messagesSignal.asReadonly();
  loading = this.loadingSignal.asReadonly();

  totalUnreadCount = computed(() =>
    this.conversationsSignal().reduce((acc, conv) => acc + (conv.unreadCount || 0), 0)
  );

  constructor(private http: HttpClient) {}

  loadMyConversations(): Observable<Conversation[]> {
    this.loadingSignal.set(true);
    return this.http.get<Conversation[]>(`${this.apiUrl}/my`).pipe(
      tap(conversations => {
        this.conversationsSignal.set(conversations);
        this.loadingSignal.set(false);
      })
    );
  }

  loadUnassignedConversations(): Observable<Conversation[]> {
    this.loadingSignal.set(true);
    return this.http.get<Conversation[]>(`${this.apiUrl}/unassigned`).pipe(
      tap(conversations => {
        this.conversationsSignal.set(conversations);
        this.loadingSignal.set(false);
      })
    );
  }

  getConversationById(id: number): Observable<Conversation> {
    return this.http.get<Conversation>(`${this.apiUrl}/${id}`).pipe(
      tap(conversation => {
        this.currentConversationSignal.set(conversation);
      })
    );
  }

  createConversation(request: CreateConversationRequest): Observable<{ message: string; conversation: Conversation }> {
    return this.http.post<{ message: string; conversation: Conversation }>(this.apiUrl, request).pipe(
      tap(response => {
        this.conversationsSignal.update(convs => [response.conversation, ...convs]);
        this.currentConversationSignal.set(response.conversation);
      })
    );
  }

  assignConversation(conversationId: number): Observable<Conversation> {
    return this.http.patch<Conversation>(`${this.apiUrl}/${conversationId}/assign`, {}).pipe(
      tap(conversation => {
        this.conversationsSignal.update(convs =>
          convs.map(c => c.id === conversationId ? conversation : c)
        );
        this.currentConversationSignal.set(conversation);
      })
    );
  }

  closeConversation(conversationId: number): Observable<Conversation> {
    return this.http.patch<Conversation>(`${this.apiUrl}/${conversationId}/close`, {}).pipe(
      tap(conversation => {
        this.conversationsSignal.update(convs =>
          convs.map(c => c.id === conversationId ? conversation : c)
        );
        this.currentConversationSignal.set(conversation);
      })
    );
  }

  loadMessages(conversationId: number): Observable<Message[]> {
    this.loadingSignal.set(true);
    return this.http.get<Message[]>(`${this.messagesUrl}/conversation/${conversationId}`).pipe(
      tap(messages => {
        this.messagesSignal.set(messages);
        this.loadingSignal.set(false);
      })
    );
  }

  sendMessage(request: SendMessageRequest): Observable<{ message: string; data: Message }> {
    return this.http.post<{ message: string; data: Message }>(this.messagesUrl, request).pipe(
      tap(response => {
        this.messagesSignal.update(msgs => [...msgs, response.data]);
      })
    );
  }

  markAsRead(conversationId: number): Observable<{ message: string }> {
    return this.http.patch<{ message: string }>(`${this.messagesUrl}/conversation/${conversationId}/read`, {}).pipe(
      tap(() => {
        this.conversationsSignal.update(convs =>
          convs.map(c => c.id === conversationId ? { ...c, unreadCount: 0 } : c)
        );
      })
    );
  }

  clearCurrentConversation(): void {
    this.currentConversationSignal.set(null);
    this.messagesSignal.set([]);
  }

  /**
   * Ajoute un message reçu en temps réel via WebSocket/STOMP.
   * Utilisé par le MessagingComponent pour afficher les messages
   * sans rechargement de page (CC-16).
   */
  addIncomingMessage(message: Message): void {
    this.messagesSignal.update(msgs => [...msgs, message]);
  }
}
