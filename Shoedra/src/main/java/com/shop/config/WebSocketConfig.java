package com.shop.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@EnableWebSocketMessageBroker
// 이 클래스가 Spring의 설정(Configuration) 클래스임을 나타냅니다.
// 스프링 컨테이너가 이 클래스에서 @Bean 메서드를 찾아 빈으로 등록하도록 합니다.
// STOMP(Simple Text Oriented Messaging Protocol) 기반의 WebSocket 메시지 브로커를 활성화합니다.
// 이 어노테이션을 통해 WebSocket 메시지 처리를 위한 Spring의 설정이 시작됩니다.
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // STOMP WebSocket 엔드포인트를 등록하는 메서드입니다.
    // 클라이언트가 WebSocket 연결을 시작할 수 있는 URL을 정의합니다.
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // "/ws"라는 WebSocket 엔드포인트를 추가합니다.
        // 클라이언트는 이 경로(예: ws://localhost:8080/ws)로 WebSocket 연결을 시도할 수 있습니다.
        registry.addEndpoint("/ws")
                // 모든 Origin(도메인)에서의 연결을 허용합니다.
                // 실제 운영 환경에서는 보안을 위해 특정 도메인만 허용하도록 설정하는 것이 좋습니다.
                .setAllowedOriginPatterns("*")
                // SockJS 지원을 추가합니다.
                // SockJS는 웹소켓을 지원하지 않는 브라우저를 위해 HTTP 폴링/스트리밍과 같은 대체 전송 방법을 제공합니다.
                .withSockJS()
                // SockJS 연결의 하트비트 시간(밀리초)을 설정합니다.
                // 클라이언트와 서버 간에 연결이 활성 상태임을 확인하기 위해 주기적으로 메시지를 보냅니다.
                .setHeartbeatTime(25000) // 25초
                // SockJS 연결이 끊어졌다고 간주하기 전까지의 지연 시간(밀리초)을 설정합니다.
                .setDisconnectDelay(5000); // 5초
    }

    // 메시지 브로커를 구성하는 메서드입니다.
    // 메시지가 어떻게 라우팅되고 처리될지 정의합니다.
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 클라이언트에서 서버로 메시지를 보낼 때 사용할 목적지(destination) 접두사를 설정합니다.
        // 예를 들어, 클라이언트가 "/app/chat"으로 메시지를 보내면, 이 메시지는 @MessageMapping("/chat")이 붙은 컨트롤러 메서드로 라우팅됩니다.
        registry.setApplicationDestinationPrefixes("/app");
        // Simple In-Memory 메시지 브로커를 활성화합니다.
        // "/topic"과 "/queue"로 시작하는 목적지로 전송된 메시지를 처리합니다.
        // "/topic"은 주로 1대다(pub-sub) 메시징에, "/queue"는 1대1 메시징에 사용됩니다.
        registry.enableSimpleBroker("/topic", "/queue");
        // 메시지 발행 순서를 보존할지 여부를 설정합니다.
        // true로 설정하면 메시지가 발행된 순서대로 구독자에게 전달되도록 시도합니다.
        registry.setPreservePublishOrder(true);
    }
    
    // WebSocket 전송 관련 설정을 구성하는 메서드입니다.
    // WebSocket 메시지의 크기 제한, 버퍼 크기, 전송 시간 제한 등을 설정할 수 있습니다.
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        // 수신 메시지의 최대 크기(바이트)를 설정합니다.
        // 이 크기를 초과하는 메시지는 거부됩니다.
        registration.setMessageSizeLimit(64 * 1024) // 64KB
                    // 전송 버퍼의 최대 크기(바이트)를 설정합니다.
                    // 서버에서 클라이언트로 보낼 메시지를 위한 버퍼 크기입니다.
                   .setSendBufferSizeLimit(512 * 1024) // 512KB
                    // 메시지 전송 작업의 시간 제한(밀리초)을 설정합니다.
                    // 이 시간 내에 메시지 전송이 완료되지 않으면 타임아웃이 발생합니다.
                   .setSendTimeLimit(20000); // 20초
    }
}
