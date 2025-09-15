package com.shop.repository;

import com.shop.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByChatRoomIdOrderBySentTimeAsc(Long chatRoomId);
    
    @Query("SELECT COUNT(c) FROM ChatMessage c WHERE c.chatRoom.id = :roomId AND c.senderType = 'USER' AND c.messageType = 'MESSAGE'")
    Long countUserMessagesByRoomId(@Param("roomId") Long roomId);
    
    @Query("SELECT c FROM ChatMessage c WHERE c.chatRoom.id = :roomId ORDER BY c.sentTime DESC LIMIT 1")
    ChatMessage findLastMessageByRoomId(@Param("roomId") Long roomId);
} 