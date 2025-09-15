package com.shop.service;

import com.shop.constant.Role;
import com.shop.entity.ChatMessage;
import com.shop.entity.ChatRoom;
import com.shop.entity.Member;
import com.shop.repository.ChatMessageRepository;
import com.shop.repository.ChatRoomRepository;
import com.shop.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatService {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MemberRepository memberRepository;

    // 챗봇 상담방 생성
    public ChatRoom createBotChat(Member member) {
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setRoomName("챗봇 상담 - " + member.getName());
        chatRoom.setMember(member);
        chatRoom.setChatType("CHATBOT");
        chatRoom.setStatus("IN_PROGRESS");

        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);

        // 챗봇 환영 메시지
//        saveBotMessage(savedChatRoom.getId(),
//                "안녕하세요! 챗봇 상담사입니다. 무엇을 도와드릴까요?",
//                "MESSAGE");

        return savedChatRoom;
    }

    // 채팅방 정보 조회
    @Transactional(readOnly = true)
    public ChatRoom getChatRoom(Long roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));
    }

    // 1:1 상담방 생성
    public ChatRoom createOneOnOneChat(Member member) {
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setRoomName("1:1 상담 - " + member.getName());
        chatRoom.setMember(member);
        chatRoom.setChatType("1:1");
        chatRoom.setStatus("WAITING");

        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);

        // 시스템 메시지 추가
//        saveBotMessage(savedChatRoom.getId(),
//                "현재 관리자 상담중이니 기존 상담 끝난후 상담을 해드리니까 조금만 기다려 주시면 감사하겠습니다.",
//                "MESSAGE");

        return savedChatRoom;
    }

    // 채팅방 정보 저장 (상태 변경 등)
    public ChatRoom saveChatRoom(ChatRoom chatRoom) {
        return chatRoomRepository.save(chatRoom);
    }

    // 채팅방 메시지 조회 (관리자 여부에 따라 챗봇 안내 메시지 제외)
    @Transactional(readOnly = true)
    public List<ChatMessage> getChatMessages(Long roomId, boolean isAdmin) {
        List<ChatMessage> allMessages = chatMessageRepository.findByChatRoomIdOrderBySentTimeAsc(roomId);
        if (isAdmin) {
            // 관리자는 senderType이 'BOT'인 모든 메시지 제외
            return allMessages.stream()
                    .filter(msg -> !"BOT".equals(msg.getSenderType()))
                    .toList();
        }
        return allMessages;
    }

    // 메시지 저장 (유저가 처음 메시지를 보낼 때 roomName을 유저 이름으로 갱신)
    public ChatMessage saveMessage(Long roomId, Member sender, String message, String messageType) {
        try {
            ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                    .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

            // 만약 roomName이 기본값(1:1 상담 - ...)이고, 유저가 처음 메시지를 보낸 경우 roomName을 유저 이름으로 변경
            if (chatRoom.getRoomName() != null && chatRoom.getRoomName().startsWith("1:1 상담 - ") && sender.getRole() != Role.ADMIN) {
                chatRoom.setRoomName(sender.getName() + "님의 상담방");
                chatRoomRepository.save(chatRoom);
            }

            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setChatRoom(chatRoom);
            chatMessage.setSender(sender);
            chatMessage.setMessage(message);
            chatMessage.setMessageType(messageType);
            chatMessage.setSenderType(sender.getRole() == Role.ADMIN ? "ADMIN" : "USER");

            ChatMessage savedMessage = chatMessageRepository.save(chatMessage);

            return savedMessage;
        } catch (Exception e) {
            System.err.println("[CHAT_ERROR] 메시지 저장 실패 - RoomID: " + roomId +
                    ", Sender: " + (sender != null ? sender.getName() : "Unknown") +
                    ", Error: " + e.getMessage());
            throw e;
        }
    }

    // 관리자가 담당하는 진행 중인 상담 목록
    @Transactional(readOnly = true)
    public List<ChatRoom> getActiveChatsForAdmin() {
        List<ChatRoom> result = chatRoomRepository.findByChatTypeAndStatusIn("1:1", List.of("WAITING", "IN_PROGRESS"));
        // 관리자(ADMIN)가 생성한 채팅방은 제외 (member.role == USER만)
        List<ChatRoom> userRooms = new java.util.ArrayList<>();
        for (ChatRoom room : result) {
            if (room.getMember() != null && room.getMember().getRole() != null && room.getMember().getRole().name().equals("USER")) {
                userRooms.add(room);
            }
        }
        return userRooms;
    }

    // 관리자 상담 시작
    public ChatRoom startChat(Long roomId, Member admin) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        chatRoom.setAdmin(admin);
        chatRoom.setStatus("IN_PROGRESS");
        chatRoomRepository.save(chatRoom);

        // 관리자 입장 메시지
        saveMessage(chatRoom.getId(), admin,
                admin.getName() + " 관리자가 상담을 시작했습니다.",
                "MESSAGE");

        return chatRoom;
    }



}
