import { Injectable, inject, signal } from '@angular/core';
import { AuthService } from './auth.service';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { environment } from '../../../environments/environment';
import { Message } from '../models';

/**
 * Service WebSocket/STOMP pour le chat en temps réel.
 * Conforme ADD V2 §6.5 : Client STOMP.js → broker → topic subscription → broadcast
 *
 * - Connexion via SockJS fallback sur /ws
 * - Authentification JWT dans le header STOMP (CC-20)
 * - Subscription par conversation : /topic/conversation/{id}
 * - Envoi via : /app/chat/{conversationId}
 */
@Injectable({
  providedIn: 'root'
})
export class WebSocketService {
  private authService = inject(AuthService);
  private client: Client | null = null;
  private subscriptions = new Map<number, StompSubscription>();

  connected = signal<boolean>(false);

  /**
   * Connexion au broker STOMP avec authentification JWT
   */
  connect(): void {
    const token = this.authService.getToken();
    if (!token || this.client?.connected) return;

    this.client = new Client({
      // Utilisation de SockJS comme transport (fallback HTTP)
      webSocketFactory: () => new SockJS(`${environment.wsUrl}`),

      // Header STOMP avec le token JWT (CC-20)
      connectHeaders: {
        Authorization: `Bearer ${token}`
      },

      // Reconnexion automatique toutes les 5 secondes
      reconnectDelay: 5000,

      // Callbacks
      onConnect: () => {
        console.log('✅ WebSocket STOMP connecté');
        this.connected.set(true);
      },

      onDisconnect: () => {
        console.log('❌ WebSocket STOMP déconnecté');
        this.connected.set(false);
      },

      onStompError: (frame) => {
        console.error('⚠️ Erreur STOMP:', frame.headers['message']);
        this.connected.set(false);
      }
    });

    this.client.activate();
  }

  /**
   * Déconnexion propre du broker STOMP
   */
  disconnect(): void {
    if (this.client?.connected) {
      // Désabonner de tous les topics
      this.subscriptions.forEach(sub => sub.unsubscribe());
      this.subscriptions.clear();
      this.client.deactivate();
      this.connected.set(false);
    }
  }

  /**
   * S'abonner au topic d'une conversation pour recevoir les messages en temps réel.
   * Topic : /topic/conversation/{conversationId}
   *
   * @param conversationId ID de la conversation
   * @param callback Fonction appelée à chaque nouveau message reçu
   */
  subscribeToConversation(conversationId: number, callback: (message: Message) => void): void {
    if (!this.client?.connected) {
      console.warn('⚠️ WebSocket non connecté, impossible de s\'abonner');
      return;
    }

    // Éviter les doublons de subscription
    if (this.subscriptions.has(conversationId)) {
      this.subscriptions.get(conversationId)!.unsubscribe();
    }

    const subscription = this.client.subscribe(
      `/topic/conversation/${conversationId}`,
      (stompMessage: IMessage) => {
        const message: Message = JSON.parse(stompMessage.body);
        callback(message);
      }
    );

    this.subscriptions.set(conversationId, subscription);
  }

  /**
   * Se désabonner du topic d'une conversation
   */
  unsubscribeFromConversation(conversationId: number): void {
    const sub = this.subscriptions.get(conversationId);
    if (sub) {
      sub.unsubscribe();
      this.subscriptions.delete(conversationId);
    }
  }

  /**
   * Envoyer un message via STOMP vers le serveur.
   * Destination : /app/chat/{conversationId}
   *
   * @param conversationId ID de la conversation
   * @param content Contenu du message
   */
  sendMessage(conversationId: number, content: string): void {
    if (!this.client?.connected) {
      console.error('⚠️ WebSocket non connecté, impossible d\'envoyer');
      return;
    }

    this.client.publish({
      destination: `/app/chat/${conversationId}`,
      body: JSON.stringify({
        conversationId: conversationId,
        content: content
      })
    });
  }
}
