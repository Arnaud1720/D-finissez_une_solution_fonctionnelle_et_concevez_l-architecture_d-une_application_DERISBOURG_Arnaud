import { Component, OnInit, OnDestroy, signal, computed, ViewChild, ElementRef, AfterViewChecked, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ConversationService, AuthService } from '../../core/services';
import { WebSocketService } from '../../core/services/websocket.service';
import { Conversation, Message, UserRole } from '../../core/models';

/**
 * Composant de messagerie en temps réel.
 * Conforme BR-SUP-02 : Chat temps réel via WebSocket/STOMP.
 *
 * Flow :
 * 1. Connexion WebSocket au montage du composant
 * 2. Subscription STOMP au topic de la conversation sélectionnée
 * 3. Messages reçus en temps réel (< 2 secondes - CC-17)
 * 4. Messages envoyés via STOMP (pas de rechargement - CC-16)
 * 5. Persistance via le backend (CC-18)
 */
@Component({
  selector: 'app-messaging',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './messaging.component.html',
  styleUrl: './messaging.component.css'
})
export class MessagingComponent implements OnInit, OnDestroy, AfterViewChecked {
  @ViewChild('messagesContainer') private messagesContainer!: ElementRef;

  private conversationService = inject(ConversationService);
  private authService = inject(AuthService);
  private wsService = inject(WebSocketService);

  newSubject = signal<string>('');
  newMessage = signal<string>('');
  showNewConversationForm = signal<boolean>(false);
  selectedConversationId = signal<number | null>(null);

  private shouldScrollToBottom = false;

  currentUserId = computed(() => this.authService.currentUser()?.id);
  userRole = computed<UserRole | undefined>(() => this.authService.currentUser()?.role);
  isEmployee = computed(() => this.userRole() === 'EMPLOYEE' || this.userRole() === 'ADMIN');

  conversations = this.conversationService.conversations;
  currentConversation = this.conversationService.currentConversation;
  messages = this.conversationService.messages;
  loading = this.conversationService.loading;
  totalUnreadCount = this.conversationService.totalUnreadCount;
  wsConnected = this.wsService.connected;

  ngOnInit(): void {
    // Connexion WebSocket/STOMP au démarrage
    this.wsService.connect();
    this.loadConversations();
  }

  ngOnDestroy(): void {
    // Déconnexion propre au démontage
    this.wsService.disconnect();
  }

  ngAfterViewChecked(): void {
    if (this.shouldScrollToBottom) {
      this.scrollToBottom();
      this.shouldScrollToBottom = false;
    }
  }

  loadConversations(): void {
    this.conversationService.loadMyConversations().subscribe();
  }

  loadUnassigned(): void {
    this.conversationService.loadUnassignedConversations().subscribe();
  }

  selectConversation(conversation: Conversation): void {
    // Désabonner de l'ancienne conversation si nécessaire
    const previousId = this.selectedConversationId();
    if (previousId !== null) {
      this.wsService.unsubscribeFromConversation(previousId);
    }

    this.selectedConversationId.set(conversation.id);
    this.conversationService.getConversationById(conversation.id).subscribe();
    this.conversationService.loadMessages(conversation.id).subscribe(() => {
      this.shouldScrollToBottom = true;
      if (conversation.unreadCount > 0) {
        this.conversationService.markAsRead(conversation.id).subscribe();
      }
    });

    // S'abonner au topic STOMP de la conversation pour le temps réel
    this.wsService.subscribeToConversation(conversation.id, (message: Message) => {
      // Ne pas ajouter le message si c'est le nôtre (déjà affiché optimistiquement)
      if (message.senderId !== this.currentUserId()) {
        this.conversationService.addIncomingMessage(message);
        this.shouldScrollToBottom = true;
      }
    });
  }

  createConversation(): void {
    const subject = this.newSubject().trim();
    if (!subject) return;

    this.conversationService.createConversation({ subject }).subscribe({
      next: (response) => {
        this.showNewConversationForm.set(false);
        this.newSubject.set('');
        this.selectConversation(response.conversation);
      }
    });
  }

  /**
   * Envoi d'un message via WebSocket/STOMP (temps réel)
   * Fallback HTTP si WebSocket non connecté
   */
  sendMessage(): void {
    const content = this.newMessage().trim();
    const conversationId = this.selectedConversationId();
    if (!content || !conversationId) return;

    if (this.wsService.connected()) {
      // Envoi via STOMP (temps réel - CC-17 : < 2s)
      this.wsService.sendMessage(conversationId, content);

      // Affichage optimiste du message
      const optimisticMessage: Message = {
        id: Date.now(),
        conversationId: conversationId,
        senderId: this.currentUserId()!,
        senderName: 'Moi',
        content: content,
        sentAt: new Date().toISOString(),
        isRead: false
      };
      this.conversationService.addIncomingMessage(optimisticMessage);
      this.newMessage.set('');
      this.shouldScrollToBottom = true;
    } else {
      // Fallback HTTP si WebSocket non disponible
      this.conversationService.sendMessage({ conversationId, content }).subscribe({
        next: () => {
          this.newMessage.set('');
          this.shouldScrollToBottom = true;
        }
      });
    }
  }

  assignToMe(conversationId: number): void {
    this.conversationService.assignConversation(conversationId).subscribe({
      next: () => {
        this.loadConversations();
      }
    });
  }

  closeConversation(conversationId: number): void {
    this.conversationService.closeConversation(conversationId).subscribe();
  }

  onKeyPress(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  isMyMessage(message: Message): boolean {
    return message.senderId === this.currentUserId();
  }

  private scrollToBottom(): void {
    if (this.messagesContainer) {
      const element = this.messagesContainer.nativeElement;
      element.scrollTop = element.scrollHeight;
    }
  }

  toggleNewConversationForm(): void {
    this.showNewConversationForm.update(v => !v);
  }

  backToList(): void {
    // Désabonner du topic actuel
    const currentId = this.selectedConversationId();
    if (currentId !== null) {
      this.wsService.unsubscribeFromConversation(currentId);
    }
    this.conversationService.clearCurrentConversation();
    this.selectedConversationId.set(null);
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'OPEN': return 'Ouvert';
      case 'CLOSED': return 'Fermé';
      case 'PENDING': return 'En attente';
      default: return status;
    }
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'OPEN': return 'status-open';
      case 'CLOSED': return 'status-closed';
      case 'PENDING': return 'status-pending';
      default: return '';
    }
  }
}
