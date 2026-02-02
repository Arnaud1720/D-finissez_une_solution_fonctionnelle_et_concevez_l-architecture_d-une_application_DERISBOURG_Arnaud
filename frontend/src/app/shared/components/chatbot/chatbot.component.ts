import { Component, inject, signal, ElementRef, ViewChild, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChatService } from '../../../core/services/chat.service';

interface Message {
  content: string;
  isUser: boolean;
  timestamp: Date;
}

@Component({
  selector: 'app-chatbot',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chatbot.component.html',
  styleUrls: ['./chatbot.component.scss']
})
export class ChatbotComponent implements AfterViewChecked {
  @ViewChild('messagesContainer') private messagesContainer!: ElementRef;
  
  private readonly chatService = inject(ChatService);
  
  isOpen = signal(false);
  isLoading = signal(false);
  userMessage = signal('');
  messages = signal<Message[]>([
    {
      content: 'Bonjour ! Je suis l\'assistant Your Car Your Way. Comment puis-je vous aider aujourd\'hui ? 🚗',
      isUser: false,
      timestamp: new Date()
    }
  ]);

  private shouldScroll = false;

  ngAfterViewChecked() {
    if (this.shouldScroll) {
      this.scrollToBottom();
      this.shouldScroll = false;
    }
  }

  toggleChat() {
    this.isOpen.update(value => !value);
  }

  sendMessage() {
    const message = this.userMessage().trim();
    if (!message || this.isLoading()) return;

    // Ajouter le message de l'utilisateur
    this.messages.update(msgs => [
      ...msgs,
      {
        content: message,
        isUser: true,
        timestamp: new Date()
      }
    ]);

    this.userMessage.set('');
    this.isLoading.set(true);
    this.shouldScroll = true;

    // Envoyer au backend
    this.chatService.sendMessage(message).subscribe({
      next: (response) => {
        this.messages.update(msgs => [
          ...msgs,
          {
            content: response.reply,
            isUser: false,
            timestamp: new Date()
          }
        ]);
        this.isLoading.set(false);
        this.shouldScroll = true;
      },
      error: (error) => {
        console.error('Erreur chat:', error);
        this.messages.update(msgs => [
          ...msgs,
          {
            content: 'Désolé, une erreur est survenue. Veuillez réessayer.',
            isUser: false,
            timestamp: new Date()
          }
        ]);
        this.isLoading.set(false);
        this.shouldScroll = true;
      }
    });
  }

  onKeyPress(event: KeyboardEvent) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  private scrollToBottom() {
    if (this.messagesContainer) {
      const container = this.messagesContainer.nativeElement;
      container.scrollTop = container.scrollHeight;
    }
  }
}
