package com.shop.controller;

import com.shop.constant.Role;
import com.shop.dto.ChatMessageDto;
import com.shop.entity.ChatRoom;
import com.shop.entity.Member;
import com.shop.repository.MemberRepository;
import com.shop.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    private final MemberRepository memberRepository;

    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessageDto sendMessage(@Payload ChatMessageDto chatMessage) {
        chatMessage.setTimestamp(LocalDateTime.now());
        return chatMessage;
    }

    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessageDto addUser(@Payload ChatMessageDto chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        // 사용자 정보 설정
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            //Member member = chatService.getMemberByEmail(email);
            Member member = memberRepository.findByEmail(email).orElse(null);
            if (member != null) {
                chatMessage.setSender(member.getName());
                chatMessage.setSenderType(member.getRole() == Role.ADMIN ? "ADMIN" : "USER");
            }
        }
        
        // WebSocket 세션에 사용자 이름 추가
        headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
        headerAccessor.getSessionAttributes().put("roomId", chatMessage.getRoomId());
        
        chatMessage.setTimestamp(LocalDateTime.now());
        return chatMessage;
    }

    @MessageMapping("/chat.room.join")
    public void joinRoom(@Payload ChatMessageDto chatMessage) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            //Member member = chatService.getMemberByEmail(email);
            Member member = memberRepository.findByEmail(email).orElse(null);
            if (member != null) {
                chatMessage.setSender(member.getName());
                chatMessage.setSenderType(member.getRole() == Role.ADMIN ? "ADMIN" : "USER");
                chatMessage.setMessage(member.getName() + "님이 채팅방에 입장했습니다.");
                
                // 입장 메시지를 데이터베이스에 저장
                try {
                    Long roomId = Long.parseLong(chatMessage.getRoomId());
                    chatService.saveMessage(roomId, member, chatMessage.getMessage(), "JOIN");
                } catch (Exception e) {
                    System.err.println("입장 메시지 저장 중 오류: " + e.getMessage());
                }
            }
        }
        chatMessage.setType("JOIN");
        chatMessage.setTimestamp(LocalDateTime.now());
        messagingTemplate.convertAndSend("/topic/room/" + chatMessage.getRoomId(), chatMessage);
    }

    @MessageMapping("/chat.room.message")
    public void sendRoomMessage(@Payload ChatMessageDto chatMessage) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            //Member member = chatService.getMemberByEmail(email);
            Member member = memberRepository.findByEmail(email).orElse(null);

            System.out.println("메시지 시작");
            if (member != null) {
                chatMessage.setSender(member.getName());
                chatMessage.setSenderType(member.getRole() == Role.ADMIN ? "ADMIN" : "USER");

                System.out.println("1:"+member.getName());
                
                // 메시지를 데이터베이스에 저장
                try {
                    Long roomId = Long.parseLong(chatMessage.getRoomId());

                    System.out.println("방 번호:"+roomId);
                    
                    // 메시지 내용 검증
                    if (chatMessage.getMessage() == null || chatMessage.getMessage().trim().isEmpty()) {
                        System.err.println("[CHAT_WARNING] 빈 메시지 감지 - RoomID: " + roomId + ", Sender: " + member.getName());
                        return;
                    }

                    System.out.println("메시지 저장 시작:"+chatMessage.getMessage());
                    
                    // 메시지 저장
                    chatService.saveMessage(roomId, member, chatMessage.getMessage(), "MESSAGE");
                    System.out.println("[CHAT_SUCCESS] 메시지 저장 완료 - RoomID: " + roomId + 
                                     ", Sender: " + member.getName() + 
                                     ", Message: " + chatMessage.getMessage().substring(0, Math.min(chatMessage.getMessage().length(), 30)) + "...");
                    
                    // 유저가 메시지를 보낼 때, 채팅방이 WAITING이면 IN_PROGRESS로 변경
                    ChatRoom chatRoom = chatService.getChatRoom(roomId);
                    if (chatRoom != null && "WAITING".equals(chatRoom.getStatus()) && member.getRole() != Role.ADMIN) {
                        chatRoom.setStatus("IN_PROGRESS");
                        chatService.saveChatRoom(chatRoom);
                        System.out.println("[DEBUG] 채팅방 상태 변경: roomId=" + chatRoom.getId() + " → IN_PROGRESS");
                    }
                    // 1:1 상담에서 관리자가 없으면 자동 배정
                    if (chatRoom != null && chatRoom.getAdmin() == null && "1:1".equals(chatRoom.getChatType())) {
                        // 대기 중인 관리자들에게 알림 전송
                        messagingTemplate.convertAndSend("/topic/admin/notification", 
                            "새로운 1:1 상담 요청이 있습니다. 채팅방 ID: " + roomId);
                    }
                } catch (Exception e) {
                    System.err.println("[CHAT_ERROR] 메시지 저장 실패 - RoomID: " + chatMessage.getRoomId() + 
                                     ", Sender: " + member.getName() + 
                                     ", Error: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        chatMessage.setType("MESSAGE");
        chatMessage.setTimestamp(LocalDateTime.now());
        messagingTemplate.convertAndSend("/topic/room/" + chatMessage.getRoomId(), chatMessage);
    }

    // 채팅방 퇴장
    @MessageMapping("/chat.room.leave")
    public void leaveRoom(@Payload ChatMessageDto chatMessage) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            //Member member = chatService.getMemberByEmail(email);
            Member member = memberRepository.findByEmail(email).orElse(null);
            if (member != null) {
                chatMessage.setSender(member.getName());
                chatMessage.setSenderType(member.getRole() == Role.ADMIN ? "ADMIN" : "USER");
                chatMessage.setMessage(member.getName() + "님이 채팅방을 나갔습니다.");
                
                // 퇴장 메시지를 데이터베이스에 저장
                try {
                    Long roomId = Long.parseLong(chatMessage.getRoomId());
                    chatService.saveMessage(roomId, member, chatMessage.getMessage(), "LEAVE");
                } catch (Exception e) {
                    System.err.println("퇴장 메시지 저장 중 오류: " + e.getMessage());
                }
            }
        }
        chatMessage.setType("LEAVE");
        chatMessage.setTimestamp(LocalDateTime.now());
        messagingTemplate.convertAndSend("/topic/room/" + chatMessage.getRoomId(), chatMessage);
    }

    // 관리자 알림 구독
    @MessageMapping("/admin.subscribe")
    public void subscribeToAdminNotifications() {
        // 관리자 알림 구독 로직
    }
    
    // 에러 핸들링을 위한 메서드
    @MessageExceptionHandler
    public void handleException(Throwable exception) {
        System.err.println("WebSocket Error: " + exception.getMessage());
        exception.printStackTrace();
    }
} 