package com.arn.ycyw.your_car_your_way.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuration WebSocket + STOMP pour le chat en temps réel.
 *
 * Architecture (conforme ADD V2 §6.5) :
 * Client STOMP.js → /ws (SockJS) → Broker /topic, /queue → Subscribers
 *
 * - /ws         : endpoint de connexion WebSocket (SockJS fallback)
 * - /app        : préfixe pour les messages envoyés par le client
 * - /topic      : destinations broadcast (conversations publiques)
 * - /queue      : destinations point-à-point (notifications privées)
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Active un broker simple en mémoire pour les topics et queues
        config.enableSimpleBroker("/topic", "/queue");
        // Préfixe pour les messages envoyés par le client vers le serveur
        config.setApplicationDestinationPrefixes("/app");
        // Préfixe pour les messages destinés à un utilisateur spécifique
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint WebSocket avec SockJS fallback
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("http://localhost:4200", "http://localhost:*")
                .withSockJS();
    }
}
