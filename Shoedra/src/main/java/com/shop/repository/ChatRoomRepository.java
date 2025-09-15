package com.shop.repository;

import com.shop.entity.ChatRoom;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    List<ChatRoom> findByMemberId(Long memberId);
    
    List<ChatRoom> findByChatType(String chatType);
    
    List<ChatRoom> findByAdminId(Long adminId);
    
    List<ChatRoom> findByStatus(String status);
    
    List<ChatRoom> findByChatTypeAndStatus(String chatType, String status);
    
    List<ChatRoom> findByAdminIdIsNullAndChatTypeAndStatus(String chatType, String status);
    
    List<ChatRoom> findByAdminIdAndChatTypeAndStatus(Long adminId, String chatType, String status);

    List<ChatRoom> findByAdminIdAndChatTypeAndStatusIn(Long adminId, String chatType, List<String> statusList);

    @EntityGraph(attributePaths = {"member"})
    List<ChatRoom> findByChatTypeAndStatusIn(String chatType, List<String> statusList);
    
    List<ChatRoom> findByMemberIdOrderByRegTimeDesc(Long memberId);
    
    List<ChatRoom> findByMemberIdAndStatus(Long memberId, String status);
    
    List<ChatRoom> findAllByOrderByRegTimeDesc();
    
    @Query("SELECT cr FROM ChatRoom cr LEFT JOIN FETCH cr.member LEFT JOIN FETCH cr.admin ORDER BY cr.regTime DESC")
    List<ChatRoom> findAllWithMemberAndAdmin();
    
    @Query("SELECT cr FROM ChatRoom cr LEFT JOIN FETCH cr.member LEFT JOIN FETCH cr.admin WHERE cr.hidden = false ORDER BY cr.regTime DESC")
    List<ChatRoom> findAllWithMemberAndAdminByHiddenFalse();
} 