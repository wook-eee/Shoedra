package com.shop.controller;

import com.shop.constant.Role;
import com.shop.entity.ChatRoom;
import com.shop.entity.Member;
import com.shop.repository.MemberRepository;
import com.shop.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Objects;

@Controller
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;
    private final MemberRepository memberRepository;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    // 챗봇 상담 채팅방 생성
    @PostMapping("/chat/bot")
    @ResponseBody
    public String createBotChat() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            Member member = memberRepository.findByEmail(email).orElse(null);

            ChatRoom chatRoom;


            chatRoom = chatService.createBotChat(Objects.requireNonNull(member));


            return String.valueOf(chatRoom.getId());

        } catch (Exception e) {
            return "error";
        }
    }

    // 채팅방 입장
    @GetMapping("/chat/room/{roomId}")
    public String enterChatRoom(@PathVariable Long roomId, Model model) {
        try {
            ChatRoom chatRoom = chatService.getChatRoom(roomId);
            System.out.println("채팅룸번호:"+chatRoom.getId());
            model.addAttribute("chatRoom", chatRoom);

            // 현재 로그인한 사용자의 이름을 model에 추가
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            //Member member = chatService.getMemberByEmail(email);
            Member member = memberRepository.findByEmail(email).orElse(null);
            String userName = (member != null) ? member.getName() : email;
            model.addAttribute("currentUserName", userName);

            boolean isAdmin = (member != null && member.getRole() == Role.ADMIN);

            model.addAttribute("messages", chatService.getChatMessages(roomId, isAdmin));

            // 1:1 상담방이고, 관리자 상담중이면 안내문구 추가
//            if ("1:1".equals(chatRoom.getChatType()) && "IN_PROGRESS".equals(chatRoom.getStatus()) && chatRoom.getAdmin() != null) {
//                model.addAttribute("adminInProgressMsg", "현재 관리자 상담중이니 기존 상담 끝난후 상담을 해드리니까 조금만 기다려 주시면 감사하겠습니다.");
//            }

            return "chat/chatRoom";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "채팅방을 찾을 수 없습니다.");
            return "error/404";
        }
    }

    // 1:1 상담 채팅방 생성
    @PostMapping("/chat/one-on-one")
    @ResponseBody
    public String createOneOnOneChat() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            //Member member = chatService.getMemberByEmail(email);
            Member member = memberRepository.findByEmail(email).orElse(null);

            // 관리자는 1:1 상담방을 생성할 수 없음
            if (member != null && member.getRole() == Role.ADMIN) {
                return "error:관리자는 1:1 상담방을 생성할 수 없습니다.";
            }

            ChatRoom chatRoom = chatService.createOneOnOneChat(member);

            // 관리자에게 실시간 알림 전송
            try {
                messagingTemplate.convertAndSend("/topic/admin/notification",
                        "새로운 1:1 상담 요청이 있습니다. 채팅방 ID: " + chatRoom.getId());
            } catch (Exception e) {
                // 알림 전송 실패 시 로그만 출력하고 계속 진행
                System.err.println("관리자 알림 전송 실패: " + e.getMessage());
            }

            return String.valueOf(chatRoom.getId());
        } catch (Exception e) {
            return "error";
        }
    }

    // 관리자 상담 중인 채팅방 목록
    @GetMapping("/admin/chat/in-progress")
    public String getInProgressChats(Model model) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();

            //Member admin = chatService.getMemberByEmail(email);
            Member admin = memberRepository.findByEmail(email).orElse(null);

            if (admin != null && admin.getRole() == Role.ADMIN) {
                List<ChatRoom> activeChats = chatService.getActiveChatsForAdmin();
                model.addAttribute("inProgressChats", activeChats);
                model.addAttribute("admin", admin);
                return "admin/inProgressChats";
            } else {
                return "redirect:/";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/";
        }
    }

    // 관리자가 상담 시작
    @PostMapping("/admin/chat/{roomId}/start")
    @ResponseBody
    public String startChat(@PathVariable Long roomId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();

            Member admin = memberRepository.findByEmail(email).orElse(null);
            //Member admin = chatService.getMemberByEmail(email);

            if (admin != null && admin.getRole() == Role.ADMIN) {
                chatService.startChat(roomId, admin);
                return "success";
            } else {
                return "error";
            }
        } catch (Exception e) {
            return "error";
        }
    }
}
